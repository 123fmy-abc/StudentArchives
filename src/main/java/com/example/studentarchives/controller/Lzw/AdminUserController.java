package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.UserManageService;
import com.example.studentarchives.service.Lzw.UserManageService.CreateUserRequest;
import com.example.studentarchives.service.Lzw.UserManageService.CreateUserResponse;
import com.example.studentarchives.service.Lzw.UserManageService.ResetPasswordRequest;
import com.example.studentarchives.service.Lzw.UserManageService.UpdateRolesRequest;
import com.example.studentarchives.service.Lzw.UserManageService.UpdateScopesRequest;
import com.example.studentarchives.service.Lzw.UserManageService.UpdateStatusRequest;
import com.example.studentarchives.service.Lzw.UserManageService.UpdateUserRequest;
import com.example.studentarchives.service.Lzw.UserManageService.UserDetail;
import com.example.studentarchives.service.Lzw.UserManageService.UserListItem;
import com.example.studentarchives.service.Lzw.UserManageService.UserListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》六、用户管理模块（6.1 ~ 6.8）。
 * 权限：读接口需 admin 角色或 user:view / user:manage，写接口需 admin 角色或 user:manage。
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManageService userManageService;

    // ==================== 6.1 获取用户列表 ====================

    @GetMapping
    public ApiResult<PageResult<UserListItem>> listUsers(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        UserListQuery query = UserListQuery.builder()
                .roleId(roleId)
                .status(status)
                .grade(grade)
                .keyword(keyword)
                .schoolId(schoolId)
                .build();
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(userManageService.listUsers(operatorId, query, pageParam));
    }

    // ==================== 6.2 获取用户详情 ====================

    @GetMapping("/{userId}")
    public ApiResult<UserDetail> getDetail(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId) {
        return ApiResult.success(userManageService.getDetail(operatorId, userId));
    }

    // ==================== 6.3 创建用户 ====================

    @AuditLog(module = "user", action = "create", description = "创建用户: #body.userNo", relatedType = "user")
    @PostMapping
    public ApiResult<CreateUserResponse> createUser(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody CreateUserRequest body) {
        return ApiResult.success("创建成功", userManageService.createUser(operatorId, body));
    }

    // ==================== 6.4 更新用户信息 ====================

    @AuditLog(module = "user", action = "update", description = "更新用户信息: #userId", relatedType = "user", relatedId = "#userId")
    @PutMapping("/{userId}")
    public ApiResult<Void> updateUser(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest body) {
        userManageService.updateUser(operatorId, userId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 6.5 启用/禁用用户 ====================

    @AuditLog(module = "user", action = "update-status", description = "启用/禁用用户: #userId → status=#body.status", relatedType = "user", relatedId = "#userId")
    @PutMapping("/{userId}/status")
    public ApiResult<Void> updateStatus(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody UpdateStatusRequest body) {
        userManageService.updateStatus(operatorId, userId, body.getStatus());
        return ApiResult.success("操作成功", null);
    }

    // ==================== 6.6 重置用户密码 ====================

    @AuditLog(module = "user", action = "reset-password", description = "重置用户密码: #userId", logParams = false, relatedType = "user", relatedId = "#userId")
    @PutMapping("/{userId}/password/reset")
    public ApiResult<Void> resetPassword(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody ResetPasswordRequest body) {
        userManageService.resetPassword(operatorId, userId, body.getNewPassword());
        return ApiResult.success("密码已重置", null);
    }

    // ==================== 6.7 分配用户角色（覆盖式） ====================

    @AuditLog(module = "user", action = "update-roles", description = "分配用户角色: #userId", relatedType = "user", relatedId = "#userId")
    @PutMapping("/{userId}/roles")
    public ApiResult<Void> updateRoles(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody UpdateRolesRequest body) {
        userManageService.updateRoles(operatorId, userId, body.getRoleIds());
        return ApiResult.success("角色已更新", null);
    }

    // ==================== 6.8 配置教师数据范围（覆盖式） ====================

    @AuditLog(module = "user", action = "update-scopes", description = "配置教师数据范围: #userId", relatedType = "user", relatedId = "#userId")
    @PutMapping("/{userId}/scopes")
    public ApiResult<Void> updateScopes(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody UpdateScopesRequest body) {
        userManageService.updateScopes(operatorId, userId, body.getScopes());
        return ApiResult.success("数据范围已更新", null);
    }
}