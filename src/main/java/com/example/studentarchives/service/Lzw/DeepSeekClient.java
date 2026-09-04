package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.config.Lzw.DeepSeekProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大模型客户端（OpenAI 兼容协议，零第三方依赖）
 * <p>
 * 调用 {@code POST {baseUrl}/chat/completions}，失败时抛出异常由调用方兜底。
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final ObjectMapper objectMapper;
    private final DeepSeekProperties props;
    private final HttpClient httpClient;
    private final String chatUrl;

    public DeepSeekClient(ObjectMapper objectMapper, DeepSeekProperties props) {
        this.objectMapper = objectMapper;
        this.props = props;
        this.chatUrl = (props.getBaseUrl() == null ? "" : props.getBaseUrl().replaceAll("/+$", ""))
                + "/chat/completions";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();
    }

    /** 一条消息（OpenAI 兼容格式） */
    public record ChatMessage(String role, String content) {
    }

    /** 调用结果 */
    public record ChatResult(String content, String model, int totalTokens, long generationTimeMs) {
    }

    /**
     * 调用 DeepSeek 生成回复。
     *
     * @throws IllegalStateException 未配置 Key、网络/超时/HTTP 非 2xx、响应缺失时
     */
    public ChatResult chat(List<ChatMessage> messages) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 DEEPSEEK_API_KEY");
        }

        long start = System.currentTimeMillis();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", props.getTemperature());
        body.put("max_tokens", props.getMaxTokens());
        body.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chatUrl))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek 调用被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 调用异常: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DeepSeek 调用失败 HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) {
                throw new IllegalStateException("DeepSeek 响应缺少 choices[0].message.content");
            }
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);
            String model = root.path("model").asText(props.getModel());
            return new ChatResult(content, model, totalTokens, System.currentTimeMillis() - start);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 响应解析异常: " + e.getMessage(), e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 DeepSeek 请求失败: " + e.getMessage(), e);
        }
    }
}