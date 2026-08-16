package com.example.studentarchives.dto.Fmy.approvalflow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建/更新审批流程映射响应 DTO（POST /admin/approval-flow-mappings，文档 6.9）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalFlowMappingResponse {

    /** 映射 ID */
    private Long id;

    /** 学校 ID */
    private Long schoolId;

    /** 业务类型 */
    private String businessType;

    /** 业务子类型 */
    private String businessSubType;

    /** 关联流程 ID */
    private Long flowId;

    /** 关联流程名称（冗余回显） */
    private String flowName;

    /** 0=非默认 1=默认流程 */
    private Integer isDefault;

    /** 生效开始时间（ISO 8601 带时区），NULL=立即生效 */
    private String effectiveStart;

    /** 生效结束时间（ISO 8601 带时区），NULL=长期有效 */
    private String effectiveEnd;

    /** 优先级，数值越大越优先 */
    private Integer priority;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
