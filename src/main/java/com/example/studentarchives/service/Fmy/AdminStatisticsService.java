package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.statistics.response.DashboardResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.DimensionAvgScoreItem;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapRow;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapSemesterItem;
import com.example.studentarchives.dto.Fmy.statistics.response.OrgOverviewResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.OrgOverviewRow;
import com.example.studentarchives.dto.Fmy.statistics.response.SnapshotRefreshResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.StatisticsParentOrg;
import com.example.studentarchives.dto.Fmy.statistics.response.StatisticsTypeCountItem;
import com.example.studentarchives.dto.Fmy.statistics.response.TopInterestItem;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.evaluation.OrgArchiveSummary;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.UserInterest;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AdminCollegeRepository;
import com.example.studentarchives.repository.AdminDataCompletenessRepository;
import com.example.studentarchives.repository.AdminOrgArchiveSummaryRepository;
import com.example.studentarchives.repository.AdminSemesterGpaSummaryRepository;
import com.example.studentarchives.repository.AdminStatisticsCacheRepository;
import com.example.studentarchives.repository.AdminUserInterestRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端统计看板与可视化服务
 * <p>
 * 对应《管理端接口文档》十六、统计看板与可视化模块（16.1 学校整体档案汇总 /
 * 16.2 组织下钻多维汇总 / 16.3 成果热力图数据），统一权限码 {@code statistics:view}。
 * <ul>
 *   <li>数据优先读 {@code statistics_cache}（L2 缓存，命中即返回完整预聚合结果），
 *       其次读 {@code org_archive_summaries} 每日快照（org_type 各维度最新 stat_date）。</li>
 *   <li>全校/全学院范围聚合缓存未命中时返回空数据集并标记 cacheHit=false，避免慢 SQL 打库；
 *       仅单班级（org_type=4）降级为实时直接查询。</li>
 *   <li>热力图数值按全校该指标最大值归一化到 0-100。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    /** 查看统计权限码 */
    private static final String STATS_PERMISSION = "statistics:view";

    /** 组织维度：1=学校 2=学院 3=专业 4=班级 6=年级 */
    private static final int ORG_SCHOOL = 1;
    private static final int ORG_COLLEGE = 2;
    private static final int ORG_MAJOR = 3;
    private static final int ORG_CLASS = 4;
    private static final int ORG_GRADE = 6;

    /** 奖项类档案类型（awardCount 口径） */
    private static final Set<String> AWARD_TYPES =
            Set.of("scholarship", "honor_certificate", "academic_competition");

    /** 实践类档案类型（practiceCount 口径） */
    private static final Set<String> PRACTICE_TYPES = Set.of("social_practice", "internship");

    /** 热力图支持的指标 */
    private static final Set<String> HEATMAP_METRICS = Set.of("gpa", "award", "practice", "interest", "archive");

    /** 教师端热力图支持的指标（《教师端接口文档》11.3：gpa/award/practice/archive 子集） */
    private static final Set<String> TEACHER_HEATMAP_METRICS = Set.of("gpa", "award", "practice", "archive");

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final AdminOrgArchiveSummaryRepository orgArchiveSummaryRepository;
    private final AdminStatisticsCacheRepository statisticsCacheRepository;
    private final AdminDataCompletenessRepository dataCompletenessRepository;
    private final AdminUserInterestRepository userInterestRepository;
    private final AdminSemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final ArchiveRepository archiveRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final AdminCollegeRepository adminCollegeRepository;
    private final SemesterRepository semesterRepository;
    private final SchoolRepository schoolRepository;
    private final StatisticsSnapshotService statisticsSnapshotService;
    private final ObjectMapper objectMapper;

    // ==================== 16.1 学校整体档案汇总（统计看板） ====================

    /**
     * 学校整体档案汇总（GET /admin/statistics/dashboard，文档 16.1）
     * <p>
     * 优先命中 statistics_cache（cacheKey=dashboard:{schoolId}:{semesterId}），
     * 其次读 org_archive_summaries 学校级快照（org_type=1）；均未命中时返回空数据集
     * 并标记 cacheHit=false。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @return 看板 KPI 与多维概览
     */
    public StatsResult<DashboardResponse> dashboard(Long userId, Long semesterId) {
        adminAuthService.requireAdminOrPermission(userId, STATS_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        if (semesterId == null) {
            semesterId = semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);
        }
        Long effSemesterId = semesterId;
        String semesterName = semesterName(effSemesterId);

        String cacheKey = "dashboard:" + schoolId + ":" + (effSemesterId == null ? "latest" : effSemesterId);
        Optional<StatsResult<DashboardResponse>> cached = readCache(cacheKey, schoolId, DashboardResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 学校级快照兜底
        OrgArchiveSummary snapshot = orgArchiveSummaryRepository
                .findLatestByOrg(ORG_SCHOOL, schoolId, effSemesterId).orElse(null);
        if (snapshot != null) {
            DashboardResponse data = DashboardResponse.builder()
                    .semesterId(effSemesterId)
                    .semesterName(semesterName)
                    .studentCount(snapshot.getTotalStudents())
                    .archiveCount(snapshot.getTotalArchives())
                    .awardCount(snapshot.getTotalAwards())
                    .avgGpa(toDouble(snapshot.getAvgGpa()))
                    .approvedCount(0)
                    .pendingCount(0)
                    .dataCompleteness(dataCompletenessRepository.avgCompletenessBySchool(schoolId, effSemesterId))
                    .dimensionAvgScores(parseTopDimensions(snapshot.getTopDimensions()))
                    .archiveTypeDistribution(parseTypeDistribution(snapshot.getArchiveTypeDistribution()))
                    .topInterests(parseHotTags(snapshot.getHotTags()))
                    .cacheHit(true)
                    .build();
            return new StatsResult<>(data, "MISS");
        }

        // 全校范围聚合缓存未命中：返回空数据集
        DashboardResponse empty = DashboardResponse.builder()
                .semesterId(effSemesterId)
                .semesterName(semesterName)
                .studentCount(0)
                .archiveCount(0)
                .awardCount(0)
                .avgGpa(null)
                .approvedCount(0)
                .pendingCount(0)
                .dataCompleteness(dataCompletenessRepository.avgCompletenessBySchool(schoolId, effSemesterId))
                .dimensionAvgScores(List.of())
                .archiveTypeDistribution(List.of())
                .topInterests(List.of())
                .cacheHit(false)
                .build();
        return new StatsResult<>(empty, "MISS");
    }

    // ==================== 16.2 组织下钻多维汇总 ====================

    /**
     * 组织下钻多维汇总（GET /admin/statistics/overview，文档 16.2）
     * <p>
     * scopeType 为当前下钻维度，rows 返回其下一级组织汇总；不传默认按学院维度。
     * 优先读对应行级维度（scopeType 下钻一级）的 org_archive_summaries 快照，
     * 单班级未命中快照时实时聚合兜底，学院/专业级未命中则返回零值行并标记 cacheHit=false。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param scopeType  下钻维度（可选，1=学校 2=学院 3=专业 4=班级 6=年级）
     * @param scopeId    当前组织 ID（可选，下钻其下一级）
     * @param grade      年级筛选（可选）
     * @return 组织多维汇总
     */
    public StatsResult<OrgOverviewResponse> overview(Long userId, Long semesterId, Integer scopeType, Long scopeId, String grade) {
        adminAuthService.requireAdminOrPermission(userId, STATS_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        if (semesterId == null) {
            semesterId = semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);
        }
        Long effSemesterId = semesterId;

        OrgIndex index = buildOrgIndex(schoolId);
        int rowScopeType = resolveRowScopeType(scopeType, scopeId);
        List<OrgScope> scopes = resolveOverviewScopes(schoolId, scopeType, scopeId, grade, index);

        String cacheKey = "overview:" + schoolId + ":" + rowScopeType + ":" + (scopeId == null ? 0 : scopeId)
                + ":" + (effSemesterId == null ? "latest" : effSemesterId)
                + ":" + (grade == null || grade.isBlank() ? "all" : grade);
        Optional<StatsResult<OrgOverviewResponse>> cached = readCache(cacheKey, schoolId, OrgOverviewResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        boolean gradeFiltered = grade != null && !grade.isBlank();
        Map<Long, OrgArchiveSummary> snapshotByOrg = null;
        Map<Long, OrgArchiveSummary> classSnapshotById = null;
        if (gradeFiltered && rowScopeType != ORG_CLASS) {
            // 带年级筛选且按学院/专业/年级下钻时，学院/专业快照未按年级拆分，
            // 需从班级快照按年级重新聚合，避免“年级无学生却仍返回全院数据”。
            classSnapshotById = orgArchiveSummaryRepository
                    .findLatestByLevel(ORG_CLASS, schoolId, effSemesterId).stream()
                    .collect(Collectors.toMap(OrgArchiveSummary::getOrgId, s -> s, (a, b) -> a));
        } else {
            snapshotByOrg = orgArchiveSummaryRepository
                    .findLatestByLevel(rowScopeType, schoolId, effSemesterId).stream()
                    .collect(Collectors.toMap(OrgArchiveSummary::getOrgId, s -> s, (a, b) -> a));
        }

        List<OrgOverviewRow> rows = new ArrayList<>();
        for (OrgScope scope : scopes) {
            OrgOverviewRow row;
            if (gradeFiltered && rowScopeType != ORG_CLASS) {
                row = aggregateFromClassSnapshots(scope, classSnapshotById);
            } else {
                OrgArchiveSummary snap = snapshotByOrg.get(scope.orgId());
                if (snap != null) {
                    row = fromSnapshot(scope, snap);
                } else if (rowScopeType == ORG_CLASS) {
                    // 单班级实时降级直接查询
                    row = aggregateClassRealtime(scope, effSemesterId, index);
                } else {
                    row = zeroRow(scope);
                }
            }
            rows.add(row);
        }
        boolean cacheHit = rows.stream().anyMatch(r -> r.getArchiveCount() != null && r.getArchiveCount() > 0);

        OrgOverviewResponse data = OrgOverviewResponse.builder()
                .scopeType(rowScopeType)
                .parentOrg(resolveParentOrg(schoolId, scopeType, scopeId, index))
                .rows(rows)
                .cacheHit(cacheHit)
                .build();
        return new StatsResult<>(data, "MISS");
    }

    // ==================== 16.3 成果热力图数据 ====================

    /**
     * 成果热力图数据（GET /admin/statistics/heatmap，文档 16.3）
     * <p>
     * 以组织单位为行、指标/学期为列；数值按全校该指标最大值归一化到 0-100。
     * 每个 (组织, 学期) 取 org_archive_summaries 最新快照对应指标原始值。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传按全校启用学期展开列）
     * @param orgType    行维度：2=学院 3=专业 4=班级（必填）
     * @param orgId      上级组织 ID（可选，返回其下各组织行）
     * @param metric     指标：gpa/award/practice/interest/archive（必填）
     * @param grade      年级筛选（可选）
     * @return 热力图矩阵
     */
    public StatsResult<HeatmapResponse> heatmap(Long userId, Long semesterId, Integer orgType, Long orgId,
                                                String metric, String grade) {
        adminAuthService.requireAdminOrPermission(userId, STATS_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        return doHeatmap(schoolId, semesterId, orgType, orgId, metric, grade);
    }

    /**
     * 成果热力图核心计算（鉴权/范围校验完成后共用）。
     * <p>
     * 以组织单位为行、指标/学期为列；数值按全校该指标最大值归一化到 0-100。
     * 每个 (组织, 学期) 取 org_archive_summaries 最新快照对应指标原始值。
     *
     * @param schoolId   学校 ID
     * @param semesterId 学期 ID（可选，不传按全校启用学期展开列）
     * @param orgType    行维度：2=学院 3=专业 4=班级
     * @param orgId      上级组织 ID（可选，返回其下各组织行）
     * @param metric     指标：gpa/award/practice/interest/archive
     * @param grade      年级筛选（可选）
     * @return 热力图矩阵
     */
    private StatsResult<HeatmapResponse> doHeatmap(Long schoolId, Long semesterId, Integer orgType, Long orgId,
                                                   String metric, String grade) {
        if (orgType == null || metric == null || metric.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "orgType 与 metric 为必填参数");
        }
        if (orgType < ORG_COLLEGE || orgType > ORG_CLASS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "orgType 仅支持 2=学院 3=专业 4=班级");
        }
        if (!HEATMAP_METRICS.contains(metric)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "metric 仅支持 gpa/award/practice/interest/archive");
        }

        // 学期列：传 semesterId 则单列，否则全校启用学期展开
        List<HeatmapSemesterItem> semesters = new ArrayList<>();
        if (semesterId != null) {
            semesters.add(new HeatmapSemesterItem(semesterId, semesterName(semesterId)));
        } else {
            for (Semester s : semesterRepository.findActiveBySchoolId(schoolId)) {
                semesters.add(new HeatmapSemesterItem(s.getId(), s.getName()));
            }
        }
        List<Long> semesterIds = semesters.stream().map(HeatmapSemesterItem::getSemesterId).toList();
        if (semesterIds.isEmpty()) {
            HeatmapResponse empty = HeatmapResponse.builder()
                    .metric(metric).grade(grade).semesters(List.of()).rows(List.of())
                    .maxValue(0).minValue(0).cacheHit(false).build();
            return new StatsResult<>(empty, "MISS");
        }

        String cacheKey = "heatmap:" + schoolId + ":" + orgType + ":" + (orgId == null ? 0 : orgId)
                + ":" + (semesterId == null ? "latest" : semesterId) + ":" + metric
                + ":" + (grade == null ? "" : grade);
        Optional<StatsResult<HeatmapResponse>> cached = readCache(cacheKey, schoolId, HeatmapResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        OrgIndex index = buildOrgIndex(schoolId);
        List<OrgScope> scopes = resolveHeatmapScopes(schoolId, orgType, orgId, grade, index);

        // 每个学期读取对应行级维度全部快照
        Map<Long, Map<Long, OrgArchiveSummary>> snapBySemesterByOrg = new HashMap<>();
        for (Long sid : semesterIds) {
            Map<Long, OrgArchiveSummary> byOrg = orgArchiveSummaryRepository
                    .findLatestByLevel(orgType, schoolId, sid).stream()
                    .collect(Collectors.toMap(OrgArchiveSummary::getOrgId, s -> s, (a, b) -> a));
            snapBySemesterByOrg.put(sid, byOrg);
        }

        List<HeatmapRow> rows = new ArrayList<>();
        int maxRaw = 0;
        for (OrgScope scope : scopes) {
            List<Integer> raw = new ArrayList<>();
            for (Long sid : semesterIds) {
                OrgArchiveSummary snap = snapBySemesterByOrg.getOrDefault(sid, Map.of()).get(scope.orgId());
                int v = rawMetric(snap, metric);
                raw.add(v);
                maxRaw = Math.max(maxRaw, v);
            }
            int total = raw.stream().mapToInt(Integer::intValue).sum();
            rows.add(new HeatmapRow(scope.orgId(), scope.orgName(), null, raw, total));
        }

        int finalMaxRaw = maxRaw;
        for (HeatmapRow row : rows) {
            List<Integer> values = row.getRawValues().stream()
                    .map(r -> normalize(r, finalMaxRaw))
                    .collect(Collectors.toList());
            row.setValues(values);
        }
        boolean cacheHit = maxRaw > 0;

        HeatmapResponse data = HeatmapResponse.builder()
                .metric(metric)
                .grade(grade)
                .semesters(semesters)
                .rows(rows)
                .maxValue(maxRaw)
                .minValue(0)
                .cacheHit(cacheHit)
                .build();
        return new StatsResult<>(data, "MISS");
    }

    /**
     * 教师端成果热力图（GET /teacher/statistics/heatmap，教师端文档 11.3）
     * <p>
     * 复用 16.3 热力图引擎（{@link #doHeatmap}），仅替换鉴权与范围策略：
     * 教师登录即可，orgId 必须落在当前教师 {@code role_scopes} 授权范围内
     * （学院/专业/班级按同类型匹配，学校级授权覆盖校内全部，admin 由
     * {@link TeacherScopeValidator} 放行），越权返回 20005。
     * orgId 为空时返回教师授权范围内全部组织行（行维度过滤）。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传按全校启用学期展开列）
     * @param orgType    行维度：2=学院 3=专业 4=班级（不传默认班级维度）
     * @param orgId      上级组织 ID（可选，返回其下各组织行；不传返回授权范围内全部组织行）
     * @param metric     指标：gpa/award/practice/archive（不传默认 award）
     * @param grade      年级筛选（可选，不传取教师主职授权范围内主要年级）
     * @return 热力图矩阵
     */
    public StatsResult<HeatmapResponse> heatmapByTeacher(Long userId, Long semesterId, Integer orgType, Long orgId,
                                                         String metric, String grade) {
        // 教师端统计不校验管理端权限码：登录即可，数据范围由 ensureOrgInScope / authorizedOrgIds 按 role_scopes 兜底
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        // 参数默认值（《教师端接口文档》11.3/12.5.4）：metric 不传默认 award，orgType 不传默认班级维度，
        // grade 不传取教师主职授权范围内主要年级
        if (metric == null || metric.isBlank()) {
            metric = "award";
        }
        if (orgType == null) {
            orgType = ORG_CLASS;
        }
        if (grade == null || grade.isBlank()) {
            grade = resolvePrimaryGrade(userId, schoolId);
        }

        if (orgType < ORG_COLLEGE || orgType > ORG_CLASS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "orgType 仅支持 2=学院 3=专业 4=班级");
        }
        if (!TEACHER_HEATMAP_METRICS.contains(metric)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "metric 仅支持 gpa/award/practice/archive");
        }

        // 指定 orgId：必须在教师授权范围内（学校级授权覆盖）
        if (orgId != null) {
            scopeValidator.ensureOrgInScope(userId, orgType, orgId, schoolId);
            return doHeatmap(schoolId, semesterId, orgType, orgId, metric, grade);
        }

        // orgId 为空：返回教师授权范围内全部组织行；学校级授权直接返回全校行
        StatsResult<HeatmapResponse> result = doHeatmap(schoolId, semesterId, orgType, null, metric, grade);
        Set<Long> authorized = scopeValidator.authorizedOrgIds(userId, orgType, schoolId);
        if (authorized == null || result.data() == null) {
            return result;
        }
        List<HeatmapRow> rows = result.data().getRows() == null ? List.of()
                : result.data().getRows().stream()
                        .filter(r -> r.getOrgId() != null && authorized.contains(r.getOrgId()))
                        .collect(Collectors.toList());
        result.data().setRows(rows);
        return result;
    }

    // ==================== 教师端默认年级推导 ====================

    /**
     * 教师主职授权范围内主要年级（《教师端接口文档》11.3/12.5.4：grade 不传时的默认值）。
     * <p>
     * 取主职授权（is_primary=1，否则首个生效授权）推导：
     * 班级(4) → 该班 grade；年级(6) → scopeId + "级"；
     * 专业(3)/学院(2)/学校(1) → 其下班级数量最多的年级，并列取年级较大者。
     * 无生效授权或无法推导返回 {@code null}（不按年级筛选）。
     */
    private String resolvePrimaryGrade(Long userId, Long schoolId) {
        List<RoleScope> scopes = scopeValidator.effectiveScopes(userId);
        if (scopes.isEmpty()) {
            return null;
        }
        RoleScope primary = scopes.stream()
                .filter(s -> s.getScopeType() != null && Objects.equals(s.getIsPrimary(), 1))
                .findFirst()
                .orElseGet(() -> scopes.stream().filter(s -> s.getScopeType() != null).findFirst().orElse(null));
        if (primary == null || primary.getScopeType() == null || primary.getScopeId() == null) {
            return null;
        }
        switch (primary.getScopeType()) {
            case ORG_GRADE:
                return primary.getScopeId() + "级";
            case ORG_CLASS:
                return clazzRepository.findById(primary.getScopeId()).map(Clazz::getGrade).orElse(null);
            case ORG_MAJOR:
                return dominantGrade(clazzRepository.findByMajorId(primary.getScopeId()));
            case ORG_COLLEGE:
                return dominantGrade(classesOfMajors(majorRepository.findByCollegeIdIn(Set.of(primary.getScopeId()))));
            case ORG_SCHOOL:
                return dominantGrade(classesOfSchool(schoolId));
            default:
                return null;
        }
    }

    /** 专业列表 → 其下班级列表 */
    private List<Clazz> classesOfMajors(List<Major> majors) {
        List<Long> majorIds = majors.stream().map(Major::getId).toList();
        return majorIds.isEmpty() ? List.of() : clazzRepository.findByMajorIdIn(majorIds);
    }

    /** 学校全部班级 */
    private List<Clazz> classesOfSchool(Long schoolId) {
        List<Long> collegeIds = adminCollegeRepository.findBySchoolId(schoolId).stream()
                .map(College::getId).toList();
        List<Long> majorIds = collegeIds.isEmpty() ? List.of()
                : majorRepository.findByCollegeIdIn(collegeIds).stream().map(Major::getId).toList();
        return majorIds.isEmpty() ? List.of() : clazzRepository.findByMajorIdIn(majorIds);
    }

    /** 班级列表中数量最多的年级；并列取年级较大者（按 "N级" 数字比较） */
    private String dominantGrade(List<Clazz> classes) {
        Map<String, Long> byGrade = classes.stream()
                .filter(c -> c.getGrade() != null && !c.getGrade().isBlank())
                .collect(Collectors.groupingBy(Clazz::getGrade, Collectors.counting()));
        return byGrade.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Long>>comparingLong(e -> e.getValue())
                        .thenComparingLong(e -> gradeNumber(e.getKey())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /** 年级字符串转数字用于比较（"2024级" → 2024；无法解析返回 0） */
    private long gradeNumber(String grade) {
        String digits = grade.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0L : Long.parseLong(digits);
    }

    // ==================== 16.4 手动刷新统计快照 ====================

    /**
     * 手动刷新学校级档案汇总快照（POST /admin/statistics/refresh）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @return 刷新结果
     */
    public SnapshotRefreshResponse refreshSnapshot(Long userId, Long semesterId) {
        adminAuthService.requireAdminOrPermission(userId, STATS_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        return statisticsSnapshotService.refresh(schoolId, semesterId);
    }

    // ==================== 快照行映射 ====================

    /** 由组织快照构建汇总行 */
    private OrgOverviewRow fromSnapshot(OrgScope scope, OrgArchiveSummary snap) {
        return OrgOverviewRow.builder()
                .orgId(scope.orgId())
                .orgName(scope.orgName())
                .studentCount(snap.getTotalStudents())
                .archiveCount(snap.getTotalArchives())
                .awardCount(snap.getTotalAwards())
                .avgGpa(toDouble(snap.getAvgGpa()))
                .avgScore(null)
                .practiceCount(practiceCountFromDistribution(snap.getArchiveTypeDistribution()))
                .topInterests(topInterestsFromHotTags(snap.getHotTags()))
                .dimensionAvgScores(parseTopDimensions(snap.getTopDimensions()))
                .archiveTypeDistribution(parseTypeDistribution(snap.getArchiveTypeDistribution()))
                .build();
    }

    /** 单班级实时聚合兜底（缓存未命中时文档允许的降级查询） */
    private OrgOverviewRow aggregateClassRealtime(OrgScope scope, Long semesterId, OrgIndex index) {
        List<Long> userIds = index.userIdsByClassId().getOrDefault(scope.orgId(), List.of());
        int archiveCount = 0;
        int awardCount = 0;
        int practiceCount = 0;
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            for (Archive a : archiveRepository.findByUserIdIn(userIds)) {
                if (semesterId != null && a.getSemesterId() != null && !Objects.equals(a.getSemesterId(), semesterId)) {
                    continue;
                }
                archiveCount++;
                if (AWARD_TYPES.contains(a.getArchiveType())) {
                    awardCount++;
                }
                if (PRACTICE_TYPES.contains(a.getArchiveType())) {
                    practiceCount++;
                }
                typeCount.merge(a.getArchiveType(), 1, Integer::sum);
            }
        }

        double avgGpa = 0d;
        double avgScore = 0d;
        if (semesterId != null && !userIds.isEmpty()) {
            List<SemesterGpaSummary> gpaSums = semesterGpaSummaryRepository
                    .findBySemesterIdAndUserIdIn(semesterId, userIds);
            avgGpa = avgBigDecimal(gpaSums.stream().map(SemesterGpaSummary::getWeightedGpa).toList());
            avgScore = avgBigDecimal(gpaSums.stream().map(SemesterGpaSummary::getAverageScore).toList());
        }

        List<StatisticsTypeCountItem> distribution = typeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(e -> StatisticsTypeCountItem.builder().archiveType(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        return OrgOverviewRow.builder()
                .orgId(scope.orgId())
                .orgName(scope.orgName())
                .studentCount(userIds.size())
                .archiveCount(archiveCount)
                .awardCount(awardCount)
                .avgGpa(avgGpa)
                .avgScore(avgScore)
                .practiceCount(practiceCount)
                .topInterests(topInterests(userIds, 5))
                .dimensionAvgScores(List.of())
                .archiveTypeDistribution(distribution)
                .build();
    }

    /** 快照未命中的学院/专业级零值行 */
    private OrgOverviewRow zeroRow(OrgScope scope) {
        return OrgOverviewRow.builder()
                .orgId(scope.orgId())
                .orgName(scope.orgName())
                .studentCount(0)
                .archiveCount(0)
                .awardCount(0)
                .avgGpa(null)
                .avgScore(null)
                .practiceCount(0)
                .topInterests(List.of())
                .dimensionAvgScores(List.of())
                .archiveTypeDistribution(List.of())
                .build();
    }

    /**
     * 按年级筛选时，从班级快照聚合出学院/专业/年级行。
     * <p>
     * 班级快照的 grade 字段已记录所属年级，因此把 scope 下所有符合年级的班级快照
     * 加权汇总，即可得到该年级在目标组织下的真实数据，而不是误用全院快照。
     */
    private OrgOverviewRow aggregateFromClassSnapshots(OrgScope scope,
                                                       Map<Long, OrgArchiveSummary> classSnapshotById) {
        int studentCount = 0;
        int archiveCount = 0;
        int awardCount = 0;
        BigDecimal weightedGpaSum = BigDecimal.ZERO;
        int gpaWeight = 0;
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> interestCount = new HashMap<>();
        Map<String, WeightedDimension> dimensionMap = new HashMap<>();

        for (Long classId : scope.classIds()) {
            OrgArchiveSummary s = classSnapshotById.get(classId);
            if (s == null) {
                continue;
            }
            studentCount += s.getTotalStudents();
            archiveCount += s.getTotalArchives();
            awardCount += s.getTotalAwards();
            if (s.getAvgGpa() != null && s.getTotalStudents() > 0) {
                weightedGpaSum = weightedGpaSum.add(
                        s.getAvgGpa().multiply(BigDecimal.valueOf(s.getTotalStudents())));
                gpaWeight += s.getTotalStudents();
            }
            for (StatisticsTypeCountItem t : parseTypeDistribution(s.getArchiveTypeDistribution())) {
                typeCount.merge(t.getArchiveType(), t.getCount() == null ? 0 : t.getCount(), Integer::sum);
            }
            for (TopInterestItem i : parseHotTags(s.getHotTags())) {
                interestCount.merge(i.getInterest(), i.getCount() == null ? 0 : i.getCount(), Integer::sum);
            }
            int classStudentCount = s.getTotalStudents();
            for (DimensionAvgScoreItem d : parseTopDimensions(s.getTopDimensions())) {
                if (d.getDimensionCode() == null || d.getAvgScore() == null) {
                    continue;
                }
                WeightedDimension wd = dimensionMap.computeIfAbsent(d.getDimensionCode(),
                        k -> new WeightedDimension(d.getDimensionName(), 0.0, 0));
                wd.weightedScore += d.getAvgScore() * classStudentCount;
                wd.weight += classStudentCount;
            }
        }

        Double avgGpa = gpaWeight > 0
                ? weightedGpaSum.divide(BigDecimal.valueOf(gpaWeight), 2, RoundingMode.HALF_UP).doubleValue()
                : null;
        List<DimensionAvgScoreItem> dimensions = dimensionMap.entrySet().stream()
                .map(e -> new DimensionAvgScoreItem(e.getKey(), e.getValue().name,
                        e.getValue().weight > 0
                                ? BigDecimal.valueOf(e.getValue().weightedScore / e.getValue().weight)
                                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                                : null))
                .sorted(Comparator.comparing(DimensionAvgScoreItem::getAvgScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
        List<String> topInterests = interestCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<StatisticsTypeCountItem> distribution = typeCount.entrySet().stream()
                .map(e -> StatisticsTypeCountItem.builder().archiveType(e.getKey()).count(e.getValue()).build())
                .sorted(Comparator.comparing(StatisticsTypeCountItem::getCount, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        int practiceCount = typeCount.entrySet().stream()
                .filter(e -> PRACTICE_TYPES.contains(e.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();

        return OrgOverviewRow.builder()
                .orgId(scope.orgId())
                .orgName(scope.orgName())
                .studentCount(studentCount)
                .archiveCount(archiveCount)
                .awardCount(awardCount)
                .avgGpa(avgGpa)
                .avgScore(null)
                .practiceCount(practiceCount)
                .topInterests(topInterests)
                .dimensionAvgScores(dimensions)
                .archiveTypeDistribution(distribution)
                .build();
    }

    // ==================== 热力图指标 ====================

    /** 读取组织快照某指标原始值（快照缺失返回 0） */
    private int rawMetric(OrgArchiveSummary snap, String metric) {
        if (snap == null) {
            return 0;
        }
        switch (metric) {
            case "gpa":
                return snap.getAvgGpa() == null ? 0
                        : (int) Math.round(snap.getAvgGpa().doubleValue() * 20.0);
            case "award":
                return snap.getTotalAwards();
            case "practice":
                return practiceCountFromDistribution(snap.getArchiveTypeDistribution());
            case "interest":
                return parseHotTags(snap.getHotTags()).stream()
                        .mapToInt(t -> t.getCount() == null ? 0 : t.getCount()).sum();
            case "archive":
                return snap.getTotalArchives();
            default:
                return 0;
        }
    }

    /** 归一化：value = raw / max * 100（全校该指标最大值作分母，与文档公式一致） */
    private int normalize(int raw, int max) {
        if (max <= 0) {
            return 0;
        }
        return (int) Math.round(raw * 100.0 / max);
    }

    // ==================== 缓存 ====================

    /** 读取 L2 统计缓存（未过期），命中返回解析后的数据 */
    private <T> Optional<StatsResult<T>> readCache(String cacheKey, Long schoolId, Class<T> type) {
        return statisticsCacheRepository.findBySchoolIdAndCacheKey(schoolId, cacheKey)
                .filter(c -> c.getExpiredAt() == null || c.getExpiredAt().isAfter(LocalDateTime.now()))
                .map(c -> {
                    try {
                        T data = objectMapper.readValue(c.getStatData(), type);
                        if (data instanceof DashboardResponse d && d.getCacheHit() == null) {
                            d.setCacheHit(true);
                        } else if (data instanceof OrgOverviewResponse o && o.getCacheHit() == null) {
                            o.setCacheHit(true);
                        } else if (data instanceof HeatmapResponse h && h.getCacheHit() == null) {
                            h.setCacheHit(true);
                        }
                        return new StatsResult<>(data, "L2");
                    } catch (Exception e) {
                        log.warn("解析统计缓存失败 cacheKey={}", cacheKey, e);
                        return null;
                    }
                });
    }

    // ==================== JSON 解析 ====================

    /** 解析档案类型分布 JSON（兼容数组与对象两种存储格式） */
    List<StatisticsTypeCountItem> parseTypeDistribution(String json) {
        List<StatisticsTypeCountItem> out = new ArrayList<>();
        JsonNode node = readJson(json);
        if (node == null) {
            return out;
        }
        if (node.isArray()) {
            for (JsonNode el : node) {
                String type = firstText(el, "archiveType", "archive_type", "type", "code");
                Integer count = firstInt(el, "count", "value", "num");
                if (type != null) {
                    out.add(StatisticsTypeCountItem.builder()
                            .archiveType(type).count(count == null ? 0 : count).build());
                }
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v != null && v.isNumber()) {
                    out.add(StatisticsTypeCountItem.builder()
                            .archiveType(e.getKey()).count(v.asInt()).build());
                }
            });
        }
        return out;
    }

    /** 解析热门兴趣 JSON（兼容数组与对象两种存储格式） */
    private List<TopInterestItem> parseHotTags(String json) {
        List<TopInterestItem> out = new ArrayList<>();
        JsonNode node = readJson(json);
        if (node == null) {
            return out;
        }
        if (node.isArray()) {
            for (JsonNode el : node) {
                String tag = firstText(el, "tag", "tagName", "interest", "name");
                Integer count = firstInt(el, "count", "weight", "value");
                if (tag != null) {
                    out.add(TopInterestItem.builder()
                            .interest(tag).count(count == null ? 0 : count).build());
                }
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v != null && v.isNumber()) {
                    out.add(TopInterestItem.builder()
                            .interest(e.getKey()).count(v.asInt()).build());
                }
            });
        }
        return out;
    }

    /** 解析画像维度平均分 JSON（快照 top_dimensions） */
    List<DimensionAvgScoreItem> parseTopDimensions(String json) {
        List<DimensionAvgScoreItem> out = new ArrayList<>();
        JsonNode node = readJson(json);
        if (node == null) {
            return out;
        }
        if (node.isArray()) {
            for (JsonNode el : node) {
                String code = firstText(el, "dimensionCode", "code", "dimension");
                String name = firstText(el, "dimensionName", "name");
                Double score = firstDouble(el, "score", "avgScore", "value");
                if (code != null) {
                    out.add(DimensionAvgScoreItem.builder()
                            .dimensionCode(code).dimensionName(name).avgScore(score).build());
                }
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v != null && v.isNumber()) {
                    out.add(DimensionAvgScoreItem.builder()
                            .dimensionCode(e.getKey()).dimensionName(null).avgScore(v.asDouble()).build());
                }
            });
        }
        return out;
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("解析统计快照 JSON 失败: {}", json, e);
            return null;
        }
    }

    private String firstText(JsonNode el, String... fields) {
        for (String f : fields) {
            JsonNode n = el.get(f);
            if (n != null && !n.isNull()) {
                return n.asText();
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode el, String... fields) {
        for (String f : fields) {
            JsonNode n = el.get(f);
            if (n != null && n.isNumber()) {
                return n.asInt();
            }
        }
        return null;
    }

    private Double firstDouble(JsonNode el, String... fields) {
        for (String f : fields) {
            JsonNode n = el.get(f);
            if (n != null && n.isNumber()) {
                return n.asDouble();
            }
        }
        return null;
    }

    /** 实践类档案数（从类型分布 JSON 求和） */
    private int practiceCountFromDistribution(String distributionJson) {
        return parseTypeDistribution(distributionJson).stream()
                .filter(t -> PRACTICE_TYPES.contains(t.getArchiveType()))
                .mapToInt(t -> t.getCount() == null ? 0 : t.getCount())
                .sum();
    }

    /** 快照热门兴趣标签 TopN */
    private List<String> topInterestsFromHotTags(String hotTagsJson) {
        return parseHotTags(hotTagsJson).stream()
                .sorted(Comparator.comparing((TopInterestItem t) ->
                        t.getCount() == null ? 0 : t.getCount()).reversed())
                .limit(5)
                .map(TopInterestItem::getInterest)
                .collect(Collectors.toList());
    }

    /** 实时聚合热门兴趣 TopN（单班级兜底） */
    private List<String> topInterests(Collection<Long> userIds, int n) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (UserInterest ui : userInterestRepository.findByUserIdIn(userIds)) {
            counts.merge(ui.getTagName(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ==================== 组织索引与范围解析 ====================

    /** 全校组织索引（校级一次构建，跨行共享） */
    private OrgIndex buildOrgIndex(Long schoolId) {
        List<College> colleges = adminCollegeRepository.findBySchoolId(schoolId);
        Map<Long, String> collegeIdToName = colleges.stream()
                .collect(Collectors.toMap(College::getId, College::getName, (a, b) -> a));

        List<Long> collegeIds = colleges.stream().map(College::getId).toList();
        List<Major> majors = collegeIds.isEmpty() ? List.of()
                : majorRepository.findByCollegeIdIn(collegeIds);
        Map<Long, Long> majorIdToCollegeId = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getCollegeId, (a, b) -> a));
        Map<Long, String> majorIdToName = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getName, (a, b) -> a));

        List<Long> majorIds = majors.stream().map(Major::getId).toList();
        List<Clazz> classes = majorIds.isEmpty() ? List.of()
                : clazzRepository.findByMajorIdIn(majorIds);
        Map<Long, Long> classIdToMajorId = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getMajorId, (a, b) -> a));
        Map<Long, String> classIdToName = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getName, (a, b) -> a));
        Map<Long, String> classIdToGrade = classes.stream()
                .filter(c -> c.getGrade() != null)
                .collect(Collectors.toMap(Clazz::getId, Clazz::getGrade, (a, b) -> a));

        List<Long> classIds = classes.stream().map(Clazz::getId).toList();
        List<StudentProfile> profiles = classIds.isEmpty() ? List.of()
                : studentProfileRepository.findByClassIdIn(classIds);
        Map<Long, List<Long>> userIdsByClassId = new LinkedHashMap<>();
        for (StudentProfile p : profiles) {
            userIdsByClassId.computeIfAbsent(p.getClassId(), k -> new ArrayList<>()).add(p.getUserId());
        }
        for (Map.Entry<Long, List<Long>> e : userIdsByClassId.entrySet()) {
            e.setValue(e.getValue().stream().distinct().toList());
        }

        return new OrgIndex(collegeIdToName, majorIdToCollegeId, majorIdToName,
                classIdToMajorId, classIdToName, classIdToGrade, userIdsByClassId);
    }

    /** 下钻响应的行级维度 */
    private int resolveRowScopeType(Integer scopeType, Long scopeId) {
        if (scopeType == null) {
            return ORG_COLLEGE;
        }
        if (scopeType == ORG_SCHOOL) {
            return ORG_SCHOOL;
        }
        if (scopeType == ORG_COLLEGE) {
            return scopeId != null ? ORG_MAJOR : ORG_COLLEGE;
        }
        if (scopeType == ORG_MAJOR) {
            return scopeId != null ? ORG_CLASS : ORG_MAJOR;
        }
        if (scopeType == ORG_CLASS) {
            return ORG_CLASS;
        }
        if (scopeType == ORG_GRADE) {
            return ORG_GRADE;
        }
        return ORG_COLLEGE;
    }

    /** 解析上级组织（面包屑定位） */
    private StatisticsParentOrg resolveParentOrg(Long schoolId, Integer scopeType, Long scopeId, OrgIndex index) {
        if (scopeType != null && scopeType == ORG_MAJOR && scopeId != null) {
            Long collegeId = index.majorIdToCollegeId().get(scopeId);
            if (collegeId != null) {
                return new StatisticsParentOrg(collegeId, index.collegeIdToName().get(collegeId));
            }
        }
        if (scopeType != null && scopeType == ORG_CLASS && scopeId != null) {
            Long majorId = index.classIdToMajorId().get(scopeId);
            if (majorId != null) {
                return new StatisticsParentOrg(majorId, index.majorIdToName().get(majorId));
            }
        }
        return new StatisticsParentOrg(schoolId, schoolName(schoolId));
    }

    /** 组织下钻各汇总行范围（16.2） */
    private List<OrgScope> resolveOverviewScopes(Long schoolId, Integer scopeType, Long scopeId, String grade,
                                                 OrgIndex index) {
        List<OrgScope> scopes = new ArrayList<>();
        int row = resolveRowScopeType(scopeType, scopeId);
        switch (row) {
            case ORG_SCHOOL -> {
                List<Long> schoolClassIds = new ArrayList<>(index.userIdsByClassId().keySet());
                if (grade != null && !grade.isBlank()) {
                    schoolClassIds.removeIf(cid -> !grade.equals(index.classIdToGrade().get(cid)));
                }
                scopes.add(new OrgScope(schoolId, schoolName(schoolId), schoolClassIds));
            }
            case ORG_COLLEGE -> index.collegeIdToName().forEach((cid, name) ->
                    scopes.add(new OrgScope(cid, name, classIdsOfCollege(cid, grade, index))));
            case ORG_MAJOR -> index.majorIdToName().forEach((mid, name) -> {
                if (scopeType != null && scopeType == ORG_COLLEGE && scopeId != null
                        && !Objects.equals(index.majorIdToCollegeId().get(mid), scopeId)) {
                    return;
                }
                scopes.add(new OrgScope(mid, name, classIdsOfMajor(mid, grade, index)));
            });
            case ORG_CLASS -> index.classIdToName().forEach((classId, name) -> {
                if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(classId))) {
                    return;
                }
                if (scopeType != null && scopeId != null) {
                    if (scopeType == ORG_MAJOR && !Objects.equals(index.classIdToMajorId().get(classId), scopeId)) {
                        return;
                    }
                    if (scopeType == ORG_CLASS && !Objects.equals(classId, scopeId)) {
                        return;
                    }
                }
                scopes.add(new OrgScope(classId, name, List.of(classId)));
            });
            case ORG_GRADE -> {
                Map<String, List<Long>> byGrade = new LinkedHashMap<>();
                index.classIdToGrade().forEach((classId, g) -> {
                    if (grade != null && !grade.isBlank() && !grade.equals(g)) {
                        return;
                    }
                    byGrade.computeIfAbsent(g, k -> new ArrayList<>()).add(classId);
                });
                byGrade.forEach((g, classIds) -> scopes.add(new OrgScope(0L, g, classIds)));
            }
            default -> {
            }
        }
        return scopes;
    }

    /** 热力图各组织行范围（16.3） */
    private List<OrgScope> resolveHeatmapScopes(Long schoolId, Integer orgType, Long orgId, String grade, OrgIndex index) {
        List<OrgScope> scopes = new ArrayList<>();
        switch (orgType) {
            case ORG_COLLEGE -> {
                if (orgId != null && !Objects.equals(orgId, schoolId)) {
                    return scopes;
                }
                index.collegeIdToName().forEach((cid, name) ->
                        scopes.add(new OrgScope(cid, name, List.of())));
            }
            case ORG_MAJOR -> index.majorIdToName().forEach((mid, name) -> {
                if (orgId != null && !Objects.equals(index.majorIdToCollegeId().get(mid), orgId)) {
                    return;
                }
                scopes.add(new OrgScope(mid, name, List.of()));
            });
            case ORG_CLASS -> index.classIdToName().forEach((classId, name) -> {
                if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(classId))) {
                    return;
                }
                if (orgId != null && !Objects.equals(index.classIdToMajorId().get(classId), orgId)) {
                    return;
                }
                scopes.add(new OrgScope(classId, name, List.of(classId)));
            });
            default -> {
            }
        }
        return scopes;
    }

    private List<Long> classIdsOfCollege(Long collegeId, String grade, OrgIndex index) {
        List<Long> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : index.classIdToMajorId().entrySet()) {
            Long majorId = e.getValue();
            if (majorId == null || !Objects.equals(index.majorIdToCollegeId().get(majorId), collegeId)) {
                continue;
            }
            if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(e.getKey()))) {
                continue;
            }
            result.add(e.getKey());
        }
        return result;
    }

    private List<Long> classIdsOfMajor(Long majorId, String grade, OrgIndex index) {
        List<Long> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : index.classIdToMajorId().entrySet()) {
            if (!Objects.equals(e.getValue(), majorId)) {
                continue;
            }
            if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(e.getKey()))) {
                continue;
            }
            result.add(e.getKey());
        }
        return result;
    }

    // ==================== 私有工具 ====================

    private String semesterName(Long semesterId) {
        if (semesterId == null) {
            return null;
        }
        return semesterRepository.findById(semesterId).map(Semester::getName).orElse(null);
    }

    private String schoolName(Long schoolId) {
        return schoolRepository.findById(schoolId).map(School::getName).orElse(null);
    }

    private static Double toDouble(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static double avgBigDecimal(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return 0d;
        }
        return nonNull.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
    }

    /**
     * 服务返回值包装：data 为接口数据，cacheHit 标记缓存命中来源（L2 / MISS），
     * 供控制器设置 X-Cache-Hit 响应头。
     */
    public record StatsResult<T>(T data, String cacheHit) {
    }

    /** 组织索引 */
    private record OrgIndex(
            Map<Long, String> collegeIdToName,
            Map<Long, Long> majorIdToCollegeId,
            Map<Long, String> majorIdToName,
            Map<Long, Long> classIdToMajorId,
            Map<Long, String> classIdToName,
            Map<Long, String> classIdToGrade,
            Map<Long, List<Long>> userIdsByClassId) {
    }

    /** 汇总行组织范围 */
    private record OrgScope(Long orgId, String orgName, List<Long> classIds) {
    }

    /** 维度加权平均中间对象 */
    private static class WeightedDimension {
        String name;
        double weightedScore;
        int weight;

        WeightedDimension(String name, double weightedScore, int weight) {
            this.name = name;
            this.weightedScore = weightedScore;
            this.weight = weight;
        }
    }
}
