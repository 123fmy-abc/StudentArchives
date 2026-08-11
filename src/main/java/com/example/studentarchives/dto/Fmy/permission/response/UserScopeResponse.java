package com.example.studentarchives.dto.Fmy.permission.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户数据范围响应
 * <p>
 * 对应 role_scopes 表：教师/辅导员可查看的学院/专业/班级范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScopeResponse {

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;

    /** 角色名称 */
    private String roleName;

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 */
    private Integer scopeType;

    /** 范围类型标签 */
    private String scopeTypeLabel;

    /** 范围 ID */
    private Long scopeId;

    /** 范围名称（学院/专业/班级名） */
    private String scopeName;

    /** 是否主范围 */
    private Integer isPrimary;

    /** 有效期开始 */
    private LocalDate validFrom;

    /** 有效期结束 */
    private LocalDate validUntil;

    /** 状态：0=禁用 1=启用 */
    private Integer status;
}
