package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成果热力图行 DTO（GET /admin/statistics/heatmap，文档 16.3）
 * <p>
 * 单个组织单位在不同学期的指标值矩阵。values 为归一化（0-100）后的数值，
 * rawValues 为原始值，total 为该组织所有学期原始值合计。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapRow {

    /** 组织 ID */
    private Long orgId;

    /** 组织名称 */
    private String orgName;

    /** 各学期归一化数值（0-100，单学期为空返回 0） */
    private List<Integer> values;

    /** 各学期原始值 */
    private List<Integer> rawValues;

    /** 所有学期原始值合计 */
    private Integer total;
}
