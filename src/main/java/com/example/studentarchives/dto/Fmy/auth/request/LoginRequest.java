package com.example.studentarchives.dto.Fmy.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "学号不能为空")
    private String userNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaKey;

    private String captchaCode;

    /** 是否记住我，默认 false */
    private Boolean rememberMe = false;
}
