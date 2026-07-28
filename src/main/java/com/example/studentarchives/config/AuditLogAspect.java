package com.example.studentarchives.config;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.util.DateUtils;
import com.example.studentarchives.util.LogUtil;
import com.example.studentarchives.util.TraceIdUtil;
import com.example.studentarchives.util.SensitiveMasker;
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

    private final ObjectMapper objectMapper;

    /** SpEL 解析器（线程安全，可复用） */
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();

            boolean success = true;
            String errorMsg = null;
            // 对于 ApiResult 响应，判断业务是否成功
            if (result instanceof ApiResult<?> apiResult) {
                success = apiResult.isSuccess();
                if (!success) {
                    errorMsg = apiResult.getMessage();
                }
            }

            writeAuditLog(joinPoint, auditLog, start, success, errorMsg, result);
            return result;
        } catch (Throwable e) {
            // 记录失败的审计日志
            writeAuditLog(joinPoint, auditLog, start, false, e.getMessage(), null);
            throw e;
        }
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
            description = SensitiveMasker.maskString(parseSpel(description, paramNames, args));
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("trace_id", TraceIdUtil.getOrCreate());
        entry.put("timestamp", DateUtils.nowFull());
        entry.put("module", auditLog.module());
        entry.put("action", auditLog.action());
        entry.put("description", description);
        entry.put("cost_ms", costMs);
        entry.put("success", success);

        if (auditLog.logParams() && args != null) {
            Map<String, Object> params = new LinkedHashMap<>();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                params.put(paramNames[i], args[i]);
            }
            entry.put("params", SensitiveMasker.maskParamMap(params, objectMapper));
        }

        if (auditLog.logResult() && result != null) {
            try {
                entry.put("result", objectMapper.writeValueAsString(SensitiveMasker.maskObject(result, objectMapper)));
            } catch (Exception e) {
                entry.put("result", "[序列化失败]");
            }
        }

        if (errorMsg != null) {
            entry.put("error", SensitiveMasker.maskString(errorMsg));
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
            SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                    .withRootObject(args)
                    .build();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            return SPEL_PARSER.parseExpression(template).getValue(context, String.class);
        } catch (Exception e) {
            return template;
        }
    }

}
