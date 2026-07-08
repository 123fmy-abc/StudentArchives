package com.example.studentarchives.service.user;

import com.example.studentarchives.dto.request.user.LoginRequest;
import com.example.studentarchives.dto.response.user.LoginResponse;
import com.example.studentarchives.dto.response.user.UserProfileResponse;

public interface UserService {

    /** 用户登录，返回 JWT 令牌和用户信息 */
    LoginResponse login(LoginRequest request);

    /** 获取当前登录用户信息 */
    UserProfileResponse getCurrentUser(Long userId);
}
