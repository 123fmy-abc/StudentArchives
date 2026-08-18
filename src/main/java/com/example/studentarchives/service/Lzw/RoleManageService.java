package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.user.Permission;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.RolePermission;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.PermissionRepository;
import com.example.studentarchives.repository.RolePermissionRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端角色与权限管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》八、角色与权限管理模块（8.1 ~ 8.7）。
 * 数据来源：roles、permissions、role_permissions、user_roles。
 * <p>
 * 权限：文档附录标注该模块「仅管理员可见」，统一要求 admin 角色，越权返回 20005。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleManageService {

    /** 自定义角色默认层级（文档 8.2：默认 7） */
    private static final int DEFAULT_LEVEL = 7;

    /** 角色类型默认值（roles.role_type：1=教学类） */
    private static final int DEFAULT_ROLE_TYPE = 1;

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final AdminAuthService adminAuthService;

    // ==================== 8.1 获取角色列表 ====================

    @Transactional(readOnly = true)
    public PageResult<RoleListItem> listRoles(Long operatorId, RoleListQuery query, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);

        Specification<Role> spec = buildRoleSpec(query.getStatus());
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<Role> page = roleRepository.findAll(spec, pageable);

        List<Role> roles = page.getContent();
        List<Long> roleIds = roles.stream().map(Role::getId).filter(Objects::nonNull).collect(Collectors.toList());

        // 批量统计权限数与用户数
        Map<Long, Long> permissionCountMap = roleIds.isEmpty() ? Map.of()
                : rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                        .filter(rp -> rp.getRoleId() != null)
                        .collect(Collectors.groupingBy(RolePermission::getRoleId, Collectors.counting()));
        Map<Long, Long> userCountMap = roleIds.isEmpty() ? Map.of()
                : userRoleRepository.findByRoleIdIn(roleIds).stream()
                        .filter(ur -> ur.getRoleId() != null)
                        .collect(Collectors.groupingBy(UserRole::getRoleId, Collectors.counting()));

        List<RoleListItem> items = roles.stream().map(r -> RoleListItem.builder()
                .roleId(r.getId())
                .roleName(r.getName())
                .roleCode(r.getCode())
                .level(r.getLevel())
                .status(r.getStatus())
                .statusLabel(statusLabel(r.getStatus()))
                .description(r.getDescription())
                .permissionCount(permissionCountMap.getOrDefault(r.getId(), 0L))
                .userCount(userCountMap.getOrDefault(r.getId(), 0L))
                .createdAt(toIso(r.getCreatedAt()))
                .build()).collect(Collectors.toList());

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 8.2 创建角色 ====================

    @Transactional
    public RoleIdResponse createRole(Long operatorId, RoleSaveRequest body) {
        adminAuthService.requireAdmin(operatorId);

        String roleName = requireNotBlank(body.getRoleName(), "角色名称不能为空");
        String roleCode = requireNotBlank(body.getRoleCode(), "角色编码不能为空");
        Integer level = body.getLevel() != null ? body.getLevel() : DEFAULT_LEVEL;
        validateLevel(level);
        Integer status = body.getStatus() != null ? body.getStatus() : 1;
        validateStatus(status);

        roleRepository.findByCode(roleCode)
                .ifPresent(r -> { throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色编码已存在"); });

        Role role = new Role();
        role.setName(roleName);
        role.setCode(roleCode);
        role.setDescription(body.getDescription());
        role.setLevel(level);
        role.setRoleType(DEFAULT_ROLE_TYPE);
        role.setIsSystem(0);
        role.setIsAuditor(0);
        role.setMaxScopeCount(0);
        role.setStatus(status);
        roleRepository.save(role);

        return RoleIdResponse.builder().roleId(role.getId()).build();
    }

    // ==================== 8.3 更新角色 ====================

    @Transactional
    public void updateRole(Long operatorId, Long roleId, RoleSaveRequest body) {
        adminAuthService.requireAdmin(operatorId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在"));

        if (body.getRoleName() != null && !body.getRoleName().isBlank()) {
            role.setName(body.getRoleName().trim());
        }
        if (body.getRoleCode() != null && !body.getRoleCode().isBlank()) {
            String roleCode = body.getRoleCode().trim();
            roleRepository.findByCode(roleCode)
                    .filter(existing -> !existing.getId().equals(roleId))
                    .ifPresent(existing -> { throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色编码已存在"); });
            role.setCode(roleCode);
        }
        if (body.getLevel() != null) {
            validateLevel(body.getLevel());
            role.setLevel(body.getLevel());
        }
        if (body.getDescription() != null) {
            role.setDescription(body.getDescription());
        }
        if (body.getStatus() != null) {
            validateStatus(body.getStatus());
            role.setStatus(body.getStatus());
        }
        roleRepository.save(role);
    }

    // ==================== 8.4 删除角色 ====================

    @Transactional
    public void deleteRole(Long operatorId, Long roleId) {
        adminAuthService.requireAdmin(operatorId);

        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在"));

        // 文档 8.4：被用户引用的角色不可删除，需先移除所有 user_roles 关联记录
        if (!userRoleRepository.findByRoleId(roleId).isEmpty()) {
            throw new BusinessException(ResultCode.DATA_RELATION_EXISTS, "该角色已被用户使用，无法删除");
        }

        roleRepository.softDeleteById(roleId, LocalDateTime.now());
    }

    // ==================== 8.5 获取角色权限 ====================

    @Transactional(readOnly = true)
    public RolePermissionsResponse getRolePermissions(Long operatorId, Long roleId) {
        adminAuthService.requireAdmin(operatorId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在"));

        List<Long> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<PermissionItem> permissions = permissionIds.isEmpty() ? Collections.emptyList()
                : permissionRepository.findByIdIn(permissionIds).stream()
                        .map(p -> PermissionItem.builder()
                                .permissionId(p.getId())
                                .permissionCode(p.getCode())
                                .permissionName(p.getName())
                                .build())
                        .collect(Collectors.toList());

        return RolePermissionsResponse.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .permissions(permissions)
                .build();
    }

    // ==================== 8.6 分配角色权限（覆盖式） ====================

    @Transactional
    public void assignRolePermissions(Long operatorId, Long roleId, List<Long> permissionIds) {
        adminAuthService.requireAdmin(operatorId);

        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在"));

        List<Long> distinctIds = permissionIds == null
                ? Collections.emptyList()
                : permissionIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!distinctIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findByIdIn(distinctIds);
            if (permissions.size() != distinctIds.size()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "存在非法的权限ID");
            }
        }

        // 覆盖式：删除旧关联再插入新关联，flush 避免 INSERT 先于 DELETE 触发
        // role_permissions 唯一索引 uk_role_permissions(role_id, permission_id, is_deleted_null)
        List<RolePermission> old = rolePermissionRepository.findByRoleId(roleId);
        if (!old.isEmpty()) {
            rolePermissionRepository.deleteAll(old);
            rolePermissionRepository.flush();
        }
        for (Long permissionId : distinctIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionRepository.save(rp);
        }
    }

    // ==================== 8.7 获取权限码列表 ====================

    @Transactional(readOnly = true)
    public List<PermissionListItem> listPermissions(Long operatorId, String module, Integer status) {
        adminAuthService.requireAdmin(operatorId);

        Specification<Permission> spec = buildPermissionSpec(module, status);
        Sort sort = Sort.by(Sort.Direction.ASC, "sort").and(Sort.by(Sort.Direction.ASC, "id"));
        return permissionRepository.findAll(spec, sort).stream()
                .map(p -> PermissionListItem.builder()
                        .permissionId(p.getId())
                        .permissionCode(p.getCode())
                        .permissionName(p.getName())
                        .status(p.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 查询辅助 ====================

    private Specification<Role> buildRoleSpec(Integer status) {
        return (root, cq, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    private Specification<Permission> buildPermissionSpec(String module, Integer status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (module != null && !module.isBlank()) {
                // 文档 8.7：按 permission_code 前缀模糊匹配
                predicates.add(cb.like(root.get("code"), module.trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ==================== 通用辅助 ====================

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, message);
        }
        return value.trim();
    }

    private void validateLevel(Integer level) {
        if (level == null || level < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "level 必须为大于等于 1 的整数");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(禁用) 或 1(启用)");
        }
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "启用" : "禁用";
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    // ==================== 内嵌 POJO ====================

    /** 8.1 查询条件 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleListQuery {
        private Integer status;
    }

    /** 8.1 列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleListItem {
        private Long roleId;
        private String roleName;
        private String roleCode;
        private Integer level;
        private Integer status;
        private String statusLabel;
        private String description;
        private Long permissionCount;
        private Long userCount;
        private String createdAt;
    }

    /** 8.2 / 8.3 角色保存请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleSaveRequest {
        private String roleName;
        private String roleCode;
        private Integer level;
        private String description;
        private Integer status;
    }

    /** 8.2 创建角色响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleIdResponse {
        private Long roleId;
    }

    /** 8.5 角色权限响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissionsResponse {
        private Long roleId;
        private String roleName;
        private List<PermissionItem> permissions;
    }

    /** 8.6 分配角色权限请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignPermissionsRequest {
        private List<Long> permissionIds;
    }

    /** 8.5 权限项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItem {
        private Long permissionId;
        private String permissionCode;
        private String permissionName;
    }

    /** 8.7 权限码列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PermissionListItem {
        private Long permissionId;
        private String permissionCode;
        private String permissionName;
        private Integer status;
    }
}