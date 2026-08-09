package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新职业规划里程碑请求 DTO（PUT /profile/career-plans/{planId}/milestones/{milestoneId}）
 * <p>
 * 部分更新语义：milestoneTitle 必填，其余字段不传（null）保留原值，传空字符串清空文本字段。
 * isAchieved=1 时写入 achieved_at，回退为 0 时清空。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerMilestoneUpdateRequest {

    @NotBlank(message = "里程碑标题不能为空")
    @Size(max = 255, message = "里程碑标题长度不能超过255")
    private String milestoneTitle;

    /** 计划完成日期，格式 YYYY-MM-DD；不传保留原值，传空字符串清空 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String milestoneDate;

    /** 达成状态 0=未完成 1=已完成；不传保留原值 */
    @Min(value = 0, message = "达成状态只能为0或1")
    @Max(value = 1, message = "达成状态只能为0或1")
    private Integer isAchieved;

    /**
     * 成果证明材料（关联 file_uploads.id）：不传保留原值；
     * 传 0 清空解绑（同时软删原证明文件）；传具体 fileId 重新绑定。
     */
    @Min(value = 0, message = "成果证明文件ID不合法")
    private Long proofFileId;
}
