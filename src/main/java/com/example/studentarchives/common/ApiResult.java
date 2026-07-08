package com.example.studentarchives.common;

import com.example.studentarchives.util.LogUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一 API 响应体
 * <p>
 * 返回格式：
 * <pre>
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": {},
 *   "trace_id": "req-20260704-143025-abc123",
 *   "timestamp": "2026-07-04 14:30"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;
    private String timestamp;

    // ==================== 成功 ====================

    public static <T> ApiResult<T> success() {
        return result(ResultCode.SUCCESS, null);
    }

    public static <T> ApiResult<T> success(T data) {
        return result(ResultCode.SUCCESS, data);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return ApiResult.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .traceId(generateTraceId())
                .timestamp(now())
                .build();
    }

    // ==================== 失败（用 ResultCode） ====================

    public static <T> ApiResult<T> error(ResultCode resultCode) {
        return result(resultCode, null);
    }

    public static <T> ApiResult<T> error(ResultCode resultCode, String message) {
        return ApiResult.<T>builder()
                .code(resultCode.getCode())
                .message(message)
                .traceId(generateTraceId())
                .timestamp(now())
                .build();
    }

    public static <T> ApiResult<T> error(ResultCode resultCode, T data) {
        return ApiResult.<T>builder()
                .code(resultCode.getCode())
                .message(resultCode.getMessage())
                .data(data)
                .traceId(generateTraceId())
                .timestamp(now())
                .build();
    }

    // ==================== 失败（直接指定 code/message） ====================

    public static <T> ApiResult<T> error(int code, String message) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .traceId(generateTraceId())
                .timestamp(now())
                .build();
    }

    // ==================== 内部构造 ====================

    private static <T> ApiResult<T> result(ResultCode resultCode, T data) {
        return ApiResult.<T>builder()
                .code(resultCode.getCode())
                .message(resultCode.getMessage())
                .data(data)
                .traceId(generateTraceId())
                .timestamp(now())
                .build();
    }

    // ==================== 快捷判断 ====================

    public boolean isSuccess() {
        return code == ResultCode.SUCCESS.getCode();
    }

    // ==================== 工具方法 ====================

    private static String generateTraceId() {
        // 优先从当前请求的 attribute 读取（Controller 异常后 MDC 可能已被清除）
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            Object traceId = request.getAttribute("trace_id");
            if (traceId instanceof String s && !s.isEmpty()) {
                return s;
            }
        }
        return LogUtil.getOrCreateTraceId();
    }

    /** 日期时间格式：yyyy-MM-dd HH:mm */
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static String now() {
        return ZonedDateTime.now(ZoneId.systemDefault()).format(DTF);
    }
}
