package com.example.studentarchives.dto.Fmy.archive.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生多维度能力画像响应 DTO（GET /archive/dimensions，《学生端接口文档》4.1.7）
 * <p>
 * 口径：各维度累计原始分（{@code SUM(growth_timeline_abilities.score)}），
 * 只统计 status=2（已通过）的成长时间轴事件；不做归一化，不按事件类型拆分。
 * <p>
 * 维度字典为空或该生暂无得分时，返回 {@code dimensions: []} / {@code totalScore: 0}，
 * 不抛异常（不要用异常表达「没数据」）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveDimensionResponse {

    /** 学生用户 ID（当前登录人本人） */
    private Long userId;

    /** 生效的学期筛选，为 null 表示全部学期累计 */
    private Long semesterId;

    /** 各维度得分之和 */
    private BigDecimal totalScore;

    /** 维度得分列表（按维度字典 sort 升序，含得分为 0 的维度） */
    private List<ArchiveDimensionItem> dimensions;
}
