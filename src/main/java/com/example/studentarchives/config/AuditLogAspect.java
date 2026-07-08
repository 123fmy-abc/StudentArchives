package com.example.studentarchives.config;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.util.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 操作审计日志切面
 * <p>
 * 拦截 &#064;AuditLog 注解，记录用户操作到 audit.log。
 * 同时写入 system_logs 表持久化。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 记录失败的审计日志
            writeAuditLog(joinPoint, auditLog, start, false, e.getMessage(), null);
            throw e;
        }

        boolean success = true;
        // 对于 ApiResult 响应，判断业务是否成功
        if (result instanceof ApiResult<?> apiResult) {
            success = apiResult.isSuccess();
        }

        String errorMsg = null;
        if (!success && result instanceof ApiResult<?> apiResult) {
            errorMsg = apiResult.getMessage();
        }

        writeAuditLog(joinPoint, auditLog, start, success, errorMsg, result);
        return result;
    }

    private void writeAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                               long start, boolean success, String errorMsg, Object result) {
        long costMs = System.currentTimeMillis() - start;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 解析 SpEL 描述
        String description = auditLog.description();
        if (description.isEmpty()) {
            description = auditLog.action();
        } else {
            description = parseSpel(description, paramNames, args);
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("trace_id", LogUtil.getOrCreateTraceId());
        entry.put("timestamp", LocalDateTime.now().format(DTF));
        entry.put("module", auditLog.module());
        entry.put("action", auditLog.action());
        entry.put("description", description);
        entry.put("cost_ms", costMs);
        entry.put("success", success);

        if (auditLog.logParams() && args != null) {
            Map<String, Object> params = new LinkedHashMap<>();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                params.put(paramNames[i], sanitize(args[i]));
            }
            entry.put("params", params);
        }

        if (auditLog.logResult() && result != null) {
            try {
                entry.put("result", sanitize(objectMapper.writeValueAsString(result)));
            } catch (Exception e) {
                entry.put("result", "[序列化失败]");
            }
        }

        if (errorMsg != null) {
            entry.put("error", errorMsg);
        }

        try {
            LogUtil.audit().info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            LogUtil.audit().info("audit-log-err: module={}, action={}", auditLog.module(), auditLog.action());
        }
    }

    /** 简单 SpEL 解析：将 #paramName 替换为实际参数值（只读上下文，禁止方法调用） */
    private String parseSpel(String template, String[] paramNames, Object[] args) {
        if (paramNames == null || args == null) return template;
        try {
            ExpressionParser parser = new SpelExpressionParser();
            SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                    .withRootObject(args)
                    .build();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            return parser.parseExpression(template).getValue(context, String.class);
        } catch (Exception e) {
            return template;
        }
    }

    /** 脱敏 */
    private Object sanitize(Object obj) {
        if (obj instanceof String s) {
            if (s.length() > 500) return s.substring(0, 500) + "...";
        }
        return obj;
    }
}
