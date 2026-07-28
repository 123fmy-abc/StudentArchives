package com.example.studentarchives.util;

import com.example.studentarchives.config.security.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    @PostConstruct
    public void validateSecret() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("JWT 签名密钥未配置，请通过环境变量 JWT_SECRET 注入。");
        }
    }

    /** 获取签名密钥 */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌（默认过期时间，tokenVersion = 0）
     * <p>
     * 已废弃：此重载硬编码 tokenVersion=0，跳过了用户实际的版本号，
     * 会导致 Token 吊销功能失效（tokenVersion 不递增）。
     * 请使用 {@link #generateToken(Long, String, Long, Integer)} 传入实际 tokenVersion。
     *
     * @param userId   用户ID
     * @param userNo   学号/工号
     * @param schoolId 学校ID
     * @return JWT 令牌字符串
     * @deprecated 应使用带 tokenVersion 参数的重载，传入用户当前的 tokenVersion
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public String generateToken(Long userId, String userNo, Long schoolId) {
        return generateToken(userId, userNo, schoolId, 0);
    }

    /**
     * 生成访问令牌（默认过期时间）
     *
     * @param userId       用户ID
     * @param userNo       学号/工号
     * @param schoolId     学校ID
     * @param tokenVersion 令牌版本号
     * @return JWT 令牌字符串
     */
    public String generateToken(Long userId, String userNo, Long schoolId, Integer tokenVersion) {
        return generateToken(userId, userNo, schoolId, tokenVersion, jwtProperties.getExpirationMs());
    }

    /**
     * 生成访问令牌（自定义过期时间）
     *
     * @param userId       用户ID
     * @param userNo       学号/工号
     * @param schoolId     学校ID
     * @param tokenVersion 令牌版本号
     * @param expirationMs 过期时间（毫秒）
     * @return JWT 令牌字符串
     */
    public String generateToken(Long userId, String userNo, Long schoolId, Integer tokenVersion, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userNo", userNo)
                .claim("schoolId", schoolId)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌（默认 7 天过期）
     *
     * @param userId             用户ID
     * @param userNo             学号/工号
     * @param schoolId           学校ID
     * @param refreshTokenVersion 刷新令牌版本号
     * @return JWT 刷新令牌字符串
     */
    public String generateRefreshToken(Long userId, String userNo, Long schoolId, Integer refreshTokenVersion) {
        return generateRefreshToken(userId, userNo, schoolId, refreshTokenVersion, jwtProperties.getRefreshExpirationMs());
    }

    /**
     * 生成刷新令牌（自定义过期时间）
     *
     * @param userId             用户ID
     * @param userNo             学号/工号
     * @param schoolId           学校ID
     * @param refreshTokenVersion 刷新令牌版本号
     * @param expirationMs       过期时间（毫秒）
     * @return JWT 刷新令牌字符串
     */
    public String generateRefreshToken(Long userId, String userNo, Long schoolId, Integer refreshTokenVersion, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userNo", userNo)
                .claim("schoolId", schoolId)
                .claim("refreshTokenVersion", refreshTokenVersion)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
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
     * 检查令牌是否已过期
     *
     * @param token JWT 令牌
     * @return true=已过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
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
     * 从令牌中提取访问令牌版本号
     *
     * @param token JWT 令牌
     * @return 令牌版本号
     */
    public Integer getTokenVersionFromToken(String token) {
        return getClaims(token).get("tokenVersion", Integer.class);
    }

    /**
     * 从令牌中提取刷新令牌版本号
     *
     * @param token JWT 刷新令牌
     * @return 刷新令牌版本号
     */
    public Integer getRefreshTokenVersionFromToken(String token) {
        return getClaims(token).get("refreshTokenVersion", Integer.class);
    }

    /**
     * 从令牌中提取过期时间
     *
     * @param token JWT 令牌
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        return getClaims(token).getExpiration();
    }

    /**
     * 解析并验证 JWT，返回 Claims
     *
     * @param token JWT 令牌字符串
     * @return JWT Claims
     * @throws ExpiredJwtException     令牌已过期
     * @throws MalformedJwtException   令牌格式错误
     * @throws SecurityException       签名验证失败
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从请求 attribute 中获取缓存的 Claims（避免重复解析 JWT）。
     * <p>
     * 由 {@link com.example.studentarchives.config.security.JwtAuthenticationFilter} 在认证通过后
     * 将 Claims 缓存到请求中 {@code request.setAttribute("jwt_claims", claims)}。
     *
     * @param request HTTP 请求
     * @return JWT Claims（如果请求已通过认证），否则返回 null
     */
    public static Claims getCachedClaims(jakarta.servlet.http.HttpServletRequest request) {
        Object claims = request.getAttribute("jwt_claims");
        if (claims instanceof Claims c) {
            return c;
        }
        return null;
    }
}
