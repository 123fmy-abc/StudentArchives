package com.example.studentarchives.config.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String[] PUBLIC_AUTH_PATHS = {
            "/auth/login",
            "/auth/captcha",
            "/auth/password/reset",
            "/auth/password/reset/confirm",
            "/auth/refresh",
            // 登录页公开统计接口（免鉴权，JwtAuthenticationFilter 按前缀匹配放行 /public/**）
            "/public"
    };
}
