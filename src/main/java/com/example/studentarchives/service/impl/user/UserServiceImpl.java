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
import com.example.studentarchives.repository.role.PermissionRepository;
import com.example.studentarchives.repository.role.RoleRepository;
import com.example.studentarchives.repository.role.UserRoleRepository;
import com.example.studentarchives.repository.user.UserRepository;
import com.example.studentarchives.service.user.UserService;
import com.example.studentarchives.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
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

        // 4. 生成令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUserNo(), user.getSchoolId());

        // 5. 记录登录成功日志
        recordLoginLog(user.getId(), (byte) 1, null);

        log.info("登录成功 userNo={}, userId={}", user.getUserNo(), user.getId());

        // 6. 返回结果
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400000)
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
            logEntry.setSchoolId(0L); // 登录时未确定 school，后续可补充
            // 使用 EntityManager 或直接注入 LoginLogRepository 来保存
            // 暂时跳过持久化以避免额外依赖，后面再完善
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
