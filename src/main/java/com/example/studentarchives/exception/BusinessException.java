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
    private final int httpStatus;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.httpStatus = resolveHttpStatus(resultCode);
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = resolveHttpStatus(resultCode);
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

    /** 根据错误码推断 HTTP 状态码 */
    private static int resolveHttpStatus(ResultCode resultCode) {
        int code = resultCode.getCode();
        if (code == 5 || code == 20005 || code == 20006 || code == 20007) {
            return 403; // 禁止操作 / 无访问权限 / 账号禁用冻结
        }
        if (code >= 20000 && code < 30000) {
            return 401; // 认证错误：未登录 / Token失效过期 / 密码错误
        }
        if (code == 2 || code == 30001) {
            return 404; // 数据不存在 / 数据已删除
        }
        if (code >= 30000 && code < 40000) {
            return 409; // 数据冲突
        }
        if (code >= 90000) {
            return 500; // 系统错误
        }
        return 400; // 默认：参数/业务错误
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
