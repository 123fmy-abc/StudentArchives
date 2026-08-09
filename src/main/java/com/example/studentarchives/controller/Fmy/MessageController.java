package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.dto.Fmy.message.request.MessageBatchIdsRequest;
import com.example.studentarchives.dto.Fmy.message.request.MessageSettingUpdateRequest;
import com.example.studentarchives.dto.Fmy.message.response.MessageArchiveResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageBatchDeleteResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageListResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageReadAllResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageSettingItem;
import com.example.studentarchives.service.Fmy.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息中心控制器
 * <p>
 * 提供学生端消息中心模块接口（《学生端接口文档》五、消息中心模块），
 * 所有接口需携带 Bearer Token 认证。
 */
@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取消息列表（5.1）
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param category   分类：all、system_notice、audit_remind、dynamic_remind、private_message
     * @param isRead     状态：0=未读 1=已读，不传为全部
     * @param isArchived 是否归档：0=未归档 1=已归档，不传则默认 0
     * @param keyword    搜索关键词（匹配 title 或 content）
     * @param page       页码，默认 1
     * @param perPage    每页条数，默认 10
     * @return 消息列表
     */
    @GetMapping
    public ApiResult<MessageListResponse> listMessages(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "isRead", required = false) Integer isRead,
            @RequestParam(value = "isArchived", required = false) Integer isArchived,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "10") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        MessageListResponse response = messageService.listMessages(
                userId, category, isRead, isArchived, keyword, pageParam);
        return ApiResult.success(response);
    }

    /**
     * 标记消息已读（5.2）
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 标记结果
     */
    @PutMapping("/{messageId}/read")
    public ApiResult<Void> markRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        messageService.markRead(userId, messageId);
        return ApiResult.success("标记成功", null);
    }

    /**
     * 批量标记消息已读（5.3）
     *
     * @param userId   当前登录用户 ID
     * @param category 分类筛选，不传则标记所有未读
     * @return 标记条数
     */
    @PutMapping("/read-all")
    public ApiResult<MessageReadAllResponse> markAllRead(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "category", required = false) String category) {
        return ApiResult.success("标记成功", messageService.markAllRead(userId, category));
    }

    /**
     * 按 ID 列表批量标记消息已读（5.8）
     *
     * @param userId  当前登录用户 ID
     * @param request 批量请求体（messageIds：1~100 个消息 ID）
     * @return 标记条数
     */
    @PutMapping("/batch-read")
    public ApiResult<MessageReadAllResponse> markBatchRead(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MessageBatchIdsRequest request) {
        return ApiResult.success("标记成功", messageService.markBatchRead(userId, request.getMessageIds()));
    }

    /**
     * 归档消息（5.4）
     * <p>
     * 重要系统消息（is_important=1）归档前需前端二次确认。
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 归档结果
     */
    @PutMapping("/{messageId}/archive")
    public ApiResult<MessageArchiveResponse> archive(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        return ApiResult.success("归档成功", messageService.archive(userId, messageId));
    }

    /**
     * 取消归档消息（5.5）
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 取消归档结果
     */
    @PutMapping("/{messageId}/unarchive")
    public ApiResult<MessageArchiveResponse> unarchive(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        return ApiResult.success("取消归档成功", messageService.unarchive(userId, messageId));
    }

    /**
     * 删除单条消息（5.9）
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 删除结果
     */
    @DeleteMapping("/{messageId}")
    public ApiResult<Void> deleteMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        messageService.deleteMessage(userId, messageId);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 按 ID 列表批量删除消息（5.10）
     *
     * @param userId  当前登录用户 ID
     * @param request 批量请求体（messageIds：1~100 个消息 ID）
     * @return 删除条数
     */
    @DeleteMapping("/batch")
    public ApiResult<MessageBatchDeleteResponse> deleteMessages(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MessageBatchIdsRequest request) {
        return ApiResult.success("删除成功", messageService.deleteMessages(userId, request.getMessageIds()));
    }

    /**
     * 获取消息通知设置（5.6）
     *
     * @param userId 当前登录用户 ID
     * @return 通知设置列表
     */
    @GetMapping("/settings")
    public ApiResult<List<MessageSettingItem>> getSettings(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(messageService.getSettings(userId));
    }

    /**
     * 更新消息通知设置（5.7）
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/settings")
    public ApiResult<Void> updateSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MessageSettingUpdateRequest request) {
        messageService.updateSetting(userId, request);
        return ApiResult.success("更新成功", null);
    }
}
