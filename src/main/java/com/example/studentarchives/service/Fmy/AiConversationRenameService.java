package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.ai.response.ConversationRenameResponse;
import com.example.studentarchives.entity.ai.AiConversation;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AiConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * AI 对话会话重命名 Service（PUT /ai/conversations/{conversationId}，《学生端接口文档》9.8）
 * <p>
 * 补齐前端已在调用、后端此前缺失的接口。会话实体 {@code ai_conversations} 已有 title 字段，
 * 无需改表结构。
 * <p>
 * 归属校验直接以 {@code ai_conversations.user_id} 判定：会话不存在或不属于当前登录用户，
 * 统一返回 30001（不区分两种情况，避免向已登录用户泄露他人会话是否存在）。
 * <p>
 * 未复用 {@code service/Lzw/AiConversationService#loadOwnedConversation}（越权返回 5/403）：
 * 该方法是 Lzw 包的私有方法，且本接口对外契约（见《待实现接口文档》一）定为 30001。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationRenameService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 标题长度上限（与请求 DTO 校验、doc 契约一致） */
    private static final int TITLE_MAX_LENGTH = 100;

    private final AiConversationRepository aiConversationRepository;

    /**
     * 重命名当前登录用户自己的对话会话
     *
     * @param userId         当前登录用户 ID
     * @param conversationId 会话 ID
     * @param title          新标题（去首尾空格后 1~100 字符）
     * @return 重命名结果（会话 ID / 新标题 / 更新时间）
     */
    @Transactional
    public ConversationRenameResponse renameConversation(Long userId, Long conversationId, String title) {
        String normalized = title == null ? null : title.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话标题不能为空");
        }
        if (normalized.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话标题长度不能超过100");
        }

        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .filter(conv -> userId != null && userId.equals(conv.getUserId()))
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "会话不存在"));

        conversation.setTitle(normalized);
        // BaseEntity 的 @PreUpdate 会在 flush 时刷新 updated_at，save 后即可读到新值
        AiConversation saved = aiConversationRepository.saveAndFlush(conversation);

        return ConversationRenameResponse.builder()
                .conversationId(saved.getId())
                .title(saved.getTitle())
                .updatedAt(toIso(saved.getUpdatedAt()))
                .build();
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
