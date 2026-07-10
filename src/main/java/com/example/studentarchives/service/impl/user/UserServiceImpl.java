package com.example.studentarchives.service.impl.user;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.request.user.LoginRequest;
import com.example.studentarchives.dto.response.user.LoginResponse;
import com.example.studentarchives.dto.response.user.UserProfileResponse;
import com.example.studentarchives.entity.log.LoginLog;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.enums.StatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.mapper.UserMapper;
import com.example.studentarchives.repository.log.LoginLogRepository;
import com.example.studentarchives.repository.role.PermissionRepository;
import com.example.studentarchives.repository.role.RoleRepository;
import com.example.studentarchives.repository.role.UserRoleRepository;
import com.example.studentarchives.repository.user.UserRepository;
import com.example.studentarchives.service.user.CaptchaService;
import com.example.studentarchives.service.user.UserService;
import com.example.studentarchives.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 记住密码：令牌过期时间（7天） */
    private static final long REMEMBER_ME_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    /** 普通登录：令牌过期时间（24小时） */
    private static final long DEFAULT_EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final LoginLogRepository loginLogRepository;
    private final CaptchaService captchaService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        // 0. 验证图形验证码（强制校验）
        if (!StringUtils.hasText(loginRequest.getCaptchaKey()) || !StringUtils.hasText(loginRequest.getCaptchaCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入验证码");
        }
        boolean valid = captchaService.validateCaptcha(
                loginRequest.getCaptchaKey(), loginRequest.getCaptchaCode());
        if (!valid) {
            log.warn("登录失败：验证码错误 userNo={}", loginRequest.getUserNo());
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }

        // 1. 查找用户
        User user = userRepository.findByUserNo(loginRequest.getUserNo())
                .orElseThrow(() -> {
                    log.warn("登录失败：用户不存在 userNo={}", loginRequest.getUserNo());
                    return new BusinessException(ResultCode.PASSWORD_ERROR, "学号/工号或密码错误");
                });

        // 2. 验证密码（不区分"用户不存在"和"密码错误"，防止枚举攻击）
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            recordLoginLog(user.getId(), (byte) 0, "密码错误");
            log.warn("登录失败：密码错误 userNo={}", loginRequest.getUserNo());
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "学号/工号或密码错误");
        }

        // 3. 检查账号状态
        if (user.getStatus() == StatusEnum.DISABLED) {
            recordLoginLog(user.getId(), (byte) 0, "账号被禁用");
            log.warn("登录失败：账号被禁用 userNo={}", loginRequest.getUserNo());
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 4. 判断是否为"记住密码"模式
        boolean rememberMe = Boolean.TRUE.equals(loginRequest.getRememberMe());
        long expiresIn = rememberMe ? REMEMBER_ME_EXPIRATION_MS : DEFAULT_EXPIRATION_MS;

        // 5. 生成令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUserNo(), user.getSchoolId(), expiresIn);

        // 6. 记住密码：生成 rememberToken 并持久化
        if (rememberMe) {
            String rememberToken = UUID.randomUUID().toString().replace("-", "");
            user.setRememberToken(rememberToken);
            userRepository.save(user);
            log.debug("记住密码：已为用户 userId={} 生成 rememberToken", user.getId());
        }

        // 7. 记录登录成功日志
        recordLoginLog(user.getId(), (byte) 1, null);

        log.info("登录成功 userNo={}, userId={}, rememberMe={}", user.getUserNo(), user.getId(), rememberMe);

        // 8. 返回结果
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userInfo(buildUserProfile(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException::notFound);
        return buildUserProfile(user);
    }

    /** 构建完整用户信息（含角色权限） */
    private UserProfileResponse buildUserProfile(User user) {
        UserProfileResponse profile = userMapper.toProfile(user);

        // 查询角色
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(UserRole::getRoleId)
                    .collect(Collectors.toList());
            List<Role> roles = roleRepository.findByIdIn(roleIds);

            profile.setRoles(roles.stream().map(Role::getCode).collect(Collectors.toList()));
            profile.setRoleNames(roles.stream().map(Role::getName).collect(Collectors.toList()));

            // 查询权限
            var permissions = permissionRepository.findByRoleIds(roleIds);
            profile.setPermissions(permissions.stream()
                    .map(p -> p.getCode())
                    .distinct()
                    .collect(Collectors.toList()));
        } else {
            profile.setRoles(Collections.emptyList());
            profile.setRoleNames(Collections.emptyList());
            profile.setPermissions(Collections.emptyList());
        }

        return profile;
    }

    /** 记录登录日志 */
    private void recordLoginLog(Long userId, byte loginStatus, String failReason) {
        try {
            LoginLog logEntry = new LoginLog();
            logEntry.setUserId(userId);
            logEntry.setLoginType((byte) 1); // 密码登录
            logEntry.setLoginStatus(loginStatus);
            logEntry.setFailReason(failReason);
            logEntry.setIpAddress(getClientIp());
            logEntry.setUserAgent(request.getHeader("User-Agent"));
            logEntry.setSchoolId(1L); // 默认为第一个学校（测试大学）
            loginLogRepository.saveAndFlush(logEntry);
        } catch (Exception e) {
            log.warn("记录登录日志异常", e);
        }
    }

    /** 获取客户端 IP */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
