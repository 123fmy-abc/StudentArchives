package com.example.studentarchives.util;

import com.example.studentarchives.config.security.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 令牌工具类
 * <p>
 * 负责令牌的生成、验证和解析。
 * 使用 HMAC-SHA256 算法，密钥从配置读取。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /** 获取签名密钥 */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌
     *
     * @param userId   用户ID
     * @param userNo   学号/工号
     * @param schoolId 学校ID
     * @return JWT 令牌字符串
     */
    public String generateToken(Long userId, String userNo, Long schoolId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userNo", userNo)
                .claim("schoolId", schoolId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证令牌是否有效
     *
     * @param token JWT 令牌
     * @return true=有效 false=无效/过期
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从令牌中提取用户ID
     *
     * @param token JWT 令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    /**
     * 从令牌中提取学号/工号
     *
     * @param token JWT 令牌
     * @return 学号/工号
     */
    public String getUserNoFromToken(String token) {
        return getClaims(token).get("userNo", String.class);
    }

    /**
     * 从令牌中提取学校ID
     *
     * @param token JWT 令牌
     * @return 学校ID
     */
    public Long getSchoolIdFromToken(String token) {
        return getClaims(token).get("schoolId", Long.class);
    }

    /**
     * 解析并验证 JWT，返回 Claims
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
