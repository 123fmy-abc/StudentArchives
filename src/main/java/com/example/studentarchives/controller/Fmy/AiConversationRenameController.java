package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.ai.request.ConversationRenameRequest;
import com.example.studentarchives.dto.Fmy.ai.response.ConversationRenameResponse;
import com.example.studentarchives.service.Fmy.AiConversationRenameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 对话会话重命名 Controller（《学生端接口文档》9.8）
 * <p>
 * 补齐前端已在调用、后端此前缺失的 {@code PUT /ai/conversations/{conversationId}}。
 * <p>
 * 与 {@code Lzw/AiConversationController}（{@code @RequestMapping("/ai")}）共享前缀但不冲突：
 * 该控制器只有 {@code DELETE /conversations/{conversationId}}，本类只有对应的 PUT。
 * 之所以另起控制器而不改既有文件，是为了不动其他开发者的代码。
 * <p>
 * 鉴权：{@code /ai/**} 在 SecurityConfig 中要求登录；归属校验在 Service 层按
 * {@code ai_conversations.user_id} 判定，会话不存在或非本人一律 30001。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiConversationRenameController {

    private final AiConversationRenameService aiConversationRenameService;

    /**
     * 重命名对话会话（PUT /ai/conversations/{conversationId}，《学生端接口文档》9.8）
     *
     * @param conversationId 会话 ID
     * @param request        请求体（含新标题）
     * @param userId         当前登录用户 ID
     */
    @PutMapping("/conversations/{conversationId:[0-9]+}")
    public ApiResult<ConversationRenameResponse> renameConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationRenameRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("重命名成功",
                aiConversationRenameService.renameConversation(userId, conversationId, request.getTitle()));
    }
}
