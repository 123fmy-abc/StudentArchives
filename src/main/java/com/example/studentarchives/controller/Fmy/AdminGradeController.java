package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportRequest;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportDetailResponse;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportListItem;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportResponse;
import com.example.studentarchives.service.Fmy.AdminGradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端成绩导入控制器
 * <p>
 * 对应《管理端接口文档》十三、成绩导入模块（13.1 导入成绩 / 13.2 导入历史列表 /
 * 13.3 导入详情 / 13.4 下载导入模板），统一前缀 /admin/grades。
 * <p>
 * 所有接口需通过 {@link AdminGradeService} 校验 admin 角色或 grade:import 权限码，
 * 越权返回 20005 无访问权限。导入成绩写入 audit_log 审计日志（module=grade, action=import）。
 */
@RestController
@RequestMapping("/admin/grades")
@RequiredArgsConstructor
public class AdminGradeController {

    private final AdminGradeService adminGradeService;

    // ==================== 13.1 导入成绩 ====================

    /**
     * 导入成绩（POST /admin/grades/import，文档 13.1）
     * <p>
     * 创建异步导入任务并立即返回任务 ID 与初始状态（0=导入中）。文件格式支持
     * .xlsx / .csv，模板可从 13.4 下载。
     *
     * @param userId  当前登录用户 ID（由 JWT 过滤器注入）
     * @param request 导入请求：semesterId（必填）、fileId（必填）、overwrite（可选，默认 false）
     * @return 任务 ID、状态与预估耗时
     */
    @AuditLog(module = "grade", action = "import",
            description = "导入成绩: semesterId=#request.semesterId, fileId=#request.fileId", logResult = true)
    @PostMapping("/import")
    public ApiResult<GradeImportResponse> importGrades(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GradeImportRequest request) {
        GradeImportResponse response = adminGradeService.importGrades(userId, request);
        return ApiResult.success("导入任务已创建", response);
    }

    // ==================== 13.2 获取导入历史列表 ====================

    /**
     * 获取导入历史列表（GET /admin/grades/imports，文档 13.2）
     * <p>
     * 按学期/导入状态筛选，按创建时间倒序分页。
     *
     * @param userId       当前登录用户 ID
     * @param semesterId   学期筛选（可选）
     * @param importStatus 状态筛选（可选，0=导入中 1=完成 2=失败）
     * @param page         页码，默认 1
     * @param perPage      每页条数，默认 20
     * @return 分页导入历史列表
     */
    @GetMapping("/imports")
    public ApiResult<PageResult<GradeImportListItem>> listImports(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "importStatus", required = false) Integer importStatus,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder().page(page).perPage(perPage).build();
        return ApiResult.success(adminGradeService.listImports(userId, semesterId, importStatus, pageParam));
    }

    // ==================== 13.3 获取导入详情 ====================

    /**
     * 获取导入详情（GET /admin/grades/imports/{importId}，文档 13.3）
     * <p>
     * 返回操作人、文件 ID 与失败明细（行号/学号/失败原因）。
     *
     * @param userId   当前登录用户 ID
     * @param importId 导入任务 ID
     * @return 导入详情
     */
    @GetMapping("/imports/{importId}")
    public ApiResult<GradeImportDetailResponse> importDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long importId) {
        return ApiResult.success(adminGradeService.importDetail(userId, importId));
    }

    // ==================== 13.4 下载导入模板 ====================

    /**
     * 下载成绩导入模板（GET /admin/grades/import-template，文档 13.4）
     * <p>
     * 返回标准 .xlsx 文件，表头与示例数据由 {@code grade_import_configs.template_columns} 配置决定，
     * 未配置或已禁用时返回业务异常。
     *
     * @param userId 当前登录用户 ID
     * @return 模板 Excel 文件下载响应
     */
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate(@AuthenticationPrincipal Long userId) {
        byte[] data = adminGradeService.importTemplate(userId);

        String filename = "成绩导入模板.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                encodedFilename, encodedFilename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
