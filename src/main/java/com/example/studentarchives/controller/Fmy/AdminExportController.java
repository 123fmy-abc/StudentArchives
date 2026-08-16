package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.export.request.ArchiveExportRequest;
import com.example.studentarchives.dto.Fmy.export.request.ResearchExportRequest;
import com.example.studentarchives.dto.Fmy.export.response.ArchiveExportResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportJobResponse;
import com.example.studentarchives.dto.Fmy.export.response.ResearchExportResponse;
import com.example.studentarchives.service.Fmy.AdminExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据导出控制器
 * <p>
 * 对应《管理端接口文档》五、数据导出模块（5.1~5.2、5.11），统一前缀 /admin/exports：
 * <ul>
 *   <li>5.1 POST /admin/exports/research：研究数据导出（任务异步执行，接口立即返回任务 ID）；</li>
 *   <li>5.2 GET /admin/exports/{jobId}：查询导出任务进度及下载链接。</li>
 *   <li>5.11 POST /admin/exports/archives：一键导出学生档案（管理端），按组织范围批量导出
 *       pdf（逐学生模板渲染后合并）/ xlsx（学生基本信息与档案列表）；</li>
 * </ul>
 * 权限校验（export:research / export:manage / archive:export）在 Service 层完成，越权返回 20005 无访问权限。
 * 研究导出与学生档案导出的创建审计写入 export_operation_logs（action=1），本接口写操作同时写入 audit_log。
 */
@Slf4j
@RestController
@RequestMapping("/admin/exports")
@RequiredArgsConstructor
public class AdminExportController {

    private final AdminExportService adminExportService;

    // ==================== 5.1 研究数据导出 ====================

    /**
     * 研究数据导出（POST /admin/exports/research，文档 5.1）
     * <p>
     * 导出用于研究分析的数据，自动使用匿名编号替代姓名和学号。仅拥有 export:research
     * 权限的用户可调用。任务进入 export_jobs 异步执行，接口立即返回任务 ID 与初始状态。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 任务 ID、初始状态与预计耗时
     */
    @AuditLog(module = "export", action = "create-research",
            description = "研究数据导出: scopeType=#request.scopeType, semesterId=#request.semesterId, dataTypes=#request.dataTypes")
    @PostMapping("/research")
    public ApiResult<ResearchExportResponse> submitResearchExport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ResearchExportRequest request) {
        ResearchExportResponse response = adminExportService.submitResearchExport(userId, request);
        return ApiResult.success("研究数据导出任务已创建", response);
    }

    // ==================== 5.11 一键导出学生档案 ====================

    /**
     * 一键导出学生档案（POST /admin/exports/archives，文档 5.11）
     * <p>
     * 按组织范围（学校/学院/专业/班级/年级）批量导出学生的基本信息与成长档案。
     * 仅拥有 archive:export 权限的用户可调用。任务进入 export_jobs 异步执行，
     * 接口立即返回任务 ID，完成后通过 5.2 查询下载链接。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 任务 ID、导出类型与初始状态（待处理，预计耗时 60s）
     */
    @AuditLog(module = "export", action = "create-archives",
            description = "一键导出学生档案: scopeType=#request.scopeType, scopeId=#request.scopeId, fileType=#request.fileType, templateId=#request.templateId, purpose=#request.purpose")
    @PostMapping("/archives")
    public ApiResult<ArchiveExportResponse> submitArchiveExport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ArchiveExportRequest request) {
        ArchiveExportResponse response = adminExportService.submitArchiveExport(userId, request);
        return ApiResult.success("一键导出学生档案任务已创建", response);
    }

    // ==================== 5.2 查询导出任务 ====================

    /**
     * 查询导出任务进度及下载链接（GET /admin/exports/{jobId}，文档 5.2）
     *
     * @param userId 当前登录用户 ID
     * @param jobId  导出任务 ID
     * @return 任务状态、进度与下载链接
     */
    // jobId 限定为纯数字：避免 /archives、/research 等路径段被 {jobId} 捕获，
    // 否则方法/路径误配时会把 "archives" 当 jobId 转 Long 失败，报误导性的「参数 jobId 格式错误」
    @GetMapping("/{jobId:[0-9]+}")
    public ApiResult<ExportJobResponse> getExportJob(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long jobId) {
        ExportJobResponse response = adminExportService.getExportJob(userId, jobId);
        return ApiResult.success(response);
    }
}
