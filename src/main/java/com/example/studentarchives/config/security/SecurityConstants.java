package com.example.studentarchives.config.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String[] PUBLIC_AUTH_PATHS = {
            "/auth/login",
            "/auth/captcha",
            "/auth/password/reset",
            "/auth/password/reset/confirm",
            "/auth/refresh",
            // 登录页公开统计接口（免鉴权）。
            // 必须用 /public/** 而非 /public：PublicStatisticsController 的实际路径是
            // /public/statistics，精确匹配 "/public" 不会命中；此前仅靠 SecurityConfig 的
            // anyRequest().permitAll() 兜底才免鉴权，收紧兜底后会 401。
            "/public/**"
    };
}
