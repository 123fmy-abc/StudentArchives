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
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ========== 通用（0-9） ==========
    SUCCESS(0, "success"),
    SYSTEM_ERROR(1, "系统异常"),
    DATA_NOT_FOUND(2, "数据不存在"),
    DATA_ALREADY_EXISTS(3, "数据已存在"),
    OPERATION_FAILED(4, "操作失败"),
    FORBIDDEN(5, "禁止操作"),
    TOO_MANY_REQUESTS(6, "请求过于频繁"),
    SERVICE_UNAVAILABLE(7, "服务暂不可用"),
    NETWORK_ERROR(8, "网络异常"),
    CONCURRENT_CONFLICT(9, "并发冲突"),

    // ========== 参数相关（10000-19999） ==========
    PARAM_ERROR(10001, "参数错误"),
    PARAM_MISSING(10002, "必填参数缺失"),
    PARAM_FORMAT_ERROR(10003, "参数格式错误"),
    PARAM_OUT_OF_RANGE(10004, "参数超出范围"),
    PARAM_ILLEGAL(10005, "非法参数"),
    FILE_FORMAT_ERROR(10006, "文件格式错误"),
    FILE_TOO_LARGE(10007, "文件过大"),
    FILE_UPLOAD_FAILED(10008, "上传失败"),

    // ========== 认证授权（20000-29999） ==========
    UNAUTHORIZED(20001, "未登录"),
    TOKEN_INVALID(20002, "Token失效"),
    TOKEN_ERROR(20003, "Token错误"),
    LOGIN_EXPIRED(20004, "登录已过期"),
    ACCESS_DENIED(20005, "无访问权限"),
    ACCOUNT_DISABLED(20006, "账号被禁用"),
    ACCOUNT_FROZEN(20007, "账号被冻结"),
    PASSWORD_ERROR(20008, "密码错误"),

    // ========== 数据相关（30000-39999） ==========
    DATA_DELETED(30001, "数据已删除"),
    DATA_DUPLICATE(30003, "数据重复"),
    DATA_STATUS_ERROR(30004, "数据状态异常"),
    DATA_LOCKED(30005, "数据已锁定"),
    DATA_RELATION_EXISTS(30006, "数据关联存在"),
    DATA_VALIDATION_FAILED(30007, "数据校验失败"),
    DATA_VERSION_CONFLICT(30008, "数据版本冲突"),

    // ========== 业务相关（40000-49999） ==========
    BIZ_OPERATION_FAILED(40001, "操作失败"),
    BIZ_STATUS_NOT_OPERABLE(40002, "当前状态不可操作"),
    BIZ_AUDIT_NOT_PASSED(40003, "审批未通过"),
    BIZ_INSUFFICIENT_STOCK(40004, "库存不足"),
    BIZ_AMOUNT_EXCEED(40005, "金额超限"),
    BIZ_QUOTA_EXCEED(40006, "超出配额"),
    BIZ_ALREADY_SUBMITTED(40007, "已提交审核"),
    BIZ_COMPLETED_NOT_MODIFIABLE(40008, "已完成不可修改"),
    BIZ_DUPLICATE_SUBMISSION(40009, "重复提交"),
    BIZ_RULE_LIMIT(40010, "超出业务规则限制"),

    // ========== 第三方服务（50000-59999） ==========
    THIRD_WECHAT_ERROR(50001, "微信接口异常"),
    THIRD_ALIPAY_ERROR(50002, "支付宝接口异常"),
    THIRD_SMS_FAILED(50003, "短信发送失败"),
    THIRD_EMAIL_FAILED(50004, "邮件发送失败"),
    THIRD_OSS_FAILED(50005, "OSS上传失败"),
    THIRD_REDIS_FAILED(50006, "Redis连接失败"),
    THIRD_MQ_FAILED(50007, "MQ消息发送失败"),
    THIRD_TIMEOUT(50008, "第三方接口超时"),

    // ========== 数据库异常（60000-69999） ==========
    DB_CONNECTION_FAILED(60001, "数据库连接失败"),
    DB_SQL_ERROR(60002, "SQL执行失败"),
    DB_TRANSACTION_FAILED(60003, "事务提交失败"),
    DB_ROLLBACK(60004, "事务回滚"),
    DB_UNIQUE_CONSTRAINT(60005, "唯一索引冲突"),
    DB_FOREIGN_KEY_ERROR(60006, "外键约束失败"),
    DB_DEADLOCK(60007, "死锁异常"),
    DB_TIMEOUT(60008, "数据库超时"),

    // ========== 系统异常（90000-99999） ==========
    SYS_ERROR(90001, "系统异常"),
    SYS_UNKNOWN(90002, "未知错误"),
    SYS_RUNTIME(90003, "程序运行异常"),
    SYS_BUSY(90004, "服务繁忙"),
    SYS_MAINTENANCE(90005, "系统维护中"),
    SYS_CONFIG_ERROR(90006, "配置错误"),
    SYS_FILE_IO_ERROR(90007, "文件读写失败"),
    SYS_INTERNAL_ERROR(90008, "服务器内部错误");

    private final int code;
    private final String message;
}
