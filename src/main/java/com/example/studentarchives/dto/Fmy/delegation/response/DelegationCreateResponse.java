package com.example.studentarchives.dto.Fmy.delegation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建审批委托响应 DTO（POST /teacher/delegations，《教师端接口文档》15.2）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationCreateResponse {

    /** 委托记录 ID（approval_delegations.id） */
    private Long delegationId;

    /** 受托人姓名 */
    private String delegateeName;

    /** 委托状态：0=待生效 1=生效中 */
    private Integer status;

    /** 委托状态中文标签 */
    private String statusLabel;

    /** 委托开始时间（ISO 8601 带时区） */
    private String startTime;

    /** 委托结束时间（ISO 8601 带时区） */
    private String endTime;
}
