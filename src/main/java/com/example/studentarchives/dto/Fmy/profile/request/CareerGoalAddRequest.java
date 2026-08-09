package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加职业规划目标请求 DTO（POST /profile/career-plans/{planId}/goals）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerGoalAddRequest {

    @NotBlank(message = "目标标题不能为空")
    @Size(max = 255, message = "目标标题长度不能超过255")
    private String goalTitle;

    private String goalDesc;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String targetDate;

    /** 排序，默认0 */
    private Integer sort;
}
