package com.example.studentarchives.util;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Trace ID 工具类
 * <p>
 * 负责 trace_id 的生成、读取、设置和清理。
 * 从 LogUtil 中拆分，遵循单一职责原则。
 */
@UtilityClass
public class TraceIdUtil {

    /** 生成 trace ID：req-20260708-143025-abc123 */
    public static String generate() {
        return "req-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /** 从 MDC 获取当前 trace_id，不存在则生成并写入 MDC */
    public static String getOrCreate() {
        String traceId = MDC.get("trace_id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = generate();
            MDC.put("trace_id", traceId);
        }
        return traceId;
    }

    /** 手动设置 trace_id 到 MDC */
    public static void set(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put("trace_id", traceId);
        }
    }

    /** 清除 MDC 中的 trace_id */
    public static void clear() {
        MDC.remove("trace_id");
    }
}
