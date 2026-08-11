package com.example.studentarchives.dto.Fmy.indicator.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标规则版本列表项 DTO（GET /admin/indicators/rule-versions，文档 1.6）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorRuleVersionItem {

    /** 规则版本 ID（indicator_rule_versions.id） */
    private Long id;

    /** 全局规则版本号 */
    private Integer version;

    /** 版本名称 */
    private String versionName;

    /** 发布版本归属学期 ID（semesters.id，null=不限定学期） */
    private Long semesterId;

    /** 生效时间（ISO 8601 带时区） */
    private String effectiveAt;

    /** 创建人 ID */
    private Long createdBy;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
