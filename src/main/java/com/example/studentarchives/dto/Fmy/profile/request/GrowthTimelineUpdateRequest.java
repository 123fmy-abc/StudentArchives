package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 修改成长时间轴事件请求 DTO（PUT /profile/growth-timeline/{id}）
 * <p>
 * 全部字段可选，仅更新传入的非空字段；content / coverImage 传空字符串表示清空。
 * abilityData / tags 传入（非 null，含空数组）时整体替换子表数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthTimelineUpdateRequest {

    /** 学期 ID，不传保留原值 */
    private Long semesterId;

    /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
    @Min(value = 1, message = "事件类型只能是1-6")
    @Max(value = 6, message = "事件类型只能是1-6")
    private Integer eventType;

    /** 事件名称，不传保留原值 */
    @Size(max = 255, message = "事件名称长度不能超过255")
    private String eventName;

    /** 事件详细描述，不传保留原值，传空字符串清空 */
    private String content;

    /** 封面图片 URL，不传保留原值，传空字符串清空 */
    @Size(max = 500, message = "封面图片URL长度不能超过500")
    private String coverImage;

    /** 发生时间，格式 YYYY-MM-DD，不传保留原值 */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String eventAt;

    /** 审核状态：0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销，不传保留原值 */
    @Min(value = 0, message = "状态只能是0-4")
    @Max(value = 4, message = "状态只能是0-4")
    private Integer status;

    /** 来源记录 ID，不传保留原值，与 sourceType 配对 */
    private Long sourceId;

    /** 来源模型类型，不传保留原值，传空字符串清空（同时清空 sourceId） */
    @Size(max = 100, message = "来源模型类型长度不能超过100")
    private String sourceType;

    /** 业务去重键，不传保留原值，传空字符串清空 */
    @Size(max = 64, message = "事件去重键长度不能超过64")
    private String eventKey;

    /** 能力维度得分列表，传入（含空数组）时整体替换 */
    @Valid
    private List<GrowthTimelineAbilityItem> abilityData;

    /** 事件标签列表，传入（含空数组）时整体替换 */
    private List<String> tags;
}
