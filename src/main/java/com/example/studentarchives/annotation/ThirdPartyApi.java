package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 第三方接口调用日志注解
 * <p>
 * 标注在调用外部 API 的方法上，自动记录请求地址、参数、响应、耗时。
 * <pre>
 * &#064;ThirdPartyApi(service = "WeChat", description = "获取OpenID")
 * public String getOpenId(String code) { ... }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThirdPartyApi {

    /** 第三方服务名称（如：WeChat、AliPay、SMS） */
    String service();

    /** 接口描述 */
    String description() default "";

    /** 超时告警阈值（毫秒），超过此值将记录 WARN 日志 */
    long warnTimeoutMs() default 3000;
}
