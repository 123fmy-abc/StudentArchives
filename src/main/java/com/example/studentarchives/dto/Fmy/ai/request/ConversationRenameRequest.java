package com.example.studentarchives.dto.Fmy.ai.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重命名对话会话请求 DTO（PUT /ai/conversations/{conversationId}，《学生端接口文档》9.8）
 * <p>
 * title 去除首尾空格后 1~100 字符。会话所属人必须是当前登录用户，否则返回 30001。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRenameRequest {

    /** 新标题（必填，去首尾空格后 1~100 字符） */
    @NotBlank(message = "会话标题不能为空")
    @Size(max = 100, message = "会话标题长度不能超过100")
    private String title;
}
