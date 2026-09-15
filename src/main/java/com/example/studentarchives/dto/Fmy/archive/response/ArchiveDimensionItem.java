package com.example.studentarchives.dto.Fmy.archive.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单个能力维度的画像得分项（GET /archive/dimensions，《学生端接口文档》4.1.7）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveDimensionItem {

    /** 维度编码（ability_dimensions.dimension_code） */
    private String dimensionCode;

    /** 维度名称（ability_dimensions.dimension_name） */
    private String dimensionName;

    /** 维度说明（可为空） */
    private String description;

    /** 该维度累计原始分（已通过成长事件下 growth_timeline_abilities.score 之和），无得分为 0 */
    private BigDecimal score;

    /** 该维度占总分比例，保留 4 位小数；总分为 0 时为 0 */
    private BigDecimal ratio;

    /** 维度排序号（与维度字典一致） */
    private Integer sort;
}
