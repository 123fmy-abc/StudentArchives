package com.example.studentarchives.dto.Fmy.indicator.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布指标规则版本请求 DTO（POST /admin/indicators/publish，文档 1.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorPublishRequest {

    /** 学校 ID */
    @NotNull(message = "schoolId 不能为空")
    private Long schoolId;

    /** 发布版本归属学期 ID（可选，不传则取该校当前学期，仍无则版本不限定学期） */
    private Long semesterId;

    /** 版本名称，如 "2026春-第1版"（indicator_rule_versions.version_name） */
    @NotBlank(message = "versionName 不能为空")
    @Size(max = 100, message = "versionName 长度不能超过100")
    private String versionName;

    /** 生效时间，ISO 8601 格式，如 2026-02-01T00:00:00+08:00，不传则取当前时间 */
    private String effectiveAt;

    /**
     * 基于指定历史版本快照发布新版本（可选）。
     * <p>
     * 传入时，系统从该历史版本的 {@code indicator_rule_versions.tree_snapshot} 深拷贝生成新版本的
     * 指标树快照与 {@code indicator_versions} 记录，而不是从当前草稿树 ({@code evaluation_indicators})
     * 发布。这样可避免连带引入草稿中其他未预期改动，也支持对历史版本做修正后重新发布。
     * 不传时保持原有行为：基于当前全校草稿树发布。
     */
    private Long sourceVersionId;
}
