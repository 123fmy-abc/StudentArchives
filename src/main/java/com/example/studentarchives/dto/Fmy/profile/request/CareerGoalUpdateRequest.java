package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新职业规划目标请求 DTO（PUT /profile/career-plans/{planId}/goals/{goalId}）
 * <p>
 * 部分更新语义：goalTitle 必填，其余字段不传（null）保留原值，传空字符串清空文本字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerGoalUpdateRequest {

    @NotBlank(message = "目标标题不能为空")
    @Size(max = 255, message = "目标标题长度不能超过255")
    private String goalTitle;

    /** 目标描述，不传保留原值，传空字符串清空 */
    private String goalDesc;

    /** 目标日期，格式 YYYY-MM-DD；不传保留原值，传空字符串清空 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String targetDate;

    /** 排序，不传保留原值 */
    private Integer sort;
}
