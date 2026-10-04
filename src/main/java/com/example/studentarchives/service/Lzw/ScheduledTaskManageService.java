package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.schedule.ScheduledTask;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ScheduledTaskRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.example.studentarchives.service.schedule.DynamicTaskScheduler;
import com.example.studentarchives.service.schedule.ScheduledTaskHandlerRegistry;
import com.example.studentarchives.service.schedule.TaskExecutionDispatcher;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端定时任务管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》十四、定时任务管理模块（14.1 ~ 14.7）。
 * 数据来源：scheduled_tasks。
 * <p>
 * 权限：关键权限码表未列出定时任务相关权限码，故要求 admin 角色（越权返回 20005）。
 * 数据隔离：按操作人所属学校隔离（school_id 不再由前端传入）。
 * <p>
 * 字段语义：{@code status} 为启停开关（0=停用 1=启用）；{@code last_run_status} 为上次执行结果
 * （0=失败 1=成功，对齐 V11 建表注释与 14.1 响应示例）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskManageService {

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final AdminAuthService adminAuthService;
    private final TaskExecutionDispatcher taskExecutionDispatcher;
    private final DynamicTaskScheduler dynamicTaskScheduler;
    private final ScheduledTaskHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper;

    // ==================== 14.1 获取定时任务列表 ====================

    @Transactional(readOnly = true)
    public PageResult<ScheduledTaskItem> listTasks(Long operatorId, String taskGroup, Integer status, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        Specification<ScheduledTask> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schoolId"), schoolId));
            if (taskGroup != null && !taskGroup.isBlank()) {
                predicates.add(cb.equal(root.get("taskGroup"), taskGroup.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "taskGroup").and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<ScheduledTask> page = scheduledTaskRepository.findAll(spec, pageable);

        List<ScheduledTaskItem> items = page.getContent().stream().map(t -> ScheduledTaskItem.builder()
                .taskId(t.getId())
                .taskName(t.getTaskName())
                .taskCode(t.getTaskCode())
                .taskGroup(t.getTaskGroup())
                .cronExpression(t.getCronExpression())
                .status(t.getStatus())
                .lastRunAt(toIso(t.getLastRunAt()))
                .lastRunStatus(t.getLastRunStatus())
                .lastRunStatusLabel(lastRunStatusLabel(t.getLastRunStatus()))
                .build()).toList();

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 14.2 启停定时任务 ====================

    @Transactional
    public ScheduledTaskStatusResponse updateStatus(Long operatorId, Long taskId, ScheduledTaskStatusUpdateRequest body) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        Integer status = body.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(停用) 或 1(启用)");
        }

        ScheduledTask task = scheduledTaskRepository.findById(taskId)
                .filter(t -> schoolId.equals(t.getSchoolId()))
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "定时任务不存在"));

        task.setStatus(status);
        scheduledTaskRepository.save(task);
        reloadSchedulesAfterCommit();

        return ScheduledTaskStatusResponse.builder()
                .taskId(taskId)
                .status(status)
                .statusLabel(status == 1 ? "已启用" : "已停用")
                .build();
    }

    // ==================== 14.3 创建自定义定时任务 ====================

    @Transactional
    public ScheduledTaskDetailResponse createTask(Long operatorId, ScheduledTaskCreateRequest body) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        String taskName = requireText(body.getTaskName(), 2, 100, "taskName");
        String taskCode = requireText(body.getTaskCode(), 2, 50, "taskCode");
        String taskGroup = requireText(body.getTaskGroup(), 1, 50, "taskGroup");
        String taskHandler = requireText(body.getTaskHandler(), 1, 255, "taskHandler");
        String cronExpression = requireText(body.getCronExpression(), 5, 100, "cronExpression");

        Integer runType = body.getRunType() == null ? 1 : body.getRunType();
        if (runType != 1 && runType != 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "runType 只能为 1(定时自动) 或 2(手动触发)");
        }
        Integer status = body.getStatus() == null ? 1 : body.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(停用) 或 1(启用)");
        }
        if (body.getDescription() != null && body.getDescription().length() > 255) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "description 长度不能超过 255");
        }

        if (scheduledTaskRepository.existsBySchoolIdAndTaskCode(schoolId, taskCode)) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "任务编码已存在");
        }
        if (!handlerRegistry.isHandlerAvailable(taskHandler)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "taskHandler 不存在，请从 /admin/scheduled-tasks/handlers 接口获取可用处理器");
        }

        ScheduledTask task = new ScheduledTask();
        task.setSchoolId(schoolId);
        task.setTaskName(taskName);
        task.setTaskCode(taskCode);
        task.setTaskGroup(taskGroup);
        task.setCronExpression(cronExpression);
        task.setTaskHandler(taskHandler);
        task.setTaskParams(serializeTaskParams(body.getTaskParams()));
        task.setDescription(body.getDescription());
        task.setIsSystem(0);
        task.setRunType(runType);
        task.setMaxRetries(body.getMaxRetries() == null ? 0 : body.getMaxRetries());
        task.setRetryDelaySec(body.getRetryDelaySec() == null ? 60 : body.getRetryDelaySec());
        task.setTimeoutSec(body.getTimeoutSec() == null ? 0 : body.getTimeoutSec());
        task.setStatus(status);
        task.setCreatedBy(operatorId);

        scheduledTaskRepository.save(task);
        reloadSchedulesAfterCommit();
        return toDetail(task);
    }

    // ==================== 14.3.1 获取可用处理器列表 ====================

    /**
     * 返回所有已注册的定时任务处理器，供前端创建任务时下拉选择。
     */
    public List<ScheduledTaskHandlerRegistry.HandlerMeta> listHandlers() {
        return handlerRegistry.listHandlers();
    }

    // ==================== 14.4 获取任务详情 ====================

    @Transactional(readOnly = true)
    public ScheduledTaskDetailResponse getTaskDetail(Long operatorId, Long taskId) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);
        return toDetail(findTask(schoolId, taskId));
    }

    // ==================== 14.5 更新任务配置 ====================

    @Transactional
    public ScheduledTaskDetailResponse updateTask(Long operatorId, Long taskId, ScheduledTaskUpdateRequest body) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);
        ScheduledTask task = findTask(schoolId, taskId);
        boolean isSystem = task.getIsSystem() != null && task.getIsSystem() == 1;

        if (body.getTaskName() != null) {
            task.setTaskName(requireText(body.getTaskName(), 2, 100, "taskName"));
        }
        if (body.getTaskGroup() != null) {
            task.setTaskGroup(requireText(body.getTaskGroup(), 1, 50, "taskGroup"));
        }
        if (body.getCronExpression() != null) {
            task.setCronExpression(requireText(body.getCronExpression(), 5, 100, "cronExpression"));
        }
        if (body.getTaskParams() != null) {
            task.setTaskParams(serializeTaskParams(body.getTaskParams()));
        }
        if (body.getDescription() != null) {
            if (body.getDescription().length() > 255) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "description 长度不能超过 255");
            }
            task.setDescription(body.getDescription());
        }
        if (body.getRunType() != null) {
            if (body.getRunType() != 1 && body.getRunType() != 2) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "runType 只能为 1(定时自动) 或 2(手动触发)");
            }
            task.setRunType(body.getRunType());
        }
        if (body.getMaxRetries() != null) {
            task.setMaxRetries(body.getMaxRetries());
        }
        if (body.getRetryDelaySec() != null) {
            task.setRetryDelaySec(body.getRetryDelaySec());
        }
        if (body.getTimeoutSec() != null) {
            task.setTimeoutSec(body.getTimeoutSec());
        }
        // 状态启停由 14.2 启停接口单独维护，更新任务配置不再修改 status
        // 仅自定义任务允许改 taskCode / taskHandler（系统内置任务禁止，见文档 14.5）
        if (!isSystem && body.getTaskCode() != null) {
            String newCode = requireText(body.getTaskCode(), 2, 50, "taskCode");
            if (!newCode.equals(task.getTaskCode())
                    && scheduledTaskRepository.existsBySchoolIdAndTaskCode(schoolId, newCode)) {
                throw new BusinessException(ResultCode.DATA_DUPLICATE, "任务编码已存在");
            }
            task.setTaskCode(newCode);
        }
        if (!isSystem && body.getTaskHandler() != null) {
            task.setTaskHandler(requireText(body.getTaskHandler(), 1, 255, "taskHandler"));
        }

        scheduledTaskRepository.save(task);
        reloadSchedulesAfterCommit();
        return toDetail(task);
    }

    // ==================== 14.6 删除自定义任务 ====================

    @Transactional
    public ScheduledTaskDeleteResponse deleteTask(Long operatorId, Long taskId) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);
        ScheduledTask task = findTask(schoolId, taskId);

        if (task.getIsSystem() != null && task.getIsSystem() == 1) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "系统内置任务禁止删除");
        }

        LocalDateTime now = LocalDateTime.now();
        scheduledTaskRepository.softDeleteById(taskId, now);
        reloadSchedulesAfterCommit();

        return ScheduledTaskDeleteResponse.builder()
                .taskId(taskId)
                .deletedAt(toIso(now))
                .build();
    }

    // ==================== 14.7 手动立即触发执行 ====================

    /**
     * 手动立即触发任务执行。
     * <p>
     * 统一走 {@link TaskExecutionDispatcher} 分发：按任务编码/处理器名解析 {@code ScheduledTaskHandler}，
     * 未接入处理器返回业务错误。本方法<strong>不开启事务</strong>——部分处理器会先落库再提交
     * {@code @Async} 异步执行，若套在事务里会导致异步线程在提交前读不到数据。
     */
    public ScheduledTaskTriggerResponse triggerTask(Long operatorId, Long taskId, Map<String, Object> runParams) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);
        ScheduledTask task = findTask(schoolId, taskId);

        if (task.getStatus() != null && task.getStatus() == 0) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "任务已停用，无法手动触发");
        }

        LocalDateTime now = LocalDateTime.now();
        taskExecutionDispatcher.execute(task, runParams, "manual");

        return ScheduledTaskTriggerResponse.builder()
                .taskId(taskId)
                .taskName(task.getTaskName())
                .runType(2)
                .runTypeLabel("手动触发")
                .triggeredAt(toIso(now))
                .executeId("exec-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                        + "-" + UUID.randomUUID().toString().substring(0, 6))
                .build();
    }

    // ==================== 通用辅助 ====================

    private ScheduledTask findTask(Long schoolId, Long taskId) {
        return scheduledTaskRepository.findById(taskId)
                .filter(t -> schoolId.equals(t.getSchoolId()))
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "定时任务不存在"));
    }

    /**
     * 在事务提交后刷新动态调度器，确保调度器读取到已提交的增删改/启停结果。
     * 无活动事务时直接刷新（如手动触发场景无需调用本方法，此处仅作兜底）。
     */
    private void reloadSchedulesAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dynamicTaskScheduler.reload();
                }
            });
        } else {
            dynamicTaskScheduler.reload();
        }
    }

    private String requireText(String value, int min, int max, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, field + " 不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    field + " 长度须在 " + min + "-" + max + " 之间");
        }
        return trimmed;
    }

    private String serializeTaskParams(Object taskParams) {
        if (taskParams == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(taskParams);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "taskParams 不是合法 JSON");
        }
    }

    private JsonNode deserializeTaskParams(String taskParams) {
        if (taskParams == null || taskParams.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(taskParams);
        } catch (JsonProcessingException e) {
            log.warn("定时任务 taskParams 解析失败，返回 null: {}", taskParams);
            return null;
        }
    }

    private ScheduledTaskDetailResponse toDetail(ScheduledTask t) {
        return ScheduledTaskDetailResponse.builder()
                .taskId(t.getId())
                .schoolId(t.getSchoolId())
                .taskName(t.getTaskName())
                .taskCode(t.getTaskCode())
                .taskGroup(t.getTaskGroup())
                .cronExpression(t.getCronExpression())
                .taskHandler(t.getTaskHandler())
                .taskParams(deserializeTaskParams(t.getTaskParams()))
                .description(t.getDescription())
                .isSystem(t.getIsSystem())
                .runType(t.getRunType())
                .runTypeLabel(runTypeLabel(t.getRunType()))
                .maxRetries(t.getMaxRetries())
                .retryDelaySec(t.getRetryDelaySec())
                .timeoutSec(t.getTimeoutSec())
                .lastRunAt(toIso(t.getLastRunAt()))
                .lastRunStatus(t.getLastRunStatus())
                .lastRunStatusLabel(lastRunStatusLabel(t.getLastRunStatus()))
                .nextRunAt(toIso(t.getNextRunAt()))
                .status(t.getStatus())
                .statusLabel(t.getStatus() != null && t.getStatus() == 1 ? "已启用" : "已停用")
                .createdBy(t.getCreatedBy())
                .createdAt(toIso(t.getCreatedAt()))
                .updatedAt(toIso(t.getUpdatedAt()))
                .build();
    }

    private String runTypeLabel(Integer runType) {
        if (runType == null) {
            return null;
        }
        return runType == 2 ? "手动触发" : "定时自动";
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    private String lastRunStatusLabel(Integer lastRunStatus) {
        if (lastRunStatus == null) {
            return null;
        }
        return lastRunStatus == 1 ? "成功" : "失败";
    }

    // ==================== 内嵌 POJO ====================

    /** 14.1 定时任务列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScheduledTaskItem {
        private Long taskId;
        private String taskName;
        private String taskCode;
        private String taskGroup;
        private String cronExpression;
        private Integer status;
        private String lastRunAt;
        private Integer lastRunStatus;
        private String lastRunStatusLabel;
    }

    /** 14.2 启停定时任务请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledTaskStatusUpdateRequest {
        private Integer status;
    }

    /** 14.2 启停定时任务响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScheduledTaskStatusResponse {
        private Long taskId;
        private Integer status;
        private String statusLabel;
    }

    /** 14.3 创建自定义定时任务请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledTaskCreateRequest {
        private String taskName;
        private String taskCode;
        private String taskGroup;
        private String cronExpression;
        private String taskHandler;
        private Object taskParams;
        private String description;
        private Integer runType;
        private Integer maxRetries;
        private Integer retryDelaySec;
        private Integer timeoutSec;
        private Integer status;
    }

    /** 14.4 定时任务详情响应（14.3 / 14.5 复用） */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScheduledTaskDetailResponse {
        private Long taskId;
        private Long schoolId;
        private String taskName;
        private String taskCode;
        private String taskGroup;
        private String cronExpression;
        private String taskHandler;
        private JsonNode taskParams;
        private String description;
        private Integer isSystem;
        private Integer runType;
        private String runTypeLabel;
        private Integer maxRetries;
        private Integer retryDelaySec;
        private Integer timeoutSec;
        private String lastRunAt;
        private Integer lastRunStatus;
        private String lastRunStatusLabel;
        private String nextRunAt;
        private Integer status;
        private String statusLabel;
        private Long createdBy;
        private String createdAt;
        private String updatedAt;
    }

    /** 14.5 更新任务配置请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledTaskUpdateRequest {
        private String taskName;
        private String taskCode;
        private String taskGroup;
        private String cronExpression;
        private String taskHandler;
        private Object taskParams;
        private String description;
        private Integer runType;
        private Integer maxRetries;
        private Integer retryDelaySec;
        private Integer timeoutSec;
    }

    /** 14.6 删除自定义任务响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScheduledTaskDeleteResponse {
        private Long taskId;
        private String deletedAt;
    }

    /** 14.7 手动立即触发执行响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScheduledTaskTriggerResponse {
        private Long taskId;
        private String taskName;
        private Integer runType;
        private String runTypeLabel;
        private String triggeredAt;
        private String executeId;
    }
}