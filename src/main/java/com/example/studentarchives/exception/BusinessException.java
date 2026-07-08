package com.example.studentarchives.exception;

import com.example.studentarchives.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 抛出时由 GlobalExceptionHandler 统一捕获并返回 ApiResult
 * <pre>
 * throw new BusinessException(ResultCode.DATA_NOT_FOUND);
 * throw new BusinessException(ResultCode.PARAM_ERROR, "学号格式不正确");
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 快速创建：数据不存在 */
    public static BusinessException notFound() {
        return new BusinessException(ResultCode.DATA_NOT_FOUND);
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
