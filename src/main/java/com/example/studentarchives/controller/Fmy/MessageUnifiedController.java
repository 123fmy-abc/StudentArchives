package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.dto.Fmy.message.response.MessageListResponse;
import com.example.studentarchives.service.Fmy.MessageUnifiedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一消息列表控制器（GET /messages/unified）
 * <p>
 * 新增端点，明确归档参数语义：不传 {@code isArchived} 时查询全部（含已归档/未归档），
 * 传 0/1 时单态查询——与既有 {@code GET /messages} 行为一致，返回结构完全相同
 * （{@link MessageListResponse}），前端可将"两态合并拉取"替换为单次调用。
 * <p>
 * 需携带 Bearer Token 认证，userId 由 JWT 过滤器注入。
 */
@Slf4j
@RestController
@RequestMapping("/messages/unified")
@RequiredArgsConstructor
public class MessageUnifiedController {

    private final MessageUnifiedService messageUnifiedService;

    /**
     * 获取统一消息列表
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param category   分类：all、system_notice、audit_remind、dynamic_remind、private_message
     * @param isRead     状态：0=未读 1=已读，不传为全部
     * @param isArchived 是否归档：0=未归档 1=已归档，不传则查询全部（含已归档）
     * @param keyword    搜索关键词（匹配 title 或 content）
     * @param page       页码，默认 1
     * @param perPage    每页条数，默认 10
     * @return 消息列表
     */
    @GetMapping
    public ApiResult<MessageListResponse> listMessages(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "isRead", required = false) Integer isRead,
            @RequestParam(value = "isArchived", required = false) Integer isArchived,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "10") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        MessageListResponse response = messageUnifiedService.listMessages(
                userId, category, isRead, isArchived, keyword, pageParam);
        return ApiResult.success(response);
    }
}
