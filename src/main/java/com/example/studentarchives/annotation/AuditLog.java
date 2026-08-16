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

    /** 关联模型类型（如 user、archive、award），写入 system_logs.related_type */
    String relatedType() default "";

    /** 关联记录 ID 的 SpEL 表达式（如 #userId），解析后写入 system_logs.related_id */
    String relatedId() default "";
}
