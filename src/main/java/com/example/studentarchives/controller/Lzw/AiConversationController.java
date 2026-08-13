package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.AiConversationService;
import com.example.studentarchives.service.Lzw.AiConversationService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * AI 对话模块控制器
 * <p>
 * 覆盖：创建会话、会话列表、消息列表、发送消息、重新生成、AI 建议、删除会话。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService aiConversationService;

    // ==================== 9.1 创建对话会话 ====================

    @PostMapping("/conversations")
    public ApiResult<ConversationCreateResponse> createConversation(
            @RequestBody(required = false) CreateConversationRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(aiConversationService.createConversation(body, userId));
    }

    // ==================== 9.2 获取对话会话列表 ====================

    @GetMapping("/conversations")
    public ApiResult<PageResult<ConversationListItem>> listConversations(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage,
            @AuthenticationPrincipal Long userId) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(perPage)
                .build();
        return ApiResult.success(aiConversationService.listConversations(userId, pageParam));
    }

    // ==================== 9.3 获取对话消息列表 ====================

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResult<ConversationMessagesResponse> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(aiConversationService.getMessages(conversationId, userId));
    }

    // ==================== 9.4 发送消息 ====================

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResult<SendMessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(aiConversationService.sendMessage(conversationId, body, userId));
    }

    // ==================== 9.5 重新生成 AI 消息 ====================

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/regenerate")
    public ApiResult<RegenerateResponse> regenerate(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(aiConversationService.regenerate(conversationId, messageId, userId));
    }

    // ==================== 9.6 获取 AI 辅助建议 ====================

    @GetMapping("/suggestions")
    public ApiResult<SuggestionsResponse> getSuggestions(
            @RequestParam(value = "sourceType") String sourceType,
            @RequestParam(value = "sourceId") Long sourceId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(aiConversationService.getSuggestions(sourceType, sourceId, userId));
    }

    // ==================== 9.7 删除对话会话 ====================

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResult<Void> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId) {
        aiConversationService.deleteConversation(conversationId, userId);
        return ApiResult.success("删除成功", null);
    }
}