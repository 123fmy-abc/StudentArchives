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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

/**
 * 验证码内存缓存
 * <p>
 * 使用 ConcurrentHashMap 存储验证码的 SHA-256 哈希值，
 * 支持 5 分钟 TTL、校验通过后一次性使用、错误次数上限、定时清理过期条目。
 */
@Component
public class CaptchaStore {

    /** 验证码过期时间：5 分钟 */
    private static final long TTL_MILLIS = 5 * 60 * 1000L;

    /** 过期（或已使用）条目的保留时间：让上层能区分「已过期」与「输错」，而非一律「查无此 key」 */
    private static final long RETENTION_MILLIS = TTL_MILLIS + 5 * 60 * 1000L;

    /**
     * 同一个验证码允许的最大错误次数，达到后立即作废。
     * <p>
     * 允许少量重试是为了容忍真实用户手滑，但重试次数必须有界：图片固定而登录接口会返回
     * 「对/错」，不限次数的重试等于把验证码变成攻击者训练/枚举 OCR 的反馈预言机，
     * 无需重新拉图、重新推理即可反复试错。
     */
    private static final int MAX_WRONG_ATTEMPTS = 3;

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /**
     * 验证码校验结果
     */
    public enum VerifyResult {
        /** 校验通过，且已标记为已使用 */
        OK,
        /** 条目存在但已超过有效期 */
        EXPIRED,
        /** 条目存在、未过期，但验证码不匹配；仍可继续重试 */
        MISMATCH,
        /** 错误次数已达 {@link #MAX_WRONG_ATTEMPTS}，该验证码已作废 */
        EXHAUSTED,
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
        store.put(key, new CaptchaEntry(codeHash, Instant.now(), false, 0));
        return key;
    }

    /**
     * 校验并消费验证码
     * <p>
     * 允许至多 {@link #MAX_WRONG_ATTEMPTS} 次错误重试以容忍用户手滑，达到上限即作废。
     * 过期、已使用、错误次数达上限的条目不再立即删除，而是在保留期内继续留存在缓存中，
     * 以便调用方区分「验证码已过期」「验证码错误」与「验证码无效」。
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
            // 错误次数自增与「是否达到上限」的判定必须在同一次 computeIfPresent 中完成，
            // 否则并发提交时多个请求会同时读到未达上限的旧值，绕过重试次数限制。
            AtomicBoolean exhausted = new AtomicBoolean(false);
            AtomicBoolean dead = new AtomicBoolean(false);
            CaptchaEntry afterMismatch = store.computeIfPresent(key, (k, v) -> {
                if (v.isUsed()) {
                    // 已被并发请求消费或作废：不能再返回「错误」，否则等于让客户端去重试一个已死的验证码
                    dead.set(true);
                } else {
                    v.setAttempts(v.getAttempts() + 1);
                    if (v.getAttempts() >= MAX_WRONG_ATTEMPTS) {
                        v.setUsed(true);
                        exhausted.set(true);
                    }
                }
                return v;
            });
            // 并发期间条目被清理
            if (afterMismatch == null || dead.get()) {
                return VerifyResult.INVALID;
            }
            // 至此可以确定本次请求自身完成了自增且该验证码仍然有效，MISMATCH 至多出现 MAX_WRONG_ATTEMPTS - 1 次
            return exhausted.get() ? VerifyResult.EXHAUSTED : VerifyResult.MISMATCH;
        }

        // 标记为已使用：置位必须在 computeIfPresent 内完成，
        // 否则并发提交正确验证码时多个请求会同时通过，使一次性验证码被复用多次。
        AtomicBoolean consumed = new AtomicBoolean(false);
        store.computeIfPresent(key, (k, v) -> {
            if (!v.isUsed()) {
                v.setUsed(true);
                consumed.set(true);
            }
            return v;
        });

        return consumed.get() ? VerifyResult.OK : VerifyResult.INVALID;
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
        /** 是否已使用（校验通过、超时、或错误次数达上限均会置位） */
        private boolean used;
        /** 累计错误次数 */
        private int attempts;
    }
}
