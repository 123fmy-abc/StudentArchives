package com.example.studentarchives.config;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.entity.log.SystemLog;
import com.example.studentarchives.service.Lzw.AdminLogService;
import com.example.studentarchives.service.common.AdminAuthService;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作审计日志切面
 * <p>
 * 拦截 &#064;AuditLog 注解，记录用户操作并写入 audit.log 文件（便于离线排查），
 * 同时写入 system_logs 表（支撑管理端 GET /admin/logs/system 查询）。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 日志级别：审计 */
    private static final int LOG_LEVEL_AUDIT = 3;

    private final ObjectMapper objectMapper;
    private final AdminLogService adminLogService;
    private final AdminAuthService adminAuthService;

    /** SpEL 解析器（线程安全，可复用） */
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /** 匹配描述模板中的 SpEL 变量引用，如 #userId 或 #body.userNo（仅属性访问，不含方法调用） */
    private static final Pattern SPEL_REF_PATTERN =
            Pattern.compile("#([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)");

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

        // 操作人（登录等公开接口可能无认证上下文）
        Long principalId = resolvePrincipalId();

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("trace_id", TraceIdUtil.getOrCreate());
        entry.put("timestamp", DateUtils.nowFull());
        entry.put("user_id", principalId);
        entry.put("module", auditLog.module());
        entry.put("action", auditLog.action());
        entry.put("description", description);
        entry.put("cost_ms", costMs);
        entry.put("success", success);

        String paramsJson = null;
        if (auditLog.logParams() && args != null) {
            Map<String, Object> params = new LinkedHashMap<>();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                params.put(paramNames[i], args[i]);
            }
            Map<String, Object> maskedParams = SensitiveMasker.maskParamMap(params, objectMapper);
            entry.put("params", maskedParams);
            try {
                paramsJson = objectMapper.writeValueAsString(maskedParams);
            } catch (Exception ignored) {
                paramsJson = null;
            }
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

        // 写入 system_logs 表（恢复数据库审计写库）
        writeSystemLog(auditLog, principalId, description, success, errorMsg, paramsJson, resultJson);
    }

    /** 写库 system_logs（独立事务，失败不影响主流程） */
    private void writeSystemLog(AuditLog auditLog, Long principalId, String description,
                                boolean success, String errorMsg, String paramsJson, String resultJson) {
        try {
            // system_logs 表 CHECK 约束要求 user_id 或 operator_id 至少一个非空，匿名操作不落库
            if (principalId == null) {
                return;
            }
            SystemLog systemLog = new SystemLog();
            systemLog.setOperatorId(principalId);
            AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(principalId);
            if (role != null) {
                systemLog.setRoleId(role.roleId());
                systemLog.setRoleName(role.roleName());
            }
            systemLog.setModule(auditLog.module());
            systemLog.setAction(auditLog.action());
            systemLog.setDescription(description);
            systemLog.setLogLevel(LOG_LEVEL_AUDIT);
            systemLog.setIsDeletable(1);
            systemLog.setIsDisplay(1);
            systemLog.setBeforeData(paramsJson);
            systemLog.setAfterData(resultJson);
            systemLog.setStatus(success ? 1 : 0);
            systemLog.setStatusLabel(success ? "成功" : "失败");
            systemLog.setIpAddress(getClientIp());
            systemLog.setUserAgent(getUserAgent());
            if (errorMsg != null) {
                systemLog.setDescription(description + "；失败原因：" + SensitiveMasker.maskString(errorMsg));
            }
            adminLogService.recordSystemLog(systemLog);
        } catch (Exception e) {
            // 数据库日志写入失败不影响主流程
            LogUtil.audit().info("system-log-err: module={}, action={}, error={}",
                    auditLog.module(), auditLog.action(), e.getMessage());
        }
    }

    /** 从安全上下文解析当前操作人 ID（匿名返回 null） */
    private Long resolvePrincipalId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        if (principal != null && !"anonymousUser".equals(principal.toString())) {
            return (Long) principal;
        }
        return null;
    }

    /** 获取客户端 IP（支持 X-Forwarded-For / X-Real-IP 反向代理） */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取客户端 User-Agent */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 简单 SpEL 模板解析：将描述模板中的 {@code #paramName} / {@code #paramName.field}
     * 逐个替换为实际参数值（只读上下文，禁止方法调用）。
     * <p>
     * 与「整段模板当作单个 SpEL 表达式」不同，这里按变量引用逐个求值并替换，
     * 从而支持「中文文本 + #变量」的混合描述，如 {@code 创建用户: #body.userNo}。
     * 解析失败的引用保留原样，不影响其余部分。
     */
    private String parseSpel(String template, String[] paramNames, Object[] args) {
        if (template == null || template.isEmpty() || paramNames == null || args == null) {
            return template;
        }
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(args)
                .build();
        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        Matcher matcher = SPEL_REF_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String ref = matcher.group(1);
            String replacement;
            try {
                Object value = SPEL_PARSER.parseExpression("#" + ref).getValue(context);
                replacement = value == null ? "" : String.valueOf(value);
            } catch (Exception e) {
                replacement = matcher.group(0); // 解析失败保留原始引用
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}