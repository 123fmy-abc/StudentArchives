package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.user.RolePermission;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.PermissionRepository;
import com.example.studentarchives.repository.RolePermissionRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端统一鉴权服务
 * <p>
 * 对齐《管理端接口文档》五、权限控制：所有 {@code /admin/*} 接口必须校验当前用户拥有
 * {@code admin} 角色或对应权限码（见文档关键权限码表），越权统一返回 {@code 20005 无访问权限}。
 * HTTP 层（SecurityConfig）仅要求认证，角色/权限码在此处逐接口校验。
 * 各管理端 Service 注入本服务并在入口处调用，避免重复实现手写鉴权查询链。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    /** 管理员角色编码（《管理端接口文档》权限控制） */
    private static final String ADMIN_ROLE_CODE = "admin";

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 当前用户的角色快照（操作人角色信息）
     *
     * @param userId 用户 ID，可为 null
     * @return 优先返回 admin 角色，否则取第一个角色；无角色返回 null
     */
    public OperatorRole resolveOperatorRole(Long userId) {
        if (userId == null) {
            return null;
        }
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return null;
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        return roleRepository.findByIdIn(roleIds).stream()
                .map(r -> new OperatorRole(r.getId(), r.getName(), r.getCode()))
                .sorted((a, b) -> {
                    boolean aAdmin = a.isAdmin();
                    boolean bAdmin = b.isAdmin();
                    if (aAdmin != bAdmin) {
                        return aAdmin ? -1 : 1;
                    }
                    return 0;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验当前用户具备 admin 角色，否则越权返回 20005。
     */
    public void requireAdmin(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        boolean isAdmin = roleRepository.findByIdIn(roleIds).stream()
                .anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getCode()));
        if (!isAdmin) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    /**
     * 校验当前用户具备 admin 角色或任一指定权限码。
     * 越权统一返回 20005 无访问权限。
     *
     * @param userId         当前登录用户 ID（@AuthenticationPrincipal Long）
     * @param permissionCodes 接口所需权限码（《管理端接口文档》关键权限码），如 indicator:manage、log:view
     */
    public void requireAdminOrPermission(Long userId, String... permissionCodes) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        boolean isAdmin = roleRepository.findByIdIn(roleIds).stream()
                .anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getCode()));
        if (isAdmin) {
            return;
        }
        List<Long> permissionIds = rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        boolean hasPermission = permissionRepository.findByIdIn(permissionIds).stream()
                .anyMatch(p -> {
                    for (String code : permissionCodes) {
                        if (Objects.equals(code, p.getCode())) {
                            return true;
                        }
                    }
                    return false;
                });
        if (!hasPermission) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    /**
     * 操作人角色快照
     *
     * @param roleId   角色 ID
     * @param roleName 角色名称快照
     * @param roleCode 角色编码（用于 admin 判定）
     */
    public record OperatorRole(Long roleId, String roleName, String roleCode) {

        /** 是否为 admin 角色（依据角色编码） */
        public boolean isAdmin() {
            return ADMIN_ROLE_CODE.equals(this.roleCode);
        }
    }
}
