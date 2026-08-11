package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 计分规则校验器（scoring_rule JSON）
 * <p>
 * 对应《学生档案系统表》evaluation_indicators.scoring_rule 字段的 JSON 校验规则：
 * 三级指标必填 scoring_rule，按 type 分类型校验，校验失败统一返回 10001 参数错误，
 * 错误信息包含 scoring_rule 字段路径。
 * <p>
 * 支持的规则类型：AVG / SUM / MAX / WEIGHTED / THRESHOLD / COUNT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringRuleValidator {

    private final AbilityDimensionRepository abilityDimensionRepository;

    /** 支持的规则类型 */
    private static final Set<String> SUPPORTED_TYPES =
            Set.of("AVG", "SUM", "MAX", "WEIGHTED", "THRESHOLD", "COUNT");

    /**
     * 校验计分规则 JSON。rule 为 null 时直接通过（仅三级指标强制必填由调用方控制）。
     *
     * @param rule 计分规则 JSON
     */
    public void validate(JsonNode rule) {
        if (rule == null || rule.isNull() || rule.isMissingNode()) {
            return;
        }
        if (!rule.isObject()) {
            throw badRule("scoring_rule 必须为 JSON 对象");
        }
        String type = text(rule, "type");
        if (type == null || type.isEmpty()) {
            throw badRule("scoring_rule.type 不能为空");
        }
        if (!SUPPORTED_TYPES.contains(type)) {
            throw badRule("不支持的计分规则类型: " + type);
        }

        switch (type) {
            case "AVG" -> {
                requireText(rule, "source");
            }
            case "SUM" -> {
                requireText(rule, "source");
                JsonNode max = rule.get("max");
                if (max != null && !max.isNull() && !(max.isNumber() && max.asInt() >= 0)) {
                    throw badRule("scoring_rule.max 必须为不小于 0 的整数");
                }
            }
            case "MAX" -> requireText(rule, "source");
            case "WEIGHTED" -> {
                requireText(rule, "source");
                JsonNode weights = rule.get("weights");
                if (weights == null || !weights.isObject() || weights.isEmpty()) {
                    throw badRule("scoring_rule.weights 必须为对象且不能为空");
                }
                validateWeighted(weights);
            }
            case "THRESHOLD" -> {
                requireText(rule, "source");
                JsonNode threshold = rule.get("threshold");
                if (threshold == null || !threshold.isNumber() || threshold.asDouble() <= 0) {
                    throw badRule("scoring_rule.threshold 必须为大于 0 的数字");
                }
                JsonNode score = rule.get("score");
                if (score == null || !score.isNumber() || score.asDouble() <= 0 || score.asDouble() > 100) {
                    throw badRule("scoring_rule.score 必须为 (0, 100] 范围内的数字");
                }
            }
            case "COUNT" -> {
                requireText(rule, "source");
                JsonNode perUnit = rule.get("perUnit");
                if (perUnit == null || !perUnit.isNumber() || perUnit.asInt() <= 0) {
                    throw badRule("scoring_rule.perUnit 必须为大于 0 的整数");
                }
                JsonNode scorePerUnit = rule.get("scorePerUnit");
                if (scorePerUnit == null || !scorePerUnit.isNumber() || scorePerUnit.asDouble() <= 0) {
                    throw badRule("scoring_rule.scorePerUnit 必须为大于 0 的数字");
                }
            }
            default -> throw badRule("不支持的计分规则类型: " + type);
        }
    }

    /** 校验 WEIGHTED 的 weights：各维度权重之和必须为 1，维度编码必须在 ability_dimensions 中存在 */
    private void validateWeighted(JsonNode weights) {
        Set<String> dimensionCodes = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode entry : weights.properties().stream().map(e -> e.getValue()).toList()) {
            if (!entry.isNumber()) {
                throw badRule("scoring_rule.weights 各项必须为数字");
            }
            sum = sum.add(entry.decimalValue());
        }
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw badRule("scoring_rule.weights 各维度权重之和必须为 1，当前为 " + sum);
        }
        weights.fieldNames().forEachRemaining(dimensionCodes::add);
        if (!dimensionCodes.isEmpty()) {
            Set<String> existing = new HashSet<>();
            abilityDimensionRepository.findAllActive()
                    .forEach(d -> existing.add(d.getDimensionCode()));
            Set<String> unknown = new HashSet<>(dimensionCodes);
            unknown.removeAll(existing);
            if (!unknown.isEmpty()) {
                throw badRule("scoring_rule.weights 存在未知维度编码: " + unknown);
            }
        }
    }

    private void requireText(JsonNode rule, String field) {
        if (text(rule, field) == null) {
            throw badRule("scoring_rule." + field + " 不能为空");
        }
    }

    private String text(JsonNode rule, String field) {
        JsonNode node = rule.get(field);
        return (node != null && !node.isNull() && node.isTextual()) ? node.asText() : null;
    }

    private BusinessException badRule(String message) {
        log.warn("scoring_rule 校验失败: {}", message);
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }
}
