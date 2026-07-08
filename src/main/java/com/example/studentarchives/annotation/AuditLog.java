package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 * <p>
 * 标注在 Service/Controller 方法上，自动记录操作人、操作类型、操作内容到审计日志。
 * <pre>
 * &#064;AuditLog(module = "档案管理", action = "提交申报", description = "#archiveType + #title")
 * public ApiResult&lt;Void&gt; submit(String archiveType, String title) { ... }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 操作模块（如：档案管理、奖项报名、审批管理） */
    String module();

    /** 操作动作（如：新增、修改、提交、审核） */
    String action();

    /** 操作描述（支持 SpEL 表达式，引用方法参数：#paramName） */
    String description() default "";

    /** 是否记录请求参数 */
    boolean logParams() default true;

    /** 是否记录响应结果（注意：大结果可能影响性能） */
    boolean logResult() default false;
}
