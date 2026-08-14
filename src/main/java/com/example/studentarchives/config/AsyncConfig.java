package com.example.studentarchives.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步与重试配置
 * <p>
 * 启用 @Retryable 注解，用于邮件发送失败自动重试。
 * 启用 @Async 注解并注册评分重算专用线程池，供管理端评分重算任务异步执行
 * （POST /admin/scores/recalculate 触发后任务进入线程池，接口立即返回任务 ID）。
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {

    /** 评分重算任务执行线程池（Bean 名与 AdminScoreService.executeAsync 的 @Async 值对应） */
    @Bean(name = "scoreRecalculationExecutor")
    public Executor scoreRecalculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心/最大线程：重算任务为低频批量操作，控制并发避免打满数据库连接
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("score-recalc-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
