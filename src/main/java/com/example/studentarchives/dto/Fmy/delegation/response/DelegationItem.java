package com.example.studentarchives.dto.Fmy.delegation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批委托列表项 DTO（GET /teacher/delegations，《教师端接口文档》15.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationItem {

    /** 委托记录 ID（approval_delegations.id） */
    private Long delegationId;

    /** 委托人信息 */
    private UserBrief delegator;

    /** 受托人信息 */
    private UserBrief delegatee;

    /** 委托角色范围 */
    private RoleBrief role;

    /** 委托范围类型：2=学院 3=专业 4=班级（null=全部范围） */
    private Integer scopeType;

    /** 委托范围类型中文标签 */
    private String scopeTypeLabel;

    /** 委托范围 ID（null=全部范围） */
    private Long scopeId;

    /** 委托范围名称（班级/专业/学院名称） */
    private String scopeName;

    /** 委托开始时间（ISO 8601 带时区） */
    private String startTime;

    /** 委托结束时间（ISO 8601 带时区） */
    private String endTime;

    /** 委托原因 */
    private String reason;

    /** 委托状态：0=待生效 1=生效中 2=已过期 3=已取消 */
    private Integer status;

    /** 委托状态中文标签 */
    private String statusLabel;

    /** 委托创建时间（ISO 8601 带时区） */
    private String createdAt;

    /**
     * 用户简信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBrief {

        /** 用户 ID */
        private Long userId;

        /** 姓名 */
        private String name;

        /** 工号/学号 */
        private String userNo;
    }

    /**
     * 角色简信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleBrief {

        /** 角色 ID */
        private Long roleId;

        /** 角色名称 */
        private String roleName;
    }
}
