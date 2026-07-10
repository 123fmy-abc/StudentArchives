package com.example.studentarchives.config;

import com.example.studentarchives.config.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置
 * <p>
 * JWT 无状态认证，登录接口公开，其余 API 需认证。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（无状态 Token 认证）
            .csrf(csrf -> csrf.disable())
            // 无状态会话（不使用 Session）
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 关闭表单登录和 HTTP Basic
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            // URL 权限配置
            .authorizeHttpRequests(auth -> auth
                    // 公开接口
                    .requestMatchers("/auth/login", "/auth/captcha").permitAll()
                    // 其余 API 需认证
                    .requestMatchers("/**").authenticated()
                    // 其他（静态资源、错误页面等）放行
                    .anyRequest().permitAll()
            )
            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
