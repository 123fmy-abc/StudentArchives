package com.example.studentarchives.dto.Fmy.message.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息通知设置项 DTO（GET /messages/settings，文档 5.6）
 * <p>
 * 数据来源：notification_settings 表。
 * 未配置的分类返回默认值：emailEnabled=1、smsEnabled=0、pushEnabled=1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageSettingItem {

    /** 通知分类：audit_remind/system_notice/dynamic_remind/private_message */
    private String category;

    /** 分类展示名称（来自字典 message_category） */
    private String categoryLabel;

    /** 0=关闭 1=开启邮件通知 */
    private Integer emailEnabled;

    /** 0=关闭 1=开启短信通知 */
    private Integer smsEnabled;

    /** 0=关闭 1=开启站内推送 */
    private Integer pushEnabled;
}
