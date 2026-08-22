package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.export.request.ArchiveExportRequest;
import com.example.studentarchives.dto.Fmy.export.response.ArchiveExportResponse;
import com.example.studentarchives.dto.Fmy.export.response.TeacherExportDeleteResponse;
import com.example.studentarchives.dto.Fmy.export.response.TeacherExportJobItem;
import com.example.studentarchives.dto.Fmy.export.response.TeacherExportTemplateResponse;
import com.example.studentarchives.entity.export.ExportJob;
import com.example.studentarchives.entity.export.ExportOperationLog;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.enums.ExportTaskStatusEnum;
import com.example.studentarchives.enums.ScopeTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.ExportJobRepository;
import com.example.studentarchives.repository.ExportOperationLogRepository;
import com.example.studentarchives.repository.ExportTemplateRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端数据导出服务
 * <p>
 * 提供教师端数据导出模块（《教师端接口文档》十二、数据导出模块）：
 * <ul>
 *   <li>12.1 模板列表：复用 {@code export_templates}（仅返回本校启用模板），权限码 {@code export:execute}；</li>
 *   <li>12.2 提交导出任务：复用 {@link AdminExportService#submitArchiveExportByTeacher}
 *       （管理端「一键导出学生档案」引擎，请求契约 {@link ArchiveExportRequest}），
 *       范围校验与权限校验在引擎内完成；</li>
 *   <li>12.3 导出任务列表：{@code export_jobs} 按 {@code operator_id} 过滤当前教师；</li>
 *   <li>12.4 删除导出任务：软删除 {@code export_jobs} + 写 {@code export_operation_logs} 删除审计（action=3）。</li>
 * </ul>
 * 所有接口权限口径：管理员放行或持有 {@code export:execute} 权限码
 * （{@code AdminAuthService#requireAdminOrPermission}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherExportService {

    /** 教师端数据导出权限码（《教师端接口文档》关键权限码） */
    private static final String PERMISSION_EXPORT_EXECUTE = "export:execute";

    /** 导出类型中文标签（与 {@link AdminExportTemplateService} 一致） */
    private static final Map<String, String> EXPORT_TYPE_LABELS = Map.of(
            "student_archive", "学生成长档案",
            "career_plan", "职业规划",
            "resume", "个人简历");

    /** 导出操作日志 action：3=删除（与《教师端接口文档》12.4 约定一致） */
    private static final int ACTION_DELETE = 3;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final AdminExportService adminExportService;
    private final ExportTemplateRepository exportTemplateRepository;
    private final ExportJobRepository exportJobRepository;
    private final ExportOperationLogRepository exportOperationLogRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;

    // ==================== 12.1 获取可导出模板列表 ====================

    /**
     * 获取可导出模板列表（GET /teacher/exports/templates，教师端文档 12.1）。
     * <p>
     * 返回当前登录用户所属学校的全部启用模板（status=1），按更新时间倒序；
     * 复用 {@code export_templates} 数据，仅映射教师导出可用的精简字段。
     *
     * @param userId 当前登录用户 ID
     * @return 可导出模板列表
     */
    @Transactional(readOnly = true)
    public List<TeacherExportTemplateResponse> listTemplates(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION_EXPORT_EXECUTE);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        return exportTemplateRepository.findAll().stream()
                .filter(t -> Objects.equals(t.getSchoolId(), schoolId))
                .filter(t -> Objects.equals(t.getStatus(), 1))
                .sorted((a, b) -> {
                    // 更新时间倒序（null 排最后）
                    if (a.getUpdatedAt() == null) return 1;
                    if (b.getUpdatedAt() == null) return -1;
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    // ==================== 12.2 提交导出任务 ====================

    /**
     * 提交导出任务（POST /teacher/exports，教师端文档 12.2）。
     * <p>
     * 复用 {@link AdminExportService#submitArchiveExportByTeacher}（管理端「一键导出学生档案」引擎）：
     * 权限校验（管理员或 {@code export:execute}）、教师 {@code role_scopes} 范围校验均在引擎内完成。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求（契约同管理端 5.11）
     * @return 任务 ID 与初始状态
     */
    public ArchiveExportResponse submitArchiveExport(Long userId, ArchiveExportRequest request) {
        return adminExportService.submitArchiveExportByTeacher(userId, request);
    }

    // ==================== 12.3 获取导出任务列表 ====================

    /**
     * 获取导出任务列表（GET /teacher/exports，教师端文档 12.3）。
     * <p>
     * {@code export_jobs} 按 {@code operator_id} 过滤当前教师，按任务 ID 倒序分页；
     * 实体 {@code @SQLRestriction} 自动过滤已软删除任务。
     *
     * @param userId    当前登录用户 ID
     * @param pageParam 分页参数
     * @return 分页的导出任务列表
     */
    @Transactional(readOnly = true)
    public PageResult<TeacherExportJobItem> listJobs(Long userId, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION_EXPORT_EXECUTE);
        PageRequest pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<ExportJob> page = exportJobRepository.findByOperatorId(userId, pageable);

        // 批量加载模板名称（templateId → export_templates.template_name）
        Set<Long> templateIds = page.getContent().stream()
                .map(ExportJob::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> templateNames = templateIds.isEmpty()
                ? Map.of()
                : exportTemplateRepository.findAllById(templateIds).stream()
                        .collect(Collectors.toMap(ExportTemplate::getId, ExportTemplate::getTemplateName, (a, b) -> a));

        List<TeacherExportJobItem> list = page.getContent().stream()
                .map(job -> toJobItem(job, templateNames.get(job.getTemplateId())))
                .collect(Collectors.toList());
        return PageResult.of(list, page.getTotalElements(), pageParam);
    }

    // ==================== 12.4 删除导出任务 ====================

    /**
     * 删除导出任务（DELETE /teacher/exports/{jobId}，教师端文档 12.4）。
     * <p>
     * 软删除 {@code export_jobs}（deleted_at 置位），并写入 {@code export_operation_logs}
     * 删除审计（action=3）。仅本人创建的任务可删除，管理员可删除任意任务；
     * 非本人且非管理员返回 20005 无访问权限。
     *
     * @param userId 当前登录用户 ID
     * @param jobId  导出任务 ID
     * @return 删除结果
     */
    @Transactional
    public TeacherExportDeleteResponse deleteExport(Long userId, Long jobId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION_EXPORT_EXECUTE);
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "导出任务不存在"));
        // 归属校验：本人任务或管理员
        if (!Objects.equals(job.getOperatorId(), userId)) {
            AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(userId);
            if (role == null || !role.isAdmin()) {
                throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = exportJobRepository.softDeleteById(jobId, now);
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "导出任务不存在");
        }
        writeDeleteAudit(job, userId);
        log.info("删除导出任务 jobId={}, operatorId={}", jobId, userId);
        return TeacherExportDeleteResponse.builder()
                .jobId(jobId)
                .deletedAt(toIso(now))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** 模板 → 12.1 列表项 DTO */
    private TeacherExportTemplateResponse toTemplateResponse(ExportTemplate t) {
        ScopeTypeEnum scopeType = ScopeTypeEnum.of(t.getScopeType());
        return TeacherExportTemplateResponse.builder()
                .templateId(t.getId())
                .templateName(t.getTemplateName())
                .exportType(t.getExportType())
                .exportTypeLabel(exportTypeLabel(t.getExportType()))
                .scopeType(t.getScopeType())
                .scopeTypeLabel(scopeType != null ? scopeType.getLabel() : null)
                .build();
    }

    /** 任务 → 12.3 列表项 DTO（templateName 为 null 表示模板已删除） */
    private TeacherExportJobItem toJobItem(ExportJob job, String templateName) {
        Integer status = job.getStatus() != null ? job.getStatus() : ExportTaskStatusEnum.PENDING.getValue();
        String downloadUrl = null;
        if (Objects.equals(status, ExportTaskStatusEnum.COMPLETED.getValue()) && job.getFileId() != null
                && attachmentRelationRepository.findById(job.getFileId()).isPresent()) {
            // 走后端下载端点而非 OSS 直链：统一权限/有效期校验，并记录 export_operation_logs(action=2) 下载审计
            downloadUrl = adminExportService.buildDownloadUrl(job.getFileId());
        }
        return TeacherExportJobItem.builder()
                .exportJobId(job.getId())
                .templateName(templateName)
                .exportType(job.getExportType())
                .status(status)
                .statusLabel(ExportTaskStatusEnum.of(status).getLabel())
                .totalCount(job.getTotalCount())
                .successCount(job.getSuccessCount())
                .downloadUrl(downloadUrl)
                .expireAt(toIso(job.getExpireAt()))
                .createdAt(toIso(job.getCreatedAt()))
                .build();
    }

    /**
     * 写删除审计（export_operation_logs action=3）。
     * <p>
     * 与 {@code AdminExportService.writeOperationLog} 口径一致：scope_type 仅支持 1-4，
     * 年级（6）删除时审计记录以学校范围（scope_type=1 + scope_id=学校）落库，
     * 年级值随 filter_conditions 快照保留。
     */
    private void writeDeleteAudit(ExportJob job, Long operatorId) {
        Integer logScopeType = Objects.equals(job.getScopeType(), 6) ? 1 : job.getScopeType();
        Long logScopeId = Objects.equals(job.getScopeType(), 6) ? job.getSchoolId() : job.getScopeId();
        ExportOperationLog opLog = new ExportOperationLog();
        opLog.setSchoolId(job.getSchoolId());
        opLog.setOperatorId(operatorId);
        opLog.setExportType(job.getExportType());
        opLog.setAction(ACTION_DELETE);
        opLog.setScopeType(logScopeType);
        opLog.setScopeId(logScopeId != null ? logScopeId : job.getSchoolId());
        opLog.setFilterConditions(job.getFilterConditions());
        opLog.setRecordCount(job.getSuccessCount());
        opLog.setIsAnonymized(0);
        opLog.setFileId(job.getFileId());
        opLog.setStatus(1);
        exportOperationLogRepository.save(opLog);
    }

    private String exportTypeLabel(String exportType) {
        return exportType != null ? EXPORT_TYPE_LABELS.get(exportType) : null;
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
