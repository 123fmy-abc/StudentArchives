package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorCreateRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorPublishRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorUpdateRequest;
import com.example.studentarchives.dto.Fmy.indicator.response.AdminIndicatorTreeResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorCreateResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorPublishResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorRuleVersionItem;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import com.example.studentarchives.entity.foundation.IndicatorRuleVersion;
import com.example.studentarchives.entity.foundation.IndicatorVersion;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.EvaluationIndicatorRepository;
import com.example.studentarchives.repository.IndicatorRuleVersionRepository;
import com.example.studentarchives.repository.IndicatorVersionRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端指标配置服务
 * <p>
 * 对应《管理端接口文档》一、指标配置模块：
 * 指标树查询、创建/更新/删除指标、发布指标规则版本、规则版本列表。
 * 所有接口需通过 {@link AdminAuthService#requireAdminOrPermission(Long, String...)} 校验
 * admin 角色或 indicator:manage 权限码。
 * <p>
 * <b>版本化模型说明：</b>
 * <ul>
 *   <li>{@code evaluation_indicators.version} 保存当前（草稿）指标树所属的全局规则版本号，
 *       新建指标沿用当前版本号，发布时全校指标统一推进到新版本号。</li>
 *   <li>{@code indicator_versions} 在发布时对全校指标写入 (indicator_id, 新版本号) 的权重/计分规则快照，
 *       作为该发布版本的权威记录，供历史评分追溯（对应文档 1.3「不影响历史评分」）。</li>
 *   <li>{@code indicator_rule_versions.tree_snapshot} 在发布时写入完整指标树 JSON 快照（含结构字段），
 *       供学生端/公共端按历史版本查询指标树（对应文档 1.1 / 学生端文档 2.4）。</li>
 *   <li>指标编辑（改 weight/scoringRule）只修改草稿树，不覆盖已发布版本快照；变更在下次发布时形成新版本。</li>
 * </ul>
 * <p>
 * <b>权重口径说明：</b>数据库 {@code evaluation_indicators.weight} 为 0-1 小数（DECIMAL(5,4)），
 * 一级指标权重之和应为 1，各级子指标权重之和应等于父指标权重。校验失败返回 41004。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminIndicatorService {

    /** 最大指标层级 */
    private static final int MAX_LEVEL = 3;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final EvaluationIndicatorRepository evaluationIndicatorRepository;
    private final IndicatorRuleVersionRepository indicatorRuleVersionRepository;
    private final IndicatorVersionRepository indicatorVersionRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final ScoringRuleValidator scoringRuleValidator;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final AdminAuthService adminAuthService;
    private final SchoolRepository schoolRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper;

    // ==================== 1.1 获取指标树 ====================

    /**
     * 获取管理端指标树（GET /admin/indicators/tree）
     * <p>
     * <b>按学期过滤：</b>semesterId 不传时取该校当前学期（is_current=1）。
     * <ul>
     *   <li>该学期已发布过规则版本 → 返回该学期最近发布版本的权威指标树（读取
     *       {@code indicator_rule_versions.tree_snapshot}，只读历史视图，不含发布后的草稿改动）；</li>
     *   <li>该学期未发布过 → 返回学校下当前草稿树（含未发布的草稿改动，便于发布前编辑确认），
     *       顶部返回当前生效规则版本元数据。</li>
     * </ul>
     * 树节点携带权重、状态、计分规则与版本信息，可按 status 过滤（0=禁用 1=启用）。
     *
     * @param userId     当前登录用户 ID
     * @param schoolId   学校 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param status     0=禁用 1=启用，不传返回全部
     */
    @Transactional(readOnly = true)
    public AdminIndicatorTreeResponse getTree(Long userId, Long schoolId, Long semesterId, Integer status) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        validateSchoolAndSemester(schoolId, semesterId);

        // 解析查询学期：参数优先，未传则取该校当前学期；仍无则按学校维度查询（原行为）
        Long querySemesterId = semesterId != null
                ? semesterId
                : semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);

        if (querySemesterId != null) {
            // 该学期已发布过规则版本：展示该学期最近发布版本的权威指标树（快照）
            IndicatorRuleVersion semesterVersion = indicatorRuleVersionRepository
                    .findTopBySchoolIdAndSemesterIdOrderByVersionDesc(schoolId, querySemesterId)
                    .orElse(null);
            if (semesterVersion != null) {
                List<AdminIndicatorTreeResponse.IndicatorNode> tree =
                        resolveVersionTree(schoolId, semesterVersion, status);
                return AdminIndicatorTreeResponse.builder()
                        .versionId(semesterVersion.getId())
                        .version(semesterVersion.getVersion())
                        .versionName(semesterVersion.getVersionName())
                        .effectiveAt(semesterVersion.getEffectiveAt() != null
                                ? toIso(semesterVersion.getEffectiveAt()) : null)
                        .indicators(tree)
                        .build();
            }
            // 该学期未发布过：回退当前草稿树（编辑视图），供发布前编辑确认
        }

        IndicatorRuleVersion current = indicatorRuleVersionRepository.findCurrentEffective(schoolId)
                .orElseGet(() -> indicatorRuleVersionRepository.findTopBySchoolIdOrderByVersionDesc(schoolId).orElse(null));

        List<EvaluationIndicator> indicators = (status != null)
                ? evaluationIndicatorRepository.findBySchoolIdAndStatusOrderBySortAsc(schoolId, status)
                : evaluationIndicatorRepository.findBySchoolIdOrderBySortAsc(schoolId);

        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        List<AdminIndicatorTreeResponse.IndicatorNode> tree = buildTree(indicators, null, dimensionNameMap);

        return AdminIndicatorTreeResponse.builder()
                .versionId(current != null ? current.getId() : null)
                .version(current != null ? current.getVersion() : null)
                .versionName(current != null ? current.getVersionName() : null)
                .effectiveAt(current != null && current.getEffectiveAt() != null ? toIso(current.getEffectiveAt()) : null)
                .indicators(tree)
                .build();
    }

    // ==================== 1.2 创建指标 ====================

    /**
     * 创建指标（POST /admin/indicators）
     * <p>
     * 创建一级/二级/三级指标。创建三级指标时必须填写 scoringRule（由 {@link ScoringRuleValidator} 校验）。
     * 创建后指标属于当前草稿树，需发布后形成规则版本快照。
     */
    @Transactional
    public IndicatorCreateResponse createIndicator(Long userId, IndicatorCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        Long schoolId = request.getSchoolId();

        EvaluationIndicator parent = null;
        int level;
        if (request.getParentId() != null) {
            parent = evaluationIndicatorRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "父级指标不存在"));
            if (!Objects.equals(parent.getSchoolId(), schoolId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "父级指标不属于该学校");
            }
            level = parent.getLevel() + 1;
            if (level > MAX_LEVEL) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持三级指标，父级指标下不能继续创建子指标");
            }
        } else {
            level = 1;
        }

        // 指标编码学校内唯一（DB 亦存在条件唯一索引兜底）
        if (evaluationIndicatorRepository.findBySchoolIdAndIndicatorCode(schoolId, request.getIndicatorCode()).isPresent()) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "指标编码已存在");
        }

        // 三级指标必填计分规则
        if (level == MAX_LEVEL && (request.getScoringRule() == null || request.getScoringRule().isNull())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "三级指标必填 scoringRule");
        }
        scoringRuleValidator.validate(request.getScoringRule());

        // 排序与路径
        List<EvaluationIndicator> siblings = siblingsOf(schoolId, parent);
        int sort = request.getSort() != null
                ? request.getSort()
                : siblings.stream().mapToInt(e -> e.getSort() == null ? 0 : e.getSort()).max().orElse(0) + 1;
        String path = (parent == null ? "" : parent.getPath() + ".")
                + String.format("%03d", sort);

        // 能力维度：二级/三级继承父级；一级取请求值（可空）
        String dimensionCode = request.getDimensionCode();
        if (parent != null && (dimensionCode == null || dimensionCode.isEmpty())) {
            dimensionCode = parent.getDimensionCode();
        }

        // 同级权重之和校验：子指标之和 = 父权重，一级之和 = 1
        validateSiblingWeights(schoolId, parent, request.getWeight());

        EvaluationIndicator indicator = new EvaluationIndicator();
        indicator.setSchoolId(schoolId);
        indicator.setParentId(parent != null ? parent.getId() : null);
        indicator.setIndicatorCode(request.getIndicatorCode());
        indicator.setIndicatorName(request.getIndicatorName());
        indicator.setLevel(level);
        indicator.setPath(path);
        indicator.setWeight(request.getWeight());
        indicator.setDescription(request.getDescription());
        indicator.setScoringRule(request.getScoringRule() != null ? request.getScoringRule().toString() : null);
        indicator.setDimensionCode(dimensionCode);
        indicator.setStatus(1);
        indicator.setVersion(currentVersionNumber(schoolId));
        indicator.setSort(sort);
        evaluationIndicatorRepository.save(indicator);

        log.info("创建指标: id={}, code={}, level={}, schoolId={}, operatorId={}",
                indicator.getId(), indicator.getIndicatorCode(), level, schoolId, userId);

        return IndicatorCreateResponse.builder()
                .id(indicator.getId())
                .indicatorCode(indicator.getIndicatorCode())
                .indicatorName(indicator.getIndicatorName())
                .level(level)
                .weight(indicator.getWeight())
                .status(indicator.getStatus())
                .createdAt(toIso(indicator.getCreatedAt()))
                .build();
    }

    // ==================== 1.3 更新指标 ====================

    /**
     * 更新指标基础信息（PUT /admin/indicators/{indicatorId}）
     * <p>
     * 可更新名称、权重、说明、计分规则（仅三级）、维度、排序、状态。
     * 修改 weight/scoringRule 仅作用于当前草稿树，已发布版本快照不被覆盖，
     * 变更在下次发布时形成新版本，不影响历史评分。
     */
    @Transactional
    public void updateIndicator(Long userId, Long indicatorId, IndicatorUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        EvaluationIndicator indicator = evaluationIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "指标不存在"));

        // 计分规则：仅三级指标可修改，且修改时必须通过校验
        boolean ruleChanged = request.getScoringRule() != null && !request.getScoringRule().isNull();
        if (ruleChanged && !Integer.valueOf(MAX_LEVEL).equals(indicator.getLevel())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅三级指标可修改 scoringRule");
        }
        scoringRuleValidator.validate(request.getScoringRule());

        // 禁用校验：某指标禁用时，其下级必须全部为禁用状态
        if (request.getStatus() != null && request.getStatus() == 0) {
            boolean hasEnabledChild = evaluationIndicatorRepository.findByParentIdOrderBySortAsc(indicatorId).stream()
                    .anyMatch(c -> Integer.valueOf(1).equals(c.getStatus()));
            if (hasEnabledChild) {
                throw new BusinessException(ResultCode.DATA_STATUS_ERROR, "存在启用状态的子指标，请先禁用全部下级");
            }
        }

        // 权重变更时校验同级权重之和
        if (request.getWeight() != null && request.getWeight().compareTo(indicator.getWeight()) != 0) {
            validateSiblingWeightsAfterChange(indicator, request.getWeight());
        }

        if (request.getIndicatorName() != null) {
            indicator.setIndicatorName(request.getIndicatorName());
        }
        if (request.getWeight() != null) {
            indicator.setWeight(request.getWeight());
        }
        if (request.getDescription() != null) {
            indicator.setDescription(request.getDescription());
        }
        if (ruleChanged) {
            indicator.setScoringRule(request.getScoringRule().toString());
        }
        if (request.getDimensionCode() != null) {
            indicator.setDimensionCode(request.getDimensionCode());
        }
        if (request.getSort() != null) {
            indicator.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            indicator.setStatus(request.getStatus());
        }
        evaluationIndicatorRepository.save(indicator);

        log.info("更新指标: id={}, operatorId={}", indicatorId, userId);
    }

    // ==================== 1.4 删除指标 ====================

    /**
     * 软删除指标（DELETE /admin/indicators/{indicatorId}）
     * <p>
     * 存在子指标时返回 41003 存在子指标。
     */
    @Transactional
    public void deleteIndicator(Long userId, Long indicatorId) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        evaluationIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "指标不存在"));

        List<EvaluationIndicator> children = evaluationIndicatorRepository.findByParentIdOrderBySortAsc(indicatorId);
        if (!children.isEmpty()) {
            throw new BusinessException(ResultCode.INDICATOR_HAS_CHILDREN, "存在子指标，请先删除下级指标");
        }

        evaluationIndicatorRepository.softDeleteById(indicatorId, LocalDateTime.now());
        log.info("软删除指标: id={}, operatorId={}", indicatorId, userId);
    }

    // ==================== 1.5 发布指标规则版本 ====================

    /**
     * 发布指标规则版本（POST /admin/indicators/publish）
     * <p>
     * 将当前草稿指标树打包为一个新的规则版本（request.semesterId 可指定归属学期，
     * 不传则取该校当前学期）：
     * <ol>
     *   <li>校验权重：一级指标之和=1，各级子指标之和=父权重，失败返回 41004；</li>
     *   <li>版本名称重复返回 41002 规则版本已发布；</li>
     *   <li>生成新全局版本号，全校指标版本号先统一推进到新版本，保证快照内节点 version 一致；</li>
     *   <li>创建 indicator_rule_versions 记录并写入完整指标树快照（tree_snapshot，节点携带新版本号）；</li>
     *   <li>将全校指标当前配置写入 indicator_versions 快照（该版本权威记录）。</li>
     * </ol>
     */
    @Transactional
    public IndicatorPublishResponse publish(Long userId, IndicatorPublishRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        Long schoolId = request.getSchoolId();

        // 发布版本归属学期：不传则取该校当前学期，仍无则版本不限定学期
        Long semesterId = request.getSemesterId();
        if (semesterId == null) {
            semesterId = semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);
        } else {
            semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "学期不存在"));
        }

        validateWeightsForPublish(schoolId);

        if (indicatorRuleVersionRepository.findBySchoolIdAndVersionName(schoolId, request.getVersionName()).isPresent()) {
            throw new BusinessException(ResultCode.INDICATOR_RULE_VERSION_PUBLISHED,
                    "规则版本已发布，版本名称不能重复");
        }

        int nextVersion = indicatorRuleVersionRepository.findTopBySchoolIdOrderByVersionDesc(schoolId)
                .map(IndicatorRuleVersion::getVersion)
                .orElse(0) + 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveAt = parseEffectiveAt(request.getEffectiveAt(), now);

        // 全校指标版本号先推进到新版本，保证 tree_snapshot 内节点 version 与本次发布版本一致
        evaluationIndicatorRepository.restampVersion(schoolId, nextVersion, now);

        IndicatorRuleVersion version = new IndicatorRuleVersion();
        version.setSchoolId(schoolId);
        version.setSemesterId(semesterId);
        version.setVersion(nextVersion);
        version.setVersionName(request.getVersionName());
        version.setEffectiveAt(effectiveAt);
        version.setCreatedBy(userId);
        // 发布时点写入完整指标树快照（节点携带新版本号），供按历史版本查询指标树
        version.setTreeSnapshot(buildTreeSnapshot(schoolId));
        indicatorRuleVersionRepository.save(version);

        // 快照当前全校指标配置到 indicator_versions
        List<EvaluationIndicator> indicators = evaluationIndicatorRepository.findBySchoolIdOrderBySortAsc(schoolId);
        for (EvaluationIndicator e : indicators) {
            IndicatorVersion snapshot = new IndicatorVersion();
            snapshot.setIndicatorId(e.getId());
            snapshot.setVersion(nextVersion);
            snapshot.setWeight(e.getWeight());
            snapshot.setScoringRule(e.getScoringRule());
            snapshot.setChangeSummary(request.getVersionName());
            snapshot.setCreatedBy(userId);
            indicatorVersionRepository.save(snapshot);
        }

        log.info("发布指标规则版本: schoolId={}, version={}, versionName={}, operatorId={}",
                schoolId, nextVersion, request.getVersionName(), userId);

        return IndicatorPublishResponse.builder()
                .version(nextVersion)
                .createdAt(toIso(now))
                .build();
    }

    // ==================== 1.6 指标规则版本列表 ====================

    /**
     * 查询学校下历史发布的指标规则版本（GET /admin/indicators/rule-versions）
     * <p>
     * semesterId 非空时仅返回该学期的发布版本，否则返回全校全部版本。
     */
    @Transactional(readOnly = true)
    public PageResult<IndicatorRuleVersionItem> listRuleVersions(Long userId, Long schoolId,
                                                                 Long semesterId, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        validateSchoolAndSemester(schoolId, semesterId);

        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage());
        List<IndicatorRuleVersion> versions;
        long total;
        if (semesterId != null) {
            versions = indicatorRuleVersionRepository
                    .findBySchoolIdAndSemesterIdOrderByVersionDesc(schoolId, semesterId, pageable);
            total = indicatorRuleVersionRepository.countBySchoolIdAndSemesterId(schoolId, semesterId);
        } else {
            versions = indicatorRuleVersionRepository.findBySchoolIdOrderByVersionDesc(schoolId, pageable);
            total = indicatorRuleVersionRepository.countBySchoolId(schoolId);
        }

        List<IndicatorRuleVersionItem> items = versions.stream()
                .map(v -> IndicatorRuleVersionItem.builder()
                        .id(v.getId())
                        .version(v.getVersion())
                        .versionName(v.getVersionName())
                        .semesterId(v.getSemesterId())
                        .effectiveAt(v.getEffectiveAt() != null ? toIso(v.getEffectiveAt()) : null)
                        .createdBy(v.getCreatedBy())
                        .createdAt(toIso(v.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(items, total, pageParam);
    }

    // ==================== 私有辅助方法 ====================

    /** 递归构建指标树 */
    private List<AdminIndicatorTreeResponse.IndicatorNode> buildTree(
            List<EvaluationIndicator> allIndicators, Long parentId, Map<String, String> dimensionNameMap) {

        return allIndicators.stream()
                .filter(e -> Objects.equals(e.getParentId(), parentId))
                .map(e -> {
                    List<AdminIndicatorTreeResponse.IndicatorNode> children = buildTree(allIndicators, e.getId(), dimensionNameMap);
                    return AdminIndicatorTreeResponse.IndicatorNode.builder()
                            .id(e.getId())
                            .indicatorCode(e.getIndicatorCode())
                            .indicatorName(e.getIndicatorName())
                            .level(e.getLevel())
                            .weight(e.getWeight())
                            .status(e.getStatus())
                            .statusLabel(Integer.valueOf(1).equals(e.getStatus()) ? "启用" : "禁用")
                            .version(e.getVersion())
                            .dimensionCode(e.getDimensionCode())
                            .dimensionName(e.getDimensionCode() != null ? dimensionNameMap.get(e.getDimensionCode()) : null)
                            .description(e.getDescription())
                            .scoringRule(parseScoringRule(e.getScoringRule()))
                            .sort(e.getSort())
                            .children(children.isEmpty() ? null : children)
                            .build();
                })
                .sorted(Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort()))
                .collect(Collectors.toList());
    }

    /**
     * 将当前学校完整指标树序列化为 JSON 快照（含结构字段），写入 indicator_rule_versions.tree_snapshot。
     * 快照为发布时点的不变数据，供学生端/公共端按历史版本查询指标树。
     */
    private String buildTreeSnapshot(Long schoolId) {
        List<EvaluationIndicator> indicators = evaluationIndicatorRepository.findBySchoolIdOrderBySortAsc(schoolId);
        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));
        List<AdminIndicatorTreeResponse.IndicatorNode> tree = buildTree(indicators, null, dimensionNameMap);
        try {
            String snapshot = objectMapper.writeValueAsString(tree);
            // 校验快照为合法 JSON，防止非法结构入库导致历史版本查询反序列化失败
            jsonSchemaValidator.validateJson(snapshot, "indicator_rule_versions.tree_snapshot");
            return snapshot;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "指标树快照序列化失败");
        }
    }

    /**
     * 按已发布规则版本解析管理端指标树。
     * <p>
     * 优先读取 {@code indicator_rule_versions.tree_snapshot}（发布时点的权威树快照）；
     * 兼容快照落地前发布的旧版本（tree_snapshot 为 NULL）按版本号回退查询当前指标表。
     * 可选按 status 过滤节点（0=禁用 1=启用）。
     */
    private List<AdminIndicatorTreeResponse.IndicatorNode> resolveVersionTree(
            Long schoolId, IndicatorRuleVersion version, Integer status) {
        if (version.getTreeSnapshot() != null && !version.getTreeSnapshot().isBlank()) {
            List<AdminIndicatorTreeResponse.IndicatorNode> tree = readTreeSnapshot(version.getTreeSnapshot());
            return status != null ? filterNodesByStatus(tree, status) : tree;
        }
        // 兼容快照落地前发布的旧版本：按版本号回退查询当前指标表
        List<EvaluationIndicator> indicators = evaluationIndicatorRepository.findBySchoolIdOrderBySortAsc(schoolId).stream()
                .filter(e -> version.getVersion().equals(e.getVersion()))
                .filter(e -> status == null || status.equals(e.getStatus()))
                .collect(Collectors.toList());
        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));
        return buildTree(indicators, null, dimensionNameMap);
    }

    /** 反序列化指标树快照 JSON（发布时写入的完整管理端节点结构） */
    private List<AdminIndicatorTreeResponse.IndicatorNode> readTreeSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson,
                    new TypeReference<List<AdminIndicatorTreeResponse.IndicatorNode>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "指标树快照解析失败");
        }
    }

    /** 按 status 递归过滤快照指标树节点：命中禁用的节点连同其子树一起剔除 */
    private List<AdminIndicatorTreeResponse.IndicatorNode> filterNodesByStatus(
            List<AdminIndicatorTreeResponse.IndicatorNode> nodes, Integer status) {
        if (nodes == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> n.getStatus() == null || status.equals(n.getStatus()))
                .map(n -> {
                    n.setChildren(filterNodesByStatus(n.getChildren(), status));
                    return n;
                })
                .collect(Collectors.toList());
    }

    /** scoring_rule JSON 字符串 → JsonNode（空值返回 null） */
    private JsonNode parseScoringRule(String scoringRule) {
        if (scoringRule == null || scoringRule.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(scoringRule);
        } catch (Exception e) {
            log.warn("scoring_rule 解析失败: {}", scoringRule, e);
            return null;
        }
    }

    /** 当前学校的一级指标（parent_id IS NULL） */
    private List<EvaluationIndicator> levelOneIndicators(Long schoolId) {
        return evaluationIndicatorRepository.findBySchoolIdAndParentIdIsNullOrderBySortAsc(schoolId);
    }

    /** 指定父级下的同级指标列表（父级为 null 时返回一级指标） */
    private List<EvaluationIndicator> siblingsOf(Long schoolId, EvaluationIndicator parent) {
        return parent == null ? levelOneIndicators(schoolId)
                : evaluationIndicatorRepository.findByParentIdOrderBySortAsc(parent.getId());
    }

    /** 创建时的同级权重之和校验：现有同级之和 + 新权重 = 父权重（一级 = 1） */
    private void validateSiblingWeights(Long schoolId, EvaluationIndicator parent, BigDecimal newWeight) {
        BigDecimal target = parent == null ? BigDecimal.ONE : parent.getWeight();
        BigDecimal sum = siblingsOf(schoolId, parent).stream()
                .map(EvaluationIndicator::getWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (newWeight != null) {
            sum = sum.add(newWeight);
        }
        if (sum.compareTo(target) != 0) {
            throw new BusinessException(ResultCode.INDICATOR_WEIGHT_SUM_INVALID,
                    "同级指标权重之和应为 " + target + "，当前合计 " + sum);
        }
    }

    /** 更新时的同级权重之和校验：剔除自身后的同级之和 + 新权重 = 父权重（一级 = 1） */
    private void validateSiblingWeightsAfterChange(EvaluationIndicator indicator, BigDecimal newWeight) {
        BigDecimal target = BigDecimal.ONE;
        List<EvaluationIndicator> siblings;
        if (indicator.getParentId() == null) {
            siblings = levelOneIndicators(indicator.getSchoolId());
        } else {
            EvaluationIndicator parent = evaluationIndicatorRepository.findById(indicator.getParentId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "父级指标不存在"));
            target = parent.getWeight();
            siblings = evaluationIndicatorRepository.findByParentIdOrderBySortAsc(parent.getId());
        }
        BigDecimal sum = siblings.stream()
                .filter(s -> !s.getId().equals(indicator.getId()))
                .map(EvaluationIndicator::getWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(newWeight);
        if (sum.compareTo(target) != 0) {
            throw new BusinessException(ResultCode.INDICATOR_WEIGHT_SUM_INVALID,
                    "同级指标权重之和应为 " + target + "，当前合计 " + sum);
        }
    }

    /** 发布前递归校验全校权重：一级之和=1；有子节点的指标，其子指标之和=父权重 */
    private void validateWeightsForPublish(Long schoolId) {
        List<EvaluationIndicator> all = evaluationIndicatorRepository.findBySchoolIdOrderBySortAsc(schoolId);
        if (all.isEmpty()) {
            throw new BusinessException(ResultCode.INDICATOR_WEIGHT_SUM_INVALID, "指标树为空，无法发布");
        }
        Map<Long, Long> childCount = all.stream()
                .filter(e -> e.getParentId() != null)
                .collect(Collectors.groupingBy(EvaluationIndicator::getParentId, Collectors.counting()));

        BigDecimal level1Sum = all.stream()
                .filter(e -> e.getParentId() == null)
                .map(EvaluationIndicator::getWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (level1Sum.compareTo(BigDecimal.ONE) != 0) {
            throw new BusinessException(ResultCode.INDICATOR_WEIGHT_SUM_INVALID,
                    "一级指标权重之和应为 1，当前合计 " + level1Sum);
        }

        for (EvaluationIndicator e : all) {
            Long children = childCount.get(e.getId());
            if (children == null || children == 0) {
                continue; // 叶子节点，无需校验子级
            }
            BigDecimal childrenSum = all.stream()
                    .filter(c -> Objects.equals(c.getParentId(), e.getId()))
                    .map(EvaluationIndicator::getWeight)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (childrenSum.compareTo(e.getWeight()) != 0) {
                throw new BusinessException(ResultCode.INDICATOR_WEIGHT_SUM_INVALID,
                        "指标 [" + e.getIndicatorName() + "] 下子指标权重之和应为 " + e.getWeight()
                                + "，当前合计 " + childrenSum);
            }
        }
    }

    /** 新建指标使用的版本号：当前学校最新规则版本号，未发布过则为 1 */
    private Integer currentVersionNumber(Long schoolId) {
        return indicatorRuleVersionRepository.findTopBySchoolIdOrderByVersionDesc(schoolId)
                .map(IndicatorRuleVersion::getVersion)
                .orElse(1);
    }

    /** 解析生效时间：支持 ISO 8601 带时区（如 2026-02-01T00:00:00+08:00，ZonedDateTime）或 ISO 本地时间（T 分隔，如 2026-02-01T00:00:00，LocalDateTime），空值返回默认 */
    private LocalDateTime parseEffectiveAt(String effectiveAt, LocalDateTime defaultVal) {
        if (effectiveAt == null || effectiveAt.isBlank()) {
            return defaultVal;
        }
        String trimmed = effectiveAt.trim();
        try {
            return ZonedDateTime.parse(trimmed).toLocalDateTime();
        } catch (DateTimeParseException ignore) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException e2) {
                throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR,
                        "effectiveAt 格式错误，应为 ISO 8601 格式，如 2026-02-01T00:00:00+08:00");
            }
        }
    }

    /** 校验学校存在、学期存在（semesterId 为预留参数，当前表结构按学校维度配置指标） */
    private void validateSchoolAndSemester(Long schoolId, Long semesterId) {
        if (schoolId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "schoolId 不能为空");
        }
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "学校不存在"));
        if (semesterId != null) {
            semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "学期不存在"));
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
