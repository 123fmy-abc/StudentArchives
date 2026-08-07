package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加职业规划里程碑请求 DTO（POST /profile/career-plans/{planId}/actions/{actionId}/milestones）
 * <p>
 * 归属行动经路径参数 actionId 指定，不在请求体中。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerMilestoneAddRequest {

    @NotBlank(message = "里程碑标题不能为空")
    @Size(max = 255, message = "里程碑标题长度不能超过255")
    private String milestoneTitle;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String milestoneDate;
}
