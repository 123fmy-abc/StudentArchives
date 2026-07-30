package com.example.studentarchives.service.Fmy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务（异步 + 自动重试）
 * <p>
 * 所有邮件发送操作使用独立线程池异步执行，避免阻塞主请求线程。
 * 发送失败时自动重试 3 次（间隔 2s、4s），重试耗尽后仅记录日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /** 发件人地址（从配置 spring.mail.properties.mail.from 读取） */
    @Value("${spring.mail.properties.mail.from}")
    private String mailFrom;

    /**
     * 异步发送简单邮件（自动重试 3 次）
     *
     * @param to      收件人地址
     * @param subject 邮件主题
     * @param text    邮件正文
     */
    @Async
    @Retryable(
            retryFor = MailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendSimpleMailAsync(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("邮件已发送至 {}（主题：{}）", to, subject);
        } catch (MailException e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw e;
        }
    }
}
