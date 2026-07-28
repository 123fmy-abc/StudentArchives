package com.example.studentarchives.config.security;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 无权限访问统一处理器
 * <p>
 * 捕获已认证但无权访问资源的请求，返回标准 ApiResult 格式的 403 响应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.debug("无权限访问: {}", request.getRequestURI());
        writeError(response, ResultCode.FORBIDDEN, "无访问权限");
    }

    private void writeError(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.error(resultCode, message)));
    }
}
