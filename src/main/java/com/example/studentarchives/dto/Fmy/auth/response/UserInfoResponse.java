package com.example.studentarchives.dto.Fmy.auth.response;

import com.example.studentarchives.annotation.Sensitive;
import com.example.studentarchives.enums.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户信息响应 DTO
 * <p>
 * 用于 /auth/me 接口，比登录返回的 user 对象多了 phone 和 permissions。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long userId;
    private String userNo;
    private String name;
    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Sensitive(SensitiveType.PHONE)
    private String phone;
    private Integer gender;
    private String genderLabel;
    private Long schoolId;
    private String schoolName;

    /**
     * 角色编码数组，按权限从高到低排序（roles[0] 恒为最高权限角色）。
     * 口径：仅含启用中（roles.status=1）的角色，禁用角色不再返回。
     */
    private List<String> roles;

    /** 角色名称数组，与 {@link #roles} 一一对应同序 */
    private List<String> roleNames;

    /**
     * 最高权限角色的层级（roles.level 最小值，见 RoleLevelEnum）：
     * 0=系统(admin) 1=学生 2=教师 3=辅导员 4=系主任 5=院长 6=校长 7=自定义。
     * 无角色时为 null。
     */
    private Integer roleLevel;

    /** {@link #roleLevel} 的中文标签（如「学生」「教师」），无角色时为 null */
    private String roleLevelLabel;

    /**
     * 首页路由建议值（由 {@link #roleLevel} 推导）：
     * 0 → {@code "admin"} 管理端首页；1 → {@code "student"} 学生端首页；
     * 其余 → {@code "teacher"} 教师端首页；无角色时为 null。
     */
    private String homePage;

    private List<String> permissions;
    private List<UserScopeInfoResponse> scopes;
    private String avatar;
}
