package com.example.studentarchives.dto.Fmy.approvalflow.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 保存审批流程步骤请求 DTO（PUT /admin/approval-flows/{flowId}/steps，文档 6.7）
 * <p>
 * 完整步骤列表，全量覆盖：后端按 step_no 匹配，已存在则更新、缺失的旧步骤软删除、新步骤插入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowStepsRequest {

    /** 完整步骤列表（全量覆盖） */
    @NotNull(message = "steps 不能为空")
    @Valid
    private List<ApprovalFlowStepItem> steps;
}
