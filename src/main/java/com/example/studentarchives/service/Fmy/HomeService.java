package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse.DataCompletenessInfo;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse.DimensionInfo;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse.IndicatorItem;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse.QuickEntry;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse.RadarChart;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse.RecentActivity;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.evaluation.DataCompleteness;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.DataCompletenessRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.SemesterGpaSummaryRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页数据概览服务
 * <p>
 * 聚合学生真实数据，返回学生端首页（GET /home/dashboard）所需的数据概览：
 * 学生信息、申报统计、学期成绩、画像指标、雷达图、数据完整度、
 * 快捷入口、最近动态、未读消息数。
 * <p>
 * 数据口径（与《学生端接口文档》3.1 及 seed_dashboard.sql 一致）：
 * - currentGpa/totalCredits/rankInClass/rankInMajor → semester_gpa_summaries
 *   （currentGpa/排名取当前学期行；totalCredits 为各学期 total_credit 累计求和）
 * - indicators/radarChart → portrait_evaluation_scores（当前学期 + 上阶段）
 * - applicationTotal/approvedCount/pendingCount/rejectedCount → archives（按 status 聚合）
 * - recentActivities → archives 最近提交
 * - dataCompleteness → data_completeness（按维度，rate 为各维度平均值）
 * - unreadMessageCount → user_messages（is_read=0 且 is_archived=0）
 */
@Slf4j//Lombok 注解，自动生成一个名为 log 的日志对象，用于在控制台打印日志（如 log.warn）。
@Service//Spring 注解，表明这是一个业务逻辑层组件，会被 Spring 容器自动扫描并管理。
@RequiredArgsConstructor// Lombok 注解，自动生成一个包含所有 final 字段的构造函数。
public class HomeService {

    /** 申报状态（ApplyStatusEnum） */
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    /** 维度名称排序用：默认排最后（未知维度） */
    private static final int DEFAULT_DIMENSION_SORT = Integer.MAX_VALUE;

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 最近动态时间格式：2026-06-28 14:30 */
    private static final DateTimeFormatter ACTIVITY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final ArchiveRepository archiveRepository;
    private final UserMessageRepository userMessageRepository;
    private final DataCompletenessRepository dataCompletenessRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;

    /**
     * 获取首页数据概览
     *
     * @param userId 当前登录用户 ID
     * @return 首页概览响应
     */
    //注解 @Transactional(readOnly = true)：只读事务，仅查询不修改库，提升性能、避免锁表
    @Transactional(readOnly = true)
    //DashboardResponse 本质上就是一个 DTO
    public DashboardResponse getDashboard(Long userId) {
        //声明一个类型为 User 的变量，变量名叫 user
        //userRepository.findById(userId)：根据主键查用户表；
        //.orElseThrow()：查询为空直接抛自定义业务异常；
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        // ==================== 基础信息（用户/学生档案/班级/专业） ====================
        //如果容器里有值，就把值拿出来；如果容器是空的（没查到数据），就直接返回 null
        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);

        Clazz clazz = (profile != null && profile.getClassId() != null)
                ? clazzRepository.findById(profile.getClassId()).orElse(null)
                : null;
        //条件判断 (profile != null && profile.getClassId() != null)：
        //首先检查 profile 是不是 null（也就是上一行没查到学生档案）。
        //其次检查 profile.getClassId() 是不是 null（也就是学生档案里有没有填班级ID）。
        //只有当这两个条件都满足时，才会去执行查询。
        //为真时 ? clazzRepository.findById(profile.getClassId()).orElse(null)：
        //拿着 classId 去数据库查班级信息。
        //如果查到了就返回班级对象；如果没查到，就返回 null。
        //为假时 : null：
        //如果学生档案不存在，或者档案里没有班级ID，直接给 clazz 赋值为 null，绝对不去查数据库
        Major major = (clazz != null && clazz.getMajorId() != null)
                ? majorRepository.findById(clazz.getMajorId()).orElse(null)
                : null;

