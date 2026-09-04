package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.evaluation.DataCompleteness;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.grade.GpaRecord;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.user.UserInterest;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.DataCompletenessRepository;
import com.example.studentarchives.repository.GpaRecordRepository;
import com.example.studentarchives.repository.SemesterGpaSummaryRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserInterestRepository;
import com.example.studentarchives.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据完整度计算服务（data_completeness 写入方）
 * <p>
 * 修复「档案数据完整度恒为 0」：此前 data_completeness 表仅有读取方
 * （ProfileService.getDataCompleteness / HomeService / AdminStatisticsService），
 * 无任何写入逻辑，导致表永远为空、overallRate 恒为 0。
 * <p>
 * 本服务按能力维度（ability_dimensions.dimension_code）统计该学生
 * user_id + semester_id 下学籍 / 联系方式 / 成绩 / 奖项 / 兴趣标签 / 自我评价等
 * 档案字段的填写情况，计算各维度 completeness_rate（0-100）与 missing_items，
 * 按唯一键 (user_id, semester_id, dimension_code) upsert 到 data_completeness。
 * <p>
 * 触发时机：
 * <ul>
 *   <li>评分重算（AdminScoreService.processStudent）——批量 / 档案审核通过自动评分 / 成绩导入后重算；</li>
 *   <li>档案审核通过但无生效指标规则版本（AdminScoreService.recalculateStudent 无规则分支）；</li>
 *   <li>学生档案信息变更（ProfileService 更新联系方式 / 政治面貌 / 学生状态 / 兴趣 / 自我评价）；</li>
 *   <li>读取兜底（ProfileService.getDataCompleteness 查询为空时现场计算并落库）。</li>
 * </ul>
 * <p>
 * 事务隔离：对外方法使用 REQUIRES_NEW 独立事务，避免从评分事务 / 只读事务内调用时
 * 因完整度计算失败而回滚外层业务；调用方应以 try/catch 包裹（完整度属可重建的派生数据，
 * 计算失败不应阻塞主流程）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCompletenessService {

    /** 已通过档案状态（与评分口径一致：仅统计审核通过的档案） */
    private static final int ARCHIVE_APPROVED = 2;

    /** 学校 ID 兜底（学生未挂学校时按学校 1 处理，与 ProfileService 一致） */
    private static final Long DEFAULT_SCHOOL_ID = 1L;

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final GpaRecordRepository gpaRecordRepository;
    private final SemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final ArchiveRepository archiveRepository;
    private final UserInterestRepository userInterestRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final DataCompletenessRepository dataCompletenessRepository;
    private final SemesterRepository semesterRepository;
    private final ClazzRepository clazzRepository;
    private final ObjectMapper objectMapper;

    /**
     * 计算并落库指定学生指定学期的数据完整度（独立事务）。
     *
     * @param userId     学生用户 ID
     * @param semesterId 学期 ID（data_completeness.semester_id 非空）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateForStudent(Long userId, Long semesterId) {
        doRecalculate(userId, semesterId);
    }

    /**
     * 计算并落库指定学生「当前学期」的数据完整度（独立事务）。
     * <p>
     * 供档案信息变更（学籍 / 联系 / 兴趣 / 自我评价）增量刷新用：取该生学校当前学期。
     * 历史学期完整度由「评分重算」批量刷新。
     *
     * @param userId 学生用户 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateCurrentSemester(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : DEFAULT_SCHOOL_ID;
        Semester current = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);
        if (current == null) {
            log.debug("数据完整度计算：学校无当前学期，跳过 userId={}", userId);
            return;
        }
        doRecalculate(userId, current.getId());
    }

    // ==================== 核心计算 ====================

    /**
     * 单学生单学期完整度计算 + upsert 到 data_completeness。
     * <p>
     * 各维度 rate = round(已填写项 / 检查项总数 × 100)，missing_items 为未填写项的
     * 中文标签列表（供前端提示「还差哪些没填」）。已存在的行按唯一键更新，不存在则新建。
     *
     * @param userId     学生用户 ID
     * @param semesterId 学期 ID
     */
    private void doRecalculate(Long userId, Long semesterId) {
        if (userId == null || semesterId == null) {
            log.warn("数据完整度计算缺少参数，跳过: userId={}, semesterId={}", userId, semesterId);
            return;
        }
        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        if (dimensions.isEmpty()) {
            log.debug("数据完整度计算：无启用能力维度，跳过 userId={}, semesterId={}", userId, semesterId);
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("数据完整度计算：用户不存在，跳过 userId={}", userId);
            return;
        }
        DataContext ctx = buildContext(user, semesterId);

        LocalDateTime now = LocalDateTime.now();
        Map<String, DataCompleteness> existing = dataCompletenessRepository
                .findByUserIdAndSemesterId(userId, semesterId).stream()
                .collect(Collectors.toMap(DataCompleteness::getDimensionCode, d -> d, (a, b) -> a));

        List<DataCompleteness> rows = new ArrayList<>();
        for (AbilityDimension dim : dimensions) {
            String dimensionCode = dim.getDimensionCode();
            List<Check> checks = buildChecks(dimensionCode, ctx);
            if (checks.isEmpty()) {
                log.debug("维度 {} 未配置完整度检查项，跳过", dimensionCode);
                continue;
            }
            long filled = checks.stream().filter(Check::filled).count();
            int rate = (int) Math.round(filled * 100.0 / checks.size());
            List<String> missing = checks.stream()
                    .filter(c -> !c.filled())
                    .map(Check::label)
                    .collect(Collectors.toList());

            DataCompleteness row = existing.get(dimensionCode);
            if (row == null) {
                row = new DataCompleteness();
                row.setUserId(userId);
                row.setSemesterId(semesterId);
                row.setDimensionCode(dimensionCode);
            }
            row.setCompletenessRate(rate);
            row.setMissingItems(toMissingItemsJson(missing));
            row.setCalculatedAt(now);
            rows.add(row);
        }

        if (!rows.isEmpty()) {
            dataCompletenessRepository.saveAll(rows);
            log.info("数据完整度计算完成: userId={}, semesterId={}, dimensions={}", userId, semesterId, rows.size());
        }
    }

    // ==================== 维度检查项 ====================

    /**
     * 构建指定维度的「缺项检查列表」。未知维度返回空列表（不产出该维度行），
     * 新增维度需在此补充检查项。
     */
    private List<Check> buildChecks(String dimensionCode, DataContext ctx) {
        return switch (dimensionCode) {
            case "academic" -> academicChecks(ctx);
            case "competition" -> competitionChecks(ctx);
            case "quality" -> qualityChecks(ctx);
            default -> List.of();
        };
    }

    /** 学业成绩：学籍信息 + 学期学业表现 */
    private List<Check> academicChecks(DataContext ctx) {
        User user = ctx.user;
        StudentProfile profile = ctx.profile;
        List<Check> checks = new ArrayList<>();
        checks.add(new Check("性别", user.getGender() != null));
        checks.add(new Check("出生日期", user.getBirthDate() != null));
        checks.add(new Check("政治面貌", profile != null && notBlank(profile.getPoliticalStatus())));
        checks.add(new Check("学生状态", profile != null && notBlank(profile.getStudentStatus())));
        checks.add(new Check("学历层次", profile != null && notBlank(profile.getDegreeType())));
        checks.add(new Check("班级信息", ctx.hasClassInfo));
        checks.add(new Check("学期课程成绩", ctx.gpaRecords != null && !ctx.gpaRecords.isEmpty()));
        checks.add(new Check("学期成绩汇总",
                ctx.gpaSummary != null && ctx.gpaSummary.getCourseCount() > 0));
        return checks;
    }

    /** 竞赛实践：竞赛 / 实践 / 创新 / 获奖 / 志愿 */
    private List<Check> competitionChecks(DataContext ctx) {
        List<Check> checks = new ArrayList<>();
        checks.add(new Check("竞赛获奖", hasAnyArchiveType(ctx, "competition", "academic_competition")));
        checks.add(new Check("社会实践", hasAnyArchiveType(ctx, "social_practice", "internship")));
        checks.add(new Check("科研创新", hasAnyArchiveType(ctx, "research", "academic_research",
                "innovation_entrepreneurship", "training_project")));
        checks.add(new Check("奖学金荣誉", hasAnyArchiveType(ctx, "scholarship", "honor_certificate")));
        checks.add(new Check("志愿服务", hasVolunteerContribution(ctx)));
        return checks;
    }

    /** 综合素质：联系方式 / 兴趣 / 自我评价 / 组织 / 证书 / 志愿 */
    private List<Check> qualityChecks(DataContext ctx) {
        UserContactInfo contact = ctx.contact;
        List<Check> checks = new ArrayList<>();
        checks.add(new Check("联系电话", contact != null && notBlank(contact.getPhone())));
        checks.add(new Check("联系邮箱", contact != null && notBlank(contact.getEmail())));
        checks.add(new Check("紧急联系人", contact != null && notBlank(contact.getEmergencyName())
                && notBlank(contact.getEmergencyPhone())));
        checks.add(new Check("兴趣标签", ctx.interests != null && !ctx.interests.isEmpty()));
        checks.add(new Check("自我评价", ctx.profile != null && notBlank(ctx.profile.getSelfEvaluation())));
        checks.add(new Check("组织任职", hasAnyArchiveType(ctx, "organization")));
        checks.add(new Check("证书资质", hasAnyArchiveType(ctx, "certificate", "training")));
        checks.add(new Check("志愿服务", hasVolunteerContribution(ctx)));
        return checks;
    }

    /** 是否存在指定档案类型的已通过档案 */
    private boolean hasAnyArchiveType(DataContext ctx, String... types) {
        Set<String> wanted = new HashSet<>(Arrays.asList(types));
        return ctx.archives.stream()
                .anyMatch(a -> a.getArchiveType() != null && wanted.contains(a.getArchiveType()));
    }

    /** 是否有志愿服务：志愿时长 > 0，或存在标题含 公益/志愿/社区 的已通过档案 */
    private boolean hasVolunteerContribution(DataContext ctx) {
        if (ctx.volunteerHours != null && ctx.volunteerHours.compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        return ctx.archives.stream().anyMatch(a -> {
            String title = a.getTitle();
            return title != null && (title.contains("公益") || title.contains("志愿") || title.contains("社区"));
        });
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    // ==================== 数据加载 ====================

    /** 单学生完整度计算所需数据上下文 */
    private static class DataContext {
        final User user;
        final StudentProfile profile;
        final UserContactInfo contact;
        final List<GpaRecord> gpaRecords;
        final SemesterGpaSummary gpaSummary;
        final List<Archive> archives;
        final List<UserInterest> interests;
        final BigDecimal volunteerHours;
        final boolean hasClassInfo;

        DataContext(User user, StudentProfile profile, UserContactInfo contact,
                    List<GpaRecord> gpaRecords, SemesterGpaSummary gpaSummary,
                    List<Archive> archives, List<UserInterest> interests,
                    BigDecimal volunteerHours, boolean hasClassInfo) {
            this.user = user;
            this.profile = profile;
            this.contact = contact;
            this.gpaRecords = gpaRecords;
            this.gpaSummary = gpaSummary;
            this.archives = archives;
            this.interests = interests;
            this.volunteerHours = volunteerHours;
            this.hasClassInfo = hasClassInfo;
        }
    }

    /** 单条完整度检查：label 为缺项展示名（缺失时返回前端），filled 表示是否已填写 */
    private record Check(String label, boolean filled) {
    }

    /**
     * 加载单学生完整度计算所需数据。
     * <p>
     * 档案口径与评分一致：仅统计 status=2（已通过）且学期匹配（semester_id 为空视为全学期适用）
     * 的档案；学期成绩取该学期 gpa_records / semester_gpa_summaries。
     */
    private DataContext buildContext(User user, Long semesterId) {
        Long userId = user.getId();
        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
        UserContactInfo contact = userContactInfoRepository.findByUserId(userId).orElse(null);
        List<GpaRecord> gpaRecords = gpaRecordRepository
                .findByUserIdAndSemesterIdOrderByCourseCodeAsc(userId, semesterId);
        SemesterGpaSummary gpaSummary = semesterGpaSummaryRepository
                .findByUserIdAndSemesterId(userId, semesterId).orElse(null);

        List<Archive> archives = archiveRepository.findByUserIdAndStatus(userId, ARCHIVE_APPROVED);
        if (semesterId != null) {
            archives = archives.stream()
                    .filter(a -> a.getSemesterId() == null || Objects.equals(a.getSemesterId(), semesterId))
                    .collect(Collectors.toList());
        }

        List<UserInterest> interests = userInterestRepository.findByUserIdOrderBySortAsc(userId);
        BigDecimal volunteerHours = profile != null ? profile.getVolunteerHours() : null;
        boolean hasClassInfo = profile != null && profile.getClassId() != null
                && clazzRepository.findById(profile.getClassId()).isPresent();

        return new DataContext(user, profile, contact, gpaRecords, gpaSummary,
                archives, interests, volunteerHours, hasClassInfo);
    }

    // ==================== 工具 ====================

    /** 缺失项序列化为 JSON 数组字符串（["联系电话", ...]）；无缺失返回 null */
    private String toMissingItemsJson(List<String> missing) {
        if (missing == null || missing.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(missing);
        } catch (JsonProcessingException e) {
            log.warn("序列化缺失项失败，回退为逗号拼接: {}", missing, e);
            return missing.stream()
                    .map(m -> "\"" + m + "\"")
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }
}
