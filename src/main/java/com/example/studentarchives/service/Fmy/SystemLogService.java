package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.log.request.SystemLogQueryRequest;
import com.example.studentarchives.dto.Fmy.log.response.SystemLogItemResponse;
import com.example.studentarchives.entity.log.SystemLog;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.repository.SystemLogRepository;
import com.example.studentarchives.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统操作日志服务
 * <p>
 * 落库：承接 {@code AuditLogAspect} 的审计日志写入 {@code system_logs} 表，
 * 独立事务（REQUIRES_NEW）确保日志写入失败不影响被审计的业务调用。
 * 查询：支撑管理端 GET /admin/logs/system 分页筛选（管理端文档 3.1）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogService {

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录系统操作日志（独立事务）
     * <p>
     * REQUIRES_NEW 确保无论主事务是否回滚，日志都能写入数据库；内部吞掉异常，
     * 日志写入失败不向上抛出，避免影响被审计的业务调用。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystemLog(SystemLog systemLog) {
        try {
            systemLogRepository.save(systemLog);
            systemLogRepository.flush();
        } catch (Exception e) {
            log.error("记录系统日志失败: module={}, action={}, operatorId={}",
                    systemLog.getModule(), systemLog.getAction(), systemLog.getOperatorId(), e);
        }
    }

    /**
     * 分页查询系统操作日志（管理端文档 3.1）
     *
     * @param query     筛选条件
     * @param pageParam 分页参数
     * @return 分页结果（operatorName 联查 users 表）
     */
    @Transactional(readOnly = true)
    public PageResult<SystemLogItemResponse> listSystemLogs(SystemLogQueryRequest query, PageParam pageParam) {
        Specification<SystemLog> spec = buildSpecification(query);
        String safeSortBy = pageParam.getSafeSortBy(Set.of("createdAt", "id"));
        Sort sort = ("asc".equals(pageParam.getSortOrder()) ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending());
        if (safeSortBy != null) {
            sort = ("asc".equals(pageParam.getSortOrder()) ? Sort.by(safeSortBy).ascending()
                    : Sort.by(safeSortBy).descending());
        }
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<SystemLog> page = systemLogRepository.findAll(spec, pageable);

        // 批量联查操作人姓名（users 表）
        Map<Long, String> nameMap = resolveOperatorNames(page.getContent().stream()
                .map(SystemLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<SystemLogItemResponse> items = page.getContent().stream()
                .map(log -> SystemLogItemResponse.builder()
                        .id(log.getId())
                        .operatorId(log.getOperatorId())
                        .operatorName(log.getOperatorId() != null ? nameMap.get(log.getOperatorId()) : null)
                        .roleId(log.getRoleId())
                        .roleName(log.getRoleName())
                        .action(log.getAction())
                        .module(log.getModule())
                        .description(log.getDescription())
                        .beforeData(parseJson(log.getBeforeData()))
                        .afterData(parseJson(log.getAfterData()))
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt() != null ? toIso(log.getCreatedAt()) : null)
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 私有辅助方法 ====================

    /** 动态筛选条件（管理端文档 3.1 请求参数） */
    private Specification<SystemLog> buildSpecification(SystemLogQueryRequest query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getOperatorId() != null) {
                predicates.add(cb.equal(root.get("operatorId"), query.getOperatorId()));
            }
            if (query.getRoleId() != null) {
                predicates.add(cb.equal(root.get("roleId"), query.getRoleId()));
            }
            if (query.getAction() != null && !query.getAction().isBlank()) {
                predicates.add(cb.equal(root.get("action"), query.getAction()));
            }
            if (query.getModule() != null && !query.getModule().isBlank()) {
                predicates.add(cb.equal(root.get("module"), query.getModule()));
            }
            if (query.getLogLevel() != null) {
                predicates.add(cb.equal(root.get("logLevel"), query.getLogLevel()));
            }
            if (query.getRelatedType() != null && !query.getRelatedType().isBlank()) {
                predicates.add(cb.equal(root.get("relatedType"), query.getRelatedType()));
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 批量解析操作人姓名：users.id → users.name */
    private Map<Long, String> resolveOperatorNames(Collection<Long> operatorIds) {
        if (operatorIds == null || operatorIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByIdIn(operatorIds).stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : "",
                        (a, b) -> a));
    }

    /** 解析 JSON 字符串 → JsonNode（非法 JSON 返回 null） */
    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("日志 JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /** 解析时间字符串：优先 ISO 8601 带时区，回退 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String trimmed = time.trim();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (DateTimeParseException ignore) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
