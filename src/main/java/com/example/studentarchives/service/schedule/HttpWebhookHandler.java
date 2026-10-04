package com.example.studentarchives.service.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.Map;

/**
 * 通用 HTTP Webhook 处理器（task_code = http_webhook）。
 * <p>
 * 定时向配置的 URL 发起 HTTP 调用，适用于调用企业微信/钉钉机器人、外部同步接口、
 * 自建脚本网关等无代码场景。
 * <p>
 * 任务参数（scheduled_tasks.task_params）：
 * <pre>
 * {
 *   "url": "https://oapi.dingtalk.com/robot/send?access_token=xxx",
 *   "method": "POST",
 *   "headers": {
 *     "Content-Type": "application/json"
 *   },
 *   "body": {
 *     "msgtype": "text",
 *     "text": {
 *       "content": "定时任务触发"
 *     }
 *   }
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpWebhookHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "http_webhook";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getHandlerName() {
        return "httpWebhookHandler";
    }

    @Override
    public String getDescription() {
        return "定时调用 HTTP 接口（Webhook），可用于通知机器人、外部系统同步等";
    }

    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        WebhookConfig config = WebhookConfig.from(context.getTaskParams());
        if (config.url == null || config.url.isBlank()) {
            throw new IllegalArgumentException("taskParams.url 不能为空");
        }

        HttpHeaders headers = new HttpHeaders();
        if (config.headers != null) {
            config.headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    headers.set(k, v);
                }
            });
        }

        HttpMethod method = HttpMethod.valueOf(config.method.toUpperCase());
        String body = config.body != null ? objectMapper.writeValueAsString(config.body) : null;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        log.info("HTTP Webhook 调用: method={}, url={}", method, config.url);
        try {
            ResponseEntity<String> response = restTemplate.exchange(config.url, method, entity, String.class);
            log.info("HTTP Webhook 响应: status={}, body={}", response.getStatusCode().value(),
                    response.getBody());
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Webhook 返回非 2xx 状态码: " + response.getStatusCode().value());
            }
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Webhook 调用失败: status=" + e.getRawStatusCode() + ", body=" + e.getResponseBodyAsString(), e);
        }
    }

    private static class WebhookConfig {
        String url;
        String method = "POST";
        Map<String, String> headers;
        Object body;

        static WebhookConfig from(JsonNode taskParams) {
            WebhookConfig config = new WebhookConfig();
            if (taskParams == null) {
                return config;
            }
            if (taskParams.hasNonNull("url")) {
                config.url = taskParams.get("url").asText();
            }
            if (taskParams.hasNonNull("method")) {
                config.method = taskParams.get("method").asText();
            }
            if (taskParams.hasNonNull("headers") && taskParams.get("headers").isObject()) {
                JsonNode headersNode = taskParams.get("headers");
                config.headers = new java.util.HashMap<>();
                for (Iterator<String> it = headersNode.fieldNames(); it.hasNext(); ) {
                    String key = it.next();
                    config.headers.put(key, headersNode.get(key).asText());
                }
            }
            if (taskParams.hasNonNull("body")) {
                config.body = taskParams.get("body");
            }
            return config;
        }
    }
}
