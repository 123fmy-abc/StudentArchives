package com.example.studentarchives.support;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 邮件验证码存储（基于 Redis，支持集群）
 * <p>
 * 用于密码重置流程：存储 6 位数字验证码的 SHA-256 哈希值。
 * 支持 5 分钟 TTL、60 秒重发冷却、一次性验证（填写错误即失效，需重新获取）。
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeStore {

    /** 验证码过期时间：5 分钟 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    /** 重发冷却时间：60 秒 */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    /** 验证码长度（6 位） */
    private static final int CODE_LENGTH = 6;

    private static final String CODE_KEY_PREFIX = "verify:code:";
    private static final String COOLDOWN_KEY_PREFIX = "verify:cooldown:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 生成并存储验证码
     *
     * @param email 邮箱地址
     * @return 明文验证码（用于发送邮件）
     * @throws BusinessException 如果在冷却期内
     */
    public String generate(String email) {
        String cooldownKey = buildCooldownKey(email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttlSeconds = redisTemplate.getExpire(cooldownKey);
            long remaining = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 1;
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "请 " + remaining + " 秒后再试");
        }

        // 生成 6 位数字验证码（使用 nextLong 避免 int 精度截断）
        long upperBound = (long) Math.pow(10, CODE_LENGTH);
        String code = String.format("%0" + CODE_LENGTH + "d",
                ThreadLocalRandom.current().nextLong(upperBound));

        String codeHash = hashCode(code);
        String codeKey = buildCodeKey(email);
        redisTemplate.opsForValue().set(codeKey, codeHash, CODE_TTL);
        redisTemplate.opsForValue().set(cooldownKey, "1", RESEND_COOLDOWN);

        return code;
    }

    /**
     * 校验验证码
     *
     * @param email 邮箱地址
     * @param code  用户输入的验证码
     * @return true=验证通过（验证码有效且未过期）
     */
    public boolean verify(String email, String code) {
        if (email == null || code == null) {
            return false;
        }

        String codeKey = buildCodeKey(email);
        String storedHash = redisTemplate.opsForValue().get(codeKey);
        if (storedHash == null) {
            return false;
        }

        // 验证哈希：验证码为一次性，填写错误即失效，需重新获取
        if (!storedHash.equals(hashCode(code))) {
            redisTemplate.delete(codeKey);
            return false;
        }

        // 验证成功，移除条目
        redisTemplate.delete(codeKey);
        return true;
    }

    /**
     * 检查是否可以重新发送验证码（60 秒冷却）
     *
     * @param email 邮箱地址
     * @return true=可以发送
     */
    public boolean canResend(String email) {
        return !Boolean.TRUE.equals(redisTemplate.hasKey(buildCooldownKey(email)));
    }

    /**
     * 移除验证码条目
     *
     * @param email 邮箱地址
     */
    public void remove(String email) {
        redisTemplate.delete(buildCodeKey(email));
    }

    /**
     * SHA-256 哈希
     */
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String buildCodeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String buildCooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }
}
