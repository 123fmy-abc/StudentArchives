package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据完整度响应 DTO（GET /profile/data-completeness）
 * <p>
 * 数据来源：data_completeness 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataCompletenessResponse {

    private Long semesterId;

    /** 综合完整度 0-100 */
    private Integer overallRate;

    /** 各维度完整度 */
    private List<CompletenessItem> dimensions;

    /** 最近更新时间（ISO 8601 带时区） */
    private String updatedAt;

    /**
     * 维度完整度项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletenessItem {

        private String dimensionCode;

        private String dimensionName;

        /** 完整度 0-100 */
        private Integer rate;

        /** 缺失项列表 */
        private List<String> missingItems;
    }
}
