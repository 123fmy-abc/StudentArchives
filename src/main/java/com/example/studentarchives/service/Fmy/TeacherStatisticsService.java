package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.TeacherDashboardResponse;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.evaluation.OrgArchiveSummary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AdminOrgArchiveSummaryRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端统计看板服务（《教师端接口文档》十六、统计看板模块）
 * <p>
 * <ul>
 *   <li>11.1 范围统计看板：复用 {@code org_archive_summaries} 快照 + {@code statistics_cache}
 *       聚合与缓存读取基础（与管理端 16.1/16.2 统计同源，{@code DimensionAvgScoreItem} /
 *       {@code StatisticsTypeCountItem} DTO 直接复用），教师 wrapper 按 {@code role_scopes}
 *       限定 {@code scopeType}/{@code scopeId}；审批状态计数（submitted/approved/pending/rejected）
 *       为教师侧新增，按 archives / award_applications / career_plans 状态补充。</li>
 *   <li>11.3 成果热力图：复用 {@link AdminStatisticsService#heatmapByTeacher}（管理端 16.3 引擎，
 *       教师侧限定组织行与指标子集）。</li>
 * </ul>
 * 权限码 {@code statistics:view}，越权返回 20005 无访问权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherStatisticsService {

    /** 教师端统计看板权限码（《教师端接口文档》关键权限码） */
    private static final String STATS_PERMISSION = "statistics:view";

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 6=年级 */
    private static final int SCOPE_SCHOOL = 1;
    private static final int SCOPE_COLLEGE = 2;
    private static final int SCOPE_MAJOR = 3;
    private static final int SCOPE_CLASS = 4;
    private static final int SCOPE_GRADE = 6;

    /** 审批状态（对齐 ApplyStatusEnum）：1=待审批 2=通过 3=已退回 */
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final AdminStatisticsService adminStatisticsService;
    private final AdminOrgArchiveSummaryRepository orgArchiveSummaryRepository;
    private final SemesterRepository semesterRepository;
    private final SchoolRepository schoolRepository;
    private final CollegeRepository collegeRepository;
    private final MajorRepository majorRepository;
    private final ClazzRepository clazzRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final CareerPlanRepository careerPlanRepository;

    /**
     * 教师范围统计看板（GET /teacher/statistics/dashboard，教师端文档 11.1）
     * <p>
     * 数据来源：org_archive_summaries 快照（studentCount / averageGpa / dimensionAvgScores /
     * archiveTypeDistribution）+ 实时审批状态计数。scopeType/scopeId 未传时取教师首个生效
     * 授权范围（学校级授权 → 学校整体看板）。
     *
     * @param userId     当前教师用户 ID
     * @param scopeType  范围类型：1/2/3/4/6
     * @param scopeId    范围 ID
     * @param semesterId 学期 ID（不传取当前学期）
     * @return 看板数据（cacheHit 标记快照来源，响应头 X-Cache-Hit 使用）
     */
    @Transactional(readOnly = true)
    public AdminStatisticsService.StatsResult<TeacherDashboardResponse> getDashboard(
            Long userId, Integer scopeType, Long scopeId, Long semesterId) {
        adminAuthService.requireAdminOrPermission(userId, STATS_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        // 默认范围：未传时取教师首个生效授权范围
        if (scopeType == null) {
            ScopeHint hint = resolveDefaultScope(userId, schoolId);
            scopeType = hint.scopeType();
            scopeId = hint.scopeId();
        }
        if (scopeType == null || (scopeId == null && scopeType != SCOPE_GRADE)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "scopeType/scopeId 不能为空");
        }
        scopeValidator.ensureOrgInScope(userId, scopeType, scopeId, schoolId);

        Long effSemesterId = semesterId;
        if (effSemesterId == null) {
            effSemesterId = semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);
        }

        // 快照：studentCount / averageGpa / dimensionAvgScores / archiveTypeDistribution
        OrgArchiveSummary snapshot = null;
        if (!(scopeType == SCOPE_GRADE && scopeId == null)) {
            snapshot = orgArchiveSummaryRepository.findLatestByOrg(scopeType, scopeId, effSemesterId).orElse(null);
        }
        boolean cacheHit = snapshot != null;

        // 审批状态计数（archives / award_applications / career_plans）
        List<Long> studentIds = resolveStudentIds(schoolId, scopeType, scopeId);
        ApprovalCounts counts = countApprovals(studentIds, effSemesterId);

        String scopeName = resolveScopeName(scopeType, scopeId);
        TeacherDashboardResponse data = TeacherDashboardResponse.builder()
                .semesterId(effSemesterId)
                .scopeName(scopeName)
                .studentCount(snapshot != null ? snapshot.getTotalStudents() : studentIds.size())
                .submittedCount(counts.submitted())
                .approvedCount(counts.approved())
                .pendingCount(counts.pending())
                .rejectedCount(counts.rejected())
                .averageGpa(snapshot != null && snapshot.getAvgGpa() != null
                        ? snapshot.getAvgGpa().doubleValue() : null)
                .dimensionAvgScores(snapshot != null
                        ? adminStatisticsService.parseTopDimensions(snapshot.getTopDimensions()) : List.of())
                .archiveTypeDistribution(snapshot != null
                        ? adminStatisticsService.parseTypeDistribution(snapshot.getArchiveTypeDistribution()) : List.of())
                .cacheHit(cacheHit)
                .build();
        return new AdminStatisticsService.StatsResult<>(data, "MISS");
    }

    /**
     * 班级/专业级成果热力图（GET /teacher/statistics/heatmap，教师端文档 11.3）
     * <p>
     * 复用管理端 16.3 引擎（{@link AdminStatisticsService#heatmapByTeacher}）：教师侧校验
     * orgType/orgId 在 role_scopes 授权范围内，orgId 为空时仅返回授权范围组织行。
     *
     * @param userId     当前教师用户 ID
     * @param semesterId 学期 ID（传则仅单学期列）
     * @param orgType    行维度：2=学院 3=专业 4=班级
     * @param orgId      上级组织 ID（可选，下钻）
     * @param metric     指标：gpa/award/practice/archive
     * @param grade      年级筛选（可选）
     * @return 热力图矩阵
     */
    @Transactional(readOnly = true)
    public AdminStatisticsService.StatsResult<HeatmapResponse> getHeatmap(
            Long userId, Long semesterId, Integer orgType, Long orgId, String metric, String grade) {
        return adminStatisticsService.heatmapByTeacher(userId, semesterId, orgType, orgId, metric, grade);
    }

    // ==================== 私有辅助方法 ====================

    /** 默认范围：优先教师 is_primary 授权，否则取首个生效授权；学校级 → 学校整体看板 */
    private ScopeHint resolveDefaultScope(Long userId, Long schoolId) {
        List<RoleScope> scopes = scopeValidator.effectiveScopes(userId);
        if (scopes.isEmpty()) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        RoleScope primary = scopes.stream()
                .filter(s -> s.getScopeType() != null && Objects.equals(s.getIsPrimary(), 1))
                .findFirst()
                .orElseGet(() -> scopes.stream().filter(s -> s.getScopeType() != null).findFirst().orElse(null));
        if (primary == null) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        if (Objects.equals(primary.getScopeType(), SCOPE_SCHOOL)) {
            return new ScopeHint(SCOPE_SCHOOL, schoolId);
        }
        return new ScopeHint(primary.getScopeType(), primary.getScopeId());
    }

    /** 解析范围内学生 userId 列表（学校/学院/专业/班级/年级） */
    private List<Long> resolveStudentIds(Long schoolId, Integer scopeType, Long scopeId) {
        List<Long> classIds;
        switch (scopeType == null ? -1 : scopeType) {
            case SCOPE_SCHOOL:
                return studentProfileRepository.findBySchoolId(schoolId).stream()
                        .map(StudentProfile::getUserId).collect(Collectors.toList());
            case SCOPE_COLLEGE:
                List<Long> collegeMajorIds = scopeId == null ? List.of()
                        : majorRepository.findByCollegeIdIn(Set.of(scopeId)).stream()
                                .map(Major::getId).collect(Collectors.toList());
                classIds = collegeMajorIds.isEmpty() ? List.of()
                        : clazzRepository.findByMajorIdIn(collegeMajorIds).stream()
                                .map(Clazz::getId).collect(Collectors.toList());
                break;
            case SCOPE_MAJOR:
                classIds = scopeId == null ? List.of()
                        : clazzRepository.findByMajorId(scopeId).stream()
                                .map(Clazz::getId).collect(Collectors.toList());
                break;
            case SCOPE_GRADE:
                classIds = scopeId == null ? List.of()
                        : clazzRepository.findByGrade(String.valueOf(scopeId)).stream()
                                .map(Clazz::getId).collect(Collectors.toList());
                break;
            case SCOPE_CLASS:
                return scopeId == null ? List.of()
                        : studentProfileRepository.findByClassId(scopeId).stream()
                                .map(StudentProfile::getUserId).collect(Collectors.toList());
            default:
                return List.of();
        }
        return classIds.isEmpty() ? List.of()
                : studentProfileRepository.findByClassIdIn(classIds).stream()
                        .map(StudentProfile::getUserId).collect(Collectors.toList());
    }

    /** 审批状态计数：submitted = approved + pending + rejected（排除草稿 0 与已撤销 4） */
    private ApprovalCounts countApprovals(List<Long> studentIds, Long semesterId) {
        if (studentIds.isEmpty()) {
            return new ApprovalCounts(0, 0, 0, 0);
        }
        long approved = 0, pending = 0, rejected = 0;
        for (Archive a : archiveRepository.findByUserIdIn(studentIds)) {
            if (semesterId != null && !Objects.equals(a.getSemesterId(), semesterId)) {
                continue;
            }
            approved += statusCount(a.getStatus(), STATUS_APPROVED);
            pending += statusCount(a.getStatus(), STATUS_PENDING);
            rejected += statusCount(a.getStatus(), STATUS_REJECTED);
        }
        for (AwardApplication a : awardApplicationRepository.findByUserIdIn(studentIds)) {
            if (semesterId != null && !Objects.equals(a.getSemesterId(), semesterId)) {
                continue;
            }
            approved += statusCount(a.getStatus(), STATUS_APPROVED);
            pending += statusCount(a.getStatus(), STATUS_PENDING);
            rejected += statusCount(a.getStatus(), STATUS_REJECTED);
        }
        for (CareerPlan c : careerPlanRepository.findByUserIdIn(studentIds)) {
            if (semesterId != null && !Objects.equals(c.getSemesterId(), semesterId)) {
                continue;
            }
            approved += statusCount(c.getStatus(), STATUS_APPROVED);
            pending += statusCount(c.getStatus(), STATUS_PENDING);
            rejected += statusCount(c.getStatus(), STATUS_REJECTED);
        }
        return new ApprovalCounts((int) (approved + pending + rejected), (int) approved, (int) pending, (int) rejected);
    }

    private int statusCount(Integer status, int target) {
        return status != null && status == target ? 1 : 0;
    }

    /** 范围名称解析（学校/学院/专业/班级名称；年级拼接 "N级"） */
    private String resolveScopeName(Integer scopeType, Long scopeId) {
        if (scopeType == null) {
            return null;
        }
        switch (scopeType) {
            case SCOPE_SCHOOL:
                return scopeId != null ? schoolRepository.findById(scopeId).map(School::getName).orElse(null) : null;
            case SCOPE_COLLEGE:
                return scopeId != null ? collegeRepository.findById(scopeId).map(College::getName).orElse(null) : null;
            case SCOPE_MAJOR:
                return scopeId != null ? majorRepository.findById(scopeId).map(Major::getName).orElse(null) : null;
            case SCOPE_CLASS:
                return scopeId != null ? clazzRepository.findById(scopeId).map(Clazz::getName).orElse(null) : null;
            case SCOPE_GRADE:
                return scopeId != null ? scopeId + "级" : "年级";
            default:
                return null;
        }
    }

    /** 默认范围提示 */
    private record ScopeHint(Integer scopeType, Long scopeId) {
    }

    /** 审批状态计数结果 */
    private record ApprovalCounts(Integer submitted, Integer approved, Integer pending, Integer rejected) {
    }
}
