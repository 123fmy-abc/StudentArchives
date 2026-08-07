package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.profile.request.BasicInfoUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.ContactUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.InterestUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.SelfEvaluationUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.StudentStatusUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.response.BasicInfoUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ContactUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.StudentStatusUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.DataCompletenessResponse;
import com.example.studentarchives.dto.Fmy.profile.response.DataCompletenessResponse.CompletenessItem;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse.AbilityItem;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse.RingYear;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse.SemesterGroup;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse.Summary;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse.TimelineItem;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse.YearGroup;
import com.example.studentarchives.dto.Fmy.profile.response.InterestUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.AcademicInfo;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.ContactInfo;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.DimensionProfileItem;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.InterestItem;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.PersonalAwardItem;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.SemesterGradeItem;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse.WeaknessItem;
import com.example.studentarchives.dto.Fmy.profile.response.ScoreDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ScoreDetailResponse.ScoreDetailItem;
import com.example.studentarchives.dto.Fmy.profile.response.ScoreListResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ScoreListResponse.ScoreItem;
import com.example.studentarchives.dto.Fmy.profile.response.SelfEvaluationResponse;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.evaluation.AwardSummary;
import com.example.studentarchives.entity.evaluation.DataCompleteness;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.evaluation.ScoreCalculation;
import com.example.studentarchives.entity.evaluation.ScoreCalculationDetail;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.growth.GrowthTimeline;
import com.example.studentarchives.entity.growth.GrowthTimelineAbility;
import com.example.studentarchives.entity.growth.GrowthTimelineTag;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.user.UserInterest;
import com.example.studentarchives.entity.weakness.WeaknessAnalysis;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.DegreeTypeEnum;
import com.example.studentarchives.enums.EventTypeEnum;
import com.example.studentarchives.enums.GenderEnum;
import com.example.studentarchives.enums.StudentStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AwardSummaryRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.DataCompletenessRepository;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.EvaluationIndicatorRepository;
import com.example.studentarchives.repository.GrowthTimelineAbilityRepository;
import com.example.studentarchives.repository.GrowthTimelineRepository;
import com.example.studentarchives.repository.GrowthTimelineTagRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.ScoreCalculationDetailRepository;
import com.example.studentarchives.repository.ScoreCalculationRepository;
import com.example.studentarchives.repository.SemesterGpaSummaryRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserInterestRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.WeaknessAnalysisRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人中心服务
 * <p>
 * 提供学生端个人中心（GET /profile/info、PUT /profile/contact、GET /profile/scores、
 * GET /profile/scores/{calculationId}/details、GET /profile/data-completeness、
 * PUT /profile/political-status、PUT /profile/interests）数据聚合与更新：
 * 学籍信息、联系信息、志愿时长、画像分数、兴趣标签、学期成绩、个人奖项汇总、
 * 短板分析、评分明细、数据完整度。
 * <p>
 * 数据口径（与《学生端接口文档》4.1 一致）：
 * - academicInfo → users / student_profiles / classes / majors / colleges
 * - contactInfo → user_contact_infos
 * - totalVolunteerHours → student_profiles.volunteer_hours（定时任务汇总回填）
 * - dimensionProfile → portrait_evaluation_scores（当前学期，按 ability_dimensions.sort 排序）
 * - interests → user_interests（按 sort 正序）
 * - semesterGrades → semester_gpa_summaries + semesters
 * - personalAwards → award_summaries
 * - weaknessAnalysis → weakness_analyses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 日期格式：2005-03-15 */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 维度名称排序用：默认排最后（未知维度） */
    private static final int DEFAULT_DIMENSION_SORT = Integer.MAX_VALUE;

    /** 兴趣熟练度标签（proficiency_level：1=入门 2=一般 3=熟练 4=精通） */
    private static final Map<Integer, String> PROFICIENCY_LABELS = Map.of(
            1, "入门",
            2, "一般",
            3, "熟练",
            4, "精通"
    );

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final UserInterestRepository userInterestRepository;
    private final AwardSummaryRepository awardSummaryRepository;
    private final WeaknessAnalysisRepository weaknessAnalysisRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final DataCompletenessRepository dataCompletenessRepository;
    private final GrowthTimelineRepository growthTimelineRepository;
    private final GrowthTimelineAbilityRepository growthTimelineAbilityRepository;
    private final GrowthTimelineTagRepository growthTimelineTagRepository;
    private final ScoreCalculationRepository scoreCalculationRepository;
    private final ScoreCalculationDetailRepository scoreCalculationDetailRepository;
    private final EvaluationIndicatorRepository evaluationIndicatorRepository;
    private final ArchiveRepository archiveRepository;
    private final DictionaryRepository dictionaryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取个人档案信息（GET /profile/info）
     *
     * @param userId 当前登录用户 ID
     * @return 个人档案信息响应
     */
    @Transactional(readOnly = true)
    public ProfileInfoResponse getProfileInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        // ==================== 学籍信息（用户/学生档案/班级/专业/学院） ====================
        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);

        Clazz clazz = (profile != null && profile.getClassId() != null)
                ? clazzRepository.findById(profile.getClassId()).orElse(null)
                : null;
        Major major = (clazz != null && clazz.getMajorId() != null)
                ? majorRepository.findById(clazz.getMajorId()).orElse(null)
                : null;
        College college = (major != null && major.getCollegeId() != null)
                ? collegeRepository.findById(major.getCollegeId()).orElse(null)
                : null;

        String politicalStatus = profile != null ? profile.getPoliticalStatus() : null;
        String studentStatus = profile != null ? profile.getStudentStatus() : null;
        String degreeType = profile != null ? profile.getDegreeType() : null;
        AcademicInfo academicInfo = AcademicInfo.builder()
                .userId(user.getId())
                .name(user.getName())
                .studentNo(user.getUserNo())
                .grade(clazz != null ? clazz.getGrade() : null)
                .major(major != null ? major.getName() : null)
                .degreeType(degreeType)
                .degreeTypeLabel(labelOfDegreeType(degreeType))
                .className(clazz != null ? clazz.getName() : null)
                .collegeName(college != null ? college.getName() : null)
                .gender(user.getGender())
                .genderLabel(GenderEnum.of(user.getGender()).getLabel())
                .politicalStatus(politicalStatus)
                .politicalStatusLabel(resolveDictLabel("political_status", politicalStatus))
                .studentStatus(studentStatus)
                .studentStatusLabel(labelOfStudentStatus(studentStatus))
                .birthDate(formatDate(user.getBirthDate()))
                .build();

        // ==================== 联系信息 + 志愿时长 ====================
        ContactInfo contactInfo = userContactInfoRepository.findByUserId(userId)
                .map(c -> ContactInfo.builder()
                        .email(c.getEmail())
                        .phone(c.getPhone())
                        .avatar(c.getAvatar())
                        .build())
                .orElse(null);

        BigDecimal volunteerHours = profile != null ? profile.getVolunteerHours() : null;

        // ==================== 画像分数（当前学期） ====================
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;
        Semester currentSemester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);

        List<PortraitEvaluationScore> currentScores = (currentSemester != null)
                ? portraitEvaluationScoreRepository.findByUserIdAndSemesterId(userId, currentSemester.getId())
                : Collections.emptyList();
        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        Map<String, String> dimensionNameMap = dimensions.stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        List<DimensionProfileItem> dimensionProfile = buildDimensionProfile(currentScores, dimensionNameMap, dimensions);

        // ==================== 兴趣标签 ====================
        List<InterestItem> interests = userInterestRepository.findByUserIdOrderBySortAsc(userId)
                .stream()
                .map(this::toInterestItem)
                .collect(Collectors.toList());

        // ==================== 学期成绩 ====================
        List<SemesterGpaSummary> gpaSummaries = semesterGpaSummaryRepository.findByUserId(userId);
        Map<Long, String> semesterNameMap = buildSemesterNameMap(gpaSummaries);
        List<SemesterGradeItem> semesterGrades = gpaSummaries.stream()
                .sorted(Comparator.comparing(s -> s.getSemesterId() != null ? s.getSemesterId() : 0L))
                .map(s -> toSemesterGradeItem(s, semesterNameMap))
                .collect(Collectors.toList());

        // ==================== 个人奖项汇总 ====================
        List<PersonalAwardItem> personalAwards = awardSummaryRepository.findByUserId(userId)
                .stream()
                .map(this::toPersonalAwardItem)
                .collect(Collectors.toList());

        // ==================== 短板分析 ====================
        List<WeaknessItem> weaknessAnalysis = weaknessAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toWeaknessItem)
                .collect(Collectors.toList());

        return ProfileInfoResponse.builder()
                .academicInfo(academicInfo)
                .contactInfo(contactInfo)
                .totalVolunteerHours(cleanDecimal(volunteerHours))
                .dimensionProfile(dimensionProfile)
                .interests(interests)
                .semesterGrades(semesterGrades)
                .personalAwards(personalAwards)
                .weaknessAnalysis(weaknessAnalysis)
                .selfEvaluation(profile != null ? profile.getSelfEvaluation() : null)
                .build();
    }

    // ==================== 更新联系信息（4.1.1） ====================

    /**
     * 更新个人联系信息（PUT /profile/contact）
     * <p>
     * 数据写入 user_contact_infos（唯一可写来源），users 表不再作为修改入口。
     * 全量更新语义：请求体三字段均必填（@NotNull 校验，缺失返回 PARAM_ERROR），
     * 空字符串表示清空对应字段。
     *
     * @param userId  当前登录用户 ID
     * @param request 联系信息更新请求
     * @return 更新后的联系信息
     */
    @Transactional
    public ContactUpdateResponse updateContact(Long userId, ContactUpdateRequest request) {
        UserContactInfo contact = userContactInfoRepository.findByUserId(userId)
                .orElseGet(UserContactInfo::new);
        contact.setUserId(userId);
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getAddress());
        contact.setUpdatedBy(userId);
        userContactInfoRepository.save(contact);

        return ContactUpdateResponse.builder()
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .avatar(contact.getAvatar())
                .build();
    }

    // ==================== 画像分数列表（4.1.2） ====================

    /**
     * 获取画像分数列表（GET /profile/scores）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID，不传返回当前学期
     * @return 画像分数列表响应
     */
    @Transactional(readOnly = true)
    public ScoreListResponse getScores(Long userId, Long semesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;
        Semester semester = resolveSemester(semesterId, schoolId);
        if (semester == null) {
            return ScoreListResponse.builder()
                    .semesterId(semesterId)
                    .list(Collections.emptyList())
                    .build();
        }

        List<PortraitEvaluationScore> scores = portraitEvaluationScoreRepository
                .findByUserIdAndSemesterId(userId, semester.getId());
        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        Map<String, String> dimensionNameMap = dimensions.stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        List<ScoreItem> items = scores.stream()
                .sorted(Comparator.comparingInt(s -> dimensionSortIndex(s.getDimensionCode(), dimensions)))
                .map(s -> ScoreItem.builder()
                        .dimensionCode(s.getDimensionCode())
                        .dimensionName(dimensionNameMap.get(s.getDimensionCode()))
                        .score(cleanDecimal(s.getScore()))
                        .targetScore(cleanDecimal(s.getTargetScore()))
                        .gap(cleanDecimal(s.getGap()))
                        .change(formatTrend(s.getChangeVal()))
                        .comparedSemesterId(s.getComparedSemesterId())
                        .comparedSemesterName(s.getComparedSemesterId() != null
                                ? semesterName(s.getComparedSemesterId()) : null)
                        .unit("分")
                        .build())
                .collect(Collectors.toList());

        PortraitEvaluationScore first = scores.stream().findFirst().orElse(null);
        return ScoreListResponse.builder()
                .semesterId(semester.getId())
                .semesterName(semester.getName())
                .calculatedAt(first != null ? toIso(first.getEvaluatedAt()) : null)
                .ruleVersion(first != null ? first.getRuleVersion() : null)
                .calculationId(first != null ? first.getCalculationId() : null)
                .list(items)
                .build();
    }

    // ==================== 分数计算明细（4.1.3） ====================

    /**
     * 获取分数计算说明（GET /profile/scores/{calculationId}/details）
     *
     * @param userId        当前登录用户 ID
     * @param calculationId 评分计算批次 ID
     * @return 分数计算明细响应
     */
    @Transactional(readOnly = true)
    public ScoreDetailResponse getScoreDetails(Long userId, Long calculationId) {
        ScoreCalculation calculation = scoreCalculationRepository.findByIdAndUserId(calculationId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "评分记录不存在"));

        List<ScoreCalculationDetail> details = scoreCalculationDetailRepository.findByCalculationId(calculationId);

        List<Long> indicatorIds = details.stream()
                .map(ScoreCalculationDetail::getIndicatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> indicatorNameMap = indicatorIds.isEmpty() ? Collections.emptyMap()
                : evaluationIndicatorRepository.findByIdIn(indicatorIds).stream()
                        .collect(Collectors.toMap(EvaluationIndicator::getId,
                                EvaluationIndicator::getIndicatorName, (a, b) -> a));

        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        Map<String, String> dimensionNameMap = dimensions.stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        // 来源档案标题（仅限当前用户，防止越权）
        Map<Long, String> archiveTitleMap = archiveRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Archive::getId, Archive::getTitle, (a, b) -> a));

        List<ScoreDetailItem> items = details.stream()
                .map(d -> {
                    List<Long> archiveIds = parseJsonArrayLong(d.getSourceArchiveIds());
                    List<String> archiveTitles = archiveIds.stream()
                            .map(archiveTitleMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return ScoreDetailItem.builder()
                            .indicatorId(d.getIndicatorId())
                            .indicatorName(indicatorNameMap.get(d.getIndicatorId()))
                            .dimensionCode(d.getDimensionCode())
                            .dimensionName(dimensionNameMap.get(d.getDimensionCode()))
                            .weight(cleanDecimal(d.getWeight()))
                            .rawScore(cleanDecimal(d.getRawScore()))
                            .weightedScore(cleanDecimal(d.getWeightedScore()))
                            .sourceArchiveIds(archiveIds)
                            .sourceArchiveTitles(archiveTitles)
                            .build();
                })
                .collect(Collectors.toList());

        return ScoreDetailResponse.builder()
                .calculationId(calculation.getId())
                .calculatedAt(toIso(calculation.getCalculatedAt()))
                .ruleVersion(calculation.getRuleVersion())
                .dataSource(calculation.getDataSource() != null
                        ? calculation.getDataSource() : "已通过档案 + 学期成绩")
                .details(items)
                .build();
    }

    // ==================== 数据完整度（4.1.4） ====================

    /**
     * 获取数据完整度（GET /profile/data-completeness）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID，不传返回当前学期
     * @return 数据完整度响应
     */
    @Transactional(readOnly = true)
    public DataCompletenessResponse getDataCompleteness(Long userId, Long semesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;
        Semester semester = resolveSemester(semesterId, schoolId);
        if (semester == null) {
            return DataCompletenessResponse.builder()
                    .semesterId(semesterId)
                    .overallRate(0)
                    .dimensions(Collections.emptyList())
                    .build();
        }

        List<DataCompleteness> list = dataCompletenessRepository
                .findByUserIdAndSemesterId(userId, semester.getId());
        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        Map<String, String> dimensionNameMap = dimensions.stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        List<CompletenessItem> dimensionItems = list.stream()
                .sorted(Comparator.comparingInt(d -> dimensionSortIndex(d.getDimensionCode(), dimensions)))
                .map(d -> CompletenessItem.builder()
                        .dimensionCode(d.getDimensionCode())
                        .dimensionName(dimensionNameMap.get(d.getDimensionCode()))
                        .rate(d.getCompletenessRate())
                        .missingItems(parseJsonArray(d.getMissingItems()))
                        .build())
                .collect(Collectors.toList());

        int overallRate = (int) Math.round(list.stream()
                .map(DataCompleteness::getCompletenessRate)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        LocalDateTime updatedAt = list.stream()
                .map(DataCompleteness::getCalculatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return DataCompletenessResponse.builder()
                .semesterId(semester.getId())
                .overallRate(overallRate)
                .dimensions(dimensionItems)
                .updatedAt(toIso(updatedAt))
                .build();
    }

    /**
     * 根据字典类型与编码解析展示名称（未找到返回 null）。
     *
     * @param dictType 字典类型，如 "political_status"
     * @param code     字典编码，如 "party_member"
     * @return 字典展示名称；code 为空或未找到时返回 null
     */
    private String resolveDictLabel(String dictType, String code) {
        if (code == null) {
            return null;
        }
        return dictionaryRepository.findActiveByDictType(dictType).stream()
                .filter(d -> code.equals(d.getDictCode()))
                .map(Dictionary::getDictName)
                .findFirst()
                .orElse(null);
    }

    private String labelOfDegreeType(String degreeType) {
        DegreeTypeEnum e = DegreeTypeEnum.of(degreeType);
        return e != null ? e.getLabel() : null;
    }

    private String labelOfStudentStatus(String studentStatus) {
        StudentStatusEnum e = StudentStatusEnum.of(studentStatus);
        return e != null ? e.getLabel() : null;
    }

    // ==================== 更新个人基本信息（4.1.5） ====================

    /**
     * 更新政治面貌（PUT /profile/political-status）
     * <p>
     * 仅可更新 student_profiles.political_status（政治面貌字典编码）。
     * 全量更新语义：politicalStatus 必填（@NotBlank 校验，缺失返回 PARAM_ERROR），
     * 且必须是政治面貌字典中的有效编码。
     *
     * @param userId  当前登录用户 ID
     * @param request 基本信息更新请求
     * @return 更新后的政治面貌及展示名称
     */
    @Transactional
    public BasicInfoUpdateResponse updateBasicInfo(Long userId, BasicInfoUpdateRequest request) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学生档案不存在"));

        String politicalStatus = request.getPoliticalStatus();
        if (politicalStatus == null || politicalStatus.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "政治面貌不能为空");
        }
        Dictionary dict = dictionaryRepository.findActiveByDictType("political_status").stream()
                .filter(d -> Objects.equals(politicalStatus, d.getDictCode()))
                .findFirst()
                .orElse(null);
        if (dict == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "政治面貌编码不存在");
        }
        profile.setPoliticalStatus(politicalStatus);
        studentProfileRepository.save(profile);

        return BasicInfoUpdateResponse.builder()
                .politicalStatus(profile.getPoliticalStatus())
                .politicalStatusLabel(dict.getDictName())
                .build();
    }

    /**
     * 更新学生状态（PUT /profile/student-status）
     *
     * @param userId  当前登录用户 ID
     * @param request 学生状态更新请求
     * @return 更新后的学生状态及展示名称
     */
    @Transactional
    public StudentStatusUpdateResponse updateStudentStatus(Long userId, StudentStatusUpdateRequest request) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学生档案不存在"));

        String studentStatus = request.getStudentStatus();
        StudentStatusEnum statusEnum = StudentStatusEnum.of(studentStatus);
        if (statusEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学生状态编码不存在");
        }
        profile.setStudentStatus(studentStatus);
        studentProfileRepository.save(profile);

        return StudentStatusUpdateResponse.builder()
                .studentStatus(profile.getStudentStatus())
                .studentStatusLabel(statusEnum.getLabel())
                .build();
    }

    // ==================== 更新兴趣标签（4.11） ====================

    /**
     * 更新个人兴趣标签（PUT /profile/interests）
     * <p>
     * id 存在则更新（校验归属），否则新增；条件唯一索引 (user_id, tag_name, is_detail)。
     *
     * @param userId  当前登录用户 ID
     * @param request 兴趣标签更新请求
     * @return 更新/新增条数
     */
    @Transactional
    public InterestUpdateResponse updateInterests(Long userId, InterestUpdateRequest request) {
        int updatedCount = 0;
        int addedCount = 0;
        List<UserInterest> existingInterests = userInterestRepository.findByUserIdOrderBySortAsc(userId);
        int maxSort = existingInterests.stream()
                .map(UserInterest::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        // 已占用标签 key（tagName + is_detail），用于在入库前查重，避免触发 uk_ui_user_tag 唯一索引冲突
        Map<String, Long> occupiedKeys = existingInterests.stream()
                .collect(Collectors.toMap(ui -> interestKey(ui.getTagName(), ui.getIsDetail()),
                        UserInterest::getId, (a, b) -> a));
        // 本次请求内已声明的标签 key，防止请求内部两条数据相互冲突
        Set<String> claimedInRequest = new HashSet<>();

        for (InterestUpdateRequest.InterestItem item : request.getInterests()) {
            String key = interestKey(item.getTagName(), item.getIsDetail() != null ? item.getIsDetail() : 1);
            if (item.getId() != null) {
                UserInterest existing = userInterestRepository.findById(item.getId())
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "兴趣标签不存在"));
                if (!userId.equals(existing.getUserId())) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无访问权限");
                }
                Long occupiedId = occupiedKeys.get(key);
                if (occupiedId != null && !occupiedId.equals(item.getId())) {
                    throw new BusinessException(ResultCode.DATA_DUPLICATE,
                            "兴趣标签「" + item.getTagName() + "」已存在");
                }
                if (!claimedInRequest.add(key)) {
                    throw new BusinessException(ResultCode.DATA_DUPLICATE,
                            "兴趣标签「" + item.getTagName() + "」在本次请求中重复");
                }
                existing.setTagName(item.getTagName());
                existing.setProficiencyLevel(item.getProficiencyLevel());
                if (item.getDetailContent() != null) {
                    existing.setDetailContent(item.getDetailContent());
                }
                if (item.getIsDetail() != null) {
                    existing.setIsDetail(item.getIsDetail());
                }
                userInterestRepository.save(existing);
                updatedCount++;
            } else {
                if (occupiedKeys.containsKey(key) || !claimedInRequest.add(key)) {
                    throw new BusinessException(ResultCode.DATA_DUPLICATE,
                            "兴趣标签「" + item.getTagName() + "」已存在");
                }
                UserInterest interest = new UserInterest();
                interest.setUserId(userId);
                interest.setTagName(item.getTagName());
                interest.setProficiencyLevel(item.getProficiencyLevel());
                interest.setDetailContent(item.getDetailContent());
                interest.setIsDetail(item.getIsDetail() != null ? item.getIsDetail() : 1);
                interest.setWeight(0);
                interest.setSort(++maxSort);
                userInterestRepository.save(interest);
                addedCount++;
            }
        }
        return InterestUpdateResponse.builder()
                .updatedCount(updatedCount)
                .addedCount(addedCount)
                .build();
    }

    /**
     * 删除个人兴趣标签（4.11.1）
     * <p>
     * 校验标签存在且归属当前用户后软删除（deleted_at 置为当前时间）。
     *
     * @param userId     当前登录用户 ID
     * @param interestId 兴趣标签 ID
     */
    @Transactional
    public void deleteInterest(Long userId, Long interestId) {
        UserInterest interest = userInterestRepository.findById(interestId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "兴趣标签不存在"));
        if (!userId.equals(interest.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无访问权限");
        }
        if (interest.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "兴趣标签不存在");
        }
        int affected = userInterestRepository.softDeleteById(interestId, LocalDateTime.now());
        if (affected == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "兴趣标签不存在");
        }
    }

    // ==================== 自我评价（4.1.6） ====================

    /**
     * 更新自我评价（PUT /profile/self-evaluation）
     * <p>
     * 全量更新语义：允许传空字符串清空内容；若学生档案不存在则自动创建空档案并写入。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求
     * @return 更新后的自我评价
     */
    @Transactional
    public SelfEvaluationResponse updateSelfEvaluation(Long userId, SelfEvaluationUpdateRequest request) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            // 创建时间等审计字段由 @EntityListeners 自动填充
        }
        profile.setSelfEvaluation(request.getSelfEvaluation());
        studentProfileRepository.save(profile);
        return SelfEvaluationResponse.builder()
                .selfEvaluation(profile.getSelfEvaluation())
                .build();
    }

    /**
     * 构造兴趣标签唯一性 key（与 uk_ui_user_tag 条件唯一索引口径一致：tag_name + is_detail）。
     * is_detail 为 null 时按 1 处理（新增默认值）。
     */
    private String interestKey(String tagName, Integer isDetail) {
        return tagName + "|" + (isDetail != null ? isDetail : 1);
    }

    // ==================== 成长时间轴（4.2） ====================

    /**
     * 获取成长时间轴（GET /profile/growth-timeline）
     * <p>
     * 数据来源：growth_timelines、growth_timeline_abilities、growth_timeline_tags。
     * eventType / status 过滤在内存中完成（单学生数据量小）。
     * viewType：list（默认，扁平时间线）/ tree（学年→学期分组树）/ ring（按年度聚合环）。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID 筛选
     * @param eventType  事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升
     * @param status     审核状态：0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销
     * @param viewType   视图类型：list / tree / ring
     * @return 成长时间轴响应（按 viewType 返回不同结构）
     */
    @Transactional(readOnly = true)
    public GrowthTimelineResponse getGrowthTimeline(Long userId, Long semesterId, Integer eventType, Integer status,
                                                    String viewType) {
        List<GrowthTimeline> timelines = (semesterId != null)
                ? growthTimelineRepository.findByUserIdAndSemesterIdOrderByEventAtDesc(userId, semesterId)
                : growthTimelineRepository.findByUserIdOrderByEventAtDesc(userId);

        if (eventType != null) {
            timelines = timelines.stream()
                    .filter(t -> eventType.equals(t.getEventType()))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            timelines = timelines.stream()
                    .filter(t -> status.equals(t.getStatus()))
                    .collect(Collectors.toList());
        }

        List<Long> timelineIds = timelines.stream()
                .map(GrowthTimeline::getId)
                .collect(Collectors.toList());
        Map<Long, List<GrowthTimelineAbility>> abilityMap = timelineIds.isEmpty() ? Collections.emptyMap()
                : growthTimelineAbilityRepository.findByTimelineIdIn(timelineIds).stream()
                        .collect(Collectors.groupingBy(GrowthTimelineAbility::getTimelineId));
        Map<Long, List<GrowthTimelineTag>> tagMap = timelineIds.isEmpty() ? Collections.emptyMap()
                : growthTimelineTagRepository.findByTimelineIdIn(timelineIds).stream()
                        .collect(Collectors.groupingBy(GrowthTimelineTag::getTimelineId));

        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        List<TimelineItem> items = timelines.stream()
                .map(t -> {
                    EventTypeEnum eventTypeEnum = EventTypeEnum.of(t.getEventType());
                    return TimelineItem.builder()
                            .id(t.getId())
                            .semesterId(t.getSemesterId())
                            .semesterName(t.getSemesterId() != null ? semesterName(t.getSemesterId()) : null)
                            .eventAt(formatDate(t.getEventAt()))
                            .eventName(t.getEventName())
                            .content(t.getContent())
                            .eventType(t.getEventType())
                            .eventTypeLabel(eventTypeEnum != null ? eventTypeEnum.getLabel() : null)
                            .status(t.getStatus())
                            .statusLabel(ApplyStatusEnum.of(t.getStatus()).getLabel())
                            .coverImage(t.getCoverImage())
                            .sourceId(t.getSourceId())
                            .sourceType(t.getSourceType())
                            .abilityData(abilityMap.getOrDefault(t.getId(), Collections.emptyList()).stream()
                                    .map(a -> AbilityItem.builder()
                                            .dimensionCode(a.getDimensionCode())
                                            .dimensionName(dimensionNameMap.get(a.getDimensionCode()))
                                            .score(cleanDecimal(a.getScore()))
                                            .build())
                                    .collect(Collectors.toList()))
                            .tags(tagMap.getOrDefault(t.getId(), Collections.emptyList()).stream()
                                    .map(GrowthTimelineTag::getTagName)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());

        long skillCount = abilityMap.values().stream()
                .flatMap(List::stream)
                .map(GrowthTimelineAbility::getDimensionCode)
                .distinct()
                .count();

        Summary summary = buildSummary(userId, items.size(), (int) skillCount);

        if ("tree".equalsIgnoreCase(viewType)) {
            return GrowthTimelineResponse.builder()
                    .summary(summary)
                    .tree(buildTree(items))
                    .build();
        }
        if ("ring".equalsIgnoreCase(viewType)) {
            return GrowthTimelineResponse.builder()
                    .summary(summary)
                    .ring(buildRing(items))
                    .build();
        }
        return GrowthTimelineResponse.builder()
                .summary(summary)
                .timeline(items)
                .build();
    }

    /**
     * 构建成长摘要（summary）
     * <p>
     * experiences / skills 取自时间轴数据（growth_timelines + growth_timeline_abilities）；
     * averageGrowth / potential 取自画像评估得分（portrait_evaluation_scores，最近一次评估），
     * 不再返回虚构占位值——无画像数据时返回 "0%"。
     *
     * @param userId     当前登录用户 ID
     * @param experiences 时间轴经历总数
     * @param skills      时间轴涉及的能力维度去重数
     * @return 成长摘要
     */
    private Summary buildSummary(Long userId, int experiences, int skills) {
        List<PortraitEvaluationScore> latestScores = latestPortraitScores(userId);
        return Summary.builder()
                .experiences(experiences)
                .skills(skills)
                .averageGrowth(avgGrowthRate(latestScores))
                .potential(potentialRate(latestScores))
                .build();
    }

    /**
     * 取最近一次画像评估的各维度得分（按学期倒序，取最大学期）。
     * 画像数据为空时返回空列表。
     */
    private List<PortraitEvaluationScore> latestPortraitScores(Long userId) {
        List<PortraitEvaluationScore> scores = portraitEvaluationScoreRepository
                .findByUserIdOrderBySemesterIdDesc(userId);
        if (scores.isEmpty()) {
            return Collections.emptyList();
        }
        Long latestSemester = scores.get(0).getSemesterId();
        return scores.stream()
                .filter(s -> Objects.equals(latestSemester, s.getSemesterId()))
                .collect(Collectors.toList());
    }

    /**
     * 平均成长率（averageGrowth）
     * <p>
     * 各维度 较上阶段变化(change) ÷ 上一阶段分数(score − change) 的算术平均，
     * 向上取整为百分比字符串，如 "7%"。change 为空或无画像数据时返回 "0%"。
     */
    private String avgGrowthRate(List<PortraitEvaluationScore> scores) {
        List<BigDecimal> rates = scores.stream()
                .filter(s -> s.getScore() != null && s.getChangeVal() != null)
                .map(s -> {
                    BigDecimal prev = s.getScore().subtract(s.getChangeVal());
                    if (prev.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }
                    return s.getChangeVal().multiply(BigDecimal.valueOf(100))
                            .divide(prev, 2, RoundingMode.HALF_UP);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (rates.isEmpty()) {
            return "0%";
        }
        double avg = rates.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        return Math.round(avg) + "%";
    }

    /**
     * 潜力/提升空间（potential）
     * <p>
     * 各维度 距目标分数 gap(target − score) ÷ 目标分 target_score 的算术平均，
     * 向上取整为百分比字符串，如 "12%"，表示离全部达标还差的提升空间；
     * 已达标（gap ≤ 0）的维度按 0 计。无目标分或无画像数据时返回 "0%"。
     */
    private String potentialRate(List<PortraitEvaluationScore> scores) {
        List<BigDecimal> potentials = scores.stream()
                .filter(s -> s.getScore() != null && s.getTargetScore() != null
                        && s.getTargetScore().compareTo(BigDecimal.ZERO) > 0)
                .map(s -> {
                    BigDecimal gap = s.getTargetScore().subtract(s.getScore());
                    if (gap.compareTo(BigDecimal.ZERO) < 0) {
                        return BigDecimal.ZERO;
                    }
                    return gap.multiply(BigDecimal.valueOf(100))
                            .divide(s.getTargetScore(), 2, RoundingMode.HALF_UP);
                })
                .collect(Collectors.toList());
        if (potentials.isEmpty()) {
            return "0%";
        }
        double avg = potentials.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        return Math.round(avg) + "%";
    }

    // ==================== 画像分数 ====================

    /**
     * 构建画像分数列表，按能力维度 sort 排序
     */
    private List<DimensionProfileItem> buildDimensionProfile(List<PortraitEvaluationScore> currentScores,
                                                             Map<String, String> dimensionNameMap,
                                                             List<AbilityDimension> dimensions) {
        Map<String, Integer> dimensionSort = new HashMap<>();
        for (int i = 0; i < dimensions.size(); i++) {
            dimensionSort.put(dimensions.get(i).getDimensionCode(), i);
        }
        return currentScores.stream()
                .sorted(Comparator.comparingInt(s -> dimensionSort.getOrDefault(s.getDimensionCode(), DEFAULT_DIMENSION_SORT)))
                .map(s -> DimensionProfileItem.builder()
                        .dimensionCode(s.getDimensionCode())
                        .dimensionName(dimensionNameMap.get(s.getDimensionCode()))
                        .score(cleanDecimal(s.getScore()))
                        .targetScore(cleanDecimal(s.getTargetScore()))
                        .gap(cleanDecimal(s.getGap()))
                        .calculationId(s.getCalculationId())
                        .ruleVersion(s.getRuleVersion())
                        .calculatedAt(toIso(s.getEvaluatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 兴趣标签 ====================

    /**
     * 兴趣实体 → 响应项
     */
    private InterestItem toInterestItem(UserInterest interest) {
        return InterestItem.builder()
                .id(interest.getId())
                .tagName(interest.getTagName())
                .detailContent(interest.getDetailContent())
                .proficiencyLevel(interest.getProficiencyLevel())
                .proficiencyLabel(PROFICIENCY_LABELS.getOrDefault(interest.getProficiencyLevel(), null))
                .weight(interest.getWeight())
                .isDetail(interest.getIsDetail())
                .build();
    }

    // ==================== 学期成绩 ====================

    /**
     * 成绩汇总实体 → 学期成绩项
     */
    private SemesterGradeItem toSemesterGradeItem(SemesterGpaSummary summary, Map<Long, String> semesterNameMap) {
        return SemesterGradeItem.builder()
                .semesterId(summary.getSemesterId())
                .semester(semesterNameMap.get(summary.getSemesterId()))
                .semesterName(generateSemesterLabel(semesterNameMap.get(summary.getSemesterId())))
                .courseCount(summary.getCourseCount())
                .totalCredit(cleanDecimal(summary.getTotalCredit()))
                .gpa(cleanDecimal(summary.getWeightedGpa()))
                .averageScore(cleanDecimal(summary.getAverageScore()))
                .rankInClass(summary.getRankInClass())
                .rankInMajor(summary.getRankInMajor())
                .build();
    }

    /**
     * 构建学期 ID → 学期名称映射
     */
    private Map<Long, String> buildSemesterNameMap(List<SemesterGpaSummary> summaries) {
        Map<Long, String> map = new HashMap<>();
        summaries.stream()
                .map(SemesterGpaSummary::getSemesterId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(sid -> semesterRepository.findById(sid)
                        .ifPresent(s -> map.put(sid, s.getName())));
        return map;
    }

    // ==================== 个人奖项汇总 ====================

    /**
     * 奖项汇总实体 → 响应项
     */
    private PersonalAwardItem toPersonalAwardItem(AwardSummary award) {
        return PersonalAwardItem.builder()
                .category(award.getCategory())
                .totalCount(award.getTotalCount())
                .maxLevel(award.getMaxLevel())
                .latestTime(formatDate(award.getLatestAt()))
                .build();
    }

    // ==================== 短板分析 ====================

    /**
     * 短板分析实体 → 响应项
     */
    private WeaknessItem toWeaknessItem(WeaknessAnalysis weakness) {
        return WeaknessItem.builder()
                .id(weakness.getId())
                .weaknessType(weakness.getWeaknessType())
                .weaknessDesc(weakness.getWeaknessDesc())
                .severityLevel(weakness.getSeverityLevel())
                .isRead(weakness.getIsRead())
                .createdAt(toIso(weakness.getCreatedAt()))
                .relatedType(weakness.getRelatedType())
                .relatedId(weakness.getRelatedId())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析学期：传 semesterId 则按 ID 查；否则取学校当前学期
     */
    private Semester resolveSemester(Long semesterId, Long schoolId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId).orElse(null);
        }
        return semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);
    }

    /**
     * 学期 ID → 学期名称
     */
    private String semesterName(Long semesterId) {
        return semesterRepository.findById(semesterId).map(Semester::getName).orElse(null);
    }

    /**
     * 维度编码 → 排序下标（未知维度排最后）
     */
    private int dimensionSortIndex(String dimensionCode, List<AbilityDimension> dimensions) {
        for (int i = 0; i < dimensions.size(); i++) {
            if (dimensions.get(i).getDimensionCode().equals(dimensionCode)) {
                return i;
            }
        }
        return DEFAULT_DIMENSION_SORT;
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
     * 解析 JSON 数组字符串为 List&lt;String&gt;
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

    /**
     * 解析 JSON 数组字符串为 List&lt;Long&gt;（如 source_archive_ids）
     */
    private List<Long> parseJsonArrayLong(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析 JSON 长整型数组失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据学期 name 生成展示 label
     * 如 "2022-2023-1" → "2022-2023第一学期"
     */
    private String generateSemesterLabel(String name) {
        if (name == null || name.isEmpty()) return name;
        int lastDash = name.lastIndexOf('-');
        if (lastDash <= 0 || lastDash >= name.length() - 1) return name;

        String prefix = name.substring(0, lastDash);
        String suffix = name.substring(lastDash + 1);
        String semesterChinese;
        try {
            int semesterNum = Integer.parseInt(suffix);
            semesterChinese = switch (semesterNum) {
                case 1 -> "第一学期";
                case 2 -> "第二学期";
                case 3 -> "第三学期";
                default -> "第" + semesterNum + "学期";
            };
        } catch (NumberFormatException e) {
            semesterChinese = suffix;
        }
        return prefix + semesterChinese;
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
     * LocalDate → "2005-03-15" 字符串
     */
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : null;
    }

    // ==================== 时间轴视图构建（tree / ring） ====================

    /** 无学期 / 无日期时的兜底分组名 */
    private static final String OTHER_GROUP = "其他";

    /**
     * 构建学年树形结构（viewType=tree）：学年 → 学期 → 事件
     * <p>
     * 学年由学期名称推导（如 "2023-2024-1" → "2023-2024"）；保持事件的
     * eventAt 倒序顺序，分组用 LinkedHashMap 保序。
     */
    private List<YearGroup> buildTree(List<TimelineItem> items) {
        Map<String, Map<String, List<TimelineItem>>> byYear = new LinkedHashMap<>();
        Map<String, SemesterGroup> semesterMeta = new HashMap<>();
        for (TimelineItem item : items) {
            String semesterName = item.getSemesterName();
            String academicYear = semesterName != null ? academicYearOf(semesterName) : OTHER_GROUP;
            String key = item.getSemesterId() != null ? "s" + item.getSemesterId() : "none";
            semesterMeta.computeIfAbsent(key, k -> SemesterGroup.builder()
                    .semesterId(item.getSemesterId())
                    .semesterName(semesterName)
                    .build());
            byYear.computeIfAbsent(academicYear, k -> new LinkedHashMap<>())
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(item);
        }
        List<YearGroup> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<TimelineItem>>> yearEntry : byYear.entrySet()) {
            List<SemesterGroup> semesters = new ArrayList<>();
            for (Map.Entry<String, List<TimelineItem>> semEntry : yearEntry.getValue().entrySet()) {
                SemesterGroup group = semesterMeta.get(semEntry.getKey());
                group.setEvents(semEntry.getValue());
                semesters.add(group);
            }
            result.add(YearGroup.builder()
                    .academicYear(yearEntry.getKey())
                    .semesters(semesters)
                    .build());
        }
        return result;
    }

    /**
     * 构建年度聚合环形数据（viewType=ring）：按 eventAt 所在自然年聚合
     * <p>
     * 每个年度节点返回事件总数与事件类型分布（eventType → 数量），
     * 供前端环形时间轴组件按年度扇区与类型着色。
     */
    private List<RingYear> buildRing(List<TimelineItem> items) {
        Map<String, List<TimelineItem>> byYear = new LinkedHashMap<>();
        for (TimelineItem item : items) {
            byYear.computeIfAbsent(yearOf(item.getEventAt()), k -> new ArrayList<>()).add(item);
        }
        List<RingYear> result = new ArrayList<>();
        for (Map.Entry<String, List<TimelineItem>> e : byYear.entrySet()) {
            Map<String, Integer> typeCounts = new LinkedHashMap<>();
            for (TimelineItem item : e.getValue()) {
                if (item.getEventType() != null) {
                    typeCounts.merge(String.valueOf(item.getEventType()), 1, Integer::sum);
                }
            }
            result.add(RingYear.builder()
                    .year(e.getKey())
                    .eventCount(e.getValue().size())
                    .typeDistribution(typeCounts)
                    .events(e.getValue())
                    .build());
        }
        return result;
    }

    /**
     * 从学期名称提取学年，如 "2023-2024-1" → "2023-2024"
     */
    private String academicYearOf(String semesterName) {
        int lastDash = semesterName.lastIndexOf('-');
        if (lastDash > 0) {
            String candidate = semesterName.substring(0, lastDash);
            if (candidate.matches("\\d{4}-\\d{4}")) {
                return candidate;
            }
        }
        return semesterName;
    }

    /**
     * 从事件日期提取年度，如 "2025-07-05" → "2025"
     */
    private String yearOf(String eventAt) {
        if (eventAt == null || eventAt.isBlank()) return OTHER_GROUP;
        int dash = eventAt.indexOf('-');
        return dash > 0 ? eventAt.substring(0, dash) : eventAt;
    }
}
