package com.example.studentarchives.dto.Fmy.ai.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重命名对话会话响应 DTO（PUT /ai/conversations/{conversationId}，《学生端接口文档》9.8）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationRenameResponse {

    /** 会话 ID（ai_conversations.id） */
    private Long conversationId;

    /** 重命名后的标题 */
    private String title;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
