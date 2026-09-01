package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.profile.request.GrowthTimelineAbilityItem;
import com.example.studentarchives.dto.Fmy.profile.request.GrowthTimelineCreateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.GrowthTimelineUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineDetailResponse.AbilityItem;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.growth.GrowthTimeline;
import com.example.studentarchives.entity.growth.GrowthTimelineAbility;
import com.example.studentarchives.entity.growth.GrowthTimelineTag;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.EventTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.GrowthTimelineAbilityRepository;
import com.example.studentarchives.repository.GrowthTimelineRepository;
import com.example.studentarchives.repository.GrowthTimelineTagRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 个人中心成长时间轴服务（新增 / 详情 / 修改 / 删除）
 * <p>
 * 在既有只读接口 {@code GET /profile/growth-timeline}（ProfileService）基础上，
 * 提供成长时间轴事件的新增（POST）、详情（GET /{id}）、修改（PUT /{id}）、删除（DELETE /{id}）。
 * <p>
 * 实现约束：仅新增本文件与对应 Controller / DTO，不改动既有代码。子表（abilities / tags）的
 * 唯一性与软删除通过 {@link EntityManager} 动态查询完成，未修改既有 Repository。
 * <p>
 * 数据口径（与《学生端接口文档》4.2 一致）：
 * - event_type：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升
 * - 不需要审核：新增即直接生效（status 固定为 2=已通过）；任意状态均可直接修改/删除
 * - 来源记录必填：每条时间轴事件必须关联一条已有来源记录（sourceType + sourceId），
 *   且该来源记录须真实存在（未软删除）并归属当前用户，否则报错
 * - 条件唯一索引：uk_gt_event_key(user_id, event_key)、uk_gt_source(source_type, source_id)
 * - 数据权限：学生仅可操作本人（user_id）的时间轴节点，越权返回 20005
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileGrowthTimelineService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 日期格式：2005-03-15 */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 来源记录白名单：source_type → 归属校验 SQL（原生查询，表名来自固定白名单，无注入风险）。
     * <p>
     * 覆盖《学生档案系统表》H. 成长时间轴事件类型的来源示例，以及《学生端接口文档》4.2 中
     * archive / award_application 别名。校验规则：来源记录须真实存在（未软删除）且归属当前学生，
     * 否则报错。子表经各自外键（archive_id / application_id / career_plan_id / weakness_id）
     * 关联父表校验归属。
     */
    private static final Map<String, String> SOURCE_VALIDATION_SQL;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        // 直接归属 user_id 的来源表
        m.put("archives", "SELECT 1 FROM archives WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        m.put("award_applications", "SELECT 1 FROM award_applications WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        m.put("semester_gpa_summaries", "SELECT 1 FROM semester_gpa_summaries WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        m.put("gpa_records", "SELECT 1 FROM gpa_records WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        m.put("career_plans", "SELECT 1 FROM career_plans WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        m.put("weakness_analyses", "SELECT 1 FROM weakness_analyses WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        m.put("portrait_evaluation_scores", "SELECT 1 FROM portrait_evaluation_scores WHERE id = :id AND deleted_at IS NULL AND user_id = :userId");
        // archive 子表：经 archive_id 关联 archives.user_id 校验归属
        m.put("archive_competitions", "SELECT 1 FROM archive_competitions s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL AND a.deleted_at IS NULL AND a.user_id = :userId");
        m.put("archive_social_practices", "SELECT 1 FROM archive_social_practices s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL AND a.deleted_at IS NULL AND a.user_id = :userId");
        m.put("archive_internships", "SELECT 1 FROM archive_internships s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL AND a.deleted_at IS NULL AND a.user_id = :userId");
        m.put("archive_organizations", "SELECT 1 FROM archive_organizations s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL AND a.deleted_at IS NULL AND a.user_id = :userId");
        // 奖项子表：经 application_id 关联 award_applications.user_id 校验归属
        m.put("award_competition_stars", "SELECT 1 FROM award_competition_stars s JOIN award_applications a ON a.id = s.application_id WHERE s.id = :id AND s.deleted_at IS NULL AND a.deleted_at IS NULL AND a.user_id = :userId");
        // 职业规划反馈：经 career_plan_id 关联 career_plans.user_id 校验归属
        m.put("career_plan_feedbacks", "SELECT 1 FROM career_plan_feedbacks s JOIN career_plans c ON c.id = s.career_plan_id WHERE s.id = :id AND s.deleted_at IS NULL AND c.deleted_at IS NULL AND c.user_id = :userId");
        // 短板改进建议：经 weakness_id 关联 weakness_analyses.user_id 校验归属
        m.put("improvement_suggestions", "SELECT 1 FROM improvement_suggestions s JOIN weakness_analyses w ON w.id = s.weakness_id WHERE s.id = :id AND s.deleted_at IS NULL AND w.deleted_at IS NULL AND w.user_id = :userId");
        // 《学生端接口文档》4.2 中的来源类型别名
        m.put("archive", m.get("archives"));
        m.put("award_application", m.get("award_applications"));
        SOURCE_VALIDATION_SQL = Collections.unmodifiableMap(m);
    }

    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final GrowthTimelineRepository growthTimelineRepository;
    private final GrowthTimelineAbilityRepository growthTimelineAbilityRepository;
    private final GrowthTimelineTagRepository growthTimelineTagRepository;
    private final EntityManager entityManager;

    /**
     * 新增成长时间轴事件（POST /profile/growth-timeline）
     *
     * @param userId  当前登录用户 ID
     * @param request 新增请求
     * @return 新建后的时间轴事件详情
     */
    @Transactional
    public GrowthTimelineDetailResponse create(Long userId, GrowthTimelineCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        validateEventType(request.getEventType());
        if (request.getSemesterId() != null) {
            validateSemester(request.getSemesterId());
        }
        // 来源记录必填：必须关联一条已有来源记录，否则报错
        validateSourceRequired(request.getSourceType(), request.getSourceId());
        String sourceType = request.getSourceType().trim();
        validateSourceRecord(userId, sourceType, request.getSourceId());
        checkSourceUnique(userId, sourceType, request.getSourceId(), null);
        if (request.getEventKey() != null && !request.getEventKey().isBlank()) {
            checkEventKeyUnique(userId, request.getEventKey().trim(), null);
        }

        GrowthTimeline timeline = new GrowthTimeline();
        timeline.setSchoolId(user.getSchoolId());
        timeline.setUserId(userId);
        timeline.setSemesterId(request.getSemesterId());
        timeline.setEventType(request.getEventType());
        timeline.setEventName(request.getEventName().trim());
        timeline.setContent(request.getContent());
        timeline.setCoverImage(request.getCoverImage());
        timeline.setEventAt(parseEventAt(request.getEventAt()));
        timeline.setSourceId(request.getSourceId());
        timeline.setSourceType(sourceType);
        timeline.setEventKey(request.getEventKey() != null ? request.getEventKey().trim() : null);
        // 不需要审核：新增即直接生效（status 固定为 2=已通过）
        timeline.setStatus(2);
        growthTimelineRepository.save(timeline);

        saveAbilities(timeline.getId(), request.getAbilityData());
        saveTags(timeline.getId(), request.getTags());

        log.info("新增成长时间轴事件: id={}, userId={}, eventType={}, eventName={}",
                timeline.getId(), userId, timeline.getEventType(), timeline.getEventName());
        return toDetail(timeline);
    }

    /**
     * 获取成长时间轴事件详情（GET /profile/growth-timeline/{id}）
     *
     * @param userId 当前登录用户 ID
     * @param id     时间轴节点 ID
     * @return 时间轴事件详情
     */
    @Transactional(readOnly = true)
    public GrowthTimelineDetailResponse getDetail(Long userId, Long id) {
        return toDetail(getOwnedTimeline(userId, id));
    }

    /**
     * 修改成长时间轴事件（PUT /profile/growth-timeline/{id}）
     * <p>
     * 全部字段可选，仅更新传入的非空字段；abilityData / tags 传入时整体替换子表数据。
     * 不需要审核：任意状态（含已通过 2）均可直接修改；来源记录仍须关联已有来源记录。
     *
     * @param userId  当前登录用户 ID
     * @param id      时间轴节点 ID
     * @param request 修改请求
     * @return 更新后的时间轴事件详情
     */
    @Transactional
    public GrowthTimelineDetailResponse update(Long userId, Long id, GrowthTimelineUpdateRequest request) {
        GrowthTimeline timeline = getOwnedTimeline(userId, id);

        if (request.getSemesterId() != null) {
            validateSemester(request.getSemesterId());
            timeline.setSemesterId(request.getSemesterId());
        }
        if (request.getEventType() != null) {
            validateEventType(request.getEventType());
            timeline.setEventType(request.getEventType());
        }
        if (request.getEventName() != null) {
            if (request.getEventName().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "事件名称不能为空");
            }
            timeline.setEventName(request.getEventName().trim());
        }
        if (request.getContent() != null) {
            timeline.setContent(request.getContent());
        }
        if (request.getCoverImage() != null) {
            timeline.setCoverImage(request.getCoverImage());
        }
        if (request.getEventAt() != null) {
            timeline.setEventAt(parseEventAt(request.getEventAt()));
        }
        if (request.getStatus() != null) {
            timeline.setStatus(request.getStatus());
        }
        if (request.getSourceType() != null) {
            if (request.getSourceType().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "来源记录不能为空，必须关联已有的来源记录");
            }
            if (request.getSourceId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "填写来源类型时必须同时提供来源ID");
            }
            String sourceType = request.getSourceType().trim();
            validateSourceRecord(userId, sourceType, request.getSourceId());
            checkSourceUnique(userId, sourceType, request.getSourceId(), id);
            timeline.setSourceType(sourceType);
            timeline.setSourceId(request.getSourceId());
        } else if (request.getSourceId() != null) {
            if (timeline.getSourceType() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "填写来源ID时必须同时提供来源类型");
            }
            validateSourceRecord(userId, timeline.getSourceType(), request.getSourceId());
            checkSourceUnique(userId, timeline.getSourceType(), request.getSourceId(), id);
            timeline.setSourceId(request.getSourceId());
        }
        if (request.getEventKey() != null) {
            if (request.getEventKey().isBlank()) {
                timeline.setEventKey(null);
            } else {
                checkEventKeyUnique(userId, request.getEventKey().trim(), id);
                timeline.setEventKey(request.getEventKey().trim());
            }
        }
        if (request.getAbilityData() != null) {
            softDeleteAbilities(id);
            saveAbilities(id, request.getAbilityData());
        }
        if (request.getTags() != null) {
            softDeleteTags(id);
            saveTags(id, request.getTags());
        }
        growthTimelineRepository.save(timeline);

        log.info("修改成长时间轴事件: id={}, userId={}", id, userId);
        return toDetail(timeline);
    }

    /**
     * 删除成长时间轴事件（DELETE /profile/growth-timeline/{id}）
     * <p>
     * 软删除主记录及其能力得分、标签子记录（置 deleted_at）。
     *
     * @param userId 当前登录用户 ID
     * @param id     时间轴节点 ID
     */
    @Transactional
    public void delete(Long userId, Long id) {
        // 仅查询归属者 ID（不加载完整实体），随后原生软删除，避免脏检查回写已删除行
        Long ownerId = entityManager
                .createQuery("SELECT t.userId FROM GrowthTimeline t WHERE t.id = :id", Long.class)
                .setParameter("id", id)
                .getResultStream().findFirst().orElse(null);
        if (ownerId == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "成长时间轴事件不存在");
        }
        if (!ownerId.equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }

        softDeleteAbilitiesAndTags(id);
        softDeleteNode(id);
        log.info("删除成长时间轴事件: id={}, userId={}", id, userId);
    }

    // ==================== 内部工具 ====================

    /** 查询本人时间轴节点（未删除），越权返回 20005 */
    private GrowthTimeline getOwnedTimeline(Long userId, Long id) {
        GrowthTimeline timeline = growthTimelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "成长时间轴事件不存在"));
        if (!timeline.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        return timeline;
    }

    /** 校验事件类型合法（1-6） */
    private void validateEventType(Integer eventType) {
        if (EventTypeEnum.of(eventType) == null) {
            throw new BusinessException(ResultCode.PARAM_ILLEGAL, "事件类型只能是1-6");
        }
    }

    /** 校验学期存在 */
    private void validateSemester(Long semesterId) {
        semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));
    }

    /** 来源记录必填校验：每条时间轴事件必须关联一条已有来源记录，否则报错 */
    private void validateSourceRequired(String sourceType, Long sourceId) {
        if (sourceType == null || sourceType.isBlank() || sourceId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "必须关联已有的来源记录，需同时提供来源类型(sourceType)和来源ID(sourceId)");
        }
    }

    /** 来源记录真实性校验：须在对应来源表中存在（未软删除）且归属当前用户，否则报错 */
    private void validateSourceRecord(Long userId, String sourceType, Long sourceId) {
        String sql = SOURCE_VALIDATION_SQL.get(sourceType);
        if (sql == null) {
            throw new BusinessException(ResultCode.PARAM_ILLEGAL, "不支持的来源类型: " + sourceType);
        }
        boolean exists = !entityManager.createNativeQuery(sql)
                .setParameter("id", sourceId)
                .setParameter("userId", userId)
                .getResultList()
                .isEmpty();
        if (!exists) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "来源记录不存在或不属于当前用户");
        }
    }

    /** 解析日期字符串，格式 YYYY-MM-DD */
    private LocalDate parseEventAt(String eventAt) {
        try {
            return LocalDate.parse(eventAt, DATE_FORMAT);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "日期格式必须为YYYY-MM-DD");
        }
    }

    /** 事件去重键唯一性校验（uk_gt_event_key），排除自身 */
    private void checkEventKeyUnique(Long userId, String eventKey, Long excludeId) {
        String jpql = excludeId != null
                ? "SELECT COUNT(t) FROM GrowthTimeline t WHERE t.userId = :userId AND t.eventKey = :eventKey AND t.id <> :excludeId"
                : "SELECT COUNT(t) FROM GrowthTimeline t WHERE t.userId = :userId AND t.eventKey = :eventKey";
        Query query = entityManager.createQuery(jpql);
        query.setParameter("userId", userId);
        query.setParameter("eventKey", eventKey);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        if (((Number) query.getSingleResult()).longValue() > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "事件去重键已存在");
        }
    }

    /** 来源唯一性校验（uk_gt_source），排除自身 */
    private void checkSourceUnique(Long userId, String sourceType, Long sourceId, Long excludeId) {
        String jpql = excludeId != null
                ? "SELECT COUNT(t) FROM GrowthTimeline t WHERE t.userId = :userId AND t.sourceType = :sourceType AND t.sourceId = :sourceId AND t.id <> :excludeId"
                : "SELECT COUNT(t) FROM GrowthTimeline t WHERE t.userId = :userId AND t.sourceType = :sourceType AND t.sourceId = :sourceId";
        Query query = entityManager.createQuery(jpql);
        query.setParameter("userId", userId);
        query.setParameter("sourceType", sourceType);
        query.setParameter("sourceId", sourceId);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        if (((Number) query.getSingleResult()).longValue() > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "该来源记录已生成时间轴事件");
        }
    }

    /** 批量保存能力得分子表 */
    private void saveAbilities(Long timelineId, List<GrowthTimelineAbilityItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<GrowthTimelineAbility> abilities = items.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getDimensionCode() != null && !i.getDimensionCode().isBlank())
                .map(i -> {
                    GrowthTimelineAbility a = new GrowthTimelineAbility();
                    a.setTimelineId(timelineId);
                    a.setDimensionCode(i.getDimensionCode().trim());
                    a.setScore(i.getScore() != null ? i.getScore() : BigDecimal.ZERO);
                    return a;
                })
                .collect(Collectors.toList());
        if (!abilities.isEmpty()) {
            growthTimelineAbilityRepository.saveAll(abilities);
        }
    }

    /** 批量保存标签子表（自动去重） */
    private void saveTags(Long timelineId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        List<String> cleaned = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return;
        }
        List<GrowthTimelineTag> entities = cleaned.stream()
                .map(tagName -> {
                    GrowthTimelineTag tag = new GrowthTimelineTag();
                    tag.setTimelineId(timelineId);
                    tag.setTagName(tagName);
                    return tag;
                })
                .collect(Collectors.toList());
        growthTimelineTagRepository.saveAll(entities);
    }

    /** 原生软删除能力得分子表（按 timeline_id） */
    private void softDeleteAbilities(Long timelineId) {
        entityManager.createNativeQuery(
                        "UPDATE growth_timeline_abilities SET deleted_at = :ts WHERE timeline_id = :id AND deleted_at IS NULL")
                .setParameter("ts", LocalDateTime.now())
                .setParameter("id", timelineId)
                .executeUpdate();
    }

    /** 原生软删除标签子表（按 timeline_id） */
    private void softDeleteTags(Long timelineId) {
        entityManager.createNativeQuery(
                        "UPDATE growth_timeline_tags SET deleted_at = :ts WHERE timeline_id = :id AND deleted_at IS NULL")
                .setParameter("ts", LocalDateTime.now())
                .setParameter("id", timelineId)
                .executeUpdate();
    }

    /** 原生软删除主记录及其子表 */
    private void softDeleteAbilitiesAndTags(Long timelineId) {
        softDeleteAbilities(timelineId);
        softDeleteTags(timelineId);
    }

    /** 原生软删除主记录 */
    private void softDeleteNode(Long id) {
        entityManager.createNativeQuery(
                        "UPDATE growth_timelines SET deleted_at = :ts WHERE id = :id AND deleted_at IS NULL")
                .setParameter("ts", LocalDateTime.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    /** 实体 → 详情响应 */
    private GrowthTimelineDetailResponse toDetail(GrowthTimeline timeline) {
        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));
        List<GrowthTimelineAbility> abilities =
                growthTimelineAbilityRepository.findByTimelineIdIn(Collections.singletonList(timeline.getId()));
        List<GrowthTimelineTag> tags =
                growthTimelineTagRepository.findByTimelineIdIn(Collections.singletonList(timeline.getId()));
        EventTypeEnum eventTypeEnum = EventTypeEnum.of(timeline.getEventType());
        return GrowthTimelineDetailResponse.builder()
                .id(timeline.getId())
                .semesterId(timeline.getSemesterId())
                .semesterName(semesterName(timeline.getSemesterId()))
                .eventAt(formatDate(timeline.getEventAt()))
                .eventName(timeline.getEventName())
                .content(timeline.getContent())
                .eventType(timeline.getEventType())
                .eventTypeLabel(eventTypeEnum != null ? eventTypeEnum.getLabel() : null)
                .status(timeline.getStatus())
                .statusLabel(ApplyStatusEnum.of(timeline.getStatus()).getLabel())
                .coverImage(timeline.getCoverImage())
                .sourceId(timeline.getSourceId())
                .sourceType(timeline.getSourceType())
                .eventKey(timeline.getEventKey())
                .abilityData(abilities.stream()
                        .map(a -> AbilityItem.builder()
                                .dimensionCode(a.getDimensionCode())
                                .dimensionName(dimensionNameMap.get(a.getDimensionCode()))
                                .score(cleanDecimal(a.getScore()))
                                .build())
                        .collect(Collectors.toList()))
                .tags(tags.stream().map(GrowthTimelineTag::getTagName).collect(Collectors.toList()))
                .createdAt(toIso(timeline.getCreatedAt()))
                .updatedAt(toIso(timeline.getUpdatedAt()))
                .build();
    }

    /** 学期 ID → 学期名称 */
    private String semesterName(Long semesterId) {
        if (semesterId == null) {
            return null;
        }
        return semesterRepository.findById(semesterId).map(Semester::getName).orElse(null);
    }

    /** 清理 BigDecimal 尾部零（88.00 → 88） */
    private BigDecimal cleanDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    /** LocalDate → "2005-03-15" 字符串 */
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : null;
    }
}
