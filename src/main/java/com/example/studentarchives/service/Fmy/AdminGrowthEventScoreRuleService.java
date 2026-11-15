package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleItem;
import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleUpsertRequest;
import com.example.studentarchives.dto.Fmy.growthscorerule.response.GrowthScoreRuleResponse;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.foundation.GrowthEventScoreRule;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.GrowthEventScoreRuleRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端成长事件计分规则服务
 * <p>
 * 提供计分规则的列表、创建、更新、删除（软删除），每种事件类型（1-6）仅一条规则。
 * 学生新增/修改成长时间轴事件时，能力得分由本规则表按事件类型自动生成，
 * 学生不再自填分数；无规则的事件类型不产生能力得分行。
 * <p>
 * 所有接口需通过 {@link AdminAuthService#requireAdminOrPermission(Long, String...)} 校验
 * admin 角色或 indicator:manage 权限码（与能力维度管理一致）。
 * <p>
 * 业务校验：eventType 唯一；abilities 内 dimensionCode 不重复；
 * dimensionCode 必须存在于 ability_dimensions 且处于启用状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGrowthEventScoreRuleService {

    private final GrowthEventScoreRuleRepository growthEventScoreRuleRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    /**
     * 获取计分规则列表
     */
    @Transactional(readOnly = true)
    public List<GrowthScoreRuleResponse> list(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        return growthEventScoreRuleRepository.findAllByOrderByEventTypeAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建计分规则（同一事件类型仅允许一条）
     */
    @Transactional
    public GrowthScoreRuleResponse create(Long userId, GrowthScoreRuleUpsertRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        if (growthEventScoreRuleRepository.findByEventType(request.getEventType()).isPresent()) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "该事件类型的计分规则已存在，请直接更新");
        }
        validateAbilities(request.getAbilities());

        GrowthEventScoreRule rule = new GrowthEventScoreRule();
        rule.setEventType(request.getEventType());
        rule.setAbilities(toJson(request.getAbilities()));
        rule.setRemark(request.getRemark());
        growthEventScoreRuleRepository.save(rule);

        log.info("创建成长事件计分规则: id={}, eventType={}, operatorId={}",
                rule.getId(), rule.getEventType(), userId);
        return toResponse(rule);
    }

    /**
     * 更新计分规则
     */
    @Transactional
    public GrowthScoreRuleResponse update(Long userId, Long id, GrowthScoreRuleUpsertRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        GrowthEventScoreRule rule = growthEventScoreRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "计分规则不存在"));

        if (!Objects.equals(rule.getEventType(), request.getEventType())) {
            growthEventScoreRuleRepository.findByEventType(request.getEventType())
                    .filter(existing -> !Objects.equals(existing.getId(), id))
                    .ifPresent(existing -> {
                        throw new BusinessException(ResultCode.DATA_DUPLICATE, "该事件类型的计分规则已存在");
                    });
            rule.setEventType(request.getEventType());
        }
        validateAbilities(request.getAbilities());
        rule.setAbilities(toJson(request.getAbilities()));
        rule.setRemark(request.getRemark());
        growthEventScoreRuleRepository.save(rule);

        log.info("更新成长事件计分规则: id={}, eventType={}, operatorId={}", id, rule.getEventType(), userId);
        return toResponse(rule);
    }

    /**
     * 删除计分规则（软删除）
     * <p>
     * 删除后，该事件类型的新增事件不再产生能力得分行；存量事件得分保留不动。
     */
    @Transactional
    public void delete(Long userId, Long id) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        GrowthEventScoreRule rule = growthEventScoreRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "计分规则不存在"));

        growthEventScoreRuleRepository.softDeleteById(id, LocalDateTime.now());
        log.info("删除成长事件计分规则: id={}, eventType={}, operatorId={}", id, rule.getEventType(), userId);
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

    private GrowthScoreRuleResponse toResponse(GrowthEventScoreRule rule) {
        return GrowthScoreRuleResponse.builder()
                .id(rule.getId())
                .eventType(rule.getEventType())
                .eventTypeLabel(eventTypeLabel(rule.getEventType()))
                .abilities(parseAbilities(rule.getAbilities()))
                .remark(rule.getRemark())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private String eventTypeLabel(Integer eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case 1 -> "奖项";
            case 2 -> "成绩";
            case 3 -> "实践";
            case 4 -> "职业规划";
            case 5 -> "短板改进";
            case 6 -> "能力提升";
            default -> null;
        };
    }
}
