package com.example.studentarchives.dto.Fmy.delegation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消审批委托响应 DTO（PUT /teacher/delegations/{delegationId}/cancel，《教师端接口文档》15.3）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationCancelResponse {

    /** 委托记录 ID（approval_delegations.id） */
    private Long delegationId;

    /** 取消后状态：固定为 3=已取消 */
    private Integer status;

    /** 取消后状态中文标签 */
    private String statusLabel;

    /** 取消时间（ISO 8601 带时区） */
    private String cancelledAt;
}
