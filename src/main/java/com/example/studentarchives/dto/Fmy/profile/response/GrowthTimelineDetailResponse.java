package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成长时间轴事件详情响应 DTO（GET/POST/PUT /profile/growth-timeline[/{id}]）
 * <p>
 * 数据来源：growth_timelines、growth_timeline_abilities、growth_timeline_tags。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrowthTimelineDetailResponse {

    /** 时间轴节点 ID */
    private Long id;

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称，如 "2023-2024-2" */
    private String semesterName;

    /** 事件日期，如 "2025-07-05" */
    private String eventAt;

    /** 事件名称 */
    private String eventName;

    /** 事件详细描述/富文本 */
    private String content;

    /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
    private Integer eventType;

    private String eventTypeLabel;

    /** 审核状态：0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销 */
    private Integer status;

    private String statusLabel;

    /** 封面图片 URL */
    private String coverImage;

    /** 来源记录 ID */
    private Long sourceId;

    /** 来源模型类型 */
    private String sourceType;

    /** 业务去重键 */
    private String eventKey;

    /** 能力得分数据 */
    private List<AbilityItem> abilityData;

    /** 标签列表 */
    private List<String> tags;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;

    /**
     * 能力得分项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityItem {

        private String dimensionCode;

        private String dimensionName;

        private BigDecimal score;
    }
}
