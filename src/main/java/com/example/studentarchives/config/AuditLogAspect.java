package com.example.studentarchives.config;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.entity.log.SystemLog;
import com.example.studentarchives.service.Fmy.AdminAuthService;
import com.example.studentarchives.service.Fmy.SystemLogService;
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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 操作审计日志切面
 * <p>
 * 拦截 &#064;AuditLog 注解，记录用户操作：
 * <ol>
 *   <li>写入 audit.log 文件（文件日志，便于离线排查）；</li>
 *   <li>写入 system_logs 表持久化（供管理端 GET /admin/logs/system 查询，日志级别 3=审计）。</li>
 * </ol>
 * 落库在 SystemLogService 内以 REQUIRES_NEW 独立事务执行并吞掉异常，
 * 日志写入失败不影响被审计的业务调用。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 日志级别：3=审计日志（V11 system_logs.log_level 注释） */
    private static final int LOG_LEVEL_AUDIT = 3;

    /** 0=禁止普通用户删除 */
    private static final int IS_DELETABLE_FALSE = 0;

    /** 0=仅后台展示 */
    private static final int IS_DISPLAY_FALSE = 0;

    /** 数据保留期（天） */
    private static final int RETENTION_DAYS = 180;

    /** 匿名/系统操作的 operator_id 哨兵（满足 ck_sl_user_or_operator 约束，且无外键引用） */
    private static final long ANONYMOUS_OPERATOR_ID = 0L;

    private final ObjectMapper objectMapper;
    private final IpAddressExtractor ipAddressExtractor;
    private final SystemLogService systemLogService;
    private final AdminAuthService adminAuthService;

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
        // 记录操作人（登录等公开接口可能无认证上下文）
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long principalId = principal != null && !"anonymousUser".equals(principal.toString())
                ? (Long) principal : null;
        entry.put("user_id", principalId);
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

        String resultJson = null;
        if (auditLog.logResult() && result != null) {
            try {
                resultJson = objectMapper.writeValueAsString(SensitiveMasker.maskObject(result, objectMapper));
                entry.put("result", resultJson);
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

        // 写入 system_logs 表持久化（独立事务，失败不影响业务）
        persistSystemLog(auditLog, description, resultJson, success, principalId);
    }

    /** 构建并持久化 system_logs 记录 */
    private void persistSystemLog(AuditLog auditLog, String description, String resultJson,
                                  boolean success, Long principalId) {
        HttpServletRequest request = currentRequest();
        String ipAddress = request != null ? ipAddressExtractor.extract(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        // 登录/找回密码等公开接口无认证上下文：用 0 哨兵作为 operator_id，
        // 满足 system_logs 的 CHECK(user_id IS NOT NULL OR operator_id IS NOT NULL) 约束。
        Long operatorId = principalId != null ? principalId : ANONYMOUS_OPERATOR_ID;

        AdminAuthService.OperatorRole operatorRole = principalId != null
                ? adminAuthService.resolveOperatorRole(principalId) : null;

        SystemLog systemLog = new SystemLog();
        systemLog.setUserId(principalId);
        systemLog.setOperatorId(operatorId);
        systemLog.setRoleId(operatorRole != null ? operatorRole.roleId() : null);
        systemLog.setRoleName(operatorRole != null ? operatorRole.roleName() : null);
        systemLog.setModule(auditLog.module());
        systemLog.setAction(auditLog.action());
        systemLog.setDescription(description);
        systemLog.setAfterData(resultJson);
        systemLog.setLogLevel(LOG_LEVEL_AUDIT);
        systemLog.setIsDeletable(IS_DELETABLE_FALSE);
        systemLog.setIsDisplay(IS_DISPLAY_FALSE);
        systemLog.setIpAddress(ipAddress);
        systemLog.setUserAgent(userAgent);
        systemLog.setRetentionUntil(LocalDateTime.now().plusDays(RETENTION_DAYS));
        systemLogService.recordSystemLog(systemLog);
    }

    /** 当前请求（异步/无请求上下文时为 null） */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
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
