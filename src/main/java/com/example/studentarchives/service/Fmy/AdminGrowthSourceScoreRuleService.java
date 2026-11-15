package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleItem;
import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleUpsertRequest;
import com.example.studentarchives.dto.Fmy.growthscorerule.response.GrowthScoreRuleResponse;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.foundation.GrowthSourceScoreRule;
import com.example.studentarchives.enums.GrowthSourceTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.GrowthSourceScoreRuleRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端成长事件计分规则服务（按来源记录类型）
 * <p>
 * 提供计分规则的列表、创建、更新、删除（软删除），每种来源记录类型仅一条规则。
 * 学生新增/修改成长时间轴事件时，能力得分由本规则表按来源记录类型自动生成，
 * 学生不自填分数；无规则的来源类型不产生能力得分行。event_type 仅用于分类筛选，
 * 不参与计分。
 * <p>
 * 所有接口需通过 {@link AdminAuthService#requireAdminOrPermission(Long, String...)} 校验
 * admin 角色或 indicator:manage 权限码（与能力维度管理一致）。
 * <p>
 * 业务校验：sourceType 须为已登记的来源类型且唯一；abilities 内 dimensionCode 不重复；
 * dimensionCode 必须存在于 ability_dimensions 且处于启用状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGrowthSourceScoreRuleService {

    private final GrowthSourceScoreRuleRepository growthSourceScoreRuleRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    /**
     * 获取计分规则列表
     */
    @Transactional(readOnly = true)
    public List<GrowthScoreRuleResponse> list(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        return growthSourceScoreRuleRepository.findAllByOrderBySourceTypeAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建计分规则（同一来源记录类型仅允许一条）
     */
    @Transactional
    public GrowthScoreRuleResponse create(Long userId, GrowthScoreRuleUpsertRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        String sourceType = validateSourceType(request.getSourceType());
        if (growthSourceScoreRuleRepository.findBySourceType(sourceType).isPresent()) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "该来源记录类型的计分规则已存在，请直接更新");
        }
        validateAbilities(request.getAbilities());

        GrowthSourceScoreRule rule = new GrowthSourceScoreRule();
        rule.setSourceType(sourceType);
        rule.setAbilities(toJson(request.getAbilities()));
        rule.setRemark(request.getRemark());
        growthSourceScoreRuleRepository.save(rule);

        log.info("创建成长事件计分规则: id={}, sourceType={}, operatorId={}",
                rule.getId(), rule.getSourceType(), userId);
        return toResponse(rule);
    }

    /**
     * 更新计分规则
     */
    @Transactional
    public GrowthScoreRuleResponse update(Long userId, Long id, GrowthScoreRuleUpsertRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        GrowthSourceScoreRule rule = growthSourceScoreRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "计分规则不存在"));

        String sourceType = validateSourceType(request.getSourceType());
        if (!Objects.equals(rule.getSourceType(), sourceType)) {
            growthSourceScoreRuleRepository.findBySourceType(sourceType)
                    .filter(existing -> !Objects.equals(existing.getId(), id))
                    .ifPresent(existing -> {
                        throw new BusinessException(ResultCode.DATA_DUPLICATE, "该来源记录类型的计分规则已存在");
                    });
            rule.setSourceType(sourceType);
        }
        validateAbilities(request.getAbilities());
        rule.setAbilities(toJson(request.getAbilities()));
        rule.setRemark(request.getRemark());
        growthSourceScoreRuleRepository.save(rule);

        log.info("更新成长事件计分规则: id={}, sourceType={}, operatorId={}", id, rule.getSourceType(), userId);
        return toResponse(rule);
    }

    /**
     * 删除计分规则（软删除）
     * <p>
     * 删除后，该来源类型的新增事件不再产生能力得分行；存量事件得分保留不动。
     */
    @Transactional
    public void delete(Long userId, Long id) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        GrowthSourceScoreRule rule = growthSourceScoreRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "计分规则不存在"));

        growthSourceScoreRuleRepository.softDeleteById(id, LocalDateTime.now());
        log.info("删除成长事件计分规则: id={}, sourceType={}, operatorId={}", id, rule.getSourceType(), userId);
    }

    /** 校验来源记录类型：须为已登记类型 */
    private String validateSourceType(String sourceType) {
        String trimmed = sourceType == null ? null : sourceType.trim();
        if (GrowthSourceTypeEnum.of(trimmed) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的来源记录类型: " + sourceType);
        }
        return trimmed;
    }

    /** 校验得分项：dimensionCode 不重复、须存在且启用 */
    private void validateAbilities(List<GrowthScoreRuleItem> abilities) {
        Set<String> seen = new HashSet<>();
        for (GrowthScoreRuleItem item : abilities) {
            String code = item.getDimensionCode().trim();
            if (!seen.add(code)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "能力维度编码重复: " + code);
            }
            AbilityDimension dimension = abilityDimensionRepository.findByDimensionCode(code)
                    .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR,
                            "能力维度不存在: " + code));
            if (!Integer.valueOf(1).equals(dimension.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "能力维度已禁用: " + code);
            }
        }
    }

    /** 得分项列表序列化为 abilities JSON */
    private String toJson(List<GrowthScoreRuleItem> abilities) {
        List<GrowthScoreRuleItem> normalized = abilities.stream()
                .map(i -> new GrowthScoreRuleItem(i.getDimensionCode().trim(), i.getScore()))
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "计分规则序列化失败");
        }
    }

    /** abilities JSON 反序列化为得分项列表 */
    private List<GrowthScoreRuleItem> parseAbilities(String abilities) {
        try {
            return objectMapper.readValue(abilities, new TypeReference<List<GrowthScoreRuleItem>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "计分规则解析失败");
        }
    }

    private GrowthScoreRuleResponse toResponse(GrowthSourceScoreRule rule) {
        return GrowthScoreRuleResponse.builder()
                .id(rule.getId())
                .sourceType(rule.getSourceType())
                .sourceTypeLabel(GrowthSourceTypeEnum.labelOf(rule.getSourceType()))
                .abilities(parseAbilities(rule.getAbilities()))
                .remark(rule.getRemark())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
