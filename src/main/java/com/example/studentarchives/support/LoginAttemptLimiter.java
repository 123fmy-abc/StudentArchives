package com.example.studentarchives.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败次数限制器（基于 Redis，支持集群）
 * <p>
 * 同一账号在 15 分钟内连续失败 5 次后锁定，登录成功或达到 TTL 后清除。
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "login:limiter:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查账号是否允许继续尝试登录。
     *
     * @param account 账号（学号/工号）
     * @return true=允许登录尝试
     */
    public boolean isAllowed(String account) {
        String key = buildKey(account);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return true;
        }
        try {
            int failureCount = Integer.parseInt(value);
            return failureCount < MAX_ATTEMPTS;
        } catch (NumberFormatException e) {
            redisTemplate.delete(key);
            return true;
        }
    }

    /**
     * 记录一次登录失败。
     *
     * @param account 账号
     */
    public void recordFailure(String account) {
        String key = buildKey(account);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次失败时设置过期时间
            redisTemplate.expire(key, LOCKOUT_DURATION);
        }
    }

    /**
     * 记录登录成功，清除失败计数。
     *
     * @param account 账号
     */
    public void recordSuccess(String account) {
        redisTemplate.delete(buildKey(account));
    }

    private String buildKey(String account) {
        return KEY_PREFIX + account;
    }
}
