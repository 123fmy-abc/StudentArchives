package com.example.studentarchives.dto.Fmy.approvalflow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批流程步骤响应 DTO（文档 6.3/6.6/6.7 中 steps 元素）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalFlowStepResponse {

    /** 步骤 ID */
    private Long id;

    /** 第几步，从 1 开始 */
    private Integer stepNo;

    /** 步骤名称 */
    private String stepName;

    /** 该节点要求的角色 ID */
    private Long roleId;

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级 */
    private Integer scopeType;

    /** 范围规则：student_class/student_major 等 */
    private String scopeRule;

    /** 1=自动分配 0=手动 */
    private Integer autoAssign;

    /** 1=允许委托 0=不允许 */
    private Integer allowDelegate;

    /** 1=允许跳过 0=不允许 */
    private Integer allowSkip;

    /** 1=允许指定下一审批人 0=不允许 */
    private Integer allowDesignateNext;

    /** 超时小时数 */
    private Integer timeoutHours;

    /** 退回动作：end/return */
    private String rejectAction;

    /** 退回后退回步骤 */
    private Integer rejectToStep;

    /** 排序 */
    private Integer sort;
}
