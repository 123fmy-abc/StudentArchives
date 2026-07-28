package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 第三方接口调用日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThirdPartyApi {

    /** 第三方服务名称 */
    String service() default "";

    /** 接口描述 */
    String description() default "";

    /** 超时警告阈值（毫秒），默认 3000ms */
    long warnTimeoutMs() default 3000;
}
