package com.example.studentarchives.dto.Fmy.approvalflow.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批流程步骤项 DTO（文档 6.7 步骤项字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowStepItem {

    /** 第几步，从 1 开始 */
    @NotNull(message = "stepNo 不能为空")
    @Min(value = 1, message = "stepNo 最小为 1")
    private Integer stepNo;

    /** 步骤名称 */
    @NotBlank(message = "stepName 不能为空")
    @Size(max = 100, message = "stepName 长度不能超过100")
    private String stepName;

    /** 该节点要求的角色 ID */
    @NotNull(message = "roleId 不能为空")
    private Long roleId;

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级 */
    @NotNull(message = "scopeType 不能为空")
    @Min(value = 1, message = "scopeType 范围为 1-6")
    @Max(value = 6, message = "scopeType 范围为 1-6")
    private Integer scopeType;

    /** 范围规则：student_class/student_major 等 */
    @NotBlank(message = "scopeRule 不能为空")
    @Size(max = 50, message = "scopeRule 长度不能超过50")
    private String scopeRule;

    /** 1=自动分配 0=手动，默认1 */
    private Integer autoAssign;

    /** 1=允许委托，默认0 */
    private Integer allowDelegate;

    /** 1=允许跳过，默认0 */
    private Integer allowSkip;

    /** 1=允许指定下一审批人，默认0 */
    private Integer allowDesignateNext;

    /** 超时小时数，默认48 */
    @Min(value = 1, message = "timeoutHours 最小为 1")
    private Integer timeoutHours;

    /** 退回动作：end/return，默认 end */
    @Size(max = 20, message = "rejectAction 长度不能超过20")
    private String rejectAction;

    /** 退回后退回步骤，rejectAction=return 时必填 */
    private Integer rejectToStep;

    /** 排序，默认0 */
    private Integer sort;
}
