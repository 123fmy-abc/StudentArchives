package com.example.studentarchives.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 日志敏感信息脱敏工具类
 * <p>
 * 对日志中常见的敏感字段（密码、Token、身份证、手机号、邮箱等）进行统一脱敏，
 * 同时把换行符替换为可见字符，防止日志注入/伪造。
 */
@UtilityClass
public class SensitiveMasker {

    private static final int DEFAULT_MAX_LENGTH = 500;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "pwd", "oldpassword", "newpassword", "confirmpassword",
            "token", "accesstoken", "refreshtoken", "authorization", "authcode",
            "secret", "jwt", "apikey", "api_key", "apisecret", "api_secret",
            "idcard", "id_card", "identitycard", "identity_card",
            "phone", "mobile", "tel", "telephone", "email", "mail"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?<=.{1})[^@]+(?=@)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{10}(\\w{4})");

    private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([?&])(token|password|pwd|secret|authorization|apikey|api_key|apisecret|api_secret|jwt)=([^&]*)");

    /**
     * 对 Map 类型的参数按 key 脱敏（仅处理 String 和简单嵌套 Map）
     */
    public static Map<String, Object> maskMap(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            masked.put(key, maskValue(key, value));
        }
        return masked;
    }

    /**
     * 对参数 Map 按 key 脱敏，并对复杂对象递归脱敏（推荐用于审计日志）
     */
    public static Map<String, Object> maskParamMap(Map<String, Object> source, ObjectMapper objectMapper) {
        if (source == null) {
            return null;
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            masked.put(key, isSensitiveKey(key) ? "****" : maskObject(value, objectMapper));
        }
        return masked;
    }

    /**
     * 根据 key 判断是否需要脱敏 value（简单类型）
     */
    public static Object maskValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key)) {
            return "****";
        }
        if (value instanceof String s) {
            return maskString(s);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String k = e.getKey() == null ? null : e.getKey().toString();
                typed.put(k, maskValue(k, e.getValue()));
            }
            return typed;
        }
        return value;
    }

    /**
     * 对任意对象递归脱敏（基于 Jackson JsonNode）
     */
    public static JsonNode maskObject(Object value, ObjectMapper objectMapper) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.valueToTree(value);
            return maskNode(node);
        } catch (Exception e) {
            // 转换失败时退化为字符串脱敏
            return TextNode.valueOf(maskString(String.valueOf(value)));
        }
    }

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间 4 位用 **** 代替
     * <pre>13812345678 → 138****5678</pre>
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /**
     * 邮箱脱敏：根据 @ 前字符数动态保留前缀
     * <ul>
     *   <li>1 字符（a@qq.com）       → a***@qq.com</li>
     *   <li>2 字符（ja@163.com）     → ja***@163.com</li>
     *   <li>3+ 字符（zhangsan@...）  → zha***@outlook.com</li>
     * </ul>
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // 异常格式：@qq.com → ***@qq.com
            return "***" + email;
        }
        int keepCount = Math.min(atIndex, 3);
        return email.substring(0, keepCount) + "***" + email.substring(atIndex);
    }

    /**
     * 对字符串内容进行手机号、邮箱、身份证等模式脱敏，并清理换行符
     */
    public static String maskString(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String masked = value;
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("****");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll("$1**********$2");
        return truncate(sanitize(masked), DEFAULT_MAX_LENGTH);
    }

    /**
     * 对 URL 查询字符串中的敏感参数进行脱敏
     */
    public static String maskQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }
        return QUERY_PARAM_PATTERN.matcher(queryString).replaceAll("$1$2=****");
    }

    /**
     * 判断 key 是否为敏感字段
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized);
    }

    /**
     * 截断超长字符串，避免单条日志过大
     * 使用 offsetByCodePoints 确保不拆散 Unicode 代理对（如 emoji）
     */
    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, maxLength);
        // 保险：offsetByCodePoints 可能返回超出 length 的值
        endIndex = Math.min(endIndex, value.length());
        return value.substring(0, endIndex) + "...";
    }

    /**
     * 清理字符串中的换行、回车、制表符，防止日志注入
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static JsonNode maskNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode result = objectNode.objectNode();
            objectNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSensitiveKey(key)) {
                    result.set(key, TextNode.valueOf("****"));
                } else {
                    result.set(key, maskNode(value));
                }
            });
            return result;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode result = arrayNode.arrayNode();
            for (JsonNode element : arrayNode) {
                result.add(maskNode(element));
            }
            return result;
        }
        if (node.isTextual()) {
            return TextNode.valueOf(maskString(node.asText()));
        }
        return node;
    }
}
