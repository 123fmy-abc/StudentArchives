package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 性能日志注解
 * <p>
 * 标注在需要监控执行时间的方法上，自动记录耗时。
 * 超过阈值输出 WARN 级别告警。
 * <pre>
 * &#064;PerfLog(label = "生成画像分析", warnMs = 500)
 * public void generatePortrait(Long userId) { ... }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerfLog {

    /** 业务标签，用于标识监控点 */
    String label() default "";

    /** 慢查询告警阈值（毫秒），默认 1000ms */
    long warnMs() default 1000;
}
