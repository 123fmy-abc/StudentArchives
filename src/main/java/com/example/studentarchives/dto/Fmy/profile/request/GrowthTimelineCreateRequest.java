package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新增成长时间轴事件请求 DTO（POST /profile/growth-timeline）
 * <p>
 * 对应表 growth_timelines、growth_timeline_abilities、growth_timeline_tags。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthTimelineCreateRequest {

    /** 学期 ID（growth_timelines.semester_id，可空） */
    private Long semesterId;

    /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
    @NotNull(message = "事件类型不能为空")
    @Min(value = 1, message = "事件类型只能是1-6")
    @Max(value = 6, message = "事件类型只能是1-6")
    private Integer eventType;

    /** 事件名称 */
    @NotBlank(message = "事件名称不能为空")
    @Size(max = 255, message = "事件名称长度不能超过255")
    private String eventName;

    /** 事件详细描述/富文本（可空） */
    private String content;

    /** 封面图片 URL（可空） */
    @Size(max = 500, message = "封面图片URL长度不能超过500")
    private String coverImage;

    /** 发生时间，格式 YYYY-MM-DD */
    @NotBlank(message = "发生时间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String eventAt;

    /** 审核状态：0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销，默认 0 */
    @Min(value = 0, message = "状态只能是0-4")
    @Max(value = 4, message = "状态只能是0-4")
    private Integer status;

    /** 来源记录 ID（可空，与 sourceType 配对，见条件唯一索引 uk_gt_source） */
    private Long sourceId;

    /** 来源模型类型（可空，如 archive / award_application） */
    @Size(max = 100, message = "来源模型类型长度不能超过100")
    private String sourceType;

    /** 业务去重键（可空，见条件唯一索引 uk_gt_event_key） */
    @Size(max = 64, message = "事件去重键长度不能超过64")
    private String eventKey;

    /** 能力维度得分列表（可空） */
    @Valid
    private List<GrowthTimelineAbilityItem> abilityData;

    /** 事件标签列表（可空，自动去重） */
    private List<String> tags;
}
