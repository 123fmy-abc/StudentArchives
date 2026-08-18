package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.RoleManageService;
import com.example.studentarchives.service.Lzw.RoleManageService.AssignPermissionsRequest;
import com.example.studentarchives.service.Lzw.RoleManageService.PermissionListItem;
import com.example.studentarchives.service.Lzw.RoleManageService.RoleIdResponse;
import com.example.studentarchives.service.Lzw.RoleManageService.RoleListItem;
import com.example.studentarchives.service.Lzw.RoleManageService.RoleListQuery;
import com.example.studentarchives.service.Lzw.RoleManageService.RolePermissionsResponse;
import com.example.studentarchives.service.Lzw.RoleManageService.RoleSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端角色与权限管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》八、角色与权限管理模块（8.1 ~ 8.7）。
 * 权限：该模块「仅管理员可见」，由 Service 层 requireAdmin 校验，越权返回 20005。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleManageService roleManageService;

    // ==================== 8.1 获取角色列表 ====================

    @GetMapping("/roles")
    public ApiResult<PageResult<RoleListItem>> listRoles(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        RoleListQuery query = RoleListQuery.builder().status(status).build();
        return ApiResult.success(roleManageService.listRoles(operatorId, query, buildPageParam(page, perPage)));
    }

    // ==================== 8.2 创建角色 ====================

    @AuditLog(module = "role", action = "create", description = "创建角色: #body.roleCode", relatedType = "role")
    @PostMapping("/roles")
    public ApiResult<RoleIdResponse> createRole(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody RoleSaveRequest body) {
        return ApiResult.success("创建成功", roleManageService.createRole(operatorId, body));
    }

    // ==================== 8.3 更新角色 ====================

    @AuditLog(module = "role", action = "update", description = "更新角色: #roleId", relatedType = "role", relatedId = "#roleId")
    @PutMapping("/roles/{roleId}")
    public ApiResult<Void> updateRole(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long roleId,
            @RequestBody RoleSaveRequest body) {
        roleManageService.updateRole(operatorId, roleId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 8.4 删除角色 ====================

    @AuditLog(module = "role", action = "delete", description = "删除角色: #roleId", relatedType = "role", relatedId = "#roleId")
    @DeleteMapping("/roles/{roleId}")
    public ApiResult<Void> deleteRole(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long roleId) {
        roleManageService.deleteRole(operatorId, roleId);
        return ApiResult.success("删除成功", null);
    }

    // ==================== 8.5 获取角色权限 ====================

    @GetMapping("/roles/{roleId}/permissions")
    public ApiResult<RolePermissionsResponse> getRolePermissions(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long roleId) {
        return ApiResult.success(roleManageService.getRolePermissions(operatorId, roleId));
    }

    // ==================== 8.6 分配角色权限（覆盖式） ====================

    @AuditLog(module = "role", action = "assign-permissions", description = "分配角色权限: #roleId", relatedType = "role", relatedId = "#roleId")
    @PutMapping("/roles/{roleId}/permissions")
    public ApiResult<Void> assignRolePermissions(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long roleId,
            @RequestBody AssignPermissionsRequest body) {
        roleManageService.assignRolePermissions(operatorId, roleId,
                body != null ? body.getPermissionIds() : null);
        return ApiResult.success("权限已更新", null);
    }

    // ==================== 8.7 获取权限码列表 ====================

    @GetMapping("/permissions")
    public ApiResult<List<PermissionListItem>> listPermissions(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(roleManageService.listPermissions(operatorId, module, status));
    }

    private PageParam buildPageParam(int page, int perPage) {
        return PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
    }
}