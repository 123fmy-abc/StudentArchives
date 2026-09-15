package com.example.studentarchives.dto.Fmy.delegation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可委托教师列表项 DTO（GET /teacher/delegations/candidates，《教师端接口文档》15.4）
 * <p>
 * 供委托页「被委托人」下拉使用：同校、持有可审批角色（is_auditor=1）且启用中的教师，
 * 不含当前登录人本人。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationCandidateItem {

    /** 教师用户 ID（作为 POST /teacher/delegations 的 delegateeId） */
    private Long userId;

    /** 姓名 */
    private String name;

    /** 工号 */
    private String userNo;

    /** 职称（teacher_profiles.title，缺失返回 null） */
    private String title;

    /** 所属学院 ID（teacher_profiles.college_id，缺失返回 null） */
    private Long collegeId;

    /** 所属学院名称（college_id 为空或学院不存在时返回 null） */
    private String collegeName;

    /** 持有的可审批角色名称，如 ["教师","辅导员"] */
    private List<String> roleNames;
}
