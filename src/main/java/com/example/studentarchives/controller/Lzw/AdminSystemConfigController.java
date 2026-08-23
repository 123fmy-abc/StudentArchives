package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.SystemConfigManageService;
import com.example.studentarchives.service.Lzw.SystemConfigManageService.AnnouncementCreateRequest;
import com.example.studentarchives.service.Lzw.SystemConfigManageService.AnnouncementIdResponse;
import com.example.studentarchives.service.Lzw.SystemConfigManageService.AnnouncementItem;
import com.example.studentarchives.service.Lzw.SystemConfigManageService.AnnouncementUpdateRequest;
import com.example.studentarchives.service.Lzw.SystemConfigManageService.SettingItem;
import com.example.studentarchives.service.Lzw.SystemConfigManageService.SettingUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统配置与公告管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》十二、系统配置与公告管理模块（12.1 ~ 12.6）。
 * 权限：要求 admin 角色（越权返回 20005），由 Service 层校验。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigManageService systemConfigManageService;

    // ==================== 12.1 获取系统配置列表 ====================

    @GetMapping("/settings")
    public ApiResult<PageResult<SettingItem>> listSettings(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(systemConfigManageService.listSettings(operatorId, group, keyword, pageParam));
    }

    // ==================== 12.2 更新系统配置 ====================

    @AuditLog(module = "config", action = "update-setting", description = "更新系统配置: #body.settingKey", relatedType = "setting")
    @PutMapping("/settings")
    public ApiResult<Void> updateSetting(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody SettingUpdateRequest body) {
        systemConfigManageService.updateSetting(operatorId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 12.3 获取公告列表 ====================

    @GetMapping("/announcements")
    public ApiResult<PageResult<AnnouncementItem>> listAnnouncements(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "targetType", required = false) String targetType,
            @RequestParam(value = "targetId", required = false) Long targetId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(systemConfigManageService.listAnnouncements(operatorId, targetType, targetId, status, pageParam));
    }

    // ==================== 12.4 发布公告 ====================

    @AuditLog(module = "announcement", action = "publish", description = "发布公告: #body.title", relatedType = "announcement")
    @PostMapping("/announcements")
    public ApiResult<AnnouncementIdResponse> createAnnouncement(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody AnnouncementCreateRequest body) {
        return ApiResult.success("发布成功", systemConfigManageService.createAnnouncement(operatorId, body));
    }

    // ==================== 12.5 更新公告 ====================

    @AuditLog(module = "announcement", action = "update", description = "更新公告: #announcementId", relatedType = "announcement", relatedId = "#announcementId")
    @PutMapping("/announcements/{announcementId}")
    public ApiResult<Void> updateAnnouncement(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long announcementId,
            @RequestBody AnnouncementUpdateRequest body) {
        systemConfigManageService.updateAnnouncement(operatorId, announcementId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 12.6 删除公告 ====================

    @AuditLog(module = "announcement", action = "delete", description = "删除公告: #announcementId", relatedType = "announcement", relatedId = "#announcementId")
    @DeleteMapping("/announcements/{announcementId}")
    public ApiResult<Void> deleteAnnouncement(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long announcementId) {
        systemConfigManageService.deleteAnnouncement(operatorId, announcementId);
        return ApiResult.success("删除成功", null);
    }
}