package com.example.studentarchives.dto.Fmy.aifaq.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 常见问题响应 DTO（管理端与学生端 /ai/faqs 共用；
 * 学生端场景下 status/statusLabel 为 null，序列化时自动省略）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiFaqQuestionResponse {

    /** 常见问题 ID */
    private Long id;

    /** 问题内容 */
    private String question;

    /** 排序号 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;

    /** 状态标签 */
    private String statusLabel;
}
