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
}
