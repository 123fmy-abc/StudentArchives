package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加职业规划行动请求 DTO（POST /profile/career-plans/{planId}/goals/{goalId}/actions）
 * <p>
 * 归属目标经路径参数 goalId 指定，不在请求体中。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerActionAddRequest {

    @NotBlank(message = "行动标题不能为空")
    @Size(max = 255, message = "行动标题长度不能超过255")
    private String actionTitle;

    private String actionDesc;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String endDate;

    /** 排序，默认0 */
    private Integer sort;
}
