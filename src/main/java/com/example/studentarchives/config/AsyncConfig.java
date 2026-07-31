package com.example.studentarchives.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 重试配置
 * <p>
 * 启用 @Retryable 注解，用于邮件发送失败自动重试。
 */
@Configuration
@EnableRetry
public class AsyncConfig {

}
