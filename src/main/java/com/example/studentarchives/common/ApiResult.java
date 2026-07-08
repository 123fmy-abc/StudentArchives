package com.example.studentarchives.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
 *   "timestamp": "2026-07-04T14:30:25+08:00"
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
        return "req-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+08:00"));
    }
}
