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

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

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
     *
     * @param key  验证码标识
     * @param code 用户输入的验证码
     * @return true=验证通过且已标记为已使用
     */
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }

        String codeHash = hashCode(code);
        CaptchaEntry entry = store.get(key);
        if (entry == null) {
            return false;
        }

        // 已使用
        if (entry.isUsed()) {
            return false;
        }

        // 已过期
        if (Instant.now().toEpochMilli() - entry.getCreatedAt().toEpochMilli() > TTL_MILLIS) {
            store.remove(key);
            return false;
        }

        // 验证 code 哈希
        if (!entry.getCodeHash().equals(codeHash)) {
            return false;
        }

        // 标记为已使用
        store.computeIfPresent(key, (k, v) -> {
            v.setUsed(true);
            return v;
        });

        // 移除已使用的条目
        store.remove(key);
        return true;
    }

    /**
     * 移除验证码
     *
     * @param key 验证码标识
     */
    public void remove(String key) {
        store.remove(key);
    }

    /** 清理过期条目（使用 forEach + remove 避免 ConcurrentHashMap.removeIf 的迭代不原子问题） */
    private void evictExpired() {
        long now = Instant.now().toEpochMilli();
        store.forEach((key, entry) -> {
            if (now - entry.getCreatedAt().toEpochMilli() > TTL_MILLIS) {
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
