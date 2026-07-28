package com.example.studentarchives.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 * <p>
 * 编码规则：
 * 0          = 成功
 * 1-9        = 通用
 * 10000-19999 = 参数相关
 * 20000-29999 = 认证授权
 * 30000-39999 = 数据相关
 * 40000-49999 = 业务相关
 * 50000-59999 = 第三方服务
 * 60000-69999 = 数据库异常
 * 90000-99999 = 系统异常
 * <p>
 * httpStatus 字段定义了该错误对应的 HTTP 响应状态码，
 * 由 BusinessException 使用，确保异常层与 API 层状态码一致。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ========== 通用（0-9） ==========
    SUCCESS(0, "success", 200),
    SYSTEM_ERROR(1, "系统异常", 500),
    DATA_NOT_FOUND(2, "查询结果为空", 404),
    DATA_ALREADY_EXISTS(3, "数据已存在", 409),
    OPERATION_FAILED(4, "操作失败", 400),
    FORBIDDEN(5, "禁止操作", 403),
    TOO_MANY_REQUESTS(6, "请求过于频繁", 429),
    SERVICE_UNAVAILABLE(7, "服务暂不可用", 503),
    NETWORK_ERROR(8, "网络异常", 502),
    CONCURRENT_CONFLICT(9, "并发冲突", 409),

    // ========== 参数相关（10000-19999） ==========
    PARAM_ERROR(10001, "参数错误", 400),
    PARAM_MISSING(10002, "必填参数缺失", 400),
    PARAM_FORMAT_ERROR(10003, "参数格式错误", 400),
    PARAM_OUT_OF_RANGE(10004, "参数超出范围", 400),
    PARAM_ILLEGAL(10005, "非法参数", 400),
    FILE_FORMAT_ERROR(10006, "文件格式错误", 400),
    FILE_TOO_LARGE(10007, "文件过大", 400),
    FILE_UPLOAD_FAILED(10008, "上传失败", 500),

    // ========== 认证授权（20000-29999） ==========
    UNAUTHORIZED(20001, "未登录", 401),
    TOKEN_INVALID(20002, "Token失效", 401),
    TOKEN_ERROR(20003, "Token错误", 401),
    LOGIN_EXPIRED(20004, "登录已过期", 401),
    ACCESS_DENIED(20005, "无访问权限", 403),
    ACCOUNT_DISABLED(20006, "账号被禁用", 403),
    ACCOUNT_FROZEN(20007, "账号被冻结", 403),
    PASSWORD_ERROR(20008, "密码错误", 401),

    // ========== 数据相关（30000-39999） ==========
    DATA_NOT_EXIST(30001, "数据不存在", 404),
    DATA_DELETED(30002, "数据已删除", 404),
    DATA_DUPLICATE(30003, "数据重复", 409),
    DATA_STATUS_ERROR(30004, "数据状态异常", 409),
    DATA_LOCKED(30005, "数据已锁定", 409),
    DATA_RELATION_EXISTS(30006, "数据关联存在", 409),
    DATA_VALIDATION_FAILED(30007, "数据校验失败", 400),
    DATA_VERSION_CONFLICT(30008, "数据版本冲突", 409),

    // ========== 业务相关（40000-49999） ==========
    BIZ_OPERATION_FAILED(40001, "操作失败", 400),
    BIZ_STATUS_NOT_OPERABLE(40002, "当前状态不可操作", 409),
    BIZ_AUDIT_NOT_PASSED(40003, "审批未通过", 400),
    BIZ_INSUFFICIENT_STOCK(40004, "库存不足", 400),
    BIZ_AMOUNT_EXCEED(40005, "金额超限", 400),
    BIZ_QUOTA_EXCEED(40006, "超出配额", 400),
    BIZ_ALREADY_SUBMITTED(40007, "已提交审核", 409),
    BIZ_COMPLETED_NOT_MODIFIABLE(40008, "已完成不可修改", 409),
    BIZ_DUPLICATE_SUBMISSION(40009, "重复提交", 409),
    BIZ_RULE_LIMIT(40010, "超出业务规则限制", 400),

    // ========== 第三方服务（50000-59999） ==========
    THIRD_WECHAT_ERROR(50001, "微信接口异常", 502),
    THIRD_ALIPAY_ERROR(50002, "支付宝接口异常", 502),
    THIRD_SMS_FAILED(50003, "短信发送失败", 502),
    THIRD_EMAIL_FAILED(50004, "邮件发送失败", 502),
    THIRD_OSS_FAILED(50005, "OSS上传失败", 502),
    THIRD_REDIS_FAILED(50006, "Redis连接失败", 502),
    THIRD_MQ_FAILED(50007, "MQ消息发送失败", 502),
    THIRD_TIMEOUT(50008, "第三方接口超时", 504),

    // ========== 数据库异常（60000-69999） ==========
    DB_CONNECTION_FAILED(60001, "数据库连接失败", 500),
    DB_SQL_ERROR(60002, "SQL执行失败", 500),
    DB_TRANSACTION_FAILED(60003, "事务提交失败", 500),
    DB_ROLLBACK(60004, "事务回滚", 500),
    DB_UNIQUE_CONSTRAINT(60005, "唯一索引冲突", 409),
    DB_FOREIGN_KEY_ERROR(60006, "外键约束失败", 409),
    DB_DEADLOCK(60007, "死锁异常", 500),
    DB_TIMEOUT(60008, "数据库超时", 504),

    // ========== 系统异常（90000-99999） ==========
    SYS_ERROR(90001, "系统异常", 500),
    SYS_UNKNOWN(90002, "未知错误", 500),
    SYS_RUNTIME(90003, "程序运行异常", 500),
    SYS_BUSY(90004, "服务繁忙", 503),
    SYS_MAINTENANCE(90005, "系统维护中", 503),
    SYS_CONFIG_ERROR(90006, "配置错误", 500),
    SYS_FILE_IO_ERROR(90007, "文件读写失败", 500),
    SYS_INTERNAL_ERROR(90008, "服务器内部错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;
}
