package com.example.studentarchives.dto.Fmy.message.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量删除消息响应 DTO（DELETE /messages/batch，文档 5.10）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageBatchDeleteResponse {

    /** 本次实际软删除的消息条数 */
    private int deletedCount;
}
