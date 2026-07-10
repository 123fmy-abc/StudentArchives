package com.example.studentarchives.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "学号/工号不能为空")
    private String userNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 key（由 /auth/captcha 返回） */
    private String captchaKey;

    /** 验证码内容（用户输入的图片中的字符） */
    private String captchaCode;

    /** 记住密码（true=7天免登录，false=24小时） */
    private Boolean rememberMe;
}
