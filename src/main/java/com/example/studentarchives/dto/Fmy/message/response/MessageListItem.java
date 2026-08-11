package com.example.studentarchives.dto.Fmy.message.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息列表项 DTO（GET /messages 的 list 元素，文档 5.1）
 * <p>
 * 数据来源：user_messages 表，按 created_at 倒序。
 * senderType 对应 user_messages.sender_type；senderName 由 sender_id 关联 users.name 得到。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageListItem {

    private Long id;

    /** 消息分类：system_notice/audit_remind/dynamic_remind/private_message */
    private String category;

    /** 分类展示名称（来自字典 message_category） */
    private String categoryLabel;

    private String title;

    private String content;

    /** 发送者类型：1=系统 2=人工 3=自动触发 */
    private Integer senderType;

    private String senderTypeLabel;

    /** 发送者姓名（系统消息为 null） */
    private String senderName;

    /** 0=未读 1=已读 */
    private Integer isRead;

    /** 阅读时间（ISO 8601 带时区） */
    private String readAt;

    /** 1=重要消息 */
    private Integer isImportant;

    /** 0=未归档 1=已归档 */
    private Integer isArchived;

    /** 归档时间（ISO 8601 带时区） */
    private String archivedAt;

    /** 提醒消息的截止日期（ISO 8601 带时区） */
    private String deadline;

    /** 审核消息直接跳转地址 */
    private String jumpUrl;

    /** 发送渠道：push/email/sms */
    private String sendChannel;

    /** 关联模型类型 */
    private String relatedType;

    /** 关联记录 ID */
    private Long relatedId;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
