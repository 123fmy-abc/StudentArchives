package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.common.response.CoverUploadResponse;
import com.example.studentarchives.service.Fmy.CoverUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 封面上传控制器（POST /common/upload/cover）
 * <p>
 * 供成长时间轴等业务模块上传封面图使用：直接上传至 OSS 正式目录 {@code cover/photo/}，
 * 返回 30 天有效的签名 URL，前端将其回填到业务的 {@code coverImage} 字段。
 * 与 {@code POST /common/upload}（临时附件流程）不同，本接口不走 temp 临时目录、
 * 不写 {@code file_uploads} 表。
 * 所有接口需携带 Bearer Token 认证（{@code /common/**} 认证范围）。
 */
@Slf4j
@RestController
@RequestMapping("/common/upload/cover")
@RequiredArgsConstructor
public class CoverUploadController {

    private final CoverUploadService coverUploadService;

    /**
     * 上传封面图
     *
     * @param file   封面文件（仅支持 jpg/png/gif/webp/bmp，最大 5MB）
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @return 封面 URL 与 OSS 对象路径
     */
    @AuditLog(module = "file", action = "upload_cover", description = "上传封面图", logResult = true)
    @PostMapping
    public ApiResult<CoverUploadResponse> uploadCover(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {

        CoverUploadResponse response = coverUploadService.uploadCover(file, userId);
        return ApiResult.success("封面上传成功", response);
    }
}
