package com.example.studentarchives.config;

import com.example.studentarchives.config.security.JwtAccessDeniedHandler;
import com.example.studentarchives.config.security.JwtAuthenticationEntryPoint;
import com.example.studentarchives.config.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.example.studentarchives.config.security.SecurityConstants.PUBLIC_AUTH_PATHS;

/**
 * 安全配置
 * <p>
 * JWT 无状态认证，登录接口公开，其余 API 需认证。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

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
            // 认证与鉴权异常统一处理
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            // URL 权限配置
            .authorizeHttpRequests(auth -> auth
                    // 公开接口（精确路径匹配优先）
                    .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
                    // 静态资源、错误页面等无需认证（放在 /** 之前才生效）
                    .requestMatchers("/error", "/favicon.ico", "/static/**", "/webjars/**").permitAll()
                    // 其余 API 需认证
                    .requestMatchers("/api/**").authenticated()
                    // 其他兜底放行
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
