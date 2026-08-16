package com.example.studentarchives.dto.Fmy.approvalflow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 保存审批流程步骤响应 DTO（PUT /admin/approval-flows/{flowId}/steps，文档 6.7）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalFlowStepsResponse {

    /** 流程 ID */
    private Long flowId;

    /** 保存后的步骤列表（按 stepNo 升序） */
    private List<ApprovalFlowStepResponse> steps;
}
