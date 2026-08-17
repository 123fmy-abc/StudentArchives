package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热力图学期列 DTO（GET /admin/statistics/heatmap，文档 16.3）
 * <p>
 * 成果热力图按学期展开的列头信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapSemesterItem {

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称 */
    private String semesterName;
}
