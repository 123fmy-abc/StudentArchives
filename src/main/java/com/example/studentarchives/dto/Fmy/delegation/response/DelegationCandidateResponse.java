package com.example.studentarchives.dto.Fmy.delegation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可委托教师列表响应 DTO（GET /teacher/delegations/candidates，《教师端接口文档》15.4）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationCandidateResponse {

    /** 可委托教师列表（按姓名排序） */
    private List<DelegationCandidateItem> list;

    /** 命中条数 */
    private Integer total;
}
