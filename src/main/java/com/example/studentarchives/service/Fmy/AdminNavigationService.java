package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.navigation.response.NavigationItemResponse;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端导航菜单服务
 * <p>
 * 按当前用户角色返回菜单树，将图片中“管理员/审核员/任课教师/系统开发员”的菜单层级
 * 映射到后端具体 Controller/接口前缀，避免前端因菜单路径与接口模块不一致而产生歧义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNavigationService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    /**
     * 根据用户角色构建菜单树
     *
     * @param userId 当前登录用户 ID
     * @return 菜单列表（已按角色过滤）
     */
    public List<NavigationItemResponse> buildNavigation(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        Set<String> roleCodes = roleRepository.findByIdIn(roleIds).stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        boolean isAdmin = roleCodes.contains("admin");
        boolean isAuditor = roleCodes.contains("auditor") || roleCodes.contains("teacher");
        boolean isTeacher = roleCodes.contains("teacher");

        if (isAdmin) {
            return adminNavigation();
        }
        if (isAuditor) {
            return auditorNavigation();
        }
        if (isTeacher) {
            return teacherNavigation();
        }
        return List.of();
    }

    private List<NavigationItemResponse> adminNavigation() {
        return List.of(
                NavigationItemResponse.builder()
                        .key("archive-view")
                        .name("档案查看")
                        .path("/admin/archives")
                        .requiredPermission("archive:view")
                        .children(List.of(
                                child("school-overview", "学校整体档案汇总", "/admin/archives/school"),
                                child("grade-filter", "按年级筛选", "/admin/archives?filter=grade"),
                                child("college-filter", "按学院筛选", "/admin/archives?filter=college"),
                                child("major-filter", "按专业筛选", "/admin/archives?filter=major"),
                                child("class-filter", "按班级筛选", "/admin/archives?filter=class")
                        ))
                        .build(),
                NavigationItemResponse.builder()
                        .key("archive-export")
                        .name("档案导出")
                        .path("/admin/exports")
                        .requiredPermission("export:manage")
                        .children(List.of(
                                child("export-all", "全校导出", "/admin/exports?scope=school"),
                                child("export-college", "按学院导出", "/admin/exports?scope=college"),
                                child("export-major", "按专业导出", "/admin/exports?scope=major"),
                                child("export-class", "按班级导出", "/admin/exports?scope=class")
                        ))
                        .build(),
                NavigationItemResponse.builder()
                        .key("role-management")
                        .name("角色选择")
                        .path("/admin/permissions")
                        .requiredPermission("permission:manage")
                        .children(List.of(
                                child("role-list", "角色与权限", "/admin/permissions/list"),
                                child("user-roles", "用户角色", "/admin/permissions/users"),
                                child("user-scopes", "用户数据范围", "/admin/permissions/users/scopes")
                        ))
                        .build(),
                NavigationItemResponse.builder()
                        .key("form-template")
                        .name("表单自定义")
                        .path("/admin/form-templates")
                        .requiredPermission("form:manage")
                        .children(List.of(
                                child("template-list", "表单模板", "/admin/form-templates"),
                                child("announcement", "信息发布", "/admin/announcements")
                        ))
                        .build(),
                NavigationItemResponse.builder()
                        .key("system-log")
                        .name("日志查看")
                        .path("/admin/logs")
                        .requiredPermission("log:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("achievement-heatmap")
                        .name("成果热力图")
                        .path("/admin/statistics/heatmap")
                        .requiredPermission("statistics:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("account-management")
                        .name("账号管理")
                        .path("/admin/users")
                        .requiredPermission("user:manage")
                        .children(List.of(
                                child("student-account", "学生账号", "/admin/users?type=student"),
                                child("teacher-account", "教师账号", "/admin/users?type=teacher")
                        ))
                        .build()
        );
    }

    private List<NavigationItemResponse> auditorNavigation() {
        return List.of(
                NavigationItemResponse.builder()
                        .key("archive-view")
                        .name("档案查看")
                        .path("/teacher/archives")
                        .requiredPermission("archive:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("material-audit")
                        .name("材料审核")
                        .path("/teacher/audits")
                        .requiredPermission("audit:manage")
                        .children(List.of(
                                child("college-audit", "院级审核", "/teacher/audits?level=college"),
                                child("department-audit", "系级审核", "/teacher/audits?level=department")
                        ))
                        .build(),
                NavigationItemResponse.builder()
                        .key("system-log")
                        .name("日志查看")
                        .path("/teacher/logs")
                        .requiredPermission("log:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("achievement-heatmap")
                        .name("成果热力图")
                        .path("/teacher/statistics/heatmap")
                        .requiredPermission("statistics:view")
                        .build()
        );
    }

    private List<NavigationItemResponse> teacherNavigation() {
        return List.of(
                NavigationItemResponse.builder()
                        .key("archive-view")
                        .name("档案查看")
                        .path("/teacher/archives")
                        .requiredPermission("archive:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("archive-export")
                        .name("档案导出")
                        .path("/teacher/exports")
                        .requiredPermission("export:view")
                        .children(List.of(
                                child("export-all", "全校导出", "/teacher/exports?scope=school"),
                                child("export-college", "按学院导出", "/teacher/exports?scope=college"),
                                child("export-major", "按专业导出", "/teacher/exports?scope=major"),
                                child("export-class", "按班级导出", "/teacher/exports?scope=class")
                        ))
                        .build(),
                NavigationItemResponse.builder()
                        .key("system-log")
                        .name("日志查看")
                        .path("/teacher/logs")
                        .requiredPermission("log:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("achievement-heatmap")
                        .name("成果热力图")
                        .path("/teacher/statistics/heatmap")
                        .requiredPermission("statistics:view")
                        .build(),
                NavigationItemResponse.builder()
                        .key("improvement-suggestion")
                        .name("改进建议")
                        .path("/teacher/improvement-suggestions")
                        .requiredPermission("suggestion:view")
                        .build()
        );
    }

    private NavigationItemResponse child(String key, String name, String path) {
        return NavigationItemResponse.builder()
                .key(key)
                .name(name)
                .path(path)
                .build();
    }
}
