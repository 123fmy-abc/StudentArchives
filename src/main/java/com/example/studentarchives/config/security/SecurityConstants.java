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
            "/public/**",
            // 学生端「评选说明」/「奖项评选说明」（静态内容，无需登录，控制器未取 principal）。
            // 收紧兜底为 authenticated() 后，这两个 guide 接口会被 /applications/** 与
            // /awards/** 的前缀规则拦住而 401，现显式放行。
            "/applications/*/guide",
            "/awards/*/guide"
    };

    /**
     * 强制改密期间仍允许访问的路径（must_change_password=1 时）。
     * 仅放行「修改密码 / 查看当前身份 / 登出」，其余接口一律返回 403，
     * 直到用户完成首次改密。
     */
    public static final String[] FORCE_PASSWORD_CHANGE_ALLOWED_PATHS = {
            "/auth/password",
            "/auth/me",
            "/auth/logout"
    };
}
