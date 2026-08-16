package com.example.studentarchives.dto.Fmy.approvalflow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批流程列表项 DTO（GET /admin/approval-flows，文档 6.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalFlowItem {

    /** 流程 ID */
    private Long id;

    /** 学校 ID */
    private Long schoolId;

    /** 流程名称 */
    private String flowName;

    /** 适用类型 */
    private String applicableType;

    /** 适用子类型 */
    private String applicableSubType;

    /** 流程版本号 */
    private Integer version;

    /** 0=非默认 1=默认流程 */
    private Integer isDefault;

    /** 0=禁用 1=启用 */
    private Integer status;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
