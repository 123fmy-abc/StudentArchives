package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.home.response.TeacherDashboardOverviewResponse;
import com.example.studentarchives.dto.Fmy.home.response.TeacherDashboardOverviewResponse.PendingStats;
import com.example.studentarchives.dto.Fmy.home.response.TeacherDashboardOverviewResponse.RecentAuditItem;
import com.example.studentarchives.dto.Fmy.home.response.TeacherDashboardOverviewResponse.ScopeItem;
import com.example.studentarchives.entity.approval.PendingApproval;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.log.AuditLog;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.ApprovableTypeEnum;
import com.example.studentarchives.enums.AuditActionEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AuditLogRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PendingApprovalRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 教师首页数据概览服务（《教师端接口文档》3.1 GET /teacher/dashboard）
 * <p>
 * 聚合当前教师的真实数据返回：
 * <ul>
 *   <li>teacherName / teacherNo → users；</li>
 *   <li>currentSemesterId / currentSemesterName → semesters（is_current=1）；</li>
 *   <li>scopes → role_scopes（生效中的授权范围，按 scopeType+scopeId 去重并解析名称）；</li>
 *   <li>pendingStats → pending_approvals（auditor_id=当前教师 且 status=1，按 approvable_type 分组）；</li>
 *   <li>todayAudited / recentAudits → audit_logs（auditor_id=当前教师）；</li>
 *   <li>unreadMessageCount → user_messages（is_read=0 且 is_archived=0）。</li>
 * </ul>
 * 数据口径与《教师端接口文档》3.1 及数据库表结构（pending_approvals / audit_logs /
 * role_scopes / user_messages）保持一致。教师登录即可访问，无需管理员权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    /** 待审批状态（pending_approvals.status=1） */
    private static final int PENDING_STATUS = 1;

    /** 范围类型：2=学院 3=专业 4=班级 */
    private static final int SCOPE_COLLEGE = 2;
    private static final int SCOPE_MAJOR = 3;
    private static final int SCOPE_CLASS = 4;

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final PendingApprovalRepository pendingApprovalRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMessageRepository userMessageRepository;
    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final CollegeRepository collegeRepository;
    private final MajorRepository majorRepository;
    private final ClazzRepository clazzRepository;

    /**
     * 获取教师首页数据概览
     *
     * @param userId 当前登录教师用户 ID
     * @return 教师首页概览
     */
    @Transactional(readOnly = true)
    public TeacherDashboardOverviewResponse getDashboard(Long userId) {
        User teacher = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "教师不存在"));
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        Semester currentSemester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);

        List<RoleScope> scopes = scopeValidator.effectiveScopes(userId);
        List<ScopeItem> scopeItems = buildScopeItems(scopes);

        // ==================== 待办统计（pending_approvals） ====================
        List<PendingApproval> pendings = pendingApprovalRepository
                .findByAuditorIdAndStatusOrderBySubmittedAtAsc(userId, PENDING_STATUS);
        PendingStats pendingStats = buildPendingStats(pendings);

        // ==================== 今日审核 + 最近审核动态（audit_logs） ====================
        LocalDateTime dayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime dayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayAudited = auditLogRepository.countByAuditorIdAndCreatedAtBetween(userId, dayStart, dayEnd);
        List<RecentAuditItem> recentAudits = buildRecentAudits(userId);

        // ==================== 未读消息（user_messages） ====================
        long unread = userMessageRepository.countByUserIdAndIsReadAndIsArchived(userId, 0, 0);

        return TeacherDashboardOverviewResponse.builder()
                .teacherName(teacher.getName())
                .teacherNo(teacher.getUserNo())
                .currentSemesterId(currentSemester != null ? currentSemester.getId() : null)
                .currentSemesterName(currentSemester != null ? currentSemester.getName() : null)
                .scopes(scopeItems)
                .pendingStats(pendingStats)
                .todayAudited(todayAudited)
                .recentAudits(recentAudits)
                .unreadMessageCount(unread)
                .build();
    }

    // ==================== 授权范围 ====================

    /**
     * 授权范围 → 响应项：按 scopeType+scopeId 去重（同一范围多角色只展示一次），
     * 解析范围名称（班级/专业/学院），学校级与年级级授权不在此展开。
     */
    private List<ScopeItem> buildScopeItems(List<RoleScope> scopes) {
        Map<String, RoleScope> deduped = new LinkedHashMap<>();
        for (RoleScope s : scopes) {
            if (s.getScopeType() == null || s.getScopeId() == null) {
                continue;
            }
            deduped.putIfAbsent(s.getScopeType() + "-" + s.getScopeId(), s);
        }
        List<ScopeItem> items = new ArrayList<>();
        for (RoleScope s : deduped.values()) {
            items.add(ScopeItem.builder()
                    .scopeType(s.getScopeType())
                    .scopeId(s.getScopeId())
                    .scopeName(resolveScopeName(s))
                    .build());
        }
        return items;
    }

    /** 按范围类型解析范围名称（班级/专业/学院；其他类型返回 null） */
    private String resolveScopeName(RoleScope scope) {
        Long scopeId = scope.getScopeId();
        if (scopeId == null) {
            return null;
        }
        return switch (scope.getScopeType()) {
            case SCOPE_COLLEGE -> collegeRepository.findById(scopeId).map(College::getName).orElse(null);
            case SCOPE_MAJOR -> majorRepository.findById(scopeId).map(Major::getName).orElse(null);
            case SCOPE_CLASS -> clazzRepository.findById(scopeId).map(Clazz::getName).orElse(null);
            default -> null;
        };
    }

    // ==================== 待办统计 ====================

    /**
     * 待审批任务 → 按 approvable_type 分组统计（Archive / AwardApplication / CareerPlan）
     */
    private PendingStats buildPendingStats(List<PendingApproval> pendings) {
        Map<String, Long> byType = pendings.stream()
                .map(PendingApproval::getApprovableType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        long archivePending = byType.getOrDefault(ApprovableTypeEnum.ARCHIVE.getValue(), 0L);
        long awardPending = byType.getOrDefault(ApprovableTypeEnum.AWARD_APPLICATION.getValue(), 0L);
        long careerPlanPending = byType.getOrDefault(ApprovableTypeEnum.CAREER_PLAN.getValue(), 0L);

        return PendingStats.builder()
                .archivePending(archivePending)
                .awardPending(awardPending)
                .careerPlanPending(careerPlanPending)
                .totalPending((long) pendings.size())
                .build();
    }

    // ==================== 最近审核动态 ====================

    /**
     * 最近审核记录 → 响应项：按 auditable_type + auditable_id 回查业务记录
     * （archives / award_applications / career_plans）补充标题、类型与申请人信息。
     */
    private List<RecentAuditItem> buildRecentAudits(Long teacherId) {
        List<AuditLog> logs = auditLogRepository.findTop10ByAuditorIdOrderByCreatedAtDesc(teacherId);
        List<RecentAuditItem> items = new ArrayList<>();
        for (AuditLog log : logs) {
            RecentAuditItem item = toRecentAuditItem(log);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /** 单条审核记录 → 响应项（业务对象缺失时跳过该条，不阻断整体） */
    private RecentAuditItem toRecentAuditItem(AuditLog log) {
        String auditableType = log.getAuditableType();
        Long auditableId = log.getAuditableId();
        Long applicantId = null;
        String title = null;
        String archiveType = null;
        String type = null;

        if (auditableId == null) {
            return null;
        }
        if (ApprovableTypeEnum.ARCHIVE.getValue().equals(auditableType)) {
            Archive archive = archiveRepository.findById(auditableId).orElse(null);
            if (archive == null) {
                return null;
            }
            type = "archive";
            title = archive.getTitle();
            archiveType = archive.getArchiveType();
            applicantId = archive.getUserId();
        } else if (ApprovableTypeEnum.AWARD_APPLICATION.getValue().equals(auditableType)) {
            AwardApplication award = awardApplicationRepository.findById(auditableId).orElse(null);
            if (award == null) {
                return null;
            }
            type = "award";
            title = award.getTitle();
            archiveType = award.getAwardType();
            applicantId = award.getUserId();
        } else if (ApprovableTypeEnum.CAREER_PLAN.getValue().equals(auditableType)) {
            CareerPlan plan = careerPlanRepository.findById(auditableId).orElse(null);
            if (plan == null) {
                return null;
            }
            type = "career_plan";
            title = plan.getTitle();
            applicantId = plan.getUserId();
        } else {
            return null;
        }

        String studentName = null;
        String studentNo = null;
        if (applicantId != null) {
            User applicant = userRepository.findById(applicantId).orElse(null);
            if (applicant != null) {
                studentName = applicant.getName();
                studentNo = applicant.getUserNo();
            }
        }

        AuditActionEnum actionEnum = AuditActionEnum.of(log.getAction());
        return RecentAuditItem.builder()
                .id(log.getId())
                .type(type)
                .archiveType(archiveType)
                .title(title)
                .studentName(studentName)
                .studentNo(studentNo)
                .action(log.getAction())
                .actionLabel(actionEnum != null ? actionEnum.getLabel() : null)
                .auditedAt(toIso(log.getCreatedAt()))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
