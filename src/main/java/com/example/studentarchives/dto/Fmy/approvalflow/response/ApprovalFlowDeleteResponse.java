package com.example.studentarchives.dto.Fmy.approvalflow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除审批流程响应 DTO（DELETE /admin/approval-flows/{flowId}，文档 6.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalFlowDeleteResponse {

    /** 流程 ID */
    private Long id;

    /** 软删除时间（ISO 8601 带时区） */
    private String deletedAt;
}
