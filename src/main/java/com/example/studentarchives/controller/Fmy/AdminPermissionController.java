package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.permission.request.RolePermissionUpdateRequest;
import com.example.studentarchives.dto.Fmy.permission.request.UserRoleUpdateRequest;
import com.example.studentarchives.dto.Fmy.permission.response.PermissionItemResponse;
import com.example.studentarchives.dto.Fmy.permission.response.RolePermissionResponse;
import com.example.studentarchives.dto.Fmy.permission.response.UserScopeResponse;
import com.example.studentarchives.service.Fmy.AdminPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端权限聚合控制器
 * <p>
 * 修复“权限管理入口分散”风险：将权限码、角色权限、用户角色、用户数据范围等
 * 分散接口统一收敛到 {@code /admin/permissions} 前缀下，供前端“管理权限”菜单
 * 一站式调用。
 * <p>
 * 权限边界：所有接口要求当前用户具备 admin 角色或对应权限码，
 * 由 {@link com.example.studentarchives.service.Fmy.AdminPermissionService} 统一校验。
 */
@Slf4j
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    // ==================== 权限码管理 ====================

    /**
     * 获取全部启用的权限码
     * <p>
     * 前端“管理权限”页面用于展示权限树，按 code 控制按钮显隐。
     */
    @GetMapping("/list")
    public ApiResult<List<PermissionItemResponse>> listPermissions(
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(adminPermissionService.listPermissions(userId));
    }

    // ==================== 角色权限管理 ====================

    /**
     * 获取指定角色的权限
     */
    @GetMapping("/roles/{roleId}")
    public ApiResult<RolePermissionResponse> getRolePermissions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roleId) {
        return ApiResult.success(adminPermissionService.getRolePermissions(userId, roleId));
    }

    /**
     * 更新角色权限（全量覆盖）
     */
    @AuditLog(module = "permission", action = "update_role_permission",
            description = "更新角色权限: roleId=#request.roleId", logParams = false, logResult = false)
    @PutMapping("/roles")
    public ApiResult<Void> updateRolePermissions(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RolePermissionUpdateRequest request) {
        adminPermissionService.updateRolePermissions(userId, request);
        return ApiResult.success("角色权限更新成功", null);
    }

    // ==================== 用户角色管理 ====================

    /**
     * 获取指定用户的角色 ID 列表
     */
    @GetMapping("/users/{targetUserId}/roles")
    public ApiResult<List<Long>> getUserRoles(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetUserId) {
        return ApiResult.success(adminPermissionService.getUserRoles(userId, targetUserId));
    }

    /**
     * 更新用户角色（全量覆盖）
     */
    @AuditLog(module = "permission", action = "update_user_role",
            description = "更新用户角色: userId=#request.userId", logParams = false, logResult = false)
    @PutMapping("/users/roles")
    public ApiResult<Void> updateUserRoles(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        adminPermissionService.updateUserRoles(userId, request);
        return ApiResult.success("用户角色更新成功", null);
    }

    // ==================== 用户数据范围 ====================

    /**
     * 获取指定用户的数据范围
     * <p>
     * 教师/辅导员可查看的学院/专业/班级范围，用于图片中“选择学院→选择专业→选择班级”
     * 等档案查看/导出/审核范围的过滤条件。
     */
    @GetMapping("/users/{targetUserId}/scopes")
    public ApiResult<List<UserScopeResponse>> getUserScopes(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetUserId) {
        return ApiResult.success(adminPermissionService.getUserScopes(userId, targetUserId));
    }
}
