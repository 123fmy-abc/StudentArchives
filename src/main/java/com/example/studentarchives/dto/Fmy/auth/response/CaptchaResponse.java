package com.example.studentarchives.dto.Fmy.auth.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /** 验证码标识（UUID） */
    private String key;

    /** Base64 编码的 PNG 图片（含 data:image/png;base64, 前缀） */
    private String image;
}
