package com.example.studentarchives.config.Lzw;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型配置属性（OpenAI 兼容协议，当前接入智谱 GLM）
 * <p>
 * 通过 .env 文件注入，不写入代码/仓库：
 * <ul>
 *   <li>{@code LLM_API_KEY} — 智谱开放平台 API Key（open.bigmodel.cn 申请）</li>
 *   <li>{@code LLM_BASE_URL} — API 基础地址，默认 {@code https://open.bigmodel.cn/api/paas/v4}</li>
 *   <li>{@code LLM_MODEL} — 模型，默认 {@code glm-4-flash}</li>
 * </ul>
 * 更换服务商时只需改 base-url / model / api-key，无需改代码（接口需为 OpenAI 兼容）。
 *
 * @author lzw
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** API Key */
    private String apiKey;

    /** API 基础地址（OpenAI 兼容） */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /** 模型名称 */
    private String model = "glm-4-flash";

    /** 超时时间（毫秒） */
    private int timeoutMs = 60000;

    /** 单次最大返回 token（咨询类回答 800 足够，过大会明显拉长生成时间） */
    private int maxTokens = 800;

    /** 采样温度（0~2） */
    private double temperature = 0.7;
}
