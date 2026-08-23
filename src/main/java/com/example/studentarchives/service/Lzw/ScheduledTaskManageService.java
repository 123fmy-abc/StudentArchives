package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.schedule.ScheduledTask;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ScheduledTaskRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端定时任务管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》十四、定时任务管理模块（14.1 ~ 14.2）。
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

        return ScheduledTaskStatusResponse.builder()
                .taskId(taskId)
                .status(status)
                .statusLabel(status == 1 ? "已启用" : "已停用")
                .build();
    }

    // ==================== 通用辅助 ====================

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
}