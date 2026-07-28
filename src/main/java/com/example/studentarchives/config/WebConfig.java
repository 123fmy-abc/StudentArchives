package com.example.studentarchives.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web MVC 配置
 * <p>
 * 配置 CORS 跨域。
 * 生产环境需通过环境变量 CORS_ALLOWED_ORIGINS 指定合法域名（多个逗号分隔），
 * 禁止在未配置时使用通配符（防止 CSRF/凭证泄露）。
 * dev 环境默认放行本地常见开发地址。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 默认本地开发地址 */
    private static final String[] DEFAULT_DEV_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:8080"
    };

    /** 是否允许携带凭证（Cookie/Authorization 头） */
    private static final boolean ALLOW_CREDENTIALS;

    /** 解析后的合法 Origin 列表 */
    private static final String[] ALLOWED_ORIGINS = resolveOrigins();

    /** 是否包含通配符/模式字符 */
    private static final boolean HAS_PATTERNS = hasPatterns(ALLOWED_ORIGINS);

    static {
        // 通配符时不允许携带凭证（CORS 规范约束）
        ALLOW_CREDENTIALS = !HAS_PATTERNS;
    }

    private static boolean hasPatterns(String[] origins) {
        return Arrays.stream(origins).anyMatch(o -> o.contains("*") || o.contains("?"));
    }

    private static String[] resolveOrigins() {
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.isBlank()) {
            return envOrigins.split(",");
        }
        return DEFAULT_DEV_ORIGINS;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(ALLOW_CREDENTIALS)
                .maxAge(3600);

        // 精确域名使用 allowedOrigins，通配符使用 allowedOriginPatterns
        if (HAS_PATTERNS) {
            registration.allowedOriginPatterns(ALLOWED_ORIGINS);
        } else {
            registration.allowedOrigins(ALLOWED_ORIGINS);
        }
    }
}
