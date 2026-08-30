package com.example.studentarchives.dto.Fmy.delegation.response;

import com.example.studentarchives.common.PageResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审批委托列表响应 DTO（GET /teacher/delegations，《教师端接口文档》15.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationListResponse {

    /** 委托记录列表 */
    private List<DelegationItem> list;

    /** 分页信息 */
    private PageResult.Pagination pagination;
}
