package com.example.studentarchives.dto.Fmy.permission.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色权限更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionUpdateRequest {

    /** 角色 ID */
    @NotNull(message = "roleId 不能为空")
    private Long roleId;

    /** 权限 ID 列表（全量覆盖） */
    @NotNull(message = "permissionIds 不能为空，如需清空请传空数组")
    private List<Long> permissionIds;
}
