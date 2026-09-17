package com.example.studentarchives.support;

import com.example.studentarchives.config.Fmy.AuthCaptchaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 免验证码登录策略（仅供测试环境自动化登录使用）
 * <p>
 * 采用 <b>fail-closed</b> 设计：以下任一条件不满足即返回 {@code false}，走正常的图形验证码校验。
 * <ol>
 *   <li><b>硬编码生产守卫</b> —— 任一 active profile 是 prod/production/prd 则永不跳过。
 *       该判断写死在代码里，无法通过任何配置项、命令行参数或环境变量绕过。</li>
 *   <li>{@code auth.captcha.skip-code-verify = true}</li>
 *   <li>当前所有 active profile 都落在 {@code auth.captcha.skip-allowed-profiles} 内</li>
 *   <li>{@code auth.captcha.skip-secret} 已配置，且与请求头密钥常量时间相等</li>
 *   <li>登录账号在 {@code auth.captcha.skip-users} 白名单内</li>
 * </ol>
 * 因此：正式账号、缺少密钥的请求、以及任何包含生产 profile 的启动方式，都照常校验验证码。
 *
 * @author fmy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaSkipPolicy {

    /**
     * 生产 profile 名称（硬编码，不可配置）。
     * <p>
     * 刻意不做成配置项：一旦可配置，{@code --spring.profiles.active=prod
     * --auth.captcha.skip-allowed-profiles=prod} 就能把生产也放行，
     * 「生产强制关闭」将形同虚设。
     */
    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production", "prd");

    private final AuthCaptchaProperties properties;
    private final Environment environment;

    /**
     * 启动自检：把「配置了但实际不会生效」这种沉默失败变成一条明确的 WARN，
     * 避免排查「为什么带了请求头还是提示验证码错误」时无从下手。
     */
    @PostConstruct
    public void reportReadiness() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        String reason = profileGateFailureReason(activeProfiles);
        if (reason == null) {
            if (properties.isSkipCodeVerify()) {
                log.warn("[CAPTCHA-SKIP] 免验证码登录已就绪: activeProfiles={}, skipUsers={}",
                        activeProfiles, properties.getSkipUsers());
            }
            return;
        }
        if (properties.isSkipCodeVerify()) {
            log.warn("[CAPTCHA-SKIP] 配置了 skip-code-verify=true，但当前环境不会生效（{}），"
                    + "登录仍将正常校验验证码。activeProfiles={}", reason, activeProfiles);
        }
    }

    /**
     * 判断本次登录是否可以跳过图形验证码校验。
     *
     * @param userNo      登录账号（学号/工号）
     * @param headerValue 约定请求头的值，未携带时为 null
     * @return true 表示跳过验证码校验；false 表示照常校验
     */
    public boolean shouldSkip(String userNo, String headerValue) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());

        // 0 + 1 + 2. 环境校验（生产守卫 / 总开关 / profile 白名单）
        if (profileGateFailureReason(activeProfiles) != null) {
            return false;
        }

        // 3. 密钥：未配置即整体关闭，避免「忘配密钥导致无门槛免验」
        String secret = properties.getSkipSecret();
        if (secret == null || secret.isBlank() || !secretMatches(headerValue, secret)) {
            return false;
        }

        // 4. 账号白名单：正式账号一律照常校验
        if (userNo == null || userNo.isBlank() || !properties.getSkipUsers().contains(userNo)) {
            return false;
        }

        return true;
    }

    /**
     * 环境相关的前三道关（生产守卫 / 总开关 / profile 白名单）。
     *
     * @return null 表示通过；否则返回未通过的原因（用于日志）
     */
    private String profileGateFailureReason(List<String> activeProfiles) {
        // 0. 生产守卫：硬编码，优先级最高，任何配置和参数都改不动它
        if (activeProfiles.isEmpty()) {
            return "未激活任何 profile";
        }
        for (String profile : activeProfiles) {
            if (PRODUCTION_PROFILES.contains(profile.toLowerCase(Locale.ROOT))) {
                return "命中生产 profile '" + profile + "'，免验证码能力被强制禁用";
            }
        }

        // 1. 总开关
        if (!properties.isSkipCodeVerify()) {
            return "skip-code-verify 未开启";
        }

        // 2. profile 白名单：要求「全部」active profile 都在名单内，
        //    这样 prod,test 这类组合不会被放行
        //    （test profile 会通过 application.yml 的 profile group 同时激活 dev，
        //     因此 skip-allowed-profiles 里需要一并写上 dev）
        List<String> allowedProfiles = properties.getSkipAllowedProfiles();
        if (allowedProfiles.isEmpty()) {
            return "skip-allowed-profiles 未配置";
        }
        if (!allowedProfiles.containsAll(activeProfiles)) {
            return "activeProfiles=" + activeProfiles + " 未全部落在 skip-allowed-profiles=" + allowedProfiles + " 内";
        }

        return null;
    }

    /**
     * 常量时间比较，避免通过响应耗时逐字符试探密钥。
     */
    private boolean secretMatches(String provided, String expected) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
