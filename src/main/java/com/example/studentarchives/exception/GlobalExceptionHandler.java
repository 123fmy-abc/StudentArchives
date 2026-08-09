package com.example.studentarchives.exception;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获各类异常，返回标准 ApiResult 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResult.error(e.getCode(), e.getMessage()));
    }

    // ==================== 参数校验异常 ====================

    /** @Valid 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        // 收集所有字段错误信息
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .reduce((a, b) -> a + "；" + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return ApiResult.error(ResultCode.PARAM_ERROR, message);
    }

    /** @Validated 方法参数校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .reduce((a, b) -> a + "；" + b)
                .orElse("参数校验失败");
        log.warn("参数约束违规: {}", message);
        return ApiResult.error(ResultCode.PARAM_ERROR, message);
    }

    /** 请求体不可读（JSON格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return ApiResult.error(ResultCode.PARAM_FORMAT_ERROR, "请求数据格式错误");
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ApiResult.error(ResultCode.PARAM_MISSING, "缺少必填参数: " + e.getParameterName());
    }

    /** 缺少文件上传 part（multipart 请求未携带 file 等字段），避免被兜底处理成 500 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingPart(MissingServletRequestPartException e) {
        log.warn("缺少文件上传参数: {}", e.getRequestPartName());
        return ApiResult.error(ResultCode.PARAM_MISSING, "缺少文件上传参数: " + e.getRequestPartName());
    }

    /** 文件上传格式错误（非 multipart/form-data 请求） */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMultipart(MultipartException e) {
        log.warn("文件上传请求格式错误: {}", e.getMessage());
        return ApiResult.error(ResultCode.PARAM_FORMAT_ERROR, "请求格式错误，请使用 multipart/form-data 格式上传文件");
    }

    /** 参数类型转换错误 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误: {}", e.getName());
        return ApiResult.error(ResultCode.PARAM_FORMAT_ERROR, "参数 " + e.getName() + " 格式错误");
    }

    // ==================== 请求方法 / Content-Type 异常 ====================

    /** 请求方法不支持（如用 GET 调 POST 接口） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ApiResult.error(ResultCode.PARAM_ERROR, "请求方法不支持，请使用 " + String.join("/", e.getSupportedMethods()) + " 方式");
    }

    /** Content-Type 不支持（如用表单提交 JSON 接口） */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResult<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的 Content-Type: {}", e.getMessage());
        return ApiResult.error(ResultCode.PARAM_FORMAT_ERROR, "不支持的 Content-Type，请使用 application/json");
    }

    // ==================== 请求路径不存在（404） ====================

    /** 请求路径无对应 Handler（Spring 6.1+ 抛 NoResourceFoundException），避免被兜底异常处理成 500 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("请求路径不存在: {}", e.getResourcePath());
        return ApiResult.error(ResultCode.DATA_NOT_EXIST, "请求的接口不存在");
    }

    // ==================== 其他异常 ====================

    /** 非法参数 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ApiResult.error(ResultCode.PARAM_ILLEGAL, e.getMessage());
    }

    // ==================== 数据访问异常 ====================

    /** 唯一索引冲突（MySQL 1062），如重复标签、重复编码 */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResult<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一索引冲突: {}", e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : e.getMessage());
        return ApiResult.error(ResultCode.DATA_DUPLICATE, "数据已存在，请勿重复提交");
    }

    /** 数据完整性违反（非空约束、字段超长、外键失败等），避免被当作缓存故障处理 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResult<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性违反: {}", e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : e.getMessage());
        return ApiResult.error(ResultCode.DB_UNIQUE_CONSTRAINT, "数据校验失败，请检查提交内容");
    }

    /** Redis 或数据库连接异常（如 Redis 不可用） */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleDataAccess(DataAccessException e) {
        log.error("数据访问异常（Redis/数据库）:", e);
        return ApiResult.error(ResultCode.THIRD_REDIS_FAILED, "缓存服务暂不可用，请稍后重试");
    }

    /** 兜底：未预期的异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常:", e);
        return ApiResult.error(ResultCode.SYS_INTERNAL_ERROR, "服务器内部错误");
    }
}
