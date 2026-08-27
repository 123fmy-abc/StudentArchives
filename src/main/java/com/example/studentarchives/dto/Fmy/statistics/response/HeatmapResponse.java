package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成果热力图数据 DTO（GET /admin/statistics/heatmap，文档 16.3）
 * <p>
 * 以组织单位为行、指标/学期为列的矩阵。maxValue / minValue 为全校该指标
 * 原始值最大/最小值，用于热力图着色刻度。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse {

    /** 指标：gpa / award / practice / interest / archive */
    private String metric;

    /** 年级（如 {@code 2024级}；grade 未传时由服务端推导回显，未推导为 null） */
    private String grade;

    /** 学期列 */
    private List<HeatmapSemesterItem> semesters;

    /** 组织行 */
    private List<HeatmapRow> rows;

    /** 全校该指标原始值最大值 */
    private Integer maxValue;

    /** 全校该指标原始值最小值 */
    private Integer minValue;

    /** 缓存命中标记 */
    private Boolean cacheHit;
}
