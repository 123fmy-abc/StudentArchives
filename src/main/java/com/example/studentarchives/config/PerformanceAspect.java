package com.example.studentarchives.config;

import com.example.studentarchives.annotation.PerfLog;
import com.example.studentarchives.util.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 性能日志切面
 * <p>
 * 拦截 &#064;PerfLog 注解，记录方法执行耗时。
 * 超过 warnMs 阈值输出 WARN 级别告警到 business.log 和 performance.log。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceAspect {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final ObjectMapper objectMapper;

    @Around("@annotation(perfLog)")
    public Object around(ProceedingJoinPoint joinPoint, PerfLog perfLog) throws Throwable {
        long start = System.nanoTime();

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            logPerf(joinPoint, perfLog, costMs, false);
            throw e;
        }

        long costMs = (System.nanoTime() - start) / 1_000_000;
        logPerf(joinPoint, perfLog, costMs, true);

        // 超过阈值，输出业务告警
        if (costMs > perfLog.warnMs()) {
            String label = perfLog.label().isEmpty() ? joinPoint.getSignature().getName() : perfLog.label();
            LogUtil.business().warn("[性能告警] {} 耗时={}ms (阈值={}ms)", label, costMs, perfLog.warnMs());
        }

        return result;
    }

    private void logPerf(ProceedingJoinPoint joinPoint, PerfLog perfLog, long costMs, boolean success) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String label = perfLog.label().isEmpty()
                ? signature.getDeclaringTypeName() + "." + signature.getName()
                : perfLog.label();

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("trace_id", LogUtil.getOrCreateTraceId());
        entry.put("timestamp", LocalDateTime.now().format(DTF));
        entry.put("label", label);
        entry.put("cost_ms", costMs);
        entry.put("threshold_ms", perfLog.warnMs());
        entry.put("slow", costMs > perfLog.warnMs());
        entry.put("success", success);

        try {
            LogUtil.performance().info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            LogUtil.performance().info("perf-log: label={}, cost={}ms", label, costMs);
        }
    }
}
