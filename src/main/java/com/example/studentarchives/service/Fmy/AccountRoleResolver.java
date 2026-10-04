package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.enums.RoleLevelEnum;
import com.example.studentarchives.enums.StatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 账号角色解析器：集中解决「同一账号同时持有多种角色时，首页到底进哪一端」的问题。
 * <p>
 * 背景（修复的问题）：{@code user_roles} 允许一个用户绑定多个角色，开发库中
 * {@code 202401001} 同时持有 {@code admin}(level=0) 与 {@code student}(level=1)。
 * 此前登录响应只返回 {@code roles} 角色编码数组、且顺序由 {@code findByIdIn} 决定（无 ORDER BY），
 * 前端无论用 {@code roles[0]} 还是「数组里有 student 就当学生」都会出现首页时分时不分、
 * 同一账号反复登录落到不同首页的情况。
 * <p>
 * 本解析器提供两件事：
 * <ol>
 *   <li><b>确定性分流</b>：把角色按权限从高到低排序，并给出 {@code roleLevel} 与
 *       {@code homePage} 建议值，供前端直接使用（见 {@link #homePage(List)}）；</li>
 *   <li><b>角色守卫</b>：{@link #requireStudent(Long)} / {@link #requireNonStudent(Long)}，
 *       供 Service 层在首页接口入口做越权拦截，越权统一返回 {@code 20005 无访问权限}。</li>
 * </ol>
 * 角色口径与全项目鉴权链路一致：仅统计 <b>启用中</b>（{@code roles.status=1}）且未软删除的角色——
 * 禁用角色不再授予任何权限（{@code deleted_at} 由实体 {@code @SQLRestriction} 过滤）。
 */
@Component
@RequiredArgsConstructor
public class AccountRoleResolver {

    /** 学生角色编码（与 {@code UserManageService.ROLE_CODE_STUDENT} 口径一致） */
    private static final String STUDENT_ROLE_CODE = "student";

    /** 首页标识：管理端 */
    public static final String HOME_ADMIN = "admin";
    /** 首页标识：教师端 */
    public static final String HOME_TEACHER = "teacher";
    /** 首页标识：学生端 */
    public static final String HOME_STUDENT = "student";

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    /**
     * 用户启用中的角色，按权限从高到低排序（{@code level} 升序，level 越小权限越高）。
     *
     * @param userId 用户 ID，可为 null
     * @return 角色列表；无角色或无启用角色时返回空列表（不为 null）
     */
    public List<Role> activeRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        return sortByPrivilege(
                roleRepository.findByIdInAndStatus(roleIds, StatusEnum.ENABLED.getValue()));
    }

    /**
     * 角色按权限从高到低排序（{@code level} 升序；level 为空的角色视为最低权限排在末尾）。
     * <p>
     * 同级角色以角色编码作为次级排序键，保证任意输入顺序下结果完全一致——
     * 登录/ {@code /auth/me} 的 {@code roles} 数组应使用本方法排序后再返回，
     * 使 {@code roles[0]} 恒为最高权限角色，前端分流结果稳定可复现。
     */
    public List<Role> sortByPrivilege(List<Role> roles) {
        return roles.stream()
                .sorted(Comparator.comparingInt(this::levelOrLowest)
                        .thenComparing(Role::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 最高权限角色的层级（{@code level} 最小值）。
     *
     * @param roles 已排序或未排序的角色列表
     * @return 层级值；列表为空时返回 null
     */
    public Integer primaryLevel(List<Role> roles) {
        return roles.stream()
                .map(Role::getLevel)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
    }

    /**
     * 首页路由建议值，规则为「取最高权限角色的 level」：
     * <ul>
     *   <li>{@code 0}（{@link RoleLevelEnum#SYSTEM}，即 admin 角色）→ {@code admin} 管理端首页；</li>
     *   <li>{@code 1}（{@link RoleLevelEnum#STUDENT}）→ {@code student} 学生端首页；</li>
     *   <li>其余（教师/辅导员/系主任/院长/自定义）→ {@code teacher} 教师端首页。</li>
     * </ul>
     *
     * @param roles 用户角色列表
     * @return {@code admin} / {@code teacher} / {@code student}；无角色时返回 null
     */
    public String homePage(List<Role> roles) {
        Integer level = primaryLevel(roles);
        if (level == null) {
            return null;
        }
        if (Objects.equals(level, RoleLevelEnum.SYSTEM.getValue())) {
            return HOME_ADMIN;
        }
        if (Objects.equals(level, RoleLevelEnum.STUDENT.getValue())) {
            return HOME_STUDENT;
        }
        return HOME_TEACHER;
    }

    /**
     * 学生端接口守卫：要求当前用户持有学生角色，否则返回 {@code 20005 无访问权限}。
     * <p>
     * 用于 {@code GET /home/dashboard}（学生端首页概览）——该接口返回的是「当前登录学生」的
     * 私有数据，非学生调用会得到一个把管理员姓名/工号当作学生姓名的空壳响应。
     *
     * @param userId 当前登录用户 ID
     */
    public void requireStudent(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        boolean isStudent = activeRoles(userId).stream().anyMatch(this::isStudentRole);
        if (!isStudent) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    /**
     * 教师/管理端首页守卫：要求当前用户持有<b>任意非学生</b>角色，否则返回 {@code 20005 无访问权限}。
     * <p>
     * 用于 {@code GET /teacher/dashboard}（教师端首页概览）。admin 角色按项目既有口径放行
     * （与 {@code TeacherScopeValidator} 对 admin 的绕过保持一致）。
     *
     * @param userId 当前登录用户 ID
     */
    public void requireNonStudent(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        boolean hasNonStudent = activeRoles(userId).stream().anyMatch(r -> !isStudentRole(r));
        if (!hasNonStudent) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    /** 是否学生角色：level=1（RoleLevelEnum.STUDENT）或角色编码为 student */
    private boolean isStudentRole(Role role) {
        return Objects.equals(role.getLevel(), RoleLevelEnum.STUDENT.getValue())
                || STUDENT_ROLE_CODE.equals(role.getCode());
    }

    /** 取角色 level；为空时视为最低权限（排在末尾） */
    private int levelOrLowest(Role role) {
        return role.getLevel() != null ? role.getLevel() : Integer.MAX_VALUE;
    }
}
