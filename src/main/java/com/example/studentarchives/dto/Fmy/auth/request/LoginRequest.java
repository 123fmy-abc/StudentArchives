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

    /**
     * 登录入口：student / admin。
     * 缺省（null 或空白）时后端按账号的实际角色兜底推导入口并照常校验，不会放行；
     * 传入值与学生入口不匹配时按方向返回对应文案；传入未知取值返回 400（见 AuthService#login 5.5）。
     */
    private String loginType;
}
