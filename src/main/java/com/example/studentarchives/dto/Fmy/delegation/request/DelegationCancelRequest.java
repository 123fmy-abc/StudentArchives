package com.example.studentarchives.dto.Fmy.delegation.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消审批委托请求 DTO（PUT /teacher/delegations/{delegationId}/cancel，《教师端接口文档》11/15.3）
 * <p>
 * 仅委托人本人可取消待生效/生效中的委托，取消原因可选但建议填写便于审计追溯。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelegationCancelRequest {

    /** 取消原因（可选） */
    @Size(max = 255, message = "取消原因长度不能超过255")
    private String cancelReason;
}
