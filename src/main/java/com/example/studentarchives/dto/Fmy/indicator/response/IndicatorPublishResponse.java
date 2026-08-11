package com.example.studentarchives.dto.Fmy.indicator.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布指标规则版本响应 DTO（POST /admin/indicators/publish，文档 1.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorPublishResponse {

    /** 新发布的全局规则版本号 */
    private Integer version;

    /** 发布时间（ISO 8601 带时区） */
    private String createdAt;
}
