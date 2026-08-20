package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.statistics.response.DimensionAvgScoreItem;
import com.example.studentarchives.dto.Fmy.statistics.response.SnapshotRefreshResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.StatisticsTypeCountItem;
import com.example.studentarchives.dto.Fmy.statistics.response.TopInterestItem;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.evaluation.OrgArchiveSummary;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.UserInterest;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.AdminCollegeRepository;
import com.example.studentarchives.repository.AdminOrgArchiveSummaryRepository;
import com.example.studentarchives.repository.AdminSemesterGpaSummaryRepository;
import com.example.studentarchives.repository.AdminUserInterestRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计快照刷新服务
 * <p>
 * 负责按学校/学期维度聚合业务表数据，生成 {@code org_archive_summaries}
 * 学校级与行级（学院/专业/班级）快照，供统计看板与组织下钻读取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsSnapshotService {

    /** 组织维度：1=学校 2=学院 3=专业 4=班级 */
    private static final int ORG_SCHOOL = 1;
    private static final int ORG_COLLEGE = 2;
    private static final int ORG_MAJOR = 3;
    private static final int ORG_CLASS = 4;

    /** 奖项申请已通过状态 */
    private static final int AWARD_STATUS_APPROVED = 2;

    /** 热门兴趣标签 TOP N */
    private static final int HOT_TAG_TOP_N = 5;

    /** 画像维度 TOP N */
    private static final int TOP_DIMENSION_N = 5;

    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final AdminSemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final AdminUserInterestRepository userInterestRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final AdminOrgArchiveSummaryRepository orgArchiveSummaryRepository;
    private final AdminCollegeRepository collegeRepository;
    private final MajorRepository majorRepository;
    private final ClazzRepository clazzRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper;

    /**
     * 刷新学校级与行级档案汇总快照
     *
     * @param schoolId   学校 ID
     * @param semesterId 学期 ID，传 null 时取当前学期
     * @return 刷新结果
     */
    @Transactional
    public SnapshotRefreshResponse refresh(Long schoolId, Long semesterId) {
        if (schoolId == null) {
            throw BusinessException.badParam("学校 ID 不能为空");
        }

        Long effSemesterId = semesterId;
        if (effSemesterId == null) {
            effSemesterId = semesterRepository.findCurrentBySchoolId(schoolId)
                    .map(Semester::getId)
                    .orElseThrow(() -> BusinessException.badParam("未找到当前学期"));
        }

        LocalDate statDate = LocalDate.now();

        // 幂等：先删除该校该学期该日的全部旧快照
        orgArchiveSummaryRepository.deleteBySchoolIdAndSemesterIdAndStatDate(schoolId, effSemesterId, statDate);

        // 1. 生成学校级快照
        OrgArchiveSummary schoolSnapshot = buildSchoolSnapshot(schoolId, effSemesterId, statDate);
        orgArchiveSummaryRepository.save(schoolSnapshot);

        // 2. 生成学院/专业/班级行级快照
        List<OrgArchiveSummary> orgSnapshots = buildOrgLevelSnapshots(schoolId, effSemesterId, statDate);
        orgArchiveSummaryRepository.saveAll(orgSnapshots);

        log.info("统计快照刷新完成: schoolId={}, semesterId={}, statDate={}, schoolSnapshot=1, orgSnapshots={}",
                schoolId, effSemesterId, statDate, orgSnapshots.size());

        return SnapshotRefreshResponse.builder()
                .schoolId(schoolId)
                .semesterId(effSemesterId)
                .statDate(statDate)
                .refreshedAt(LocalDateTime.now())
                .studentCount(schoolSnapshot.getTotalStudents())
                .archiveCount(schoolSnapshot.getTotalArchives())
                .awardCount(schoolSnapshot.getTotalAwards())
                .avgGpa(schoolSnapshot.getAvgGpa() == null ? null : schoolSnapshot.getAvgGpa().doubleValue())
                .build();
    }

    private OrgArchiveSummary buildSchoolSnapshot(Long schoolId, Long semesterId, LocalDate statDate) {
        int totalStudents = toInt(archiveRepository.countDistinctUserIdBySchoolIdAndSemesterId(schoolId, semesterId));
        int totalArchives = toInt(archiveRepository.countBySchoolIdAndSemesterId(schoolId, semesterId));
        int totalAwards = toInt(awardApplicationRepository.countBySchoolIdAndSemesterIdAndStatus(schoolId, semesterId, AWARD_STATUS_APPROVED));
        Double avgGpa = semesterGpaSummaryRepository.avgWeightedGpaBySchoolIdAndSemesterId(schoolId, semesterId);

        List<StatisticsTypeCountItem> typeDistribution = aggregateArchiveTypeDistribution(schoolId, semesterId);
        List<TopInterestItem> hotTags = aggregateHotTags(schoolId);
        List<DimensionAvgScoreItem> topDimensions = aggregateTopDimensions(schoolId, semesterId);

        OrgArchiveSummary snapshot = new OrgArchiveSummary();
        snapshot.setSchoolId(schoolId);
        snapshot.setSemesterId(semesterId);
        snapshot.setStatDate(statDate);
        snapshot.setOrgType(ORG_SCHOOL);
        snapshot.setOrgId(schoolId);
        snapshot.setTotalStudents(totalStudents);
        snapshot.setTotalArchives(totalArchives);
        snapshot.setTotalAwards(totalAwards);
        snapshot.setAvgGpa(roundGpa(avgGpa));
        snapshot.setArchiveTypeDistribution(toJson(typeDistribution));
        snapshot.setHotTags(toJson(hotTags));
        snapshot.setTopDimensions(toJson(topDimensions));
        return snapshot;
    }

    private List<OrgArchiveSummary> buildOrgLevelSnapshots(Long schoolId, Long semesterId, LocalDate statDate) {
        // 构建组织索引
        List<College> colleges = collegeRepository.findBySchoolId(schoolId);
        Map<Long, String> collegeNames = colleges.stream()
                .collect(Collectors.toMap(College::getId, College::getName, (a, b) -> a));
        List<Long> collegeIds = colleges.stream().map(College::getId).toList();

        List<Major> majors = collegeIds.isEmpty() ? List.of()
                : majorRepository.findByCollegeIdIn(collegeIds);
        Map<Long, Long> majorToCollege = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getCollegeId, (a, b) -> a));
        Map<Long, String> majorNames = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getName, (a, b) -> a));
        List<Long> majorIds = majors.stream().map(Major::getId).toList();

        List<Clazz> classes = majorIds.isEmpty() ? List.of()
                : clazzRepository.findByMajorIdIn(majorIds);
        Map<Long, Long> classToMajor = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getMajorId, (a, b) -> a));
        Map<Long, String> classNames = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getName, (a, b) -> a));
        Map<Long, String> classToGrade = classes.stream()
                .filter(c -> c.getGrade() != null)
                .collect(Collectors.toMap(Clazz::getId, Clazz::getGrade, (a, b) -> a));
        List<Long> classIds = classes.stream().map(Clazz::getId).toList();

        // 学生与班级映射
        List<StudentProfile> profiles = classIds.isEmpty() ? List.of()
                : studentProfileRepository.findByClassIdIn(classIds);
        Map<Long, Long> userToClass = profiles.stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getClassId, (a, b) -> a));
        List<Long> userIds = profiles.stream().map(StudentProfile::getUserId).distinct().toList();

        // 初始化班级级统计桶
        Map<Long, OrgStats> classStats = new LinkedHashMap<>();
        for (Clazz c : classes) {
            classStats.put(c.getId(), new OrgStats(c.getId(), classNames.get(c.getId()), classToGrade.get(c.getId())));
        }

        // 汇总学生
        for (StudentProfile p : profiles) {
            OrgStats stat = classStats.get(p.getClassId());
            if (stat != null) {
                stat.studentIds.add(p.getUserId());
            }
        }

        // 加载业务数据（已按班级维度过滤）
        List<Archive> archives = userIds.isEmpty() ? List.of() : archiveRepository.findByUserIdIn(userIds).stream()
                .filter(a -> Objects.equals(a.getSemesterId(), semesterId))
                .toList();
        List<AwardApplication> awards = userIds.isEmpty() ? List.of()
                : awardApplicationRepository.findByUserIdInAndSemesterIdAndStatus(userIds, semesterId, AWARD_STATUS_APPROVED);
        List<SemesterGpaSummary> gpaList = userIds.isEmpty() ? List.of()
                : semesterGpaSummaryRepository.findBySemesterIdAndUserIdIn(semesterId, userIds);
        List<PortraitEvaluationScore> scores = userIds.isEmpty() ? List.of()
                : portraitEvaluationScoreRepository.findByUserIdInAndSemesterId(userIds, semesterId);
        List<UserInterest> interests = userIds.isEmpty() ? List.of()
                : userInterestRepository.findByUserIdIn(userIds);

        // 聚合到班级
        for (Archive a : archives) {
            OrgStats stat = classStats.get(userToClass.get(a.getUserId()));
            if (stat != null) {
                stat.archiveCount++;
                stat.typeCount.merge(a.getArchiveType(), 1, Integer::sum);
            }
        }
        for (AwardApplication a : awards) {
            OrgStats stat = classStats.get(userToClass.get(a.getUserId()));
            if (stat != null) {
                stat.awardCount++;
            }
        }
        for (SemesterGpaSummary g : gpaList) {
            OrgStats stat = classStats.get(userToClass.get(g.getUserId()));
            if (stat != null && g.getWeightedGpa() != null) {
                stat.gpas.add(g.getWeightedGpa());
            }
        }
        for (PortraitEvaluationScore s : scores) {
            OrgStats stat = classStats.get(userToClass.get(s.getUserId()));
            if (stat != null && s.getScore() != null) {
                stat.dimensionScores.computeIfAbsent(s.getDimensionCode(), k -> new ArrayList<>()).add(s.getScore());
            }
        }
        for (UserInterest i : interests) {
            OrgStats stat = classStats.get(userToClass.get(i.getUserId()));
            if (stat != null) {
                stat.interestTags.merge(i.getTagName(), 1, Integer::sum);
            }
        }

        // 向上汇总到专业与学院
        Map<Long, OrgStats> majorStats = new LinkedHashMap<>();
        Map<Long, OrgStats> collegeStats = new LinkedHashMap<>();
        for (OrgStats cls : classStats.values()) {
            Long majorId = classToMajor.get(cls.orgId);
            Long collegeId = majorToCollege.get(majorId);
            if (majorId == null || collegeId == null) {
                continue;
            }
            majorStats.computeIfAbsent(majorId, k -> new OrgStats(k, majorNames.get(k), null)).merge(cls);
            collegeStats.computeIfAbsent(collegeId, k -> new OrgStats(k, collegeNames.get(k), null)).merge(cls);
        }

        // 构建快照
        List<OrgArchiveSummary> snapshots = new ArrayList<>();
        for (OrgStats s : classStats.values()) {
            snapshots.add(toSnapshot(s, ORG_CLASS, schoolId, semesterId, statDate));
        }
        for (OrgStats s : majorStats.values()) {
            snapshots.add(toSnapshot(s, ORG_MAJOR, schoolId, semesterId, statDate));
        }
        for (OrgStats s : collegeStats.values()) {
            snapshots.add(toSnapshot(s, ORG_COLLEGE, schoolId, semesterId, statDate));
        }
        return snapshots;
    }

    private OrgArchiveSummary toSnapshot(OrgStats stat, int orgType, Long schoolId, Long semesterId, LocalDate statDate) {
        OrgArchiveSummary snapshot = new OrgArchiveSummary();
        snapshot.setSchoolId(schoolId);
        snapshot.setSemesterId(semesterId);
        snapshot.setStatDate(statDate);
        snapshot.setOrgType(orgType);
        snapshot.setOrgId(stat.orgId);
        snapshot.setGrade(stat.grade);
        snapshot.setTotalStudents(stat.studentIds.size());
        snapshot.setTotalArchives(stat.archiveCount);
        snapshot.setTotalAwards(stat.awardCount);
        snapshot.setAvgGpa(avgBigDecimal(stat.gpas));
        snapshot.setArchiveTypeDistribution(toJson(buildTypeDistribution(stat.typeCount)));
        snapshot.setHotTags(toJson(buildHotTags(stat.interestTags)));
        snapshot.setTopDimensions(toJson(buildTopDimensions(stat.dimensionScores)));
        return snapshot;
    }

    private List<StatisticsTypeCountItem> aggregateArchiveTypeDistribution(Long schoolId, Long semesterId) {
        List<StatisticsTypeCountItem> result = new ArrayList<>();
        List<Object[]> rows = archiveRepository.countGroupByArchiveType(schoolId, semesterId);
        for (Object[] row : rows) {
            String type = Objects.toString(row[0], null);
            Number count = (Number) row[1];
            if (type != null && count != null) {
                result.add(StatisticsTypeCountItem.builder()
                        .archiveType(type)
                        .count(count.intValue())
                        .build());
            }
        }
        return result;
    }

    private List<TopInterestItem> aggregateHotTags(Long schoolId) {
        List<TopInterestItem> result = new ArrayList<>();
        List<Object[]> rows = userInterestRepository.countGroupByTagNameTopN(schoolId, PageRequest.of(0, HOT_TAG_TOP_N));
        for (Object[] row : rows) {
            String tag = Objects.toString(row[0], null);
            Number count = (Number) row[1];
            if (tag != null && count != null) {
                result.add(TopInterestItem.builder()
                        .interest(tag)
                        .count(count.intValue())
                        .build());
            }
        }
        return result;
    }

    private List<DimensionAvgScoreItem> aggregateTopDimensions(Long schoolId, Long semesterId) {
        List<DimensionAvgScoreItem> result = new ArrayList<>();
        List<Object[]> rows = portraitEvaluationScoreRepository.avgScoreGroupByDimension(schoolId, semesterId);
        for (Object[] row : rows) {
            String dimensionCode = Objects.toString(row[0], null);
            Number avgScore = (Number) row[1];
            if (dimensionCode == null || avgScore == null) {
                continue;
            }
            String dimensionName = abilityDimensionRepository.findByDimensionCode(dimensionCode)
                    .map(AbilityDimension::getDimensionName)
                    .orElse(dimensionCode);
            result.add(DimensionAvgScoreItem.builder()
                    .dimensionCode(dimensionCode)
                    .dimensionName(dimensionName)
                    .avgScore(BigDecimal.valueOf(avgScore.doubleValue())
                            .setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .build());
        }
        return result;
    }

    private List<StatisticsTypeCountItem> buildTypeDistribution(Map<String, Integer> typeCount) {
        return typeCount.entrySet().stream()
                .map(e -> StatisticsTypeCountItem.builder().archiveType(e.getKey()).count(e.getValue()).build())
                .sorted(Comparator.comparing(StatisticsTypeCountItem::getCount).reversed())
                .toList();
    }

    private List<TopInterestItem> buildHotTags(Map<String, Integer> interestTags) {
        return interestTags.entrySet().stream()
                .map(e -> TopInterestItem.builder().interest(e.getKey()).count(e.getValue()).build())
                .sorted(Comparator.comparing(TopInterestItem::getCount).reversed())
                .limit(HOT_TAG_TOP_N)
                .toList();
    }

    private List<DimensionAvgScoreItem> buildTopDimensions(Map<String, List<BigDecimal>> dimensionScores) {
        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllByOrderBySortAsc().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode, AbilityDimension::getDimensionName, (a, b) -> a));
        return dimensionScores.entrySet().stream()
                .map(e -> {
                    double avg = e.getValue().stream()
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0);
                    String code = e.getKey();
                    String name = dimensionNameMap.getOrDefault(code, code);
                    return DimensionAvgScoreItem.builder()
                            .dimensionCode(code)
                            .dimensionName(name)
                            .avgScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue())
                            .build();
                })
                .sorted(Comparator.comparing(DimensionAvgScoreItem::getAvgScore).reversed())
                .limit(TOP_DIMENSION_N)
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("统计快照 JSON 序列化失败", e);
            return null;
        }
    }

    private static int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static BigDecimal roundGpa(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal avgBigDecimal(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        double avg = values.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 组织维度聚合中间对象
     */
    private static class OrgStats {
        final Long orgId;
        final String orgName;
        final String grade;
        final Set<Long> studentIds = java.util.Collections.newSetFromMap(new LinkedHashMap<>());
        int archiveCount = 0;
        int awardCount = 0;
        final List<BigDecimal> gpas = new ArrayList<>();
        final Map<String, List<BigDecimal>> dimensionScores = new HashMap<>();
        final Map<String, Integer> interestTags = new HashMap<>();
        final Map<String, Integer> typeCount = new HashMap<>();

        OrgStats(Long orgId, String orgName, String grade) {
            this.orgId = orgId;
            this.orgName = orgName;
            this.grade = grade;
        }

        void merge(OrgStats other) {
            this.studentIds.addAll(other.studentIds);
            this.archiveCount += other.archiveCount;
            this.awardCount += other.awardCount;
            this.gpas.addAll(other.gpas);
            other.dimensionScores.forEach((k, v) ->
                    this.dimensionScores.computeIfAbsent(k, ignored -> new ArrayList<>()).addAll(v));
            other.interestTags.forEach((k, v) ->
                    this.interestTags.merge(k, v, Integer::sum));
            other.typeCount.forEach((k, v) ->
                    this.typeCount.merge(k, v, Integer::sum));
        }
    }
}
