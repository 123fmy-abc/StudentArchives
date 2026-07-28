package com.example.studentarchives.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 * <p>
 * 用于邮件发送等非关键路径的异步操作。
 * 使用单独的线程池避免占用 Tomcat 请求处理线程。
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {

    /** 核心线程数 */
    private static final int CORE_POOL_SIZE = 2;
    /** 最大线程数 */
    private static final int MAX_POOL_SIZE = 5;
    /** 队列容量 */
    private static final int QUEUE_CAPACITY = 50;
    /** 线程名前缀 */
    private static final String THREAD_NAME_PREFIX = "async-task-";

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
