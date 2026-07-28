package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 模块 */
    String module() default "";

    /** 操作 */
    String action() default "";

    /** SpEL 描述 */
    String description() default "";

    /** 是否记录请求参数（默认记录） */
    boolean logParams() default true;

    /** 是否记录返回结果 */
    boolean logResult() default false;
}
