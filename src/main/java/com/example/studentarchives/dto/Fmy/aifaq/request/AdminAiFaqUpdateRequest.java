package com.example.studentarchives.dto.Fmy.aifaq.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新 AI 常见问题请求 DTO（PUT /admin/ai-faqs/{id}），全字段可选
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAiFaqUpdateRequest {

    /** 问题内容 */
    @Size(max = 200, message = "question 长度不能超过200")
    private String question;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    @Min(value = 0, message = "status 只能为 0 或 1")
    @Max(value = 1, message = "status 只能为 0 或 1")
    private Integer status;
}
