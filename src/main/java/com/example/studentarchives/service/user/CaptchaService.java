package com.example.studentarchives.service.user;

import com.example.studentarchives.dto.response.user.CaptchaResponse;

/**
 * 图形验证码服务
 */
public interface CaptchaService {

    /** 生成验证码，返回 key 和 base64 图片 */
    CaptchaResponse generateCaptcha();

    /** 验证验证码是否正确 */
    boolean validateCaptcha(String key, String code);
}
