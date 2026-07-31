package com.example.studentarchives.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败次数限制器（基于 Redis，支持集群）
 * <p>
 * 同一账号在 2 分钟内连续失败 5 次后锁定，登录成功或达到 TTL 后清除。
 * Redis 不可用时自动降级（允许登录），避免因 Redis 故障导致登录接口 500。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(2);
    private static final String KEY_PREFIX = "login:limiter:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查账号是否允许继续尝试登录。
     * Redis 不可用时降级为允许登录。
     *
     * @param account 账号（学号/工号）
     * @return true=允许登录尝试
     */
    public boolean isAllowed(String account) {
        try {
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
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，登录限流降级（允许登录）: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 获取账号锁定的剩余秒数。
     * Redis 不可用时返回 0。
     *
     * @param account 账号（学号/工号）
     * @return 剩余锁定秒数，未锁定时返回 0
     */
    public long getLockoutRemainingSeconds(String account) {
        try {
            String key = buildKey(account);
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，获取锁定时间降级: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 记录一次登录失败。
     * Redis 不可用时静默忽略。
     *
     * @param account 账号
     */
    public void recordFailure(String account) {
        try {
            String key = buildKey(account);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, LOCKOUT_DURATION);
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，登录失败计数无法记录: {}", e.getMessage());
        }
    }

    /**
     * 记录登录成功，清除失败计数。
     * Redis 不可用时静默忽略。
     *
     * @param account 账号
     */
    public void recordSuccess(String account) {
        try {
            redisTemplate.delete(buildKey(account));
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，登录成功计数无法清除: {}", e.getMessage());
        }
    }

    private String buildKey(String account) {
        return KEY_PREFIX + account;
    }
}
