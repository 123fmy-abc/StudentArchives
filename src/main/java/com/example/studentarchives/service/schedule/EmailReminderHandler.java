package com.example.studentarchives.service.schedule;

import com.example.studentarchives.service.Fmy.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 通用邮件提醒处理器（task_code = email_reminder）。
 * <p>
 * 按配置的收件人、主题、正文定时发送邮件，适用于周期性通知、提醒、公告等。
 * <p>
 * 任务参数（scheduled_tasks.task_params）：
 * <pre>
 * {
 *   "to": ["a@b.com", "c@d.com"],
 *   "subject": "系统定时提醒",
 *   "body": "这是定时任务发送的邮件内容"
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailReminderHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "email_reminder";

    private final EmailService emailService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getHandlerName() {
        return "emailReminderHandler";
    }

    @Override
    public String getDescription() {
        return "定时发送邮件提醒，支持多个收件人";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        ReminderConfig config = ReminderConfig.from(context.getTaskParams());
        if (config.to == null || config.to.isEmpty()) {
            throw new IllegalArgumentException("taskParams.to 收件人不能为空");
        }
        if (config.subject == null || config.subject.isBlank()) {
            throw new IllegalArgumentException("taskParams.subject 邮件主题不能为空");
        }

        String body = config.body != null ? config.body : "";
        for (String to : config.to) {
            if (to == null || to.isBlank()) {
                continue;
            }
            try {
                emailService.sendSimpleMail(to, config.subject, body);
                log.info("定时邮件提醒已发送: to={}", to);
            } catch (Exception e) {
                log.error("定时邮件提醒发送失败: to={}, err={}", to, e.getMessage(), e);
                throw e;
            }
        }
    }

    private static class ReminderConfig {
        List<String> to;
        String subject;
        String body;

        static ReminderConfig from(JsonNode taskParams) {
            ReminderConfig config = new ReminderConfig();
            if (taskParams == null) {
                return config;
            }
            if (taskParams.hasNonNull("to") && taskParams.get("to").isArray()) {
                config.to = new ArrayList<>();
                taskParams.get("to").forEach(n -> config.to.add(n.asText()));
            }
            if (taskParams.hasNonNull("subject")) {
                config.subject = taskParams.get("subject").asText();
            }
            if (taskParams.hasNonNull("body")) {
                config.body = taskParams.get("body").asText();
            }
            return config;
        }
    }
}
