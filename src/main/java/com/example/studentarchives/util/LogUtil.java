package com.example.studentarchives.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志工具类
 * <p>
 * 提供按类别获取 Logger 的统一入口，确保 logback-spring.xml 中 Logger 名称一致。
 * Trace ID 相关操作已拆分至 {@link TraceIdUtil}，遵循单一职责原则。
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

    /**
     * 生成 trace ID
     * @deprecated 请使用 {@link TraceIdUtil#generate()}
     */
    @Deprecated(since = "2.0")
    public static String generateTraceId() {
        return TraceIdUtil.generate();
    }

    /**
     * 从 MDC 获取或创建 trace ID
     * @deprecated 请使用 {@link TraceIdUtil#getOrCreate()}
     */
    @Deprecated(since = "2.0")
    public static String getOrCreateTraceId() {
        return TraceIdUtil.getOrCreate();
    }

    /**
     * 设置 trace_id
     * @deprecated 请使用 {@link TraceIdUtil#set(String)}
     */
    @Deprecated(since = "2.0")
    public static void setTraceId(String traceId) {
        TraceIdUtil.set(traceId);
    }

    /**
     * 清除 MDC 中的 trace_id
     * @deprecated 请使用 {@link TraceIdUtil#clear()}
     */
    @Deprecated(since = "2.0")
    public static void clearTraceId() {
        TraceIdUtil.clear();
    }

    /** 生成时间戳：2026-07-08 14:30:25.123 */
    public static String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
}
