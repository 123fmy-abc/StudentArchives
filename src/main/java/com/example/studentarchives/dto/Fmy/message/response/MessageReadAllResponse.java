package com.example.studentarchives.dto.Fmy.message.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量标记已读响应 DTO（PUT /messages/read-all，文档 5.3）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageReadAllResponse {

    /** 本次标记已读的消息条数 */
    private int markedCount;
}
