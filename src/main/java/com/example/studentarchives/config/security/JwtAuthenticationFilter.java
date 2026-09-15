package com.example.studentarchives.config.security;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.enums.StatusEnum;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.projection.UserAuthStatus;
import com.example.studentarchives.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import static com.example.studentarchives.config.security.SecurityConstants.PUBLIC_AUTH_PATHS;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 从请求头中提取 JWT 令牌，验证后设置 SecurityContext。
 * 支持 tokenVersion 校验（用于踢人/退出所有设备）、
 * 用户状态校验、以及区分不同 Token 错误类型。
 * <p>
 * 公开路径（{@link SecurityConstants#PUBLIC_AUTH_PATHS}）同时支持两种语义：
 * Ant 风格通配（{@code /public/**}）与「条目 + /」的前缀匹配（{@code /auth/password/reset} 覆盖其子路径）。
 * 新增公开路径时按这两者之一书写即可，两者都会在 {@link #shouldNotFilter} 中命中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    /** 公开路径匹配器：必须支持 `**` 通配，否则 /public/** 之类的条目在这里永远不会命中 */
    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String publicPath : PUBLIC_AUTH_PATHS) {
            // 通配匹配：/public/** 覆盖 /public/statistics 等子路径
            if (PATH_MATCHER.match(publicPath, path)) {
                return true;
            }
            // 前缀匹配：/auth/password/reset 覆盖其下子路径，且兼容条目末尾带 / 的写法
            if (path.startsWith(publicPath + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        try {
            String token = extractToken(request);

            if (!StringUtils.hasText(token)) {
                writeError(response, ResultCode.UNAUTHORIZED, "未登录");
                return;
            }

            // 解析 Claims，捕获不同异常
            Claims claims;
            try {
                claims = jwtUtil.getClaims(token);
            } catch (ExpiredJwtException e) {
                log.debug("JWT 已过期: {}", e.getMessage());
                writeError(response, ResultCode.LOGIN_EXPIRED, "登录已过期，请重新登录");
                return;
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("JWT 无效: {}", e.getMessage());
                writeError(response, ResultCode.TOKEN_ERROR, "Token无效");
                return;
            }

            Long userId = Long.valueOf(claims.getSubject());
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);

            // 类型检查：只有携带 tokenVersion 的令牌（accessToken）才允许访问受保护接口
            // refreshToken 仅应通过 /auth/refresh 端点使用，不应作为 API 访问凭证
            if (tokenVersion == null) {
                log.warn("检测到刷新令牌被错误地用作访问令牌: userId={}", userId);
                writeError(response, ResultCode.TOKEN_INVALID, "Token类型错误，请使用访问令牌");
                return;
            }

            // 查询用户认证状态（带缓存）
            UserAuthStatus authStatus = userRepository.findAuthStatusById(userId).orElse(null);
            if (authStatus == null) {
                writeError(response, ResultCode.TOKEN_INVALID, "用户不存在");
                return;
            }

            // 检查账号状态
            if (!StatusEnum.ENABLED.equalsValue(authStatus.getStatus())) {
                writeError(response, ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
                return;
            }

            // 检查 tokenVersion — 实现"退出所有设备"功能
            if (!tokenVersion.equals(authStatus.getTokenVersion())) {
                writeError(response, ResultCode.TOKEN_INVALID, "Token已失效，请重新登录");
                return;
            }

            // 设置认证上下文
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, token, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 将 Claims 缓存到请求 attribute 中，避免后续重复解析 JWT
            request.setAttribute("jwt_claims", claims);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("JWT认证异常: {}", e.getMessage());
            writeError(response, ResultCode.TOKEN_ERROR, "Token验证失败");
        }
    }

    /** 从请求头提取 Bearer Token */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /** 返回错误响应（使用 ResultCode 动态 HTTP 状态码） */
    private void writeError(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(resultCode.getHttpStatus());
        ApiResult<Void> result = ApiResult.error(resultCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
