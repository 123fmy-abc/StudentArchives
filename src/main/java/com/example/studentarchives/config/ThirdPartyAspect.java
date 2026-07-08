package com.example.studentarchives.config;

import com.example.studentarchives.annotation.ThirdPartyApi;
import com.example.studentarchives.util.LogUtil;
import com.example.studentarchives.util.ThirdPartyLogUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方接口日志切面
 * <p>
 * 拦截 &#064;ThirdPartyApi 注解，自动记录调用日志。
 */
@Aspect
@Component
public class ThirdPartyAspect {

    @Around("@annotation(thirdPartyApi)")
    public Object around(ProceedingJoinPoint joinPoint, ThirdPartyApi thirdPartyApi) throws Throwable {
        long start = System.currentTimeMillis();
        String service = thirdPartyApi.service();
        String api = thirdPartyApi.description();
        if (api.isEmpty()) {
            api = joinPoint.getSignature().getName();
        }

        // 收集原始请求参数，脱敏由 ThirdPartyLogUtil 统一处理
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> params = new LinkedHashMap<>();
        if (paramNames != null && args != null) {
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                params.put(paramNames[i], args[i]);
            }
        }

        Object result;
        boolean success;
        try {
            result = joinPoint.proceed();
            success = true;
        } catch (Throwable e) {
            long costMs = System.currentTimeMillis() - start;
            ThirdPartyLogUtil.logError(service, api, costMs, params, e.getMessage());
            LogUtil.business().warn("[第三方接口异常] {}:{} - {} ({}ms)", service, api, e.getMessage(), costMs);
            throw e;
        }

        long costMs = System.currentTimeMillis() - start;
        ThirdPartyLogUtil.log(service, api, costMs, success, params, result);

        // 超时告警
        if (costMs > thirdPartyApi.warnTimeoutMs()) {
            LogUtil.business().warn("[第三方接口超时] {}:{} 耗时={}ms, 阈值={}ms", service, api, costMs, thirdPartyApi.warnTimeoutMs());
        }

        return result;
    }
}
