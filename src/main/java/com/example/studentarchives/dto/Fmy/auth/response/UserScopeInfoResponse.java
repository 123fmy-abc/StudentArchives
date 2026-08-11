package com.example.studentarchives.dto.Fmy.auth.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户数据范围信息响应 DTO
 * <p>
 * 用于 /auth/me 接口，返回教师/辅导员在 role_scopes 表中的授权范围
 * （学校/学院/专业/班级/课程/年级）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScopeInfoResponse {

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级 */
    private Integer scopeType;

    /** 范围类型标签 */
    private String scopeTypeLabel;

    /** 范围 ID */
    private Long scopeId;

    /** 范围名称（学校/学院/专业/班级/课程名） */
    private String scopeName;

    /** 学期 ID（可空） */
    private Long semesterId;
}
