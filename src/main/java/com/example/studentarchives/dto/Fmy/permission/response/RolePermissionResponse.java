package com.example.studentarchives.dto.Fmy.permission.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色权限详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionResponse {

    /** 角色 ID */
    private Long roleId;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;

    /** 该角色已拥有的权限 ID 列表 */
    private List<Long> permissionIds;

    /** 该角色已拥有的权限编码列表（便于前端按钮控制） */
    private List<String> permissionCodes;
}
