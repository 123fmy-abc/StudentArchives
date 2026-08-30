package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.AuditHistoryService;
import com.example.studentarchives.service.Lzw.AuditHistoryService.HistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端「审核历史模块」控制器（Lzw）
 * <p>
 * 对应《教师端接口文档》五、审核历史模块（5.1）。
 * 路径 {@code /teacher/audits/history}，需认证。
 */
@RestController
@RequestMapping("/teacher/audits")
@RequiredArgsConstructor
public class TeacherAuditHistoryController {

    private final AuditHistoryService auditHistoryService;

    @GetMapping("/history")
    public ApiResult<PageResult<HistoryItem>> listHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "action", required = false) Integer action,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(auditHistoryService.listHistory(userId, type, action, semesterId,
                startDate, endDate, keyword, pageParam));
    }
}