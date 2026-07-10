package com.example.studentarchives.controller.user;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.request.user.LoginRequest;
import com.example.studentarchives.dto.response.user.CaptchaResponse;
import com.example.studentarchives.dto.response.user.LoginResponse;
import com.example.studentarchives.dto.response.user.UserProfileResponse;
import com.example.studentarchives.service.user.CaptchaService;
import com.example.studentarchives.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;

    /** 用户登录 */
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResult.success("登录成功", response);
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public ApiResult<UserProfileResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse response = userService.getCurrentUser(userId);
        return ApiResult.success(response);
    }

    /** 获取图形验证码 */
    @GetMapping("/captcha")
    public ApiResult<CaptchaResponse> captcha() {
        CaptchaResponse response = captchaService.generateCaptcha();
        return ApiResult.success(response);
    }
}
