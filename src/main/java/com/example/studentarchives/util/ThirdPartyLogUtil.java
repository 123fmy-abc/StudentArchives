package com.example.studentarchives.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 第三方接口调用日志工具类
 * <p>
 * 手动记录第三方 API 调用的请求/响应/耗时，无需 AOP 注解。
 * 适用于无法使用 &#064;ThirdPartyApi 注解的场景（如 RestTemplate 封装层）。
 * <pre>
 * ThirdPartyLogUtil.log("WeChat", "getAccessToken", 1200, true, paramMap, result);
 * </pre>
 */
@Slf4j
@UtilityClass
public class ThirdPartyLogUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 记录第三方调用
     *
     * @param service     服务名称
     * @param api         接口名称
     * @param costMs      耗时（毫秒）
     * @param success     是否成功
     * @param request     请求参数（自动脱敏）
     * @param response    响应结果；失败时可传入错误信息
     */
    public static void log(String service, String api, long costMs, boolean success,
                           Object request, Object response) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("trace_id", LogUtil.getOrCreateTraceId());
        entry.put("timestamp", LocalDateTime.now().format(DTF));
        entry.put("service", service);
        entry.put("api", api);
        entry.put("cost_ms", costMs);
        entry.put("success", success);
        entry.put("request", sanitizeObject(request));

        if (success) {
            entry.put("response", sanitizeObject(response));
        } else {
            entry.put("response", "ERROR");
            entry.put("error", response);
        }

        String logLine;
        try {
            logLine = MAPPER.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            logLine = "{\"service\":\"" + service + "\",\"api\":\"" + api + "\",\"cost_ms\":" + costMs + "}";
        }

        if (success) {
            LogUtil.thirdParty().info(logLine);
        } else {
            LogUtil.thirdParty().warn(logLine);
        }
    }

    /** 快速记录失败调用 */
    public static void logError(String service, String api, long costMs, Object request, String errorMsg) {
        log(service, api, costMs, false, request, errorMsg);
    }

    // ==================== 内部工具 ====================

    /** 递归脱敏对象/Map/集合中的敏感字段 */
    private static Object sanitizeObject(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (isSensitiveKey(key)) {
                        result.put(key, "***");
                    } else {
                        result.put(key, sanitizeObject(entry.getValue()));
                    }
                }
                return result;
            }
            if (obj instanceof Collection<?> collection) {
                return collection.stream().map(ThirdPartyLogUtil::sanitizeObject).collect(Collectors.toList());
            }
            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
                return obj;
            }
            // POJO 转为 Map 后递归脱敏
            Map<String, Object> map = MAPPER.convertValue(obj, new TypeReference<>() {});
            return sanitizeObject(map);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /** 脱敏 JSON 字符串中的敏感字段（忽略大小写） */
    public static String sanitize(String json) {
        if (json == null || json.isEmpty()) return json;
        return json.replaceAll("(?i)\"(password|token|secret|oldPassword|newPassword|accessToken|refreshToken|authorization|apiKey|apiSecret)\"\\s*:\\s*\"[^\"]*\"",
                "\"$1\":\"***\"");
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("token") || lower.contains("secret")
                || lower.contains("authorization") || lower.contains("apikey") || lower.contains("apisecret");
    }
}
