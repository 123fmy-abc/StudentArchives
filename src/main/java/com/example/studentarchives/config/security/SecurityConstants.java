package com.example.studentarchives.config.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String[] PUBLIC_AUTH_PATHS = {
            "/auth/login",
            "/auth/captcha",
            "/auth/password/reset",
            "/auth/password/reset/confirm",
            "/auth/refresh"
    };
}
