package com.example.studentarchives.dto.Fmy.message.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息归档/取消归档响应 DTO（PUT /messages/{messageId}/archive 与 /unarchive，文档 5.4/5.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageArchiveResponse {

    private Long messageId;

    /** 0=未归档 1=已归档 */
    private Integer isArchived;

    /** 归档时间（ISO 8601 带时区）；取消归档时为 null */
    private String archivedAt;
}
