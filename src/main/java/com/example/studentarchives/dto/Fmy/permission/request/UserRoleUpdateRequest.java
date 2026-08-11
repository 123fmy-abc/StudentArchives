package com.example.studentarchives.dto.Fmy.permission.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户角色更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {

    /** 目标用户 ID */
    @NotNull(message = "userId 不能为空")
    private Long userId;

    /** 角色 ID 列表（全量覆盖） */
    @NotNull(message = "roleIds 不能为空，如需清空请传空数组")
    private List<Long> roleIds;
}
