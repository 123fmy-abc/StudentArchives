package com.example.studentarchives.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方接口调用日志工具
 * <p>
 * 被 ThirdPartyAspect 调用，记录第三方接口调用的请求参数和耗时。
 * 不依赖 ObjectMapper 注入：由调用方传入，避免静态代理的脆弱性。
 */
@Component
public class ThirdPartyLogUtil {

    private final ObjectMapper objectMapper;

    public ThirdPartyLogUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 记录第三方接口调用日志（成功）
     */
    public void log(String service, String api, long costMs, boolean success,
                    Map<String, Object> params, Object result) {
        Map<String, Object> entry = buildEntry(service, api, costMs, success, params, null);
        if (result != null) {
            entry.put("result", "[已记录]");
        }
        writeLog(entry);
    }

    /**
     * 记录第三方接口调用日志（失败）
     */
    public void logError(String service, String api, long costMs,
                         Map<String, Object> params, String errorMsg) {
        Map<String, Object> entry = buildEntry(service, api, costMs, false, params, errorMsg);
        writeLog(entry);
    }

    private Map<String, Object> buildEntry(String service, String api, long costMs,
                                           boolean success, Map<String, Object> params,
                                           String errorMsg) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("service", service);
        entry.put("api", api);
        entry.put("cost_ms", costMs);
        entry.put("success", success);
        entry.put("params", SensitiveMasker.maskParamMap(params, objectMapper));
        if (errorMsg != null) {
            entry.put("error", SensitiveMasker.maskString(errorMsg));
        }
        return entry;
    }

    private void writeLog(Map<String, Object> entry) {
        try {
            LogUtil.thirdParty().info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            LogUtil.thirdParty().info("third-party-log-err: service={}, api={}, cost={}ms",
                    entry.get("service"), entry.get("api"), entry.get("cost_ms"));
        }
    }
}
