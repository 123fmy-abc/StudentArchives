package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.permission.request.RolePermissionUpdateRequest;
import com.example.studentarchives.dto.Fmy.permission.request.UserRoleUpdateRequest;
import com.example.studentarchives.dto.Fmy.permission.response.PermissionItemResponse;
import com.example.studentarchives.dto.Fmy.permission.response.RolePermissionResponse;
import com.example.studentarchives.dto.Fmy.permission.response.UserScopeResponse;
import com.example.studentarchives.entity.user.Permission;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.RolePermission;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.PermissionRepository;
import com.example.studentarchives.repository.RolePermissionRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.RoleScopeRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端权限聚合服务
 * <p>
 * 对齐图片中“管理员 → 角色选择/账号管理 → 管理权限”菜单，将原本分散在
 * AdminAuthService、各 Repository 中的权限查询/分配能力聚合为统一服务层，
 * 供 {@link com.example.studentarchives.controller.Fmy.AdminPermissionController} 暴露。
 * <p>
 * 所有写操作要求当前用户具备 admin 角色或 user:manage + permission:manage 权限码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final AdminAuthService adminAuthService;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleScopeRepository roleScopeRepository;
    private final UserRepository userRepository;

    /**
     * 查询全部启用的权限码
     */
    @Transactional(readOnly = true)
    public List<PermissionItemResponse> listPermissions(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, "permission:view", "permission:manage");
        return permissionRepository.findAll().stream()
                .filter(p -> Integer.valueOf(1).equals(p.getStatus()))
                .sorted(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toPermissionItem)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定角色的权限
     */
    @Transactional(readOnly = true)
    public RolePermissionResponse getRolePermissions(Long userId, Long roleId) {
        adminAuthService.requireAdminOrPermission(userId, "permission:view", "permission:manage");
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在"));

        List<Long> permissionIds = rolePermissionRepository.findByRoleIdIn(List.of(roleId)).stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        List<String> permissionCodes = permissionIds.isEmpty()
                ? List.of()
                : permissionRepository.findByIdIn(permissionIds).stream()
                        .map(Permission::getCode)
                        .collect(Collectors.toList());

        return RolePermissionResponse.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .roleCode(role.getCode())
                .permissionIds(permissionIds)
                .permissionCodes(permissionCodes)
                .build();
    }

    /**
     * 更新角色权限（全量覆盖）
     */
    @Transactional
    public void updateRolePermissions(Long operatorId, RolePermissionUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(operatorId, "permission:manage");
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在"));

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            // 校验权限 ID 存在
            List<Permission> permissions = permissionRepository.findByIdIn(request.getPermissionIds());
            Set<Long> existingIds = permissions.stream().map(Permission::getId).collect(Collectors.toSet());
            List<Long> invalidIds = request.getPermissionIds().stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toList());
            if (!invalidIds.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "非法的权限 ID: " + invalidIds);
            }
        }

        // 删除旧关联并写入新关联
        List<RolePermission> old = rolePermissionRepository.findByRoleIdIn(List.of(role.getId()));
        if (!old.isEmpty()) {
            rolePermissionRepository.deleteAll(old);
        }

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<RolePermission> newRelations = request.getPermissionIds().stream()
                    .distinct()
                    .map(pid -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(role.getId());
                        rp.setPermissionId(pid);
                        return rp;
                    })
                    .collect(Collectors.toList());
            rolePermissionRepository.saveAll(newRelations);
        }

        log.info("更新角色权限: roleId={}, operatorId={}, permissionCount={}",
                role.getId(), operatorId, request.getPermissionIds() == null ? 0 : request.getPermissionIds().size());
    }

    /**
     * 查询指定用户的角色 ID 列表
     */
    @Transactional(readOnly = true)
    public List<Long> getUserRoles(Long userId, Long targetUserId) {
        adminAuthService.requireAdminOrPermission(userId, "user:view", "user:manage");
        return userRoleRepository.findByUserId(targetUserId).stream()
                .map(UserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 更新用户角色（全量覆盖）
     */
    @Transactional
    public void updateUserRoles(Long operatorId, UserRoleUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findByIdIn(request.getRoleIds());
            if (roles.size() != request.getRoleIds().size()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "存在非法的角色 ID");
            }
        }

        List<UserRole> old = userRoleRepository.findByUserId(request.getUserId());
        if (!old.isEmpty()) {
            userRoleRepository.deleteAll(old);
        }

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            List<UserRole> newRelations = request.getRoleIds().stream()
                    .distinct()
                    .map(roleId -> {
                        UserRole ur = new UserRole();
                        ur.setUserId(request.getUserId());
                        ur.setRoleId(roleId);
                        return ur;
                    })
                    .collect(Collectors.toList());
            userRoleRepository.saveAll(newRelations);
        }

        log.info("更新用户角色: userId={}, operatorId={}, roleCount={}",
                request.getUserId(), operatorId, request.getRoleIds() == null ? 0 : request.getRoleIds().size());
    }

    /**
     * 查询用户数据范围（按角色分组）
     */
    @Transactional(readOnly = true)
    public List<UserScopeResponse> getUserScopes(Long userId, Long targetUserId) {
        adminAuthService.requireAdminOrPermission(userId, "user:view", "user:manage");
        List<RoleScope> scopes = roleScopeRepository.findByUserIdAndStatus(targetUserId, 1);
        Map<Long, String> roleNameMap = roleRepository.findByIdIn(
                        scopes.stream().map(RoleScope::getRoleId).distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));

        return scopes.stream()
                .map(s -> UserScopeResponse.builder()
                        .userId(s.getUserId())
                        .roleId(s.getRoleId())
                        .roleName(roleNameMap.getOrDefault(s.getRoleId(), ""))
                        .scopeType(s.getScopeType())
                        .scopeTypeLabel(scopeTypeLabel(s.getScopeType()))
                        .scopeId(s.getScopeId())
                        .scopeName(null) // 范围名称由上层按需关联学院/专业/班级表查询
                        .isPrimary(s.getIsPrimary())
                        .validFrom(s.getValidFrom())
                        .validUntil(s.getValidUntil())
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    private PermissionItemResponse toPermissionItem(Permission p) {
        return PermissionItemResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .type(p.getType())
                .parentId(p.getParentId())
                .sort(p.getSort())
                .status(p.getStatus())
                .build();
    }

    private String scopeTypeLabel(Integer scopeType) {
        return switch (Objects.requireNonNullElse(scopeType, 0)) {
            case 1 -> "学校";
            case 2 -> "学院";
            case 3 -> "专业";
            case 4 -> "班级";
            case 5 -> "课程";
            default -> "未知";
        };
    }
}
