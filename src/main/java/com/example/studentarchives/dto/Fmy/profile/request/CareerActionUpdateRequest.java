package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新职业规划行动请求 DTO（PUT /profile/career-plans/{planId}/actions/{actionId}）
 * <p>
 * 部分更新语义：actionTitle 必填，其余字段不传（null）保留原值，传空字符串清空文本字段。
 * 状态仍走 4.12 更新行动状态接口，本接口不接收 status。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerActionUpdateRequest {

    @NotBlank(message = "行动标题不能为空")
    @Size(max = 255, message = "行动标题长度不能超过255")
    private String actionTitle;

    /** 行动描述，不传保留原值，传空字符串清空 */
    private String actionDesc;

    /** 开始日期，格式 YYYY-MM-DD；不传保留原值，传空字符串清空 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String startDate;

    /** 结束日期，格式 YYYY-MM-DD；不传保留原值，传空字符串清空 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String endDate;

    /** 排序，不传保留原值 */
    private Integer sort;
}
