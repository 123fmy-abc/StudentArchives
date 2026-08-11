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
 * <p>
 * 鉴权边界说明：
 * <ul>
 *   <li>{@code /admin/**}：HTTP 层仅要求认证（authenticated），角色/权限码由各管理端 Service 经
 *       {@link com.example.studentarchives.service.Fmy.AdminAuthService} 逐接口校验（admin 角色或对应权限码），
 *       越权统一返回 20005 无访问权限。</li>
 *   <li><code>/teacher/audits/&#42;/revoke</code> - 撤销已审核记录属于管理员纠错权限, HTTP 层兜底要求 ADMIN 角色,
 *       避免未来实现 /teacher/audits/{taskId}/revoke 时因路径前缀被误解为普通教师可操作.</li>
 *   <li>{@code /teacher/**} - 教师端接口需登录, 具体数据范围由 Service 层按教师授权班级/专业校验.</li>
 *   <li>{@code /activities/**}：学生端动态记录模块，需登录。</li>
 * </ul>
 */
//类级别的注解
@Configuration //告诉 Spring 这是一个配置类，里面定义的 @Bean 方法会被注册到 Spring 容器中
@EnableWebSecurity//启用 Spring Security 的 Web 安全功能。
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;//自定义的 JWT 过滤器，用于拦截请求并验证 Token
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;//处理未认证异常（如未登录或 Token 失效）的入口点
    private final JwtAccessDeniedHandler accessDeniedHandler;//登录但无访问权限 处理器

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
                    // 业务接口需认证（公开路径已在之前通过 permitAll 排除）
                    .requestMatchers("/auth/**", "/common/**", "/home/**", "/profile/**", "/messages/**").authenticated()
                    // 管理端接口需认证（角色/权限码校验由各服务层经 AdminAuthService 执行，越权返回 20005）
                    .requestMatchers("/admin/**").authenticated()
                    // 撤销已审核记录：HTTP 层要求 ADMIN 角色，防止路径前缀 /teacher 造成权限误解
                    .requestMatchers("/teacher/audits/*/revoke").hasRole("ADMIN")
                    // 教师端接口需登录，具体数据范围由 Service 层按教师授权班级/专业校验
                    .requestMatchers("/teacher/**").authenticated()
                    // 学生端动态记录模块需登录（对齐《学生端接口文档》六、动态记录模块）
                    .requestMatchers("/activities/**").authenticated()
                    // 其他未匹配的兜底放行（如健康检查、静态资源等）
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
