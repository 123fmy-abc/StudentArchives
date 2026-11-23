package com.example.studentarchives.dto.Fmy.aifaq.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建 AI 常见问题请求 DTO（POST /admin/ai-faqs）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAiFaqCreateRequest {

    /** 问题内容（点击后作为消息发送给 AI） */
    @NotBlank(message = "question 不能为空")
    @Size(max = 200, message = "question 长度不能超过200")
    private String question;

    /** 排序号，越小越靠前 */
    @NotNull(message = "sort 不能为空")
    private Integer sort;
}
