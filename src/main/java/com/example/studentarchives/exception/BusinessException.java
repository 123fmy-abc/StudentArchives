package com.example.studentarchives.exception;

import com.example.studentarchives.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 抛出时由 GlobalExceptionHandler 统一捕获并返回 ApiResult
 * <pre>
 * throw new BusinessException(ResultCode.DATA_NOT_EXIST);
 * throw new BusinessException(ResultCode.PARAM_ERROR, "学号格式不正确");
 * </pre>
 * <p>
 * HTTP 状态码直接从 {@link ResultCode#getHttpStatus()} 获取，
 * 确保异常层与 API 响应状态码一致。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = 400;
    }

    public BusinessException(ResultCode resultCode, String message, int httpStatus) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = httpStatus;
    }

    /** 快速创建：数据不存在（30001） */
    public static BusinessException notFound() {
        return new BusinessException(ResultCode.DATA_NOT_EXIST);
    }

    /** 快速创建：禁止操作 */
    public static BusinessException forbidden() {
        return new BusinessException(ResultCode.FORBIDDEN);
    }

    /** 快速创建：参数错误 */
    public static BusinessException badParam(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }
}
