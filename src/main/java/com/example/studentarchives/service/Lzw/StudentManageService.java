package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Lzw.activity.request.ActivityListRequest;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityListItemResponse;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.GenderEnum;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.SemesterGpaSummaryRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.Fmy.TeacherScopeValidator;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端「学生管理模块」Service（《教师端接口文档》七，7.1 / 7.2）。
 * <p>
 * 7.1 学生列表：按 {@code role_scopes} 授权范围过滤，返回学生基础信息 + 当前 GPA
 * （学期成绩汇总 weighted_gpa）+ 已通过/待审批档案计数。
 * 7.2 学生申报记录：复用 {@link ActivityService#list}，先做授权范围校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentManageService {

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final ActivityService activityService;

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final ArchiveRepository archiveRepository;

    // ==================== 7.1 获取授权范围内的学生列表 ====================

    /**
     * 获取授权范围内的学生列表（GET /teacher/students）
     *
     * @param scopeType 范围类型：1学校 2学院 3专业 4班级 6年级（可选，未传返回全部授权范围）
     * @param scopeId   范围 ID（可选）
     * @param semesterId 学期 ID（可选，用于档案计数与 GPA 学期）
     * @param grade     年级（可选）
     * @param keyword   姓名/学号（可选）
     */
    public PageResult<StudentItem> listStudents(Long teacherId, Integer scopeType, Long scopeId,
                                                Long semesterId, String grade, String keyword,
                                                PageParam pageParam) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        // null = admin 或学校级授权（不限定范围）
        Set<Long> authorized = resolveAuthorizedStudentIds(teacherId, schoolId);

        // 可选：按指定范围收窄（校验该范围在教师授权内，越权 20005）
        Set<Long> candidate;
        if (scopeType != null) {
            scopeValidator.ensureOrgInScope(teacherId, scopeType, scopeId, schoolId);
            Set<Long> scopeSet = new HashSet<>(resolveScopeStudentIds(scopeType, scopeId, schoolId));
            candidate = authorized == null ? scopeSet : intersect(authorized, scopeSet);
        } else {
            candidate = authorized == null ? null : new HashSet<>(authorized);
        }

        // 年级筛选
        if (grade != null && !grade.isBlank()) {
            Set<Long> gradeSet = new HashSet<>(resolveGradeStudentIds(grade));
            candidate = candidate == null ? gradeSet : intersect(candidate, gradeSet);
        }

        List<Long> userIds;
        if (candidate == null) {
            userIds = studentProfileRepository.findBySchoolId(schoolId).stream()
                    .map(StudentProfile::getUserId).filter(Objects::nonNull)
                    .distinct().sorted().collect(Collectors.toList());
        } else {
            userIds = candidate.stream().filter(Objects::nonNull)
                    .sorted().collect(Collectors.toList());
        }
        if (userIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageParam);
        }

        // 姓名/学号筛选
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            Map<Long, User> kwUsers = userRepository.findByIdIn(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
            userIds = userIds.stream()
                    .filter(id -> {
                        User u = kwUsers.get(id);
                        return u != null
                                && (contains(u.getName(), kw) || contains(u.getUserNo(), kw));
                    })
                    .collect(Collectors.toList());
        }

        int total = userIds.size();
        int from = pageParam.getOffset();
        int to = Math.min(from + pageParam.getPerPage(), total);
        List<Long> pageIds = from < total ? userIds.subList(from, to) : Collections.emptyList();
        if (pageIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, pageParam);
        }

        List<StudentItem> items = buildItems(pageIds, schoolId, semesterId);
        return PageResult.of(items, total, pageParam);
    }

    // ==================== 7.2 获取学生申报记录 ====================

    /**
     * 获取学生申报记录（GET /teacher/students/{studentId}/activities）。
     * <p>
     * 复用学生端 {@link ActivityService#list}，先做 {@code role_scopes} 授权范围校验，
     * 越权返回 20005。
     */
    public PageResult<ActivityListItemResponse> listActivities(Long teacherId, Long studentId,
                                                               String type, Integer status,
                                                               Long semesterId, PageParam pageParam) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        scopeValidator.ensureStudentInScope(teacherId, studentId, schoolId);

        ActivityListRequest request = new ActivityListRequest();
        request.setType(type);
        request.setStatus(status);
        request.setSemesterId(semesterId);
        request.setPage(pageParam.getPage());
        request.setPerPage(pageParam.getPerPage());
        return activityService.list(request, studentId);
    }

    // ==================== 私有：范围解析 ====================

    /** 解析教师授权范围内学生 userId 集合；null=不限定（admin 或学校级授权），空集合=无授权 */
    private Set<Long> resolveAuthorizedStudentIds(Long teacherId, Long schoolId) {
        if (isAdmin(teacherId)) {
            return null;
        }
        List<RoleScope> scopes = scopeValidator.effectiveScopes(teacherId);
        if (scopes.isEmpty()) {
            return Set.of();
        }
        boolean schoolLevel = scopes.stream().anyMatch(s ->
                Objects.equals(s.getScopeType(), 1) && Objects.equals(s.getScopeId(), schoolId));
        if (schoolLevel) {
            return null;
        }
        Set<Long> result = new HashSet<>();
        for (RoleScope s : scopes) {
            if (s.getScopeType() == null || s.getScopeId() == null) {
                continue;
            }
            result.addAll(resolveScopeStudentIds(s.getScopeType(), s.getScopeId(), schoolId));
        }
        return result;
    }

    /** 解析某范围类型下的学生 userId 列表（1=学校 2=学院 3=专业 4=班级 6=年级） */
    private List<Long> resolveScopeStudentIds(Integer scopeType, Long scopeId, Long schoolId) {
        if (scopeType == null || scopeId == null) {
            return List.of();
        }
        List<Long> classIds;
        switch (scopeType) {
            case 1 -> {
                return studentProfileRepository.findBySchoolId(schoolId).stream()
                        .map(StudentProfile::getUserId).filter(Objects::nonNull).toList();
            }
            case 2 -> {
                List<Long> majorIds = majorRepository.findByCollegeIdIn(Set.of(scopeId)).stream()
                        .map(Major::getId).filter(Objects::nonNull).toList();
                classIds = majorIds.isEmpty() ? List.of()
                        : clazzRepository.findByMajorIdIn(majorIds).stream()
                                .map(Clazz::getId).filter(Objects::nonNull).toList();
            }
            case 3 -> classIds = clazzRepository.findByMajorId(scopeId).stream()
                    .map(Clazz::getId).filter(Objects::nonNull).toList();
            case 4 -> {
                return studentProfileRepository.findByClassId(scopeId).stream()
                        .map(StudentProfile::getUserId).filter(Objects::nonNull).toList();
            }
            case 6 -> classIds = clazzRepository.findByGrade(String.valueOf(scopeId)).stream()
                    .map(Clazz::getId).filter(Objects::nonNull).toList();
            default -> {
                return List.of();
            }
        }
        return classIds.isEmpty() ? List.of()
                : studentProfileRepository.findByClassIdIn(classIds).stream()
                        .map(StudentProfile::getUserId).filter(Objects::nonNull).toList();
    }

    /** 年级筛选：解析年级对应的学生 userId 列表 */
    private List<Long> resolveGradeStudentIds(String grade) {
        List<Long> classIds = clazzRepository.findByGrade(grade.trim()).stream()
                .map(Clazz::getId).filter(Objects::nonNull).toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        return studentProfileRepository.findByClassIdIn(classIds).stream()
                .map(StudentProfile::getUserId).filter(Objects::nonNull).toList();
    }

    private boolean isAdmin(Long userId) {
        AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(userId);
        return role != null && role.isAdmin();
    }

    private Set<Long> intersect(Set<Long> a, Set<Long> b) {
        Set<Long> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private boolean contains(String s, String k) {
        return s != null && s.toLowerCase().contains(k);
    }

    // ==================== 私有：响应组装 ====================

    private List<StudentItem> buildItems(List<Long> userIds, Long schoolId, Long semesterId) {
        Map<Long, User> userMap = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, StudentProfile> profileMap = studentProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, sp -> sp, (a, b) -> a));

        Set<Long> classIds = profileMap.values().stream()
                .map(StudentProfile::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Clazz> classMap = classIds.isEmpty() ? Map.of()
                : clazzRepository.findByIdIn(new ArrayList<>(classIds)).stream()
                        .collect(Collectors.toMap(Clazz::getId, c -> c, (a, b) -> a));

        Set<Long> majorIds = classMap.values().stream()
                .map(Clazz::getMajorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Major> majorMap = majorIds.isEmpty() ? Map.of()
                : majorRepository.findByIdIn(new ArrayList<>(majorIds)).stream()
                        .collect(Collectors.toMap(Major::getId, m -> m, (a, b) -> a));

        Set<Long> collegeIds = majorMap.values().stream()
                .map(Major::getCollegeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, College> collegeMap = collegeIds.isEmpty() ? Map.of()
                : collegeRepository.findByIdIn(new ArrayList<>(collegeIds)).stream()
                        .collect(Collectors.toMap(College::getId, c -> c, (a, b) -> a));

        // 当前学期 GPA（未传 semesterId 时取当前学期）
        Long gpaSemesterId = semesterId != null ? semesterId
                : semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);
        Map<Long, BigDecimal> gpaMap = gpaSemesterId == null ? Map.of()
                : semesterGpaSummaryRepository.findByUserIdInAndSemesterId(userIds, gpaSemesterId).stream()
                        .filter(s -> s.getWeightedGpa() != null)
                        .collect(Collectors.toMap(SemesterGpaSummary::getUserId,
                                SemesterGpaSummary::getWeightedGpa,
                                (a, b) -> a));

        // 档案计数：已通过(2) / 待审批(1)，按 semesterId 过滤
        Map<Long, int[]> counts = new HashMap<>();
        for (Archive a : archiveRepository.findByUserIdIn(userIds)) {
            if (semesterId != null && !Objects.equals(a.getSemesterId(), semesterId)) {
                continue;
            }
            Integer st = a.getStatus();
            if (st == null) {
                continue;
            }
            int[] c = counts.computeIfAbsent(a.getUserId(), k -> new int[2]);
            if (st == 2) {
                c[0]++;
            } else if (st == 1) {
                c[1]++;
            }
        }

        List<StudentItem> items = new ArrayList<>();
        for (Long id : userIds) {
            User u = userMap.get(id);
            if (u == null) {
                continue;
            }
            StudentProfile sp = profileMap.get(id);
            Clazz clazz = sp != null && sp.getClassId() != null ? classMap.get(sp.getClassId()) : null;
            Major major = clazz != null && clazz.getMajorId() != null ? majorMap.get(clazz.getMajorId()) : null;
            College college = major != null && major.getCollegeId() != null ? collegeMap.get(major.getCollegeId()) : null;
            BigDecimal gpa = gpaMap.get(id);
            int[] c = counts.getOrDefault(id, new int[2]);

            items.add(StudentItem.builder()
                    .userId(id)
                    .studentNo(u.getUserNo())
                    .name(u.getName())
                    .gender(u.getGender())
                    .genderLabel(GenderEnum.of(u.getGender()).getLabel())
                    .className(clazz != null ? clazz.getName() : null)
                    .majorName(major != null ? major.getName() : null)
                    .collegeName(college != null ? college.getName() : null)
                    .grade(clazz != null ? clazz.getGrade() : null)
                    .currentGpa(gpa != null ? gpa.doubleValue() : null)
                    .approvedArchiveCount(c[0])
                    .pendingArchiveCount(c[1])
                    .build());
        }
        return items;
    }

    // ==================== 响应 DTO ====================

    /** 7.1 列表项 */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentItem {
        private Long userId;
        private String studentNo;
        private String name;
        private Integer gender;
        private String genderLabel;
        private String className;
        private String majorName;
        private String collegeName;
        private String grade;
        private Double currentGpa;
        private Integer approvedArchiveCount;
        private Integer pendingArchiveCount;
    }
}