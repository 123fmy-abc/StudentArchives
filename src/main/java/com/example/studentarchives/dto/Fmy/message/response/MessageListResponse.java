package com.example.studentarchives.dto.Fmy.message.response;

import com.example.studentarchives.common.PageResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息列表响应 DTO（GET /messages，文档 5.1）
 * <p>
 * total 与 pagination.total 均为当前筛选条件下的消息总数；
 * unread 为当前分类/归档范围内未读消息数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageListResponse {

    /** 消息总数（与 pagination.total 一致） */
    private long total;

    /** 未读消息数（当前分类/归档范围内，is_read=0） */
    private long unread;

    private List<MessageListItem> list;

    private PageResult.Pagination pagination;
}
