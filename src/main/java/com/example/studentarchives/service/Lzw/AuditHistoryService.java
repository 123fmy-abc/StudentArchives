package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.log.AuditLog;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.AuditActionEnum;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AuditLogRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端「审核历史模块」Service（《教师端接口文档》五，5.1）。
 * <p>
 * 数据来源：audit_logs，按 {@code auditor_id} 过滤当前教师；标题/类型/学生信息
 * 由 auditableType + auditableId 关联 archives / award_applications / career_plans 解析。
 * 动作枚举对齐 {@link AuditActionEnum}（1通过 2退回 3撤回 4转交）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditHistoryService {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AuditLogRepository auditLogRepository;
    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final UserRepository userRepository;

    /**
     * 获取我的审核记录（GET /teacher/audits/history）
     *
     * @param teacherId  当前教师用户 ID
     * @param type       业务类型：archive/award/career_plan（可选）
     * @param action     审核动作：1通过 2退回 3撤回 4转交（可选）
     * @param semesterId 学期 ID（可选）
     * @param startDate  开始时间 YYYY-MM-DD（可选）
     * @param endDate    结束时间 YYYY-MM-DD（可选）
     * @param keyword    搜索学生姓名/学号/标题（可选）
     */
    public PageResult<HistoryItem> listHistory(Long teacherId, String type, Integer action,
                                               Long semesterId, String startDate, String endDate,
                                               String keyword, PageParam pageParam) {
        List<AuditLog> logs = auditLogRepository.findByAuditorIdOrderByIdDesc(teacherId);
        if (logs.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageParam);
        }

        // 批量加载申报主体记录（按类型分组）
        List<Long> archiveIds = new ArrayList<>();
        List<Long> awardIds = new ArrayList<>();
        List<Long> planIds = new ArrayList<>();
        for (AuditLog l : logs) {
            String at = l.getAuditableType();
            if ("Archive".equals(at)) {
                archiveIds.add(l.getAuditableId());
            } else if ("AwardApplication".equals(at)) {
                awardIds.add(l.getAuditableId());
            } else if ("CareerPlan".equals(at)) {
                planIds.add(l.getAuditableId());
            }
        }
        Map<Long, Archive> archiveMap = toMap(archiveRepository.findAllById(archiveIds), Archive::getId);
        Map<Long, AwardApplication> awardMap = toMap(awardApplicationRepository.findAllById(awardIds), AwardApplication::getId);
        Map<Long, CareerPlan> planMap = toMap(careerPlanRepository.findAllById(planIds), CareerPlan::getId);

        // 批量加载学生用户
        Set<Long> userIds = new HashSet<>();
        for (AuditLog l : logs) {
            BusinessInfo info = resolveBusinessInfo(l.getAuditableType(), l.getAuditableId(),
                    archiveMap, awardMap, planMap);
            if (info != null && info.userId() != null) {
                userIds.add(info.userId());
            }
        }
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userRepository.findByIdIn(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        LocalDate sd = parseDate(startDate);
        LocalDate ed = parseDate(endDate);
        String kw = keyword == null ? null : keyword.trim().toLowerCase();

        List<HistoryItem> result = new ArrayList<>();
        for (AuditLog l : logs) {
            String typeCode = toTypeCode(l.getAuditableType());
            if (type != null && !type.isEmpty() && !type.equals(typeCode)) {
                continue;
            }
            if (action != null && !action.equals(l.getAction())) {
                continue;
            }
            BusinessInfo info = resolveBusinessInfo(l.getAuditableType(), l.getAuditableId(),
                    archiveMap, awardMap, planMap);
            if (semesterId != null && (info == null || !semesterId.equals(info.semesterId()))) {
                continue;
            }
            LocalDate d = l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate() : null;
            if (sd != null && (d == null || d.isBefore(sd))) {
                continue;
            }
            if (ed != null && (d == null || d.isAfter(ed))) {
                continue;
            }
            String studentName = null;
            String studentNo = null;
            if (info != null && info.userId() != null) {
                User u = userMap.get(info.userId());
                if (u != null) {
                    studentName = u.getName();
                    studentNo = u.getUserNo();
                }
            }
            if (kw != null && !kw.isEmpty()
                    && !contains(studentName, kw) && !contains(studentNo, kw)
                    && !contains(info != null ? info.title() : null, kw)) {
                continue;
            }
            AuditActionEnum actionEnum = AuditActionEnum.of(l.getAction());
            result.add(HistoryItem.builder()
                    .auditId(l.getId())
                    .type(typeCode)
                    .archiveType(info != null ? info.archiveType() : null)
                    .title(info != null ? info.title() : null)
                    .studentName(studentName)
                    .studentNo(studentNo)
                    .action(l.getAction())
                    .actionLabel(actionEnum != null ? actionEnum.getLabel() : null)
                    .comment(l.getComment())
                    .auditedAt(format(l.getCreatedAt()))
                    .build());
        }

        int total = result.size();
        int from = pageParam.getOffset();
        int to = Math.min(from + pageParam.getPerPage(), total);
        List<HistoryItem> page = from < total ? result.subList(from, to) : Collections.emptyList();
        return PageResult.of(page, total, pageParam);
    }

    // ==================== 私有：字段解析 ====================

    /** 申报主体快照（类型编码 / 标题 / 学生用户 ID / 学期 ID） */
    private record BusinessInfo(String archiveType, String title, Long userId, Long semesterId) {
    }

    private BusinessInfo resolveBusinessInfo(String approvableType, Long approvableId,
                                             Map<Long, Archive> archiveMap,
                                             Map<Long, AwardApplication> awardMap,
                                             Map<Long, CareerPlan> planMap) {
        if (approvableType == null || approvableId == null) {
            return null;
        }
        if ("Archive".equals(approvableType)) {
            Archive a = archiveMap.get(approvableId);
            if (a == null) {
                return null;
            }
            return new BusinessInfo(a.getArchiveType(), a.getTitle(), a.getUserId(), a.getSemesterId());
        }
        if ("AwardApplication".equals(approvableType)) {
            AwardApplication a = awardMap.get(approvableId);
            if (a == null) {
                return null;
            }
            return new BusinessInfo(a.getAwardType(), a.getTitle(), a.getUserId(), a.getSemesterId());
        }
        if ("CareerPlan".equals(approvableType)) {
            CareerPlan p = planMap.get(approvableId);
            if (p == null) {
                return null;
            }
            return new BusinessInfo(null, p.getTitle(), p.getUserId(), p.getSemesterId());
        }
        return null;
    }

    private String toTypeCode(String approvableType) {
        if (approvableType == null) {
            return null;
        }
        return switch (approvableType) {
            case "Archive" -> "archive";
            case "AwardApplication" -> "award";
            case "CareerPlan" -> "career_plan";
            default -> approvableType;
        };
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            log.warn("无法解析日期参数: {}", s);
            return null;
        }
    }

    private static boolean contains(String s, String k) {
        return s != null && s.toLowerCase().contains(k);
    }

    private static String format(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(ZONE).format(ISO_WITH_ZONE);
    }

    private static <T> Map<Long, T> toMap(List<T> list, java.util.function.Function<T, Long> keyFn) {
        if (list.isEmpty()) {
            return Map.of();
        }
        return list.stream().collect(Collectors.toMap(keyFn, t -> t, (a, b) -> a));
    }

    // ==================== 响应 DTO ====================

    /** 5.1 列表项 */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HistoryItem {
        private Long auditId;
        private String type;
        private String archiveType;
        private String title;
        private String studentName;
        private String studentNo;
        private Integer action;
        private String actionLabel;
        private String comment;
        private String auditedAt;
    }
}