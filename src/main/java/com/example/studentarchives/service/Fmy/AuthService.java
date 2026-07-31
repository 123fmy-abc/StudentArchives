package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.security.JwtProperties;
import com.example.studentarchives.dto.Fmy.auth.request.LoginRequest;
import com.example.studentarchives.enums.GenderEnum;
import com.example.studentarchives.enums.StatusEnum;
import com.example.studentarchives.util.DateUtils;
import com.example.studentarchives.dto.Fmy.auth.request.LogoutRequest;
import com.example.studentarchives.dto.Fmy.auth.request.PasswordChangeRequest;
import com.example.studentarchives.dto.Fmy.auth.request.PasswordResetConfirmRequest;
import com.example.studentarchives.dto.Fmy.auth.request.PasswordResetRequest;
import com.example.studentarchives.dto.Fmy.auth.request.RefreshTokenRequest;
import com.example.studentarchives.dto.Fmy.auth.response.CaptchaResponse;
import com.example.studentarchives.dto.Fmy.auth.response.LoginResponse;
import com.example.studentarchives.dto.Fmy.auth.response.TokenRefreshResponse;
import com.example.studentarchives.dto.Fmy.auth.response.UserInfoResponse;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.user.Permission;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.entity.user.RolePermission;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.PermissionRepository;
import com.example.studentarchives.repository.RolePermissionRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.example.studentarchives.repository.projection.UserAuthStatus;
import com.example.studentarchives.support.CaptchaGenerator;
import com.example.studentarchives.support.CaptchaStore;
import com.example.studentarchives.support.LoginAttemptLimiter;
import com.example.studentarchives.support.VerificationCodeStore;
import com.example.studentarchives.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 认证服务
 * <p>
 * 处理用户认证相关的所有业务逻辑：验证码、登录、登出、
 * 密码修改/重置、获取当前用户信息等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 登录类型：密码登录 */
    private static final int LOGIN_TYPE_PASSWORD = 1;
    /** 登录状态：成功 */
    private static final int LOGIN_STATUS_SUCCESS = 1;
    /** 登录状态：失败 */
    private static final int LOGIN_STATUS_FAILED = 0;

    private final UserRepository userRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final SchoolRepository schoolRepository;
    private final LoginLogService loginLogService;

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaStore captchaStore;
    private final CaptchaGenerator captchaGenerator;
    private final VerificationCodeStore verificationCodeStore;
    private final LoginAttemptLimiter loginAttemptLimiter;

    private final EmailService emailService;

    // ==================== 验证码 ====================

    /**
     * 生成图形验证码
     */
    public CaptchaResponse generateCaptcha() {
        CaptchaGenerator.CaptchaResult result = captchaGenerator.generate();
        String key = captchaStore.store(result.getCode());
        return CaptchaResponse.builder()
                .key(key)
                .image(result.getBase64Image())
                .build();
    }

    // ==================== 登录 ====================

    /**
     * 用户登录
     *
     * @param request   登录请求
     * @param ipAddress 客户端 IP
     * @param userAgent 客户端 User-Agent
     * @return 登录响应（含 JWT 令牌和用户信息）
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // 1. 校验验证码
        if (!captchaStore.verify(request.getCaptchaKey(), request.getCaptchaCode())) {
            log.warn("[登录调试] 步骤1失败: 验证码错误, key={}, code={}", request.getCaptchaKey(), request.getCaptchaCode());
            recordLoginLog(null, null, LOGIN_STATUS_FAILED, "验证码错误", ipAddress, userAgent);
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }
        log.info("[登录调试] 步骤1通过: 验证码正确");

        // 2. 检查登录失败次数
        boolean allowed = loginAttemptLimiter.isAllowed(request.getUserNo());
        log.info("[登录调试] 步骤2: 登录限流检查结果={}", allowed);
        if (!allowed) {
            long remainingSeconds = loginAttemptLimiter.getLockoutRemainingSeconds(request.getUserNo());
            recordLoginLog(null, null, LOGIN_STATUS_FAILED, "登录失败次数过多", ipAddress, userAgent);
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "登录失败次数过多，请 " + remainingSeconds + " 秒后重试");
        }

        // 3. 查询用户
        User user = userRepository.findByUserNo(request.getUserNo()).orElse(null);
        log.info("[登录调试] 步骤3: 查询用户 userNo={}, 结果={}", request.getUserNo(), user != null ? "找到(id=" + user.getId() + ")" : "未找到");
        if (user == null) {
            recordLoginLog(null, null, LOGIN_STATUS_FAILED, "账号不存在", ipAddress, userAgent);
            loginAttemptLimiter.recordFailure(request.getUserNo());
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "账号或密码错误");
        }

        // 4. 检查账号状态
        log.info("[登录调试] 步骤4: 账号状态 status={}", user.getStatus());
        if (!StatusEnum.ENABLED.equalsValue(user.getStatus())) {
            recordLoginLog(user.getSchoolId(), user.getId(), LOGIN_STATUS_FAILED, "账号被禁用", ipAddress, userAgent);
            loginAttemptLimiter.recordFailure(request.getUserNo());
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "账号或密码错误");
        }

        // 5. 校验密码
        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("[登录调试] 步骤5: 密码校验结果={}, 密码哈希前20位={}", matched, user.getPassword().substring(0, Math.min(20, user.getPassword().length())));
        if (!matched) {
            recordLoginLog(user.getSchoolId(), user.getId(), LOGIN_STATUS_FAILED, "密码错误", ipAddress, userAgent);
            loginAttemptLimiter.recordFailure(request.getUserNo());
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "账号或密码错误");
        }

        // 6. 登录成功，清除失败计数
        loginAttemptLimiter.recordSuccess(request.getUserNo());

        // 7. 使旧 Token 失效，并获取最新版本号
        userRepository.revokeAllTokens(user.getId());
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(BusinessException::notFound);

        // 8. 生成令牌
        boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
        long accessExpiresIn = rememberMe ? jwtProperties.getRememberMeAccessExpirationMs() : jwtProperties.getExpirationMs();
        Integer tokenVersion = currentUser.getTokenVersion();
        String accessToken = jwtUtil.generateToken(
                currentUser.getId(), currentUser.getUserNo(), currentUser.getSchoolId(), tokenVersion, accessExpiresIn);
        String refreshToken = null;
        if (rememberMe) {
            refreshToken = jwtUtil.generateRefreshToken(
                    currentUser.getId(), currentUser.getUserNo(), currentUser.getSchoolId(),
                    currentUser.getRefreshTokenVersion(),
                    jwtProperties.getRememberMeRefreshExpirationMs());
        }

        // 9. 记录登录日志
        recordLoginLog(currentUser.getSchoolId(), currentUser.getId(), LOGIN_STATUS_SUCCESS, null, ipAddress, userAgent);

        // 10. 查询关联数据
        UserContactInfo contactInfo = userContactInfoRepository.findByUserId(currentUser.getId()).orElse(null);
        School school = schoolRepository.findById(currentUser.getSchoolId()).orElse(null);
        List<Role> roles = getUserRoles(currentUser.getId());

        // 11. 构建响应
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(currentUser.getId())
                .userNo(currentUser.getUserNo())
                .name(currentUser.getName())
                .email(contactInfo != null ? contactInfo.getEmail() : null)
                .gender(currentUser.getGender())
                .genderLabel(GenderEnum.of(currentUser.getGender()).getLabel())
                .schoolId(currentUser.getSchoolId())
                .schoolName(school != null ? school.getName() : null)
                .roles(roleCodes)
                .roleNames(roleNames)
                .avatar(contactInfo != null ? contactInfo.getAvatar() : null)
                .build();

        LoginResponse.LoginResponseBuilder responseBuilder = LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiresIn / 1000)
                .user(userInfo);
        if (refreshToken != null) {
            responseBuilder.refreshToken(refreshToken);
        }
        return responseBuilder.build();
    }

    // ==================== 当前用户信息 ====================

    /**
     * 获取当前登录用户信息
     *
     * @param userId 当前用户 ID
     * @return 用户详细信息（含权限）
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        UserContactInfo contactInfo = userContactInfoRepository.findByUserId(user.getId()).orElse(null);
        School school = schoolRepository.findById(user.getSchoolId()).orElse(null);
        List<Role> roles = getUserRoles(user.getId());
        List<String> permissions = getUserPermissions(roles);

        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());

        return UserInfoResponse.builder()
                .userId(user.getId())
                .userNo(user.getUserNo())
                .name(user.getName())
                .email(contactInfo != null ? contactInfo.getEmail() : null)
                .phone(contactInfo != null ? contactInfo.getPhone() : null)
                .gender(user.getGender())
                .genderLabel(GenderEnum.of(user.getGender()).getLabel())
                .schoolId(user.getSchoolId())
                .schoolName(school != null ? school.getName() : null)
                .roles(roleCodes)
                .roleNames(roleNames)
                .permissions(permissions)
                .avatar(contactInfo != null ? contactInfo.getAvatar() : null)
                .build();
    }

    // ==================== 退出登录 ====================

    /**
     * 退出登录
     *
     * @param userId  当前用户 ID
     * @param request 退出请求
     *               all=true（或 null）时吊销全部令牌（所有设备下线）；
     *               all=false 时吊销所有访问令牌和刷新令牌（仍视为当前设备下线）。
     *               因无法区分同一用户的不同客户端 Token，
     *               任何退出操作都会使该用户所有 Token 失效。
     */
    @Transactional
    public void logout(Long userId, LogoutRequest request) {
        if (!userRepository.existsById(userId)) {
            throw BusinessException.notFound();
        }
        boolean all = request != null && Boolean.TRUE.equals(request.getAll());
        userRepository.revokeAllTokens(userId);
        log.info("用户 {} 已退出{}，tokenVersion 与 refreshTokenVersion 已递增",
                userId, all ? "所有设备" : "（当前设备）");
    }

    // ==================== 刷新访问令牌 ====================

    /**
     * 使用 refreshToken 换取新的 accessToken 与 refreshToken（Rotation）
     *
     * @param request 刷新令牌请求
     * @return 新的访问令牌与刷新令牌
     */
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.getClaims(request.getRefreshToken());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(ResultCode.LOGIN_EXPIRED, "刷新令牌已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.TOKEN_ERROR, "刷新令牌无效");
        }

        Long userId = Long.valueOf(claims.getSubject());
        Integer refreshTokenVersion = claims.get("refreshTokenVersion", Integer.class);

        UserAuthStatus authStatus = userRepository.findAuthStatusById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        if (!StatusEnum.ENABLED.equalsValue(authStatus.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
        }

        // 刷新令牌版本不匹配：可能已过期、被轮换或被吊销
        if (refreshTokenVersion == null || !refreshTokenVersion.equals(authStatus.getRefreshTokenVersion())) {
            // 检测到旧 refreshToken 被复用，直接吊销用户全部令牌
            userRepository.revokeAllTokens(userId);
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token已失效，请重新登录");
        }

        // CAS 递增 refreshTokenVersion，使当前 refreshToken 一次性失效，并防止并发刷新冲突
        int updated = userRepository.compareAndIncrementRefreshTokenVersion(userId, refreshTokenVersion);
        if (updated == 0) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "并发刷新，请使用最新的刷新令牌");
        }

        // 重新读取最新版本
        UserAuthStatus newStatus = userRepository.findAuthStatusById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        String accessToken = jwtUtil.generateToken(
                userId,
                claims.get("userNo", String.class),
                claims.get("schoolId", Long.class),
                newStatus.getTokenVersion(),
                jwtProperties.getExpirationMs());

        String newRefreshToken = jwtUtil.generateRefreshToken(
                userId,
                claims.get("userNo", String.class),
                claims.get("schoolId", Long.class),
                newStatus.getRefreshTokenVersion(),
                jwtProperties.getRememberMeRefreshExpirationMs());

        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs() / 1000)
                .build();
    }

    // ==================== 修改密码 ====================

    /**
     * 修改密码
     *
     * @param userId  当前用户 ID
     * @param request 修改密码请求
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        // 校验两次密码一致
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw BusinessException.badParam("两次输入的新密码不一致");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException::notFound);

        // 校验旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }

        // 校验新密码与原密码不能相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BusinessException.badParam("新密码不能与原密码相同");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 使当前用户所有已签发 Token 失效
        userRepository.revokeAllTokens(userId);

        log.info("用户 {} 密码修改成功", userId);
    }

    // ==================== 发送密码重置邮件 ====================

    /**
     * 发送密码重置验证码邮件
     *
     * @param request     密码重置请求（含邮箱）
     * @param clientIp    请求来源 IP（用于邮件安全提示）
     */
    @Transactional(readOnly = true)
    public void sendPasswordResetEmail(PasswordResetRequest request, String clientIp) {
        String email = request.getEmail();
        String nowStr = DateUtils.nowFull();

        // 1. 校验邮箱是否注册
        UserContactInfo contactInfo = userContactInfoRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "该邮箱未注册"));

        // 确保该用户存在且有效
        User user = userRepository.findById(contactInfo.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "该邮箱未注册"));

        if (!StatusEnum.ENABLED.equalsValue(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用，无法重置密码");
        }

        // 2. 生成并存储验证码
        String code;
        try {
            code = verificationCodeStore.generate(email);
        } catch (BusinessException e) {
            // 限流异常直接抛出
            throw e;
        }

        // 3. 同步发送邮件（失败时自动重试 3 次，重试耗尽后抛异常）
        String text = "您好，\n\n"
                + "您正在申请重置学生档案系统的登录密码。\n\n"
                + "验证码：" + code + "\n"
                + "有效期：5 分钟\n"
                + "请求时间：" + nowStr + "\n"
                + "请求 IP：" + (clientIp != null ? clientIp : "未知") + "\n\n"
                + "如非本人操作，请忽略此邮件，并建议立即修改您的登录密码。\n"
                + "若重复收到此类邮件，请联系系统管理员。";
        emailService.sendSimpleMail(email, "[学生档案系统] 密码重置验证码", text);
        log.info("密码重置邮件已发送至 {}（IP: {}）", email, clientIp);
    }

    // ==================== 确认密码重置 ====================

    /**
     * 确认密码重置
     *
     * @param request 密码重置确认请求
     */
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        // 1. 校验两次密码一致
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw BusinessException.badParam("两次输入的新密码不一致");
        }

        // 2. 校验验证码
        boolean verified = verificationCodeStore.verify(request.getEmail(), request.getVerificationCode());
        if (!verified) {
            throw new BusinessException(ResultCode.TOKEN_ERROR, "验证码无效或已过期，请重新获取");
        }

        // 3. 根据邮箱查找用户
        UserContactInfo contactInfo = userContactInfoRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "该邮箱未注册"));

        User user = userRepository.findById(contactInfo.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "该邮箱未注册"));

        // 4. 校验新密码与原密码不能相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BusinessException.badParam("重置密码不能与原密码相同");
        }

        // 5. 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 6. 使旧 Token 失效
        userRepository.revokeAllTokens(user.getId());

        log.info("用户 {} 密码重置成功（邮箱：{}）", user.getId(), request.getEmail());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取用户的所有角色
     */
    private List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        return roleRepository.findByIdIn(roleIds);
    }

    /**
     * 获取用户的所有权限编码（通过角色继承获取）
     */
    private List<String> getUserPermissions(List<Role> roles) {
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdIn(roleIds);
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        return permissionRepository.findByIdIn(permissionIds).stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 记录登录日志
     * <p>
     * 所有登录尝试（成功/失败）均记录，失败原因通过 failReason 传递，
     * 保留截止时间默认 180 天，逾期可由定时任务清理。
     */
    private void recordLoginLog(Long schoolId, Long userId, int status, String failReason,
                                String ipAddress, String userAgent) {
        loginLogService.recordLoginLog(schoolId, userId, status, failReason, ipAddress, userAgent);
    }
}
