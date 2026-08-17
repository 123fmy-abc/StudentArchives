package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 档案类型计数 DTO（统计看板/组织下钻共用）
 * <p>
 * 档案类型分布项，archiveType 为档案类型编码（如 academic_competition），
 * count 为该类型档案数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsTypeCountItem {

    /** 档案类型编码 */
    private String archiveType;

    /** 数量 */
    private Integer count;
}
