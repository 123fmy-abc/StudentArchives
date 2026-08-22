package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.dto.Fmy.message.response.MessageArchiveResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageListResponse;
import com.example.studentarchives.service.Fmy.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端消息中心控制器
 * <p>
 * 提供教师端消息中心模块接口（《教师端接口文档》十三、消息中心模块）。
 * 消息按 {@code user_messages.user_id} 归属，教师/学生同为 users 记录，无学生特有逻辑，
 * 因此直接复用学生端 {@link MessageService}（《学生端接口文档》五、消息中心模块）——
 * 仅 URL 前缀不同（{@code /teacher/messages} ↔ {@code /messages}），
 * 请求/响应 DTO 与参数校验完全一致，无需独立服务。
 * <p>
 * 消息触发点：学生提交申报、审核结果反馈、计划反馈、导出任务完成等。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/messages")
@RequiredArgsConstructor
public class TeacherMessageController {

    private final MessageService messageService;

    /**
     * 获取消息列表（复用学生端 5.1）
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param category   分类：all、system_notice、audit_remind、dynamic_remind、private_message
     * @param isRead     状态：0=未读 1=已读，不传为全部
     * @param isArchived 是否归档：0=未归档 1=已归档，不传则默认 0
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
        MessageListResponse response = messageService.listMessages(
                userId, category, isRead, isArchived, keyword, pageParam);
        return ApiResult.success(response);
    }

    /**
     * 标记消息已读（复用学生端 5.2）
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 标记结果
     */
    @PutMapping("/{messageId}/read")
    public ApiResult<Void> markRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        messageService.markRead(userId, messageId);
        return ApiResult.success("标记成功", null);
    }

    /**
     * 归档消息（复用学生端 5.4）
     * <p>
     * 重要系统消息（is_important=1）归档前需前端二次确认。
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 归档结果
     */
    @PutMapping("/{messageId}/archive")
    public ApiResult<MessageArchiveResponse> archive(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        return ApiResult.success("归档成功", messageService.archive(userId, messageId));
    }
}
