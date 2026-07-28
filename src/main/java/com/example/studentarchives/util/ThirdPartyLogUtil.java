package com.example.studentarchives.util;

import com.example.studentarchives.util.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方接口调用日志工具
 */
public class ThirdPartyLogUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录第三方接口调用日志（成功）
     */
    public static void log(String service, String api, long costMs, boolean success,
                           Map<String, Object> params, Object result) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("service", service);
        entry.put("api", api);
        entry.put("cost_ms", costMs);
        entry.put("success", success);
        entry.put("params", maskSensitive(params));
        if (result != null) {
            entry.put("result", "[已记录]");
        }
        try {
            LogUtil.thirdParty().info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            LogUtil.thirdParty().info("third-party-log: service={}, api={}, cost={}ms", service, api, costMs);
        }
    }

    /**
     * 记录第三方接口调用日志（失败）
     */
    public static void logError(String service, String api, long costMs,
                                Map<String, Object> params, String errorMsg) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("service", service);
        entry.put("api", api);
        entry.put("cost_ms", costMs);
        entry.put("success", false);
        entry.put("error", errorMsg);
        entry.put("params", maskSensitive(params));
        try {
            LogUtil.thirdParty().info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            LogUtil.thirdParty().info("third-party-log-err: service={}, api={}, cost={}ms", service, api, costMs);
        }
    }

    /** 简单脱敏（密码、token 等敏感信息） */
    private static Object maskSensitive(Map<String, Object> params) {
        if (params == null) return null;
        Map<String, Object> masked = new LinkedHashMap<>(params);
        for (String key : new String[]{"password", "token", "secret", "authorization"}) {
            if (masked.containsKey(key)) {
                masked.put(key, "****");
            }
        }
        return masked;
    }
}
