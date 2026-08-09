package com.example.studentarchives.dto.Fmy.message.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新消息通知设置请求（PUT /messages/settings，文档 5.7）
 * <p>
 * 数据来源：notification_settings 表，条件唯一索引 (user_id, category)，
 * 不存在则新增，存在则更新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSettingUpdateRequest {

    /** 通知分类（audit_remind/system_notice/dynamic_remind/private_message） */
    @NotBlank(message = "通知分类不能为空")
    private String category;

    /** 0=关闭 1=开启邮件通知 */
    @NotNull(message = "邮件通知开关不能为空")
    private Integer emailEnabled;

    /** 0=关闭 1=开启短信通知 */
    @NotNull(message = "短信通知开关不能为空")
    private Integer smsEnabled;

    /** 0=关闭 1=开启站内推送 */
    @NotNull(message = "站内推送开关不能为空")
    private Integer pushEnabled;
}
