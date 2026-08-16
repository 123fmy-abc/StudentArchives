package com.example.studentarchives.dto.Fmy.approvalflow.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新审批流程请求 DTO（PUT /admin/approval-flows/{flowId}，文档 6.4）
 * <p>
 * 仅更新流程模板基础信息，步骤维护使用 6.7（PUT /admin/approval-flows/{flowId}/steps）。
 * 所有字段可选，未传（null）表示不修改。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowUpdateRequest {

    /** 流程名称 */
    @Size(max = 100, message = "flowName 长度不能超过100")
    private String flowName;

    /** 适用类型（已存在审批实例引用的流程禁止修改） */
    @Size(max = 50, message = "applicableType 长度不能超过50")
    private String applicableType;

    /** 适用子类型 */
    @Size(max = 50, message = "applicableSubType 长度不能超过50")
    private String applicableSubType;

    /** 0=非默认 1=默认流程 */
    private Integer isDefault;

    /** 0=禁用 1=启用 */
    private Integer status;
}
