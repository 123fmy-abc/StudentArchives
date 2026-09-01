package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.entity.log.SystemLog;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.SystemLogRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.Fmy.TeacherScopeValidator;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Data;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端「操作日志查询模块」Service（《教师端接口文档》六，6.1）。
 * <p>
 * 数据来源：system_logs（只读）。权限码 {@code log:view}。数据范围：
 * 仅返回「当前教师自身操作」（operator_id = 当前教师）与「授权范围内学生相关操作」
 * （system_logs.user_id 落在教师 role_scopes 范围内）的日志；admin 角色不限定范围。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherLogService {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final ObjectMapper objectMapper;

    /**
     * 查询授权范围内的系统操作日志（GET /teacher/logs）
     */
    public PageResult<LogItem> listLogs(Long teacherId, LogQuery query, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(teacherId, "log:view");
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);

        // null = admin 或学校级授权（不限定范围）
        Set<Long> authorized = resolveAuthorizedStudentIds(teacherId, schoolId);
        Set<Long> gradeIds = resolveGradeStudentIds(query.getGrade());
        Set<Long> keywordUserIds = resolveKeywordUserIds(query.getKeyword());

        Specification<SystemLog> spec = buildSpec(query, teacherId, authorized, gradeIds, keywordUserIds);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<SystemLog> page = systemLogRepository.findAll(spec, pageable);

        return PageResult.of(toItems(page.getContent()), page.getTotalElements(), pageParam);
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
            result.addAll(resolveScopeStudentIds(s.getScopeType(), s.getScopeId()));
        }
        return result;
    }

    /** 解析某范围类型下的学生 userId 列表（2=学院 3=专业 4=班级 6=年级） */
    private List<Long> resolveScopeStudentIds(Integer scopeType, Long scopeId) {
        List<Long> classIds;
        switch (scopeType) {
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

    /** 年级筛选：解析年级对应的学生 userId 集合；null=无年级筛选 */
    private Set<Long> resolveGradeStudentIds(String grade) {
        if (grade == null || grade.isBlank()) {
            return null;
        }
        List<Long> classIds = clazzRepository.findByGrade(grade.trim()).stream()
                .map(Clazz::getId).filter(Objects::nonNull).toList();
        if (classIds.isEmpty()) {
            return Set.of();
        }
        return studentProfileRepository.findByClassIdIn(classIds).stream()
                .map(StudentProfile::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** 关键词筛选：解析姓名/学号命中的学生 userId 集合；null=无关键词筛选 */
    private Set<Long> resolveKeywordUserIds(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String kw = keyword.trim();
        return userRepository.findByNameContainingOrUserNoContaining(kw, kw).stream()
                .map(User::getId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private boolean isAdmin(Long userId) {
        AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(userId);
        return role != null && role.isAdmin();
    }

    // ==================== Specification 构建 ====================

    private Specification<SystemLog> buildSpec(LogQuery q, Long teacherId, Set<Long> authorized,
                                               Set<Long> gradeIds, Set<Long> keywordUserIds) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据范围：教师自身操作 OR 授权范围内学生相关操作（admin/学校级授权不限定）
            if (authorized != null) {
                Predicate own = cb.equal(root.get("operatorId"), teacherId);
                Predicate student = authorized.isEmpty()
                        ? cb.disjunction() : root.get("userId").in(authorized);
                predicates.add(cb.or(own, student));
            }

            if (q.getOperatorId() != null) {
                predicates.add(cb.equal(root.get("operatorId"), q.getOperatorId()));
            }
            if (notBlank(q.getAction())) {
                predicates.add(cb.equal(root.get("action"), q.getAction().trim()));
            }
            if (notBlank(q.getModule())) {
                predicates.add(cb.equal(root.get("module"), q.getModule().trim()));
            }
            if (q.getLogLevel() != null) {
                predicates.add(cb.equal(root.get("logLevel"), q.getLogLevel()));
            }
            if (notBlank(q.getRelatedType())) {
                predicates.add(cb.equal(root.get("relatedType"), q.getRelatedType().trim()));
            }
            if (q.getRelatedId() != null) {
                predicates.add(cb.equal(root.get("relatedId"), q.getRelatedId()));
            }
            LocalDateTime start = parseTime(q.getStartTime());
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            LocalDateTime end = parseTime(q.getEndTime());
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            if (gradeIds != null) {
                predicates.add(gradeIds.isEmpty()
                        ? cb.disjunction() : root.get("userId").in(gradeIds));
            }
            if (keywordUserIds != null) {
                Predicate desc = cb.like(root.get("description"), "%" + q.getKeyword().trim() + "%");
                Predicate kw = keywordUserIds.isEmpty()
                        ? cb.disjunction() : root.get("userId").in(keywordUserIds);
                predicates.add(cb.or(desc, kw));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ==================== 响应组装 ====================

    private List<LogItem> toItems(List<SystemLog> logs) {
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> operatorIds = logs.stream().map(SystemLog::getOperatorId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = operatorIds.isEmpty() ? Map.of()
                : userRepository.findByIdIn(operatorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        return logs.stream().map(l -> LogItem.builder()
                .id(l.getId())
                .operatorId(l.getOperatorId())
                .operatorName(l.getOperatorId() != null ? nameMap.get(l.getOperatorId()) : null)
                .roleId(l.getRoleId())
                .roleName(l.getRoleName())
                .action(l.getAction())
                .module(l.getModule())
                .description(l.getDescription())
                .beforeData(parseJson(l.getBeforeData()))
                .afterData(parseJson(l.getAfterData()))
                .ipAddress(l.getIpAddress())
                .createdAt(toIso(l.getCreatedAt()))
                .build()).collect(Collectors.toList());
    }

    // ==================== 通用辅助 ====================

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        try {
            return OffsetDateTime.parse(v).atZoneSameInstant(ZONE).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(v);
            } catch (Exception e2) {
                log.warn("无法解析时间参数: {}", v);
                return null;
            }
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return json;
        }
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZONE).format(ISO_WITH_ZONE)
                : null;
    }

    // ==================== 内嵌 POJO ====================

    /** 6.1 查询条件 */
    @Data
    @Builder
    public static class LogQuery {
        private Long operatorId;
        private String action;
        private String module;
        private Integer logLevel;
        private String startTime;
        private String endTime;
        private String relatedType;
        private Long relatedId;
        private String grade;
        private String keyword;
    }

    /** 6.1 列表项 */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LogItem {
        private Long id;
        private Long operatorId;
        private String operatorName;
        private Long roleId;
        private String roleName;
        private String action;
        private String module;
        private String description;
        private Object beforeData;
        private Object afterData;
        private String ipAddress;
        private String createdAt;
    }
}