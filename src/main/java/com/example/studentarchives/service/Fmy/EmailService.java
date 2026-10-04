package com.example.studentarchives.service.Fmy;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 邮件发送服务（同步 + 自动重试）
 * <p>
 * 同步发送邮件，失败时自动重试 3 次（间隔 2s、4s），重试耗尽后抛出异常。
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
     * 发送简单邮件（同步，自动重试 3 次）
     *
     * @param to      收件人地址
     * @param subject 邮件主题
     * @param text    邮件正文
     */
    @Retryable(
            retryFor = MailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendSimpleMail(String to, String subject, String text) {
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

    /**
     * 发送带附件的邮件（同步，自动重试 3 次）。
     *
     * @param to             收件人地址
     * @param subject        邮件主题
     * @param text           邮件正文
     * @param attachmentName 附件文件名
     * @param attachmentData 附件字节内容
     * @param contentType    附件 MIME 类型（如 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendMailWithAttachment(String to, String subject, String text,
                                       String attachmentName, byte[] attachmentData, String contentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            helper.addAttachment(attachmentName, new ByteArrayDataSource(attachmentData, contentType));
            mailSender.send(message);
            log.info("带附件邮件已发送至 {}（主题：{}，附件：{}）", to, subject, attachmentName);
        } catch (MessagingException | MailException e) {
            log.error("带附件邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw new RuntimeException("带附件邮件发送失败", e);
        }
    }
}
