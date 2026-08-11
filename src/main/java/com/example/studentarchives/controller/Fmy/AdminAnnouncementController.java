package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.announcement.request.AnnouncementPublishRequest;
import com.example.studentarchives.dto.Fmy.announcement.response.AnnouncementResponse;
import com.example.studentarchives.service.Fmy.AdminAnnouncementService;
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

import java.util.List;

/**
 * 管理端信息发布（公告）控制器
 * <p>
 * 对应图片“管理员 → 表单自定义 → 发布信息”。
 * 明确与 {@link AdminFormTemplateController#publish(Long, Long)} 区分：
 * 此处发布的是面向用户可见的公告/通知，而非表单模板版本。
 */
@Slf4j
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    @GetMapping
    public ApiResult<List<AnnouncementResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam("schoolId") Long schoolId) {
        return ApiResult.success(adminAnnouncementService.list(userId, schoolId));
    }

    @AuditLog(module = "announcement", action = "publish",
            description = "发布公告: #request.title", logResult = true)
    @PostMapping
    public ApiResult<AnnouncementResponse> publish(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AnnouncementPublishRequest request) {
        return ApiResult.success("发布成功", adminAnnouncementService.publish(userId, request));
    }

    @AuditLog(module = "announcement", action = "delete",
            description = "删除公告: #announcementId")
    @DeleteMapping("/{announcementId}")
    public ApiResult<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long announcementId) {
        adminAnnouncementService.delete(userId, announcementId);
        return ApiResult.success("删除成功", null);
    }
}
