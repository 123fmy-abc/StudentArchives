package com.example.studentarchives.dto.Fmy.message.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量操作消息请求（PUT /messages/batch-read、DELETE /messages/batch，文档 5.8/5.10）
 * <p>
 * 已读与删除共用：messageIds 为当前用户待操作的消息 ID 列表。
 * 批量操作按 user_id 过滤，非本人 ID 静默跳过，返回实际处理条数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageBatchIdsRequest {

    /** 待操作消息 ID 列表（1~100 个） */
    @NotEmpty(message = "messageIds 不能为空")
    @Size(min = 1, max = 100, message = "messageIds 数量需在 1~100 之间")
    private List<Long> messageIds;
}
