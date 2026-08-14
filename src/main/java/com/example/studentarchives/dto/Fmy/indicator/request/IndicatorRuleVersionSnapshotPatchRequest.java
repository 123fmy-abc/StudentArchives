package com.example.studentarchives.dto.Fmy.indicator.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修补历史指标规则版本快照请求 DTO（PATCH /admin/indicators/rule-versions/{versionId}/snapshot，文档 1.7）
 * <p>
 * 仅允许修改快照中指定指标的元数据字段（名称、说明、编码），
 * 禁止修改 weight、scoringRule、status、parentId、level 等影响评分或树结构的字段。
 * 如需调整权重/计分规则，请使用 {@link IndicatorPublishRequest#getSourceVersionId()} 基于该版本发新版本。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorRuleVersionSnapshotPatchRequest {

    /** 目标指标编码（在快照中定位节点） */
    @NotBlank(message = "indicatorCode 不能为空")
    @Size(max = 50, message = "indicatorCode 长度不能超过50")
    private String indicatorCode;

    /** 新的指标名称，不传表示不修改 */
    @Size(max = 100, message = "indicatorName 长度不能超过100")
    private String indicatorName;

    /** 新的指标说明，不传表示不修改 */
    @Size(max = 500, message = "description 长度不能超过500")
    private String description;

    /** 新的指标编码，不传表示不修改 */
    @Size(max = 50, message = "newIndicatorCode 长度不能超过50")
    private String newIndicatorCode;
}
