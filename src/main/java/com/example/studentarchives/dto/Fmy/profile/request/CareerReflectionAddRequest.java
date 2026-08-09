package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加职业规划阶段反思请求 DTO（POST /profile/career-plans/{planId}/reflections）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerReflectionAddRequest {

    @NotBlank(message = "反思内容不能为空")
    private String reflectionContent;
}
