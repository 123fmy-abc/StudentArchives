package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.SemesterManageService;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterIdResponse;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterImportRequest;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterImportResponse;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterListItem;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterListQuery;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterSaveRequest;
import com.example.studentarchives.service.Lzw.SemesterManageService.SemesterStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端学期管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》九、学期管理模块（9.1 ~ 9.7）。
 * 权限：9.1~9.5 要求 admin 角色；9.6/9.7 校验 {@code semester:import} 权限码，
 * 由 Service 层校验，越权返回 20005。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSemesterController {

    private final SemesterManageService semesterManageService;

    // ==================== 9.1 获取学期列表 ====================

    @GetMapping("/semesters")
    public ApiResult<PageResult<SemesterListItem>> listSemesters(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        SemesterListQuery query = SemesterListQuery.builder().schoolId(schoolId).status(status).build();
        return ApiResult.success(semesterManageService.listSemesters(operatorId, query, buildPageParam(page, perPage)));
    }

    // ==================== 9.2 创建学期 ====================

    @AuditLog(module = "semester", action = "create", description = "创建学期: #body.name", relatedType = "semester")
    @PostMapping("/semesters")
    public ApiResult<SemesterIdResponse> createSemester(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody SemesterSaveRequest body) {
        return ApiResult.success("创建成功", semesterManageService.createSemester(operatorId, body));
    }

    // ==================== 9.3 更新学期 ====================

    @AuditLog(module = "semester", action = "update", description = "更新学期: #semesterId", relatedType = "semester", relatedId = "#semesterId")
    @PutMapping("/semesters/{semesterId}")
    public ApiResult<Void> updateSemester(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long semesterId,
            @RequestBody SemesterSaveRequest body) {
        semesterManageService.updateSemester(operatorId, semesterId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 9.4 设置当前学期 ====================

    @AuditLog(module = "semester", action = "set-current", description = "设置当前学期: #semesterId", relatedType = "semester", relatedId = "#semesterId")
    @PutMapping("/semesters/{semesterId}/set-current")
    public ApiResult<Void> setCurrentSemester(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long semesterId) {
        semesterManageService.setCurrentSemester(operatorId, semesterId);
        return ApiResult.success("设置成功", null);
    }

    // ==================== 9.5 启用/禁用学期 ====================

    @AuditLog(module = "semester", action = "update-status", description = "启用/禁用学期: #semesterId", relatedType = "semester", relatedId = "#semesterId")
    @PutMapping("/semesters/{semesterId}/status")
    public ApiResult<Void> updateSemesterStatus(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long semesterId,
            @RequestBody SemesterStatusRequest body) {
        semesterManageService.updateSemesterStatus(operatorId, semesterId,
                body != null ? body.getStatus() : null);
        return ApiResult.success("操作成功", null);
    }

    // ==================== 9.6 批量导入学期 ====================

    @AuditLog(module = "semester", action = "import",
            description = "导入学期: schoolId=#body.schoolId, fileId=#body.fileId", logResult = true)
    @PostMapping("/semesters/import")
    public ApiResult<SemesterImportResponse> importSemesters(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody SemesterImportRequest body) {
        return ApiResult.success("导入完成", semesterManageService.importSemesters(operatorId, body));
    }

    // ==================== 9.7 下载导入模板 ====================

    @GetMapping("/semesters/import-template")
    public ResponseEntity<byte[]> importTemplate(@AuthenticationPrincipal Long operatorId) {
        byte[] data = semesterManageService.importTemplate(operatorId);

        String filename = "学期导入模板.xlsx";
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

    private PageParam buildPageParam(int page, int perPage) {
        return PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
    }
}
