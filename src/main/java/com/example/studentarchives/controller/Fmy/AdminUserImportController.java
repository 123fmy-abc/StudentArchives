package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.service.Fmy.UserImportService;
import com.example.studentarchives.service.Fmy.UserImportService.UserImportRequest;
import com.example.studentarchives.service.Fmy.UserImportService.UserImportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端学生账号批量导入接口（Fmy）
 * <p>
 * 在已有 {@code AdminUserController}（/admin/users 的 6.1~6.8）之外补充导入能力，
 * 与学期导入（9.6/9.7）响应风格保持一致：
 * <ul>
 *   <li>POST /admin/users/import —— 批量导入学生账号（逐行结果）；</li>
 *   <li>GET /admin/users/import-template —— 下载导入模板（.xlsx）。</li>
 * </ul>
 * 权限校验在 Service 层执行（admin 角色或 user:manage）。
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserImportController {

    private final UserImportService userImportService;

    @AuditLog(module = "user", action = "import",
            description = "导入学生账号: schoolId=#body.schoolId, fileId=#body.fileId", logResult = true)
    @PostMapping("/import")
    public ApiResult<UserImportResponse> importUsers(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody UserImportRequest body) {
        return ApiResult.success("导入完成", userImportService.importUsers(operatorId, body));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate(@AuthenticationPrincipal Long operatorId) {
        byte[] data = userImportService.importTemplate(operatorId);

        String filename = "学生账号导入模板.xlsx";
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
