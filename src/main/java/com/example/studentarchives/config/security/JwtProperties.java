package com.example.studentarchives.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（Base64 编码，至少 256 位） */
    private String secret;

    /** 令牌过期时间（毫秒），默认 24 小时 */
    private long expirationMs = 86400000;

    /** 刷新令牌过期时间（毫秒），默认 7 天 */
    private long refreshExpirationMs = 604800000;

    /** 记住我时刷新令牌过期时间（毫秒），默认 30 天 */
    private long rememberMeRefreshExpirationMs = 2592000000L;

    /** 记住我时访问令牌过期时间（毫秒），默认 7 天 */
    private long rememberMeAccessExpirationMs = 604800000L;
}
