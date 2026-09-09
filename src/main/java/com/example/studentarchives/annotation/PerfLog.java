package com.example.studentarchives.annotation;

import java.lang.annotation.*;

/**
 * 性能日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerfLog {

    /** 业务标签 */
    //给性能监控起名，[用户导出] 接口执行耗时过长
    String label() default "";

    /** 慢查询告警阈值（毫秒），默认 500ms */
    long warnMs() default 500;
}
