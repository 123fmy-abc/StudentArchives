package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 */
@Target({ElementType.METHOD, ElementType.TYPE}) //规定了这个注解能贴在哪里。METHOD 表示可以贴在方法上，TYPE 表示可以贴在类上。
@Retention(RetentionPolicy.RUNTIME)
@Documented//注解会被包含在 JavaDoc 文档中
//定义了一个自定义注解（Annotation），名为 @AuditLog（审计日志）
//在你的 Controller 或 Service 方法上加上 @AuditLog(module="学生管理", action="删除档案", relatedId="#id")
//当方法执行时，提取出配置的 module、action、参数、SpEL 解析出的 ID，然后拼装成一条日志数据，自动插入到数据库的 system_logs 表中
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
