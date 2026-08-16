package com.example.studentarchives.dto.Fmy.approvalflow.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建审批流程请求 DTO（POST /admin/approval-flows，文档 6.2）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowCreateRequest {

    /** 流程名称 */
    @NotBlank(message = "flowName 不能为空")
    @Size(max = 100, message = "flowName 长度不能超过100")
    private String flowName;

    /** 适用类型：Archive/AwardApplication/CareerPlan/GrowthTimeline/Announcement */
    @NotBlank(message = "applicableType 不能为空")
    @Size(max = 50, message = "applicableType 长度不能超过50")
    private String applicableType;

    /** 适用子类型（NULL=通用） */
    @Size(max = 50, message = "applicableSubType 长度不能超过50")
    private String applicableSubType;

    /** 0=非默认 1=默认流程，默认0 */
    private Integer isDefault;

    /** 0=禁用 1=启用，默认1 */
    private Integer status;

    /** 初始步骤列表（可选，结构见 6.7 步骤项字段） */
    @Valid
    private List<ApprovalFlowStepItem> steps;
}
