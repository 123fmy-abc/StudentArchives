package com.example.studentarchives.config.security;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证统一入口
 * <p>
 * 捕获未携带有效 Token 或 Token 验证失败的请求，返回标准 ApiResult 格式的 401 响应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("未认证请求: {}", request.getRequestURI());
        writeError(response, ResultCode.UNAUTHORIZED, "未登录或登录已过期");
    }

    private void writeError(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.error(resultCode, message)));
    }
}
