package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.auth.request.LoginRequest;
import com.example.studentarchives.dto.Fmy.auth.request.LogoutRequest;
import com.example.studentarchives.dto.Fmy.auth.request.PasswordChangeRequest;
import com.example.studentarchives.dto.Fmy.auth.request.PasswordResetConfirmRequest;
import com.example.studentarchives.dto.Fmy.auth.request.PasswordResetRequest;
import com.example.studentarchives.dto.Fmy.auth.request.RefreshTokenRequest;
import com.example.studentarchives.dto.Fmy.auth.response.CaptchaResponse;
import com.example.studentarchives.dto.Fmy.auth.response.LoginResponse;
import com.example.studentarchives.dto.Fmy.auth.response.TokenRefreshResponse;
import com.example.studentarchives.dto.Fmy.auth.response.UserInfoResponse;
import com.example.studentarchives.service.Fmy.AuthService;
import com.example.studentarchives.support.IpAddressExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * <p>
 * 提供登录、登出、验证码、密码管理、用户信息等认证相关接口。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final IpAddressExtractor ipAddressExtractor;

    /**
     * 获取图形验证码
     * <p>
     * 公开接口，无需登录。
     */
    @GetMapping("/captcha")
    public ApiResult<CaptchaResponse> getCaptcha() {
        CaptchaResponse response = authService.generateCaptcha();
        return ApiResult.success(response);
    }

    /**
     * 用户登录
     * <p>
     * 公开接口，验证验证码 + 学号密码后返回 JWT 令牌。
     */
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest) {
        String ip = ipAddressExtractor.extract(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ip, userAgent);
        return ApiResult.success("登录成功", response);
    }

    /**
     * 获取当前用户信息
     * <p>
     * 需携带 Bearer Token。
     */
    @GetMapping("/me")
    public ApiResult<UserInfoResponse> me(@AuthenticationPrincipal Long userId) {
        UserInfoResponse response = authService.getCurrentUser(userId);
        return ApiResult.success(response);
    }

    /**
     * 退出登录
     * <p>
     * 需携带 Bearer Token。all=true 时使所有设备下线。
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@AuthenticationPrincipal Long userId,
                                  @RequestBody(required = false) LogoutRequest request) {
        if (request == null) {
            request = new LogoutRequest();
        }
        authService.logout(userId, request);
        return ApiResult.success("退出成功", null);
    }

    /**
     * 修改密码
     * <p>
     * 需携带 Bearer Token。需提供原密码和新密码。
     */
    @PutMapping("/password")
    public ApiResult<Void> changePassword(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(userId, request);
        return ApiResult.success("密码修改成功", null);
    }

    /**
     * 发送密码重置验证码邮件
     * <p>
     * 公开接口。向注册邮箱发送 6 位数字验证码。
     */
    @PostMapping("/password/reset")
    public ApiResult<Void> sendResetEmail(@Valid @RequestBody PasswordResetRequest request,
                                           HttpServletRequest httpRequest) {
        String ip = ipAddressExtractor.extract(httpRequest);
        authService.sendPasswordResetEmail(request, ip);
        return ApiResult.success("验证码已发送至您的邮箱，请查收", null);
    }

    /**
     * 确认密码重置
     * <p>
     * 公开接口。验证验证码后更新密码。
     */
    @PostMapping("/password/reset/confirm")
    public ApiResult<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ApiResult.success("密码重置成功", null);
    }

    /**
     * 刷新访问令牌
     * <p>
     * 公开接口。使用未过期的 refreshToken 换取新的 accessToken。
     */
    @PostMapping("/refresh")
    public ApiResult<TokenRefreshResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ApiResult.success(response);
    }

}
