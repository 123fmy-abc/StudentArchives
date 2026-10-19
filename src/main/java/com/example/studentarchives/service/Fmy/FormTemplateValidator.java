package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.foundation.FormTemplate;
import com.example.studentarchives.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 表单模板运行时校验器（学生端/教师端提交时）
 * <p>
 * 按模板 {@code fields} 逐条校验提交请求 DTO：{@code required} / {@code type} /
 * {@code options} 白名单（select/radio 单选、checkbox 子集）/ {@code rules}
 * （minLength/maxLength/min/max/pattern）。取值统一走 {@link BeanWrapperImpl} 在 DTO 实例上
 * {@code getPropertyValue(key)}（key = DTO 属性小写驼峰名），无需为各档案类型维护字段映射。
 * <p>
 * {@code visible} 联动首版不支持，仅透传不校验。校验失败统一抛
 * {@link BusinessException}({@code 10001 参数错误})。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateValidator {

    private final ObjectMapper objectMapper;

    /**
     * 按模板校验提交请求 DTO 实例。
     *
     * @param template    生效模板（其 fields 为 JSON 字符串）
     * @param dtoInstance 提交请求 DTO 实例（如 {@code ApplicationService.CompetitionSubmitRequest}）
     */
    public void validate(FormTemplate template, Object dtoInstance) {
        if (template == null || dtoInstance == null) {
            return;
        }
        JsonNode fields = parseFields(template.getFields());
        if (fields == null || !fields.isArray()) {
            return;
        }
        for (JsonNode field : fields) {
            validateField(field, dtoInstance);
        }
    }

    private void validateField(JsonNode field, Object dtoInstance) {
        String key = text(field, "key");
        if (key == null || key.isBlank()) {
            return;
        }
        String label = text(field, "label");
        String display = (label != null && !label.isBlank()) ? label : key;
        String type = text(field, "type");
        Object value = readValue(dtoInstance, key);

        // required 校验
        if (field.path("required").asBoolean(false) && isBlank(value)) {
            throw bad(display + " 不能为空");
        }
        if (value == null) {
            return;
        }

        // type 校验（按 Java 类型分支）
        validateType(type, display, value);

        // options 白名单（select/radio 单选、checkbox 子集）
        validateOptions(type, display, value, field.get("options"));

        // rules 校验
        validateRules(display, value, field.get("rules"));
    }

    private void validateType(String type, String display, Object value) {
        if (type == null) {
            return;
        }
        switch (type) {
            case "number" -> {
                if (!(value instanceof Number) && !isNumericString(value)) {
                    throw bad(display + " 必须为数字");
                }
            }
            case "date" -> {
                if (!(value instanceof LocalDate) && !(value instanceof LocalDateTime)
                        && !(value instanceof Temporal)) {
                    throw bad(display + " 必须为日期");
                }
            }
            case "checkbox" -> {
                if (!(value instanceof Collection<?>)) {
                    throw bad(display + " 必须为多选数组");
                }
            }
            default -> {
                // input/textarea/select/radio/upload/switch 不做严格类型约束
            }
        }
    }

    private void validateOptions(String type, String display, Object value, JsonNode options) {
        boolean supportOptions = "select".equals(type) || "radio".equals(type) || "checkbox".equals(type);
        if (!supportOptions || options == null || !options.isArray()) {
            return;
        }
        Set<String> allowed = new HashSet<>();
        for (JsonNode opt : options) {
            if (opt.isObject()) {
                JsonNode v = opt.get("value");
                if (v != null && !v.isNull()) {
                    allowed.add(v.asText());
                }
            } else if (!opt.isNull()) {
                allowed.add(opt.asText());
            }
        }
        if (allowed.isEmpty()) {
            return;
        }
        if ("checkbox".equals(type)) {
            if (!(value instanceof Collection<?> values)) {
                throw bad(display + " 必须为多选数组");
            }
            for (Object v : values) {
                if (v == null || !allowed.contains(String.valueOf(v))) {
                    throw bad(display + " 存在不合法取值: " + v + "，允许取值: " + allowed);
                }
            }
        } else {
            if (!allowed.contains(String.valueOf(value))) {
                throw bad(display + " 取值不合法: " + value + "，允许取值: " + allowed);
            }
        }
    }

    private void validateRules(String display, Object value, JsonNode rules) {
        if (rules == null || !rules.isObject()) {
            return;
        }
        if (value instanceof CharSequence cs) {
            String s = cs.toString();
            if (rules.hasNonNull("minLength") && s.length() < rules.get("minLength").asInt()) {
                throw bad(display + " 长度不能小于 " + rules.get("minLength").asInt());
            }
            if (rules.hasNonNull("maxLength") && s.length() > rules.get("maxLength").asInt()) {
                throw bad(display + " 长度不能超过 " + rules.get("maxLength").asInt());
            }
            if (rules.hasNonNull("pattern")) {
                String pattern = rules.get("pattern").asText();
                if (!s.matches(pattern)) {
                    throw bad(display + " 格式不正确");
                }
            }
        }
        if (value instanceof Number num) {
            BigDecimal bd = new BigDecimal(num.toString());
            if (rules.hasNonNull("min") && bd.compareTo(new BigDecimal(rules.get("min").asText())) < 0) {
                throw bad(display + " 不能小于 " + rules.get("min").asText());
            }
            if (rules.hasNonNull("max") && bd.compareTo(new BigDecimal(rules.get("max").asText())) > 0) {
                throw bad(display + " 不能大于 " + rules.get("max").asText());
            }
        }
    }

    // ==================== 辅助方法 ====================

    private JsonNode parseFields(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("模板 fields 解析失败: {}", json, e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "表单模板字段配置不合法");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        return (child != null && child.isTextual()) ? child.asText() : null;
    }

    /** 用 BeanWrapperImpl 在 DTO 实例上取属性值；属性不存在时返回 null */
    private Object readValue(Object dtoInstance, String key) {
        try {
            return new BeanWrapperImpl(dtoInstance).getPropertyValue(key);
        } catch (BeansException e) {
            return null;
        }
    }

    private boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence cs) {
            return cs.toString().isBlank();
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        return false;
    }

    private boolean isNumericString(Object value) {
        if (!(value instanceof CharSequence cs)) {
            return false;
        }
        try {
            new BigDecimal(cs.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }
}
