package com.example.studentarchives.dto.Fmy.approvalflow.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建/更新审批流程映射请求 DTO（POST /admin/approval-flow-mappings，文档 6.9）
 * <p>
 * 若传入 id 则更新，否则创建。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowMappingUpsertRequest {

    /** 映射 ID，更新时必填 */
    private Long id;

    /** 业务类型 */
    @NotBlank(message = "businessType 不能为空")
    @Size(max = 50, message = "businessType 长度不能超过50")
    private String businessType;

    /** 业务子类型（NULL=通用） */
    @Size(max = 50, message = "businessSubType 长度不能超过50")
    private String businessSubType;

    /** 关联流程 ID */
    @NotNull(message = "flowId 不能为空")
    private Long flowId;

    /** 0=非默认 1=默认流程，默认0 */
    private Integer isDefault;

    /** 生效开始时间，ISO 8601，NULL=立即生效 */
    private String effectiveStart;

    /** 生效结束时间，ISO 8601，NULL=长期有效 */
    private String effectiveEnd;

    /** 优先级，默认0，数值越大越优先 */
    private Integer priority;
}
