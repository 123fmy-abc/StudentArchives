package com.example.studentarchives.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志工具类
 * <p>
 * 提供按类别获取 Logger 的统一入口，确保 logback-spring.xml 中 Logger 名称一致。
 */
@UtilityClass
public class LogUtil {

    /** 业务日志 */
    public static Logger business() {
        return LoggerFactory.getLogger("com.example.studentarchives");
    }

    /** API 请求日志 */
    public static Logger apiAccess() {
        return LoggerFactory.getLogger("API_ACCESS_LOGGER");
    }

    /** 操作审计日志 */
    public static Logger audit() {
        return LoggerFactory.getLogger("AUDIT_LOGGER");
    }

    /** 第三方接口日志 */
    public static Logger thirdParty() {
        return LoggerFactory.getLogger("THIRD_PARTY_LOGGER");
    }

    /** 性能日志 */
    public static Logger performance() {
        return LoggerFactory.getLogger("PERFORMANCE_LOGGER");
    }

    /** 生成 trace ID：req-20260708-143025-abc123 */
    public static String generateTraceId() {
        return "req-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /** 从 MDC 获取当前 trace_id，不存在则生成并写入 MDC */
    public static String getOrCreateTraceId() {
        String traceId = MDC.get("trace_id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            MDC.put("trace_id", traceId);
        }
        return traceId;
    }

    /** 手动设置 trace_id 到 MDC */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put("trace_id", traceId);
        }
    }

    /** 清除 MDC 中的 trace_id */
    public static void clearTraceId() {
        MDC.remove("trace_id");
    }

    /** 生成时间戳：2026-07-08 14:30:25.123 */
    public static String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
}
