package com.example.studentarchives.service.schedule;

import com.example.studentarchives.service.Fmy.MessageProducer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用站内信提醒处理器（task_code = message_reminder）。
 * <p>
 * 按配置的用户 ID 列表定时发送站内信/系统通知，适用于系统公告、待办提醒等。
 * <p>
 * 任务参数（scheduled_tasks.task_params）：
 * <pre>
 * {
 *   "userIds": [1, 2, 3],
 *   "category": "system_notice",
 *   "title": "系统公告",
 *   "content": "这是一条定时发送的站内信",
 *   "isImportant": 0,
 *   "jumpUrl": "https://example.com"
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageReminderHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "message_reminder";

    private final MessageProducer messageProducer;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getHandlerName() {
        return "messageReminderHandler";
    }

    @Override
    public String getDescription() {
        return "定时发送站内信/系统通知，支持多个接收用户";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        MessageConfig config = MessageConfig.from(context.getTaskParams());
        if (config.userIds == null || config.userIds.isEmpty()) {
            throw new IllegalArgumentException("taskParams.userIds 接收用户不能为空");
        }
        if (config.title == null || config.title.isBlank()) {
            throw new IllegalArgumentException("taskParams.title 站内信标题不能为空");
        }

        MessageProducer.MessageSpec spec = MessageProducer.MessageSpec.builder()
                .category(config.category)
                .title(config.title)
                .content(config.content != null ? config.content : "")
                .relatedType("scheduled_task")
                .relatedId(context.getTaskId())
                .jumpUrl(config.jumpUrl)
                .isImportant(config.isImportant)
                .build();

        int sent = messageProducer.sendToUsers(config.userIds, spec);
        log.info("站内信定时提醒已发送: count={}, category={}, title={}", sent, config.category, config.title);
    }

    private static class MessageConfig {
        List<Long> userIds;
        String category = MessageProducer.CATEGORY_SYSTEM_NOTICE;
        String title;
        String content;
        Integer isImportant = 0;
        String jumpUrl;

        static MessageConfig from(JsonNode taskParams) {
            MessageConfig config = new MessageConfig();
            if (taskParams == null) {
                return config;
            }
            if (taskParams.hasNonNull("userIds") && taskParams.get("userIds").isArray()) {
                config.userIds = new ArrayList<>();
                taskParams.get("userIds").forEach(n -> config.userIds.add(n.asLong()));
            }
            if (taskParams.hasNonNull("category")) {
                config.category = taskParams.get("category").asText();
            }
            if (taskParams.hasNonNull("title")) {
                config.title = taskParams.get("title").asText();
            }
            if (taskParams.hasNonNull("content")) {
                config.content = taskParams.get("content").asText();
            }
            if (taskParams.hasNonNull("isImportant")) {
                config.isImportant = taskParams.get("isImportant").asInt(0);
            }
            if (taskParams.hasNonNull("jumpUrl")) {
                config.jumpUrl = taskParams.get("jumpUrl").asText();
            }
            return config;
        }
    }
}
