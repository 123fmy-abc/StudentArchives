package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.config.Lzw.LlmProperties;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 大模型客户端（OpenAI 兼容协议，零第三方依赖）
 * <p>
 * 当前接入智谱 GLM（{@code https://open.bigmodel.cn/api/paas/v4}），
 * 调用 {@code POST {baseUrl}/chat/completions}，失败时抛出异常由调用方兜底。
 */
@Slf4j
@Component
public class LlmClient {

    private final ObjectMapper objectMapper;
    private final LlmProperties props;
    private final HttpClient httpClient;
    private final String chatUrl;

    public LlmClient(ObjectMapper objectMapper, LlmProperties props) {
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
     * 非流式调用大模型。
     *
     * @throws IllegalStateException 未配置 Key、网络/超时/HTTP 非 2xx、响应缺失时
     */
    public ChatResult chat(List<ChatMessage> messages) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 LLM_API_KEY");
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
            throw new IllegalStateException("大模型调用被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("大模型调用异常: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("大模型调用失败 HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) {
                throw new IllegalStateException("大模型响应缺少 choices[0].message.content");
            }
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);
            String model = root.path("model").asText(props.getModel());
            return new ChatResult(content, model, totalTokens, System.currentTimeMillis() - start);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("大模型响应解析异常: " + e.getMessage(), e);
        }
    }

    /**
     * 流式调用大模型：每收到一个增量片段就回调一次 {@code onDelta}，全部结束时返回完整结果。
     * <p>
     * 请求体设置 {@code stream=true} 与 {@code stream_options.include_usage=true}，
     * 逐行解析 SSE 的 {@code data:} 事件，取 {@code choices[0].delta.content}。
     * GLM 的 usage 只在最后一帧返回，故在循环中持续覆盖。
     *
     * @param onDelta 增量回调，在调用线程（流读取线程）上执行，不可为空
     * @throws IllegalStateException 未配置 Key、网络/超时/HTTP 非 2xx、响应为空时
     */
    public ChatResult streamChat(List<ChatMessage> messages, Consumer<String> onDelta) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 LLM_API_KEY");
        }

        long start = System.currentTimeMillis();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", props.getTemperature());
        body.put("max_tokens", props.getMaxTokens());
        body.put("stream", true);
        // 要求最后一帧携带 usage，否则流式模式下拿不到 token 统计
        body.put("stream_options", Map.of("include_usage", true));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chatUrl))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<Stream<String>> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("大模型流式调用被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("大模型流式调用异常: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody;
            try (Stream<String> lines = response.body()) {
                errorBody = lines.collect(Collectors.joining("\n"));
            }
            throw new IllegalStateException("大模型流式调用失败 HTTP " + response.statusCode() + ": " + errorBody);
        }

        StringBuilder content = new StringBuilder();
        int[] totalTokens = {0};
        String[] model = {props.getModel()};
        try (Stream<String> lines = response.body()) {
            for (String raw : (Iterable<String>) lines::iterator) {
                String line = raw.trim();
                // SSE 空行与心跳注释行跳过
                if (line.isEmpty() || line.startsWith(":") || !line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                if (delta.isTextual() && !delta.asText().isEmpty()) {
                    String piece = delta.asText();
                    content.append(piece);
                    onDelta.accept(piece);
                }
                JsonNode usage = root.path("usage");
                if (usage.isObject()) {
                    totalTokens[0] = usage.path("total_tokens").asInt(totalTokens[0]);
                }
                JsonNode modelNode = root.path("model");
                if (modelNode.isTextual()) {
                    model[0] = modelNode.asText();
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("大模型流式响应解析异常: " + e.getMessage(), e);
        }

        if (content.isEmpty()) {
            throw new IllegalStateException("大模型流式响应未返回任何内容");
        }
        return new ChatResult(content.toString(), model[0], totalTokens[0], System.currentTimeMillis() - start);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化大模型请求失败: " + e.getMessage(), e);
        }
    }
}
