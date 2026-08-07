package com.example.studentarchives.common;

/**
 * 通用校验常量
 */
public final class ValidationConstants {

    private ValidationConstants() {}

    /**
     * 密码强度正则：6-32位，必须包含大写字母、小写字母、数字和特殊字符
     */
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{6,32}$";

    /** 密码不符合规则时的提示信息 */
    public static final String PASSWORD_MESSAGE = "密码长度需6-32位，且包含大写字母、小写字母、数字和特殊字符";

    /**
     * 中国大陆手机号正则：空串（表示清空该字段）或 11 位、以 1 开头、第二位为 3-9 的手机号
     */
    public static final String PHONE_PATTERN = "^(|1[3-9]\\d{9})$";

    /** 手机号格式不正确时的提示信息 */
    public static final String PHONE_MESSAGE = "手机号格式不正确";
}