        // ==================== 当前学期与成绩 ====================
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;
        //如果用户有学校，就用他的学校 ID；如果没有，就默认当作 1 号学校处理
        Semester currentSemester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);

        SemesterGpaSummary currentGpaSummary = (currentSemester != null)
                ? semesterGpaSummaryRepository.findByUserIdAndSemesterId(userId, currentSemester.getId()).orElse(null)
                : null;
        // 累计总学分：各学期 total_credit 求和（对齐文档示例 86.5 的累计口径）
        BigDecimal totalCredits = semesterGpaSummaryRepository.findByUserId(userId).stream()
                .map(SemesterGpaSummary::getTotalCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ==================== 画像指标 + 雷达图 ====================
        List<PortraitEvaluationScore> currentScores = (currentSemester != null)
                ? portraitEvaluationScoreRepository.findByUserIdAndSemesterId(userId, currentSemester.getId())
                : Collections.emptyList();
        // 上阶段学期：取当前学期评分的对比学期（compared_semester_id）
        Long comparedSemesterId = currentScores.stream()
                .map(PortraitEvaluationScore::getComparedSemesterId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        List<PortraitEvaluationScore> previousScores = (comparedSemesterId != null)
                ? portraitEvaluationScoreRepository.findByUserIdAndSemesterId(userId, comparedSemesterId)
                : Collections.emptyList();

        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        Map<String, String> dimensionNameMap = dimensions.stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));
        Map<Long, String> semesterNameMap = buildSemesterNameMap(currentSemester, comparedSemesterId);

        List<IndicatorItem> indicators = buildIndicators(currentScores, dimensionNameMap, semesterNameMap, dimensions);
        RadarChart radarChart = buildRadarChart(dimensions, currentScores, previousScores);

        // ==================== 申报统计 + 最近动态 ====================
        List<Archive> archives = archiveRepository.findByUserId(userId);
        int approvedCount = countByStatus(archives, STATUS_APPROVED);
        int pendingCount = countByStatus(archives, STATUS_PENDING);
        int rejectedCount = countByStatus(archives, STATUS_REJECTED);

        List<RecentActivity> recentActivities = archiveRepository
                .findTop5ByUserIdAndAuditInfo_SubmittedAtIsNotNullOrderByAuditInfo_SubmittedAtDesc(userId)
                .stream()
                .map(this::toRecentActivity)
                .collect(Collectors.toList());

        // ==================== 数据完整度 + 未读消息 ====================
        DataCompletenessInfo completeness = buildDataCompleteness(userId, currentSemester);
        long unreadMessageCount = userMessageRepository
                .countByUserIdAndIsReadAndIsArchived(userId, 0, 0);

        return DashboardResponse.builder()
                .studentName(user.getName())
                .studentNo(user.getUserNo())
                .major(major != null ? major.getName() : null)
                .className(clazz != null ? clazz.getName() : null)
                .grade(clazz != null ? clazz.getGrade() : null)
                .currentDate(formatCurrentDate(LocalDate.now()))
                .applicationTotal(archives.size())
                .approvedCount(approvedCount)
                .pendingCount(pendingCount)
                .rejectedCount(rejectedCount)
                .currentGpa(cleanDecimal(currentGpaSummary != null ? currentGpaSummary.getWeightedGpa() : null))
                .totalCredits(cleanDecimal(totalCredits))
                .rankInClass(currentGpaSummary != null ? currentGpaSummary.getRankInClass() : null)
                .rankInMajor(currentGpaSummary != null ? currentGpaSummary.getRankInMajor() : null)
                .indicators(indicators)
                .radarChart(radarChart)
                .dataCompleteness(completeness)
                .quickEntries(buildQuickEntries(archives))
                .recentActivities(recentActivities)
                .unreadMessageCount(unreadMessageCount)
                .build();
    }

    // ==================== 画像指标 ====================

    /**
     * 构建画像指标列表，按能力维度 sort 排序
     */
    private List<IndicatorItem> buildIndicators(List<PortraitEvaluationScore> currentScores,
                                                Map<String, String> dimensionNameMap,
                                                Map<Long, String> semesterNameMap,
                                                List<AbilityDimension> dimensions) {
        Map<String, Integer> dimensionSort = new HashMap<>();
        for (int i = 0; i < dimensions.size(); i++) {
            dimensionSort.put(dimensions.get(i).getDimensionCode(), i);
        }
        return currentScores.stream()
                .sorted(Comparator.comparingInt(s -> dimensionSort.getOrDefault(s.getDimensionCode(), DEFAULT_DIMENSION_SORT)))
                .map(s -> IndicatorItem.builder()
                        .dimensionCode(s.getDimensionCode())
                        .dimensionName(dimensionNameMap.get(s.getDimensionCode()))
                        .score(cleanDecimal(s.getScore()))
                        .trend(formatTrend(s.getChangeVal()))
                        .targetScore(cleanDecimal(s.getTargetScore()))
                        .gap(cleanDecimal(s.getGap()))
                        .unit("分")
                        .comparedSemesterId(s.getComparedSemesterId())
                        .comparedSemesterName(s.getComparedSemesterId() != null
                                ? semesterNameMap.get(s.getComparedSemesterId()) : null)
                        .calculationId(s.getCalculationId())
                        .ruleVersion(s.getRuleVersion())
                        .calculatedAt(toIso(s.getEvaluatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建雷达图数据：维度按 ability_dimensions.sort 对齐，
     * 当前/目标取当前学期评分，previous 取上阶段评分，缺失维度补 0。
     */
    private RadarChart buildRadarChart(List<AbilityDimension> dimensions,
                                       List<PortraitEvaluationScore> currentScores,
                                       List<PortraitEvaluationScore> previousScores) {
        Map<String, PortraitEvaluationScore> currentMap = toDimensionMap(currentScores);
        Map<String, PortraitEvaluationScore> previousMap = toDimensionMap(previousScores);

        List<DimensionInfo> dimensionInfos = new ArrayList<>();
        List<BigDecimal> current = new ArrayList<>();
        List<BigDecimal> target = new ArrayList<>();
        List<BigDecimal> previous = new ArrayList<>();

        for (AbilityDimension d : dimensions) {
            dimensionInfos.add(DimensionInfo.builder()
                    .code(d.getDimensionCode())
                    .name(d.getDimensionName())
                    .build());
            PortraitEvaluationScore cur = currentMap.get(d.getDimensionCode());
            PortraitEvaluationScore prev = previousMap.get(d.getDimensionCode());
            current.add(cleanDecimal(cur != null ? cur.getScore() : BigDecimal.ZERO));
            target.add(cleanDecimal(cur != null ? cur.getTargetScore() : BigDecimal.ZERO));
            previous.add(cleanDecimal(prev != null ? prev.getScore() : BigDecimal.ZERO));
        }
        return RadarChart.builder()
                .dimensions(dimensionInfos)
                .current(current)
                .target(target)
                .previous(previous)
                .build();
    }

    /**
     * 按维度编码建立评分映射（同一维度仅保留一条）
     */
    private Map<String, PortraitEvaluationScore> toDimensionMap(List<PortraitEvaluationScore> scores) {
        return scores.stream().collect(Collectors.toMap(
                PortraitEvaluationScore::getDimensionCode, s -> s, (a, b) -> a));
    }

    // ==================== 数据完整度 ====================

    /**
     * 构建数据完整度：rate 为各维度完整度平均值（四舍五入），
     * missingItems 为各维度缺失项汇总去重。
     */
    private DataCompletenessInfo buildDataCompleteness(Long userId, Semester currentSemester) {
        if (currentSemester == null) {
            return emptyCompleteness();
        }
        List<DataCompleteness> list = dataCompletenessRepository
                .findByUserIdAndSemesterId(userId, currentSemester.getId());
        if (list.isEmpty()) {
            return emptyCompleteness();
        }
        Set<String> missing = new LinkedHashSet<>();
        for (DataCompleteness dc : list) {
            missing.addAll(parseJsonArray(dc.getMissingItems()));
        }
        int rate = (int) Math.round(list.stream()
                .map(DataCompleteness::getCompletenessRate)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        return DataCompletenessInfo.builder()
                .rate(rate)
                .missingItems(new ArrayList<>(missing))
                .build();
    }

    private DataCompletenessInfo emptyCompleteness() {
        return DataCompletenessInfo.builder()
                .rate(0)
                .missingItems(Collections.emptyList())
                .build();
    }

    // ==================== 快捷入口 ====================

    /**
     * 构建快捷入口（导航配置固定，recent 由真实档案数据判断）
     */
    private List<QuickEntry> buildQuickEntries(List<Archive> archives) {
        Set<String> archiveTypes = archives.stream()
                .map(Archive::getArchiveType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<QuickEntry> entries = new ArrayList<>();
        entries.add(QuickEntry.builder()
                .name("成长时间轴").icon("timeline").path("/growth-timeline")
                .recent(!archives.isEmpty()).build());
        entries.add(QuickEntry.builder()
                .name("奖学金").icon("scholarship").path("/applications?tab=scholarship")
                .recent(archiveTypes.contains("scholarship")).build());
        entries.add(QuickEntry.builder()
                .name("社会实践").icon("practice").path("/applications?tab=practice")
                .recent(archiveTypes.contains("social_practice")).build());
        entries.add(QuickEntry.builder()
                .name("学科竞赛").icon("competition").path("/applications?tab=competition")
                .recent(archiveTypes.contains("competition")).build());
        return entries;
    }

    // ==================== 最近动态 ====================

    /**
     * 档案记录 → 最近动态项
     */
    private RecentActivity toRecentActivity(Archive archive) {
        LocalDateTime time = archive.getAuditInfo() != null ? archive.getAuditInfo().getSubmittedAt() : null;
        return RecentActivity.builder()
                .id(archive.getId())
                .title(archive.getTitle())
                .time(time != null ? time.format(ACTIVITY_TIME_FORMAT) : null)
                .type(activityType(archive.getStatus()))
                .archiveType(archive.getArchiveType())
                .status(archive.getStatus())
                .build();
    }

    /**
     * 申报状态 → 动态类型
     */
    private String activityType(Integer status) {
        if (status == null) return "draft";
        return switch (status) {
            case STATUS_PENDING -> "submitted";
            case STATUS_APPROVED -> "approved";
            case STATUS_REJECTED -> "rejected";
            case 4 -> "revoked";
            default -> "draft";
        };
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 统计指定状态的档案数
     */
    private int countByStatus(List<Archive> archives, int status) {
        return (int) archives.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == status)
                .count();
    }

    /**
     * 构建学期 ID → 学期名称映射（当前学期 + 对比学期）
     */
    private Map<Long, String> buildSemesterNameMap(Semester currentSemester, Long comparedSemesterId) {
        Map<Long, String> map = new HashMap<>();
        if (currentSemester != null) {
            map.put(currentSemester.getId(), currentSemester.getName());
        }
        if (comparedSemesterId != null) {
            semesterRepository.findById(comparedSemesterId)
                    .ifPresent(s -> map.put(s.getId(), s.getName()));
        }
        return map;
    }

    /**
     * 格式化趋势：7.00 → "+7"，-3.50 → "-3.5"
     */
    private String formatTrend(BigDecimal change) {
        if (change == null) return null;
        BigDecimal c = cleanDecimal(change);
        return c.compareTo(BigDecimal.ZERO) >= 0 ? "+" + c.toPlainString() : c.toPlainString();
    }

    /**
     * 清理 BigDecimal 的尾部零（88.00 → 88，86.50 → 86.5），保持 JSON 数值简洁
     */
    private BigDecimal cleanDecimal(BigDecimal value) {
        if (value == null) return null;
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    /**
     * LocalDateTime → ISO 8601 带时区字符串
     */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    /**
     * 当前日期中文格式：2026年7月7日星期一
     */
    private String formatCurrentDate(LocalDate date) {
        String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        String weekday = weekdays[date.getDayOfWeek().getValue() - 1];
        return date.getYear() + "年" + date.getMonthValue() + "月"
                + date.getDayOfMonth() + "日" + weekday;
    }

    /**
     * 解析 JSON 数组字符串为 List
     * 如 "[\"学科竞赛-国际级竞赛经历\"]" → ["学科竞赛-国际级竞赛经历"]
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            if (trimmed.isEmpty()) return Collections.emptyList();
            return Arrays.stream(trimmed.split(","))
                    .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
