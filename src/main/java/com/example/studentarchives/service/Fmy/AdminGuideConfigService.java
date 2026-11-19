package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.guideconfig.request.GuideConfigUpdateRequest;
import com.example.studentarchives.dto.Fmy.guideconfig.request.GuideRequirementItem;
import com.example.studentarchives.dto.Fmy.guideconfig.response.GuideConfigItem;
import com.example.studentarchives.dto.Fmy.guideconfig.response.GuideConfigListResponse;
import com.example.studentarchives.entity.foundation.ArchiveTypeConfig;
import com.example.studentarchives.entity.foundation.AwardTypeConfig;
import com.example.studentarchives.enums.ArchiveTypeEnum;
import com.example.studentarchives.enums.AwardTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveTypeConfigRepository;
import com.example.studentarchives.repository.AwardTypeConfigRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 管理端评选说明配置服务
 * <p>
 * 对应《管理端接口文档》十八、评选说明配置模块。评选说明自 V57 起按学校
 * （{@code archive_type_configs.school_id} / {@code award_type_configs.school_id}）
 * 独立配置，schoolId 不再由前端传入，统一取当前登录管理员所属学校。
 * <p>
 * 所有接口需通过 {@link AdminAuthService#requireAdminOrPermission(Long, String...)}
 * 校验 admin 角色或 {@code guide:manage} 权限码，越权返回 20005 无访问权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGuideConfigService {

    /** 档案类型 */
    public static final String KIND_ARCHIVE = "archive";
    /** 奖项类型 */
    public static final String KIND_AWARD = "award";

    private static final String PERMISSION = "guide:manage";
    private static final String STATUS_LABEL_ENABLED = "启用";
    private static final String STATUS_LABEL_DISABLED = "禁用";

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final ArchiveTypeConfigRepository archiveTypeConfigRepository;
    private final AwardTypeConfigRepository awardTypeConfigRepository;
    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    // ==================== 18.1 获取评选说明配置列表 ====================

    /**
     * 获取本校评选说明配置列表。
     * <p>
     * 返回枚举中全部类型：已配置的取库中数据，未配置的类型以枚举名称补齐（id 为空），
     * 便于管理端一次性看到可配置的全量类型。
     *
     * @param userId  当前登录用户 ID
     * @param kind    archive=档案类型，award=奖项类型
     * @param status  0=禁用 1=启用，不传返回全部
     * @param keyword 按类型中文名/编码模糊搜索，不传返回全部
     * @return 配置列表
     */
    @Transactional(readOnly = true)
    public GuideConfigListResponse list(Long userId, String kind, Integer status, String keyword) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        String normalizedKind = normalizeKind(kind);

        List<GuideConfigItem> items = KIND_ARCHIVE.equals(normalizedKind)
                ? listArchive(schoolId)
                : listAward(schoolId);

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        List<GuideConfigItem> filtered = items.stream()
                .filter(i -> status == null || status.equals(i.getStatus()))
                .filter(i -> kw == null
                        || (i.getTypeLabel() != null && i.getTypeLabel().toLowerCase().contains(kw))
                        || (i.getType() != null && i.getType().toLowerCase().contains(kw)))
                .collect(Collectors.toList());

        return GuideConfigListResponse.builder()
                .kind(normalizedKind)
                .list(filtered)
                .build();
    }

    // ==================== 18.2 更新（或新增）评选说明配置 ====================

    /**
     * 保存本校指定类型的评选说明配置（不存在时自动新增）。
     *
     * @param userId  当前登录用户 ID
     * @param kind    archive=档案类型，award=奖项类型
     * @param type    类型编码，必须属于对应枚举
     * @param request 保存请求（局部更新语义）
     * @return 保存后的配置项
     */
    @Transactional
    public GuideConfigItem upsert(Long userId, String kind, String type, GuideConfigUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        String normalizedKind = normalizeKind(kind);
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请求参数不能为空");
        }
        return KIND_ARCHIVE.equals(normalizedKind)
                ? upsertArchive(userId, schoolId, type, request)
                : upsertAward(userId, schoolId, type, request);
    }

    // ==================== 档案侧 ====================

    private List<GuideConfigItem> listArchive(Long schoolId) {
        Map<String, ArchiveTypeConfig> existing = new HashMap<>();
        for (ArchiveTypeConfig c : archiveTypeConfigRepository.findBySchoolIdOrderBySortAsc(schoolId)) {
            existing.put(c.getArchiveType(), c);
        }
        List<GuideConfigItem> items = new ArrayList<>();
        int index = 1;
        for (ArchiveTypeEnum e : ArchiveTypeEnum.values()) {
            ArchiveTypeConfig config = existing.remove(e.getValue());
            items.add(config != null
                    ? toArchiveItem(KIND_ARCHIVE, e, config)
                    : emptyItem(KIND_ARCHIVE, e.getValue(), e.getLabel(), index));
            index++;
        }
        // 库中存在但枚举未覆盖的历史类型同样返回，避免配置行在列表中消失
        for (ArchiveTypeConfig config : existing.values()) {
            items.add(toArchiveItem(KIND_ARCHIVE, ArchiveTypeEnum.of(config.getArchiveType()), config));
        }
        return items;
    }

    private GuideConfigItem upsertArchive(Long userId, Long schoolId, String type, GuideConfigUpdateRequest request) {
        ArchiveTypeEnum typeEnum = ArchiveTypeEnum.of(type);
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的档案类型：" + type);
        }
        ArchiveTypeConfig config = archiveTypeConfigRepository
                .findBySchoolIdAndArchiveType(schoolId, type)
                .orElse(null);
        boolean created = config == null;
        if (created) {
            config = new ArchiveTypeConfig();
            config.setSchoolId(schoolId);
            config.setArchiveType(type);
            config.setTypeName(typeEnum.getLabel());
            config.setSort(nextArchiveSort(schoolId));
            config.setStatus(1);
            config.setCreatedBy(userId);
        }
        applyFields(request.getTypeName(), config::setTypeName,
                request.getContent(), config::setEvaluateDesc,
                request.getRequirements(), config::setEvaluateRequirements,
                request.getNotes(), config::setEvaluateNotes,
                request.getApplyDesc(), config::setApplyDesc,
                request.getIcon(), config::setIcon,
                request.getSort(), config::setSort,
                request.getStatus(), config::setStatus);
        config.setUpdatedBy(userId);
        archiveTypeConfigRepository.save(config);

        log.info("保存档案评选说明配置: schoolId={}, type={}, created={}, operatorId={}",
                schoolId, type, created, userId);
        return toArchiveItem(KIND_ARCHIVE, typeEnum, config);
    }

    // ==================== 奖项侧 ====================

    private List<GuideConfigItem> listAward(Long schoolId) {
        Map<String, AwardTypeConfig> existing = new HashMap<>();
        for (AwardTypeConfig c : awardTypeConfigRepository.findBySchoolIdOrderBySortAsc(schoolId)) {
            existing.put(c.getAwardType(), c);
        }
        List<GuideConfigItem> items = new ArrayList<>();
        int index = 1;
        for (AwardTypeEnum e : AwardTypeEnum.values()) {
            AwardTypeConfig config = existing.remove(e.getValue());
            items.add(config != null
                    ? toAwardItem(e, config)
                    : emptyItem(KIND_AWARD, e.getValue(), e.getLabel(), index));
            index++;
        }
        for (AwardTypeConfig config : existing.values()) {
            items.add(toAwardItem(AwardTypeEnum.of(config.getAwardType()), config));
        }
        return items;
    }

    private GuideConfigItem upsertAward(Long userId, Long schoolId, String type, GuideConfigUpdateRequest request) {
        AwardTypeEnum typeEnum = AwardTypeEnum.of(type);
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的奖项类型：" + type);
        }
        AwardTypeConfig config = awardTypeConfigRepository
                .findBySchoolIdAndAwardType(schoolId, type)
                .orElse(null);
        boolean created = config == null;
        if (created) {
            config = new AwardTypeConfig();
            config.setSchoolId(schoolId);
            config.setAwardType(type);
            config.setTypeName(typeEnum.getLabel());
            config.setSort(nextAwardSort(schoolId));
            config.setStatus(1);
            config.setCreatedBy(userId);
        }
        applyFields(request.getTypeName(), config::setTypeName,
                request.getContent(), config::setEvaluateDesc,
                request.getRequirements(), config::setEvaluateRequirements,
                request.getNotes(), config::setEvaluateNotes,
                request.getApplyDesc(), config::setApplyDesc,
                request.getIcon(), config::setIcon,
                request.getSort(), config::setSort,
                request.getStatus(), config::setStatus);
        config.setUpdatedBy(userId);
        awardTypeConfigRepository.save(config);

        log.info("保存奖项评选说明配置: schoolId={}, type={}, created={}, operatorId={}",
                schoolId, type, created, userId);
        return toAwardItem(typeEnum, config);
    }

    // ==================== 字段写入与映射 ====================

    /**
     * 按「局部更新」语义写入字段：请求中为 null 的字段保持不变；
     * 文本空串表示清空（落库为 null）；列表空数组表示清空（落库为 "[]"）。
     */
    private void applyFields(String typeName, Consumer<String> typeNameSetter,
                             String content, Consumer<String> contentSetter,
                             List<GuideRequirementItem> requirements, Consumer<String> requirementsSetter,
                             List<String> notes, Consumer<String> notesSetter,
                             String applyDesc, Consumer<String> applyDescSetter,
                             String icon, Consumer<String> iconSetter,
                             Integer sort, Consumer<Integer> sortSetter,
                             Integer status, Consumer<Integer> statusSetter) {
        String normalizedTypeName = blankToNull(typeName);
        if (normalizedTypeName != null) {
            typeNameSetter.accept(normalizedTypeName);
        }
        if (content != null) {
            contentSetter.accept(blankToNull(content));
        }
        if (requirements != null) {
            requirementsSetter.accept(toRequirementsJson(requirements));
        }
        if (notes != null) {
            notesSetter.accept(toNotesJson(notes));
        }
        if (applyDesc != null) {
            applyDescSetter.accept(blankToNull(applyDesc));
        }
        if (icon != null) {
            iconSetter.accept(blankToNull(icon));
        }
        if (sort != null) {
            sortSetter.accept(sort);
        }
        if (status != null) {
            statusSetter.accept(status);
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private GuideConfigItem toArchiveItem(String kind, ArchiveTypeEnum typeEnum, ArchiveTypeConfig config) {
        String label = config.getTypeName() != null && !config.getTypeName().isBlank()
                ? config.getTypeName()
                : (typeEnum != null ? typeEnum.getLabel() : config.getArchiveType());
        return GuideConfigItem.builder()
                .id(config.getId())
                .kind(kind)
                .type(config.getArchiveType())
                .typeLabel(label)
                .title(label + "评选说明")
                .content(config.getEvaluateDesc())
                .requirements(parseRequirements(config.getEvaluateRequirements()))
                .notes(parseNotes(config.getEvaluateNotes()))
                .applyDesc(config.getApplyDesc())
                .icon(config.getIcon())
                .sort(config.getSort())
                .status(config.getStatus())
                .statusLabel(statusLabel(config.getStatus()))
                .updatedAt(toIso(config.getUpdatedAt()))
                .build();
    }

    private GuideConfigItem toAwardItem(AwardTypeEnum typeEnum, AwardTypeConfig config) {
        String label = config.getTypeName() != null && !config.getTypeName().isBlank()
                ? config.getTypeName()
                : (typeEnum != null ? typeEnum.getLabel() : config.getAwardType());
        return GuideConfigItem.builder()
                .id(config.getId())
                .kind(KIND_AWARD)
                .type(config.getAwardType())
                .typeLabel(label)
                .title(label + "评选说明")
                .content(config.getEvaluateDesc())
                .requirements(parseRequirements(config.getEvaluateRequirements()))
                .notes(parseNotes(config.getEvaluateNotes()))
                .applyDesc(config.getApplyDesc())
                .icon(config.getIcon())
                .sort(config.getSort())
                .status(config.getStatus())
                .statusLabel(statusLabel(config.getStatus()))
                .updatedAt(toIso(config.getUpdatedAt()))
                .build();
    }

    private GuideConfigItem emptyItem(String kind, String type, String typeLabel, int index) {
        return GuideConfigItem.builder()
                .kind(kind)
                .type(type)
                .typeLabel(typeLabel)
                .title(typeLabel + "评选说明")
                .requirements(new ArrayList<>())
                .notes(new ArrayList<>())
                .sort(index)
                .status(1)
                .statusLabel(STATUS_LABEL_ENABLED)
                .build();
    }

    private String normalizeKind(String kind) {
        String trimmed = kind == null ? null : kind.trim();
        if (KIND_ARCHIVE.equalsIgnoreCase(trimmed)) {
            return KIND_ARCHIVE;
        }
        if (KIND_AWARD.equalsIgnoreCase(trimmed)) {
            return KIND_AWARD;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的类型种类：" + kind);
    }

    private int nextArchiveSort(Long schoolId) {
        return archiveTypeConfigRepository.findTopBySchoolIdOrderBySortDesc(schoolId)
                .map(c -> c.getSort() == null ? 1 : c.getSort() + 1)
                .orElse(1);
    }

    private int nextAwardSort(Long schoolId) {
        return awardTypeConfigRepository.findTopBySchoolIdOrderBySortDesc(schoolId)
                .map(c -> c.getSort() == null ? 1 : c.getSort() + 1)
                .orElse(1);
    }

    private String toRequirementsJson(List<GuideRequirementItem> requirements) {
        List<GuideRequirementItem> normalized = requirements.stream()
                .filter(Objects::nonNull)
                .map(i -> new GuideRequirementItem(
                        requireText(i.getField(), "评选要求字段名不能为空"),
                        requireText(i.getLabel(), "评选要求字段中文名不能为空"),
                        i.isRequired(),
                        blankToNull(i.getDescription())))
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评选要求序列化失败");
        }
    }

    private String toNotesJson(List<String> notes) {
        List<String> normalized = notes.stream()
                .filter(Objects::nonNull)
                .map(this::blankToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评选注意事项序列化失败");
        }
    }

    private String requireText(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
        return normalized;
    }

    private List<GuideRequirementItem> parseRequirements(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<GuideRequirementItem>>() {
            });
        } catch (Exception e) {
            log.warn("解析评选要求配置失败，已按空列表返回: {}", json, e);
            return new ArrayList<>();
        }
    }

    private List<String> parseNotes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析评选注意事项配置失败，已按空列表返回: {}", json, e);
            return new ArrayList<>();
        }
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(0).equals(status) ? STATUS_LABEL_DISABLED : STATUS_LABEL_ENABLED;
    }

    private String toIso(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE);
    }
}
