package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 通用 JSON 字段校验器
 * <p>
 * 对应《学生档案系统表》中多个 JSON 字段（scoring_rule、tree_snapshot、
 * form_templates.fields、model_versions.data_snapshot 等）的应用层校验兜底。
 * 校验失败统一返回 {@code 10001 参数错误}，错误信息包含字段路径，便于前端定位。
 * <p>
 * 本校验器不替代数据库 JSON 类型约束，仅在业务写入前做结构性校验，防止非法 JSON
 * 进入表字段导致后续反序列化/计算失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonSchemaValidator {

    private final ObjectMapper objectMapper;

    /** 合法 JSON 字符串校验：必须是对象或数组，且能解析 */
    public void validateJson(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject() && !node.isArray()) {
                throw bad(fieldName, "必须为 JSON 对象或数组");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("{} 不是合法 JSON: {}", fieldName, json, e);
            throw bad(fieldName, "不是合法 JSON");
        }
    }

    /** 校验 JSON 字符串必须是对象 */
    public JsonNode requireObject(String json, String fieldName) {
        JsonNode node = read(json, fieldName);
        if (!node.isObject()) {
            throw bad(fieldName, "必须为 JSON 对象");
        }
        return node;
    }

    /** 校验 JSON 字符串必须是数组 */
    public JsonNode requireArray(String json, String fieldName) {
        JsonNode node = read(json, fieldName);
        if (!node.isArray()) {
            throw bad(fieldName, "必须为 JSON 数组");
        }
        return node;
    }

    /** 校验对象必填字段 */
    public void requireFields(JsonNode node, String fieldName, String... requiredFields) {
        for (String required : requiredFields) {
            if (!node.hasNonNull(required) || node.get(required).isNull()) {
                throw bad(fieldName, "缺少必填字段: " + required);
            }
        }
    }

    /** 校验字段值类型：必须是文本 */
    public void requireText(JsonNode node, String fieldName, String childField) {
        JsonNode child = node.get(childField);
        if (child == null || child.isNull() || !child.isTextual()) {
            throw bad(fieldName, "." + childField + " 必须为文本");
        }
    }

    /** 校验字段值类型：必须是数字 */
    public void requireNumber(JsonNode node, String fieldName, String childField) {
        JsonNode child = node.get(childField);
        if (child == null || child.isNull() || !child.isNumber()) {
            throw bad(fieldName, "." + childField + " 必须为数字");
        }
    }

    /** 校验字段值类型：必须是布尔 */
    public void requireBoolean(JsonNode node, String fieldName, String childField) {
        JsonNode child = node.get(childField);
        if (child == null || child.isNull() || !child.isBoolean()) {
            throw bad(fieldName, "." + childField + " 必须为布尔值");
        }
    }

    /** 校验字段值必须在指定枚举范围内 */
    public void requireEnum(JsonNode node, String fieldName, String childField, Collection<String> allowed) {
        JsonNode child = node.get(childField);
        if (child == null || child.isNull() || !child.isTextual()) {
            throw bad(fieldName, "." + childField + " 不能为空");
        }
        Set<String> allowedSet = new HashSet<>(allowed);
        String value = child.asText();
        if (!allowedSet.contains(value)) {
            throw bad(fieldName, "." + childField + " 取值不合法: " + value
                    + "，允许取值: " + allowedSet);
        }
    }

    /** 校验数组非空 */
    public void requireNonEmptyArray(JsonNode node, String fieldName, String childField) {
        JsonNode child = node.get(childField);
        if (child == null || child.isNull() || !child.isArray() || child.isEmpty()) {
            throw bad(fieldName, "." + childField + " 必须为非空数组");
        }
    }

    /** 校验对象中仅允许出现指定字段（防止脏字段入库） */
    public void allowOnlyFields(JsonNode node, String fieldName, String... allowedFields) {
        if (!node.isObject()) {
            return;
        }
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedFields));
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            if (!allowed.contains(key)) {
                throw bad(fieldName, " 包含非法字段: " + key);
            }
        }
    }

    /** 使用自定义断言校验节点 */
    public void check(JsonNode node, String fieldName, Predicate<JsonNode> predicate, String message) {
        if (!predicate.test(node)) {
            throw bad(fieldName, message);
        }
    }

    private JsonNode read(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            throw bad(fieldName, "不能为空");
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("{} 解析失败: {}", fieldName, json, e);
            throw bad(fieldName, "JSON 解析失败");
        }
    }

    private BusinessException bad(String fieldName, String message) {
        String fullMessage = fieldName + message;
        log.warn("JSON 字段校验失败: {}", fullMessage);
        return new BusinessException(ResultCode.PARAM_ERROR, fullMessage);
    }
}
