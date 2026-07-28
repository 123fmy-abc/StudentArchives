package com.example.studentarchives.config;

import com.example.studentarchives.support.IpAddressExtractor;
import com.example.studentarchives.util.DateUtils;
import com.example.studentarchives.util.LogUtil;
import com.example.studentarchives.util.TraceIdUtil;
import com.example.studentarchives.util.SensitiveMasker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 请求日志切面
 * <p>
 * 自动记录所有 Controller 请求的：HTTP 方法、URL、参数、响应状态码、耗时。
 * 输出到 api-access.log 文件，JSON 格式一行一条。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final ObjectMapper objectMapper;
    private final IpAddressExtractor ipAddressExtractor;

    /** 慢请求告警阈值（毫秒），超过此阈值记录 WARN 日志，可通过配置调整 */
    @Value("${api.slow-request-threshold:3000}")
    private long slowRequestThresholdMs;

    /** 匹配所有 Controller 方法 */
    @Around("execution(* com.example.studentarchives.controller..*.*(..))")
    public Object logApiAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String traceId = TraceIdUtil.getOrCreate();

        // 获取请求上下文
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (attrs != null) ? attrs.getRequest() : null;
        if (request != null) {
            request.setAttribute("trace_id", traceId);
        }

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            int status = (attrs != null && attrs.getResponse() != null) ? attrs.getResponse().getStatus() : 200;
            logRequest(request, joinPoint, cost, status, null);

            // 慢请求告警（超过阈值，可通过配置调整）
            if (cost > slowRequestThresholdMs && request != null) {
                LogUtil.business().warn("[慢请求] {} {} 耗时={}ms", request.getMethod(), request.getRequestURI(), cost);
            }

            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            logRequest(request, joinPoint, cost, 500, e.getMessage());
            throw e;
        } finally {
            TraceIdUtil.clear();
        }
    }

    private void logRequest(HttpServletRequest request, ProceedingJoinPoint joinPoint,
                            long costMs, int status, String error) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("trace_id", TraceIdUtil.getOrCreate());
        entry.put("timestamp", DateUtils.nowFull());
        entry.put("method", request != null ? request.getMethod() : "UNKNOWN");
        entry.put("path", request != null ? request.getRequestURI() : "UNKNOWN");
        entry.put("query", request != null ? SensitiveMasker.maskQueryString(request.getQueryString()) : null);
        entry.put("status", status);
        entry.put("cost_ms", costMs);
        entry.put("controller", joinPoint.getSignature().getDeclaringTypeName());
        entry.put("action", joinPoint.getSignature().getName());

        // 客户端 IP（统一通过 IpAddressExtractor 提取，确保与认证层一致）
        if (request != null) {
            entry.put("client_ip", ipAddressExtractor.extract(request));
        }

        if (error != null) {
            entry.put("error", error);
        }

        try {
            LogUtil.apiAccess().info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            LogUtil.apiAccess().info("api-access-log-err: method={}, path={}, cost={}ms",
                    request != null ? request.getMethod() : "?",
                    request != null ? request.getRequestURI() : "?",
                    costMs);
        }
    }
}
