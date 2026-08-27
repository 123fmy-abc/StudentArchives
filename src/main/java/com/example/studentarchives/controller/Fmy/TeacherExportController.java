package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.export.request.ArchiveExportRequest;
import com.example.studentarchives.dto.Fmy.export.response.ArchiveExportResponse;
import com.example.studentarchives.dto.Fmy.export.response.TeacherExportDeleteResponse;
import com.example.studentarchives.dto.Fmy.export.response.TeacherExportJobItem;
import com.example.studentarchives.dto.Fmy.export.response.TeacherExportTemplateResponse;
import com.example.studentarchives.service.Fmy.TeacherExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端数据导出控制器
 * <p>
 * 提供教师端数据导出模块接口（《教师端接口文档》十二、数据导出模块），统一前缀 /teacher/exports：
 * <ul>
 *   <li>12.1 GET /teacher/exports/templates：获取可导出模板列表（复用 {@code export_templates}）；</li>
 *   <li>12.2 POST /teacher/exports：提交导出任务（复用 {@link AdminExportService#submitArchiveExportByTeacher}
 *       管理端「一键导出学生档案」引擎，请求契约同管理端 5.11 {@link ArchiveExportRequest}）；</li>
 *   <li>12.3 GET /teacher/exports：获取导出任务列表（按 {@code operator_id} 过滤）；</li>
 *   <li>12.4 DELETE /teacher/exports/{jobId}：删除导出任务（软删除 + {@code export_operation_logs} 删除审计）。</li>
 * </ul>
 * 权限校验（管理员放行或 {@code export:execute} 权限码）在 Service 层完成，
 * 越权返回 20005 无访问权限。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/exports")
@RequiredArgsConstructor
public class TeacherExportController {

    private final TeacherExportService teacherExportService;

    // ==================== 12.1 获取可导出模板列表 ====================

    /**
     * 获取可导出模板列表（GET /teacher/exports/templates，文档 12.1）
     * <p>
     * 查询当前登录用户所属学校的启用模板（status=1），按更新时间倒序分页返回；
     * 复用 {@code export_templates} 表，仅映射教师导出可用的精简字段。
     *
     * @param userId  当前登录用户 ID
     * @param page    页码，默认 1
     * @param perPage 每页条数，默认 20，最大 100
     * @return 分页的可导出模板列表
     */
    @GetMapping("/templates")
    public ApiResult<PageResult<TeacherExportTemplateResponse>> listTemplates(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(teacherExportService.listTemplates(userId, pageParam));
    }

    // ==================== 12.2 提交导出任务 ====================

    /**
     * 提交导出任务（POST /teacher/exports，文档 12.2）
     * <p>
     * 复用管理端「一键导出学生档案」引擎，任务进入 {@code export_jobs} 异步执行，
     * 接口立即返回任务 ID；完成后通过 12.3 任务列表获取下载链接。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 任务 ID、导出类型与初始状态（待处理，预计耗时 60s）
     */
    @AuditLog(module = "export", action = "create-archives",
            description = "教师端导出学生档案: scopeType=#request.scopeType, scopeId=#request.scopeId, fileType=#request.fileType, templateId=#request.templateId")
    @PostMapping
    public ApiResult<ArchiveExportResponse> submitArchiveExport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ArchiveExportRequest request) {
        ArchiveExportResponse response = teacherExportService.submitArchiveExport(userId, request);
        return ApiResult.success("导出任务已创建", response);
    }

    // ==================== 12.3 获取导出任务列表 ====================

    /**
     * 获取导出任务列表（GET /teacher/exports，文档 12.3）
     *
     * @param userId  当前登录用户 ID
     * @param page    页码，默认 1
     * @param perPage 每页条数，默认 10
     * @return 分页的导出任务列表
     */
    @GetMapping
    public ApiResult<PageResult<TeacherExportJobItem>> listJobs(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "10") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(teacherExportService.listJobs(userId, pageParam));
    }

    // ==================== 12.4 删除导出任务 ====================

    /**
     * 删除导出任务（DELETE /teacher/exports/{jobId}，文档 12.4）
     * <p>
     * 软删除 {@code export_jobs} 并写入 {@code export_operation_logs} 删除审计（action=3）。
     *
     * @param userId 当前登录用户 ID
     * @param jobId  导出任务 ID
     * @return 删除结果
     */
    @AuditLog(module = "export", action = "delete", description = "教师端删除导出任务: jobId=#jobId")
    @DeleteMapping("/{jobId:[0-9]+}")
    public ApiResult<TeacherExportDeleteResponse> deleteExport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long jobId) {
        return ApiResult.success("删除成功", teacherExportService.deleteExport(userId, jobId));
    }
}
