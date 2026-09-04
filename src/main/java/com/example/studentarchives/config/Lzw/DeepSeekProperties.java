package com.example.studentarchives.config.Lzw;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 大模型配置属性
 * <p>
 * 通过 .env 文件注入 API Key（不写入代码/仓库）：
 * <ul>
 *   <li>{@code DEEPSEEK_API_KEY} — DeepSeek API Key（platform.deepseek.com 申请）</li>
 *   <li>{@code DEEPSEEK_BASE_URL} — API 基础地址，默认 {@code https://api.deepseek.com}</li>
 *   <li>{@code DEEPSEEK_MODEL} — 模型，默认 {@code deepseek-chat}</li>
 * </ul>
 *
 * @author lzw
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    /** API Key */
    private String apiKey;

    /** API 基础地址（OpenAI 兼容） */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 超时时间（毫秒） */
    private int timeoutMs = 60000;

    /** 单次最大返回 token */
    private int maxTokens = 2000;

    /** 采样温度（0~2） */
    private double temperature = 0.7;
}