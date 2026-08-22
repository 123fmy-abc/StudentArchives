package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.RoleScopeRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端 {@code role_scopes} 范围校验器（《教师端接口文档》十六、已复用模块）
 * <p>
 * 学生管理 / 统计看板 / 评分重算三个教师端模块共用同一套范围口径，收敛在本组件：
 * 仅启用状态（status=1）且生效期内（valid_from / valid_until）的授权记录生效。规则：
 * <ul>
 *   <li>学校级授权（scopeType=1，scopeId=学校）：覆盖校内全部范围；</li>
 *   <li>学院(2)/专业(3)/班级(4)：按同类型 scopeId 严格匹配；</li>
 *   <li>年级(6)：粗粒度校验——存在生效中的年级级授权即放行（与 {@code AdminExportService} 导出范围校验口径一致）。</li>
 * </ul>
 * admin 角色绕过范围校验（由调用方 {@code AdminAuthService#requireAdminOrPermission} 先行放行）。
 * 越权统一返回 20005 无访问权限。
 */
@Component
@RequiredArgsConstructor
public class TeacherScopeValidator {

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 6=年级 */
    private static final int SCOPE_SCHOOL = 1;
    private static final int SCOPE_COLLEGE = 2;
    private static final int SCOPE_MAJOR = 3;
    private static final int SCOPE_CLASS = 4;
    private static final int SCOPE_GRADE = 6;

    private final AdminAuthService adminAuthService;
    private final RoleScopeRepository roleScopeRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;

    /**
     * 校验教师授权范围内是否存在指定学生（studentId），越权返回 20005。
     * <p>
     * 学生归属通过 student_profiles.class_id → classes.major_id → majors.college_id
     * 解析组织链，与教师 role_scopes 按 学校/学院/专业/班级/年级 匹配。
     *
     * @param teacherId 当前教师用户 ID
     * @param studentId 目标学生用户 ID
     * @param schoolId  操作人所属学校 ID
     */
    public void ensureStudentInScope(Long teacherId, Long studentId, Long schoolId) {
        if (isAdmin(teacherId)) {
            return;
        }
        StudentContext ctx = resolveStudentContext(studentId);
        List<RoleScope> scopes = effectiveScopes(teacherId);
        // 年级授权（粗粒度）：存在生效中的年级级授权即放行
        if (scopes.stream().anyMatch(s -> Objects.equals(s.getScopeType(), SCOPE_GRADE))) {
            return;
        }
        for (RoleScope s : scopes) {
            if (s.getScopeType() == null || s.getScopeId() == null) {
                continue;
            }
            // 学校级授权：scopeId 匹配学校即覆盖校内全部学生
            if (Objects.equals(s.getScopeType(), SCOPE_SCHOOL) && Objects.equals(s.getScopeId(), schoolId)) {
                return;
            }
            // 班级
            if (Objects.equals(s.getScopeType(), SCOPE_CLASS) && ctx.classId() != null
                    && Objects.equals(s.getScopeId(), ctx.classId())) {
                return;
            }
            // 专业
            if (Objects.equals(s.getScopeType(), SCOPE_MAJOR) && ctx.majorId() != null
                    && Objects.equals(s.getScopeId(), ctx.majorId())) {
                return;
            }
            // 学院
            if (Objects.equals(s.getScopeType(), SCOPE_COLLEGE) && ctx.collegeId() != null
                    && Objects.equals(s.getScopeId(), ctx.collegeId())) {
                return;
            }
        }
        throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
    }

    /**
     * 校验指定组织范围（scopeType / scopeId）在教师授权范围内，越权返回 20005。
     * <p>
     * scopeType=1（学校）需持有匹配学校的学校级授权；2/3/4 按同类型 scopeId 严格匹配；
     * 6（年级）粗粒度放行。scopeType 为空视为不限定（调用方自行决定默认范围）。
     *
     * @param teacherId 当前教师用户 ID
     * @param scopeType 范围类型：1/2/3/4/6
     * @param scopeId   范围 ID（scopeType=1 时为学校 ID）
     * @param schoolId  操作人所属学校 ID
     */
    public void ensureOrgInScope(Long teacherId, Integer scopeType, Long scopeId, Long schoolId) {
        if (scopeType == null) {
            return;
        }
        if (isAdmin(teacherId)) {
            return;
        }
        List<RoleScope> scopes = effectiveScopes(teacherId);
        if (Objects.equals(scopeType, SCOPE_GRADE)) {
            boolean hasGrade = scopes.stream().anyMatch(s -> Objects.equals(s.getScopeType(), SCOPE_GRADE));
            if (hasGrade) {
                return;
            }
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        for (RoleScope s : scopes) {
            if (s.getScopeType() == null || s.getScopeId() == null) {
                continue;
            }
            if (Objects.equals(s.getScopeType(), SCOPE_SCHOOL) && Objects.equals(s.getScopeId(), schoolId)) {
                return;
            }
            if ((Objects.equals(s.getScopeType(), SCOPE_COLLEGE)
                    || Objects.equals(s.getScopeType(), SCOPE_MAJOR)
                    || Objects.equals(s.getScopeType(), SCOPE_CLASS))
                    && Objects.equals(s.getScopeType(), scopeType)
                    && Objects.equals(s.getScopeId(), scopeId)) {
                return;
            }
        }
        throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
    }

    /**
     * 校验教师持有学校级授权（或 admin），用于学期级评分重算等全校范围操作。
     */
    public void ensureSchoolScope(Long teacherId, Long schoolId) {
        if (isAdmin(teacherId)) {
            return;
        }
        boolean schoolLevel = effectiveScopes(teacherId).stream()
                .anyMatch(s -> Objects.equals(s.getScopeType(), SCOPE_SCHOOL)
                        && Objects.equals(s.getScopeId(), schoolId));
        if (!schoolLevel) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    /**
     * 返回教师某组织维度下授权的全部 scopeId 集合。
     * <p>
     * 返回 {@code null} 表示学校级授权覆盖全部范围（不限定）；返回空集合表示无任何授权。
     * 用于热力图等行维度过滤：调用方仅展示授权范围内的组织行。
     *
     * @param teacherId 当前教师用户 ID
     * @param orgType   组织维度：2=学院 3=专业 4=班级
     * @param schoolId  操作人所属学校 ID
     * @return 授权 orgId 集合；null=全部（学校级授权）
     */
    public Set<Long> authorizedOrgIds(Long teacherId, Integer orgType, Long schoolId) {
        if (isAdmin(teacherId)) {
            return null;
        }
        List<RoleScope> scopes = effectiveScopes(teacherId);
        if (scopes.isEmpty()) {
            return Set.of();
        }
        boolean schoolLevel = scopes.stream().anyMatch(s ->
                Objects.equals(s.getScopeType(), SCOPE_SCHOOL) && Objects.equals(s.getScopeId(), schoolId));
        if (schoolLevel) {
            return null;
        }
        if (orgType == null) {
            return Set.of();
        }
        return scopes.stream()
                .filter(s -> Objects.equals(s.getScopeType(), orgType) && s.getScopeId() != null)
                .map(RoleScope::getScopeId)
                .collect(Collectors.toSet());
    }

    /** 教师生效中的授权范围快照（status=1 + 生效期内） */
    public List<RoleScope> effectiveScopes(Long teacherId) {
        List<RoleScope> scopes = roleScopeRepository.findByUserIdAndStatus(teacherId, 1);
        LocalDate today = LocalDate.now();
        return scopes.stream().filter(s -> isInEffect(s, today)).collect(Collectors.toList());
    }

    /** 解析学生归属组织链（班级 → 专业 → 学院 → 班级年级） */
    public StudentContext resolveStudentContext(Long studentId) {
        StudentProfile sp = studentProfileRepository.findByUserId(studentId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学生不存在"));
        Long classId = sp.getClassId();
        Clazz clazz = classId != null ? clazzRepository.findById(classId).orElse(null) : null;
        Long majorId = clazz != null ? clazz.getMajorId() : null;
        Major major = majorId != null ? majorRepository.findById(majorId).orElse(null) : null;
        return new StudentContext(classId, majorId,
                major != null ? major.getCollegeId() : null,
                clazz != null ? clazz.getGrade() : null);
    }

    /** 学生组织归属上下文 */
    public record StudentContext(Long classId, Long majorId, Long collegeId, String classGrade) {
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(userId);
        return role != null && role.isAdmin();
    }

    /** 授权生效期校验：valid_from / valid_until 未设置视为永久有效 */
    private boolean isInEffect(RoleScope scope, LocalDate today) {
        if (scope.getValidFrom() != null && today.isBefore(scope.getValidFrom())) {
            return false;
        }
        if (scope.getValidUntil() != null && today.isAfter(scope.getValidUntil())) {
            return false;
        }
        return true;
    }
}
