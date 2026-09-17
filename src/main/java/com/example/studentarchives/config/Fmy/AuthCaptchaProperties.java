package com.example.studentarchives.config.Fmy;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 登录验证码配置属性
 * <p>
 * 提供「测试环境指定账号免图形验证码登录」能力，用于端到端自动化测试，
 * 避免每次登录都要人工识别 4 位图形验证码。
 * <p>
 * <b>该能力默认整体关闭</b>，只有配置齐全且运行在非生产 profile 下才可能生效，
 * 具体判定逻辑见 {@link com.example.studentarchives.support.CaptchaSkipPolicy}。
 * <p>
 * 生产环境禁止使用：{@code skip-code-verify} 即使被命令行参数或环境变量强行覆盖为
 * {@code true}，也会被 {@code CaptchaSkipPolicy} 中硬编码的生产 profile 守卫拦下。
 *
 * @author fmy
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "auth.captcha")
public class AuthCaptchaProperties {

    /**
     * 免验证码总开关，默认 false。
     * <p>
     * 对应需求中的 {@code skipCodeVerify}。
     */
    private boolean skipCodeVerify = false;

    /**
     * 允许启用免验证码能力的 profile 白名单，默认空（= 关闭）。
     * <p>
     * 要求「当前所有 active profile 都落在此列表内」才生效，
     * 因此 {@code prod,test} 这类组合不会被放行。
     */
    private List<String> skipAllowedProfiles = List.of();

    /**
     * 免验证码账号白名单（学号/工号），默认空（= 关闭）。
     * <p>
     * 只有列在这里的测试账号可以跳过验证码；正式账号一律照常校验。
     */
    private List<String> skipUsers = List.of();

    /** 调用免验证码能力必须携带的请求头名称 */
    private String skipHeaderName = "X-Captcha-Skip-Token";

    /**
     * 免验证码请求头密钥，默认空（= 关闭）。
     * <p>
     * 禁止硬编码在仓库中，生产/测试均由环境变量 {@code CAPTCHA_SKIP_SECRET} 注入。
     * 为空时功能整体失效（fail closed）。
     */
    private String skipSecret = "";

    /**
     * 启动时若开关处于打开状态，打印一条醒目的 WARN，方便审计确认实例状态。
     */
    @PostConstruct
    public void logState() {
        if (skipCodeVerify) {
            log.warn("[CAPTCHA-SKIP] 免验证码登录开关已打开: allowedProfiles={}, skipUsers={}, "
                            + "header={}, secretConfigured={}。该能力仅限测试环境使用，生产环境会被强制忽略。",
                    skipAllowedProfiles, skipUsers, skipHeaderName, !skipSecret.isBlank());
        }
    }
}
