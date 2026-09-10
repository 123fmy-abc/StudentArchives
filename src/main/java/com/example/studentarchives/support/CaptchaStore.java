package com.example.studentarchives.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * 验证码内存缓存
 * <p>
 * 使用 ConcurrentHashMap 存储验证码的 SHA-256 哈希值，
 * 支持 5 分钟 TTL、一次性使用、定时清理过期条目。
 */
@Component
public class CaptchaStore {

    /** 验证码过期时间：5 分钟 */
    private static final long TTL_MILLIS = 5 * 60 * 1000L;

    /** 过期（或已使用）条目的保留时间：让上层能区分「已过期」与「输错」，而非一律「查无此 key」 */
    private static final long RETENTION_MILLIS = TTL_MILLIS + 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /**
     * 验证码校验结果
     */
    public enum VerifyResult {
        /** 校验通过，且已标记为已使用 */
        OK,
        /** 条目存在但已超过有效期 */
        EXPIRED,
        /** 条目存在、未过期，但验证码不匹配 */
        MISMATCH,
        /** 查无此 key、key/code 为空、或该验证码已被使用过 */
        INVALID
    }

    /**
     * 验证码有效期（秒），供接口返回 expiresIn 使用
     */
    public int getTtlSeconds() {
        return (int) (TTL_MILLIS / 1000);
    }

    /**
     * 存储验证码
     *
     * @param code 验证码明文
     * @return 验证码唯一标识 key（UUID）
     */
    public String store(String code) {
        evictExpired();
        String key = UUID.randomUUID().toString();
        String codeHash = hashCode(code);
        store.put(key, new CaptchaEntry(codeHash, Instant.now(), false));
        return key;
    }

    /**
     * 校验并消费验证码（一次性使用，原子操作）
     * <p>
     * 过期与已使用的条目不再立即删除，而是在保留期内继续留存在缓存中，
     * 以便调用方区分「验证码已过期」与「验证码错误」。
     *
     * @param key  验证码标识
     * @param code 用户输入的验证码
     * @return 校验结果；OK 表示验证通过且已标记为已使用
     */
    public VerifyResult verify(String key, String code) {
        if (key == null || code == null) {
            return VerifyResult.INVALID;
        }

        CaptchaEntry entry = store.get(key);
        if (entry == null) {
            return VerifyResult.INVALID;
        }

        // 已使用（一次性验证码重复提交）
        if (entry.isUsed()) {
            return VerifyResult.INVALID;
        }

        // 已过期（条目仍在保留期内，可明确判定为过期）
        if (Instant.now().toEpochMilli() - entry.getCreatedAt().toEpochMilli() > TTL_MILLIS) {
            return VerifyResult.EXPIRED;
        }

        // 验证 code 哈希
        if (!entry.getCodeHash().equals(hashCode(code))) {
            return VerifyResult.MISMATCH;
        }

        // 标记为已使用
        store.computeIfPresent(key, (k, v) -> {
            v.setUsed(true);
            return v;
        });

        return VerifyResult.OK;
    }

    /**
     * 移除验证码
     *
     * @param key 验证码标识
     */
    public void remove(String key) {
        store.remove(key);
    }

    /**
     * 清理超出保留期的条目（使用 forEach + remove 避免 ConcurrentHashMap.removeIf 的迭代不原子问题）
     * <p>
     * 过期条目本身已不可用，保留 {@link #RETENTION_MILLIS} 仅用于向调用方反馈过期原因。
     */
    private void evictExpired() {
        long now = Instant.now().toEpochMilli();
        store.forEach((key, entry) -> {
            if (now - entry.getCreatedAt().toEpochMilli() > RETENTION_MILLIS) {
                store.remove(key, entry);
            }
        });
    }

    /**
     * 对验证码进行 SHA-256 哈希
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

    @Data
    @AllArgsConstructor
    private static class CaptchaEntry {
        /** 验证码 SHA-256 哈希值 */
        private String codeHash;
        /** 创建时间 */
        private Instant createdAt;
        /** 是否已使用 */
        private boolean used;
    }
}
