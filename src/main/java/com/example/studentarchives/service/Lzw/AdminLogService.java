package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.entity.export.ExportOperationLog;
import com.example.studentarchives.entity.log.LoginLog;
import com.example.studentarchives.entity.log.SystemLog;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.ExportOperationLogRepository;
import com.example.studentarchives.repository.LoginLogRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.SystemLogRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端操作日志服务（Lzw）
 * <p>
 * 对应《管理端接口文档》四、操作日志模块（4.1 ~ 4.3）。
 * 数据来源：system_logs、login_logs、export_operation_logs；
 * 组织维度下钻经 student_profiles.class_id → classes → majors → colleges。
 * <p>
 * 权限：读接口要求 admin 角色或 log:view / log:manage 权限码，越权统一返回 20005。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final SystemLogRepository systemLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final ExportOperationLogRepository exportOperationLogRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    // ==================== 4.1 查询系统操作日志 ====================

    @Transactional(readOnly = true)
    public PageResult<SystemLogItem> listSystemLogs(Long operatorId, SystemLogQuery query, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(operatorId, "log:view");

        Set<Long> candidateUserIds = resolveOrgCandidateUserIds(query);
        if (candidateUserIds != null && candidateUserIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageParam);
        }

        Specification<SystemLog> spec = buildSystemLogSpec(query, candidateUserIds);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<SystemLog> page = systemLogRepository.findAll(spec, pageable);

        return PageResult.of(toSystemLogItems(page.getContent()), page.getTotalElements(), pageParam);
    }

    // ==================== 4.2 查询登录日志 ====================

    @Transactional(readOnly = true)
    public PageResult<LoginLogItem> listLoginLogs(Long operatorId, LoginLogQuery query, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(operatorId, "log:view");

        Specification<LoginLog> spec = buildLoginLogSpec(query);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<LoginLog> page = loginLogRepository.findAll(spec, pageable);

        return PageResult.of(toLoginLogItems(page.getContent()), page.getTotalElements(), pageParam);
    }

    // ==================== 4.3 查询导出操作日志 ====================

    @Transactional(readOnly = true)
    public PageResult<ExportLogItem> listExportLogs(Long operatorId, ExportLogQuery query, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(operatorId, "log:view");

        Specification<ExportOperationLog> spec = buildExportLogSpec(query);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<ExportOperationLog> page = exportOperationLogRepository.findAll(spec, pageable);

        return PageResult.of(toExportLogItems(page.getContent()), page.getTotalElements(), pageParam);
    }

    // ==================== 系统操作日志写入（供 AuditLogAspect 调用） ====================

    /**
     * 写入系统操作日志（独立事务）。
     * <p>
     * REQUIRES_NEW 确保无论主事务是否回滚，审计日志都能落库；
     * flush() 让 INSERT 在 try 块内执行，异常在 catch 中可见。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystemLog(SystemLog systemLog) {
        try {
            systemLogRepository.save(systemLog);
            systemLogRepository.flush();
        } catch (Exception e) {
            log.error("写入系统操作日志失败: operatorId={}, module={}, action={}",
                    systemLog.getOperatorId(), systemLog.getModule(), systemLog.getAction(), e);
        }
    }

    // ==================== 组织维度下钻 ====================

    /** 解析 classId / majorId / collegeId / grade 为被操作学生 ID 集合（多条件取交集，无筛选返回 null） */
    private Set<Long> resolveOrgCandidateUserIds(SystemLogQuery query) {
        Set<Long> result = null;
        if (query.getClassId() != null) {
            result = intersect(result, resolveClassStudentIds(Collections.singletonList(query.getClassId())));
        }
        if (query.getMajorId() != null) {
            result = intersect(result, resolveMajorStudentIds(Collections.singletonList(query.getMajorId())));
        }
        if (query.getCollegeId() != null) {
            result = intersect(result, resolveCollegeStudentIds(Collections.singletonList(query.getCollegeId())));
        }
        if (query.getGrade() != null && !query.getGrade().isBlank()) {
            result = intersect(result, resolveGradeStudentIds(query.getGrade()));
        }
        return result;
    }

    private Set<Long> resolveClassStudentIds(Collection<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptySet();
        }
        return studentProfileRepository.findByClassIdIn(new ArrayList<>(classIds)).stream()
                .map(StudentProfile::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Long> resolveMajorStudentIds(Collection<Long> majorIds) {
        if (majorIds == null || majorIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> classIds = clazzRepository.findByMajorIdIn(new ArrayList<>(majorIds)).stream()
                .map(Clazz::getId).filter(Objects::nonNull).collect(Collectors.toList());
        return classIds.isEmpty() ? Collections.emptySet() : resolveClassStudentIds(classIds);
    }

    private Set<Long> resolveCollegeStudentIds(Collection<Long> collegeIds) {
        if (collegeIds == null || collegeIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> majorIds = majorRepository.findByCollegeIdIn(new ArrayList<>(collegeIds)).stream()
                .map(Major::getId).filter(Objects::nonNull).collect(Collectors.toList());
        return majorIds.isEmpty() ? Collections.emptySet() : resolveMajorStudentIds(majorIds);
    }

    private Set<Long> resolveGradeStudentIds(String grade) {
        List<Long> classIds = clazzRepository.findByGrade(grade.trim()).stream()
                .map(Clazz::getId).filter(Objects::nonNull).collect(Collectors.toList());
        return classIds.isEmpty() ? Collections.emptySet() : resolveClassStudentIds(classIds);
    }

    private Set<Long> intersect(Set<Long> current, Set<Long> next) {
        if (current == null) {
            return new HashSet<>(next);
        }
        current.retainAll(next);
        return current;
    }

    // ==================== Specification 构建 ====================

    private Specification<SystemLog> buildSystemLogSpec(SystemLogQuery query, Set<Long> candidateUserIds) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getOperatorId() != null) {
                predicates.add(cb.equal(root.get("operatorId"), query.getOperatorId()));
            }
            if (query.getRoleId() != null) {
                predicates.add(cb.equal(root.get("roleId"), query.getRoleId()));
            }
            if (notBlank(query.getAction())) {
                predicates.add(cb.equal(root.get("action"), query.getAction().trim()));
            }
            if (notBlank(query.getModule())) {
                predicates.add(cb.equal(root.get("module"), query.getModule().trim()));
            }
            if (query.getLogLevel() != null) {
                predicates.add(cb.equal(root.get("logLevel"), query.getLogLevel()));
            }
            if (notBlank(query.getRelatedType())) {
                predicates.add(cb.equal(root.get("relatedType"), query.getRelatedType().trim()));
            }
            if (query.getRelatedId() != null) {
                predicates.add(cb.equal(root.get("relatedId"), query.getRelatedId()));
            }
            LocalDateTime start = parseTime(query.getStartTime());
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            LocalDateTime end = parseTime(query.getEndTime());
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            if (candidateUserIds != null) {
                predicates.add(root.get("userId").in(candidateUserIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<LoginLog> buildLoginLogSpec(LoginLogQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), query.getUserId()));
            }
            if (query.getLoginStatus() != null) {
                predicates.add(cb.equal(root.get("loginStatus"), query.getLoginStatus()));
            }
            if (notBlank(query.getIpAddress())) {
                predicates.add(cb.equal(root.get("ipAddress"), query.getIpAddress().trim()));
            }
            LocalDateTime start = parseTime(query.getStartTime());
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            LocalDateTime end = parseTime(query.getEndTime());
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<ExportOperationLog> buildExportLogSpec(ExportLogQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getOperatorId() != null) {
                predicates.add(cb.equal(root.get("operatorId"), query.getOperatorId()));
            }
            if (notBlank(query.getExportType())) {
                predicates.add(cb.equal(root.get("exportType"), query.getExportType().trim()));
            }
            if (query.getIsAnonymized() != null) {
                predicates.add(cb.equal(root.get("isAnonymized"), query.getIsAnonymized()));
            }
            LocalDateTime start = parseTime(query.getStartTime());
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            LocalDateTime end = parseTime(query.getEndTime());
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ==================== 响应组装 ====================

    private List<SystemLogItem> toSystemLogItems(List<SystemLog> logs) {
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> operatorIds = logs.stream().map(SystemLog::getOperatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = operatorIds.isEmpty() ? Map.of()
                : userRepository.findByIdIn(operatorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        return logs.stream().map(l -> SystemLogItem.builder()
                .id(l.getId())
                .operatorId(l.getOperatorId())
                .operatorName(l.getOperatorId() != null ? nameMap.get(l.getOperatorId()) : null)
                .roleId(l.getRoleId())
                .roleName(l.getRoleName())
                .action(l.getAction())
                .module(l.getModule())
                .description(l.getDescription())
                .relatedType(l.getRelatedType())
                .relatedId(l.getRelatedId())
                .beforeData(parseJson(l.getBeforeData()))
                .afterData(parseJson(l.getAfterData()))
                .ipAddress(l.getIpAddress())
                .createdAt(toIso(l.getCreatedAt()))
                .build()).collect(Collectors.toList());
    }

    private List<LoginLogItem> toLoginLogItems(List<LoginLog> logs) {
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = logs.stream().map(LoginLog::getUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = userIds.isEmpty() ? Map.of()
                : userRepository.findByIdIn(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        return logs.stream().map(l -> LoginLogItem.builder()
                .id(l.getId())
                .userId(l.getUserId())
                .userName(l.getUserId() != null ? nameMap.get(l.getUserId()) : null)
                .loginType(l.getLoginType())
                .ipAddress(l.getIpAddress())
                .userAgent(l.getUserAgent())
                .loginStatus(l.getLoginStatus())
                .loginAt(toIso(l.getCreatedAt()))
                .logoutAt(null)
                .build()).collect(Collectors.toList());
    }

    private List<ExportLogItem> toExportLogItems(List<ExportOperationLog> logs) {
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> operatorIds = logs.stream().map(ExportOperationLog::getOperatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = operatorIds.isEmpty() ? Map.of()
                : userRepository.findByIdIn(operatorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        Map<Long, String> roleNameMap = operatorIds.stream()
                .collect(Collectors.toMap(id -> id, id -> {
                    AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(id);
                    return role != null ? role.roleName() : null;
                }, (a, b) -> a));

        return logs.stream().map(l -> ExportLogItem.builder()
                .id(l.getId())
                .exportJobId(null)
                .operatorId(l.getOperatorId())
                .operatorName(l.getOperatorId() != null ? nameMap.get(l.getOperatorId()) : null)
                .roleName(l.getOperatorId() != null ? roleNameMap.get(l.getOperatorId()) : null)
                .exportType(l.getExportType())
                .isAnonymized(l.getIsAnonymized())
                .filterConditions(parseJson(l.getFilterConditions()))
                .downloadedAt(null)
                .ipAddress(null)
                .createdAt(toIso(l.getCreatedAt()))
                .build()).collect(Collectors.toList());
    }

    // ==================== 通用辅助 ====================

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** 解析 ISO 8601 时间字符串（支持带时区，统一转为系统默认时区 LocalDateTime） */
    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        try {
            return OffsetDateTime.parse(v).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(v);
            } catch (Exception e2) {
                log.warn("无法解析时间参数: {}", v);
                return null;
            }
        }
    }

    /** JSON 字符串 → JsonNode（便于在响应中输出为对象而非字符串），解析失败时回退原始字符串 */
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
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    // ==================== 内嵌 POJO ====================

    /** 4.1 查询条件 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemLogQuery {
        private Long operatorId;
        private Long roleId;
        private String action;
        private String module;
        private Integer logLevel;
        private String startTime;
        private String endTime;
        private String relatedType;
        private Long relatedId;
        private String grade;
        private Long collegeId;
        private Long majorId;
        private Long classId;
    }

    /** 4.1 列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SystemLogItem {
        private Long id;
        private Long operatorId;
        private String operatorName;
        private Long roleId;
        private String roleName;
        private String action;
        private String module;
        private String description;
        private String relatedType;
        private Long relatedId;
        private Object beforeData;
        private Object afterData;
        private String ipAddress;
        private String createdAt;
    }

    /** 4.2 查询条件 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginLogQuery {
        private Long userId;
        private Integer loginStatus;
        private String startTime;
        private String endTime;
        private String ipAddress;
    }

    /** 4.2 列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginLogItem {
        private Long id;
        private Long userId;
        private String userName;
        private Integer loginType;
        private String ipAddress;
        private String userAgent;
        private Integer loginStatus;
        private String loginAt;
        private String logoutAt;
    }

    /** 4.3 查询条件 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportLogQuery {
        private Long operatorId;
        private String exportType;
        private Integer isAnonymized;
        private String startTime;
        private String endTime;
    }

    /** 4.3 列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExportLogItem {
        private Long id;
        private Long exportJobId;
        private Long operatorId;
        private String operatorName;
        private String roleName;
        private String exportType;
        private Integer isAnonymized;
        private Object filterConditions;
        private String downloadedAt;
        private String ipAddress;
        private String createdAt;
    }
}