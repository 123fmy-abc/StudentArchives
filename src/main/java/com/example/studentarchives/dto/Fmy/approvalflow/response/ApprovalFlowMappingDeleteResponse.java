package com.example.studentarchives.dto.Fmy.approvalflow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除审批流程映射响应 DTO（DELETE /admin/approval-flow-mappings/{mappingId}，文档 6.10）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalFlowMappingDeleteResponse {

    /** 映射 ID */
    private Long id;

    /** 软删除时间（ISO 8601 带时区） */
    private String deletedAt;
}
