package com.example.studentarchives.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /** 验证码唯一标识 */
    private String key;

    /** Base64 图片（含 data:image/png;base64, 前缀） */
    private String image;

    /** 明文验证码（仅开发环境返回，方便调试） */
    private String devCode;
}
