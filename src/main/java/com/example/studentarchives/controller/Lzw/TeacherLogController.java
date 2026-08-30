package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.TeacherLogService;
import com.example.studentarchives.service.Lzw.TeacherLogService.LogItem;
import com.example.studentarchives.service.Lzw.TeacherLogService.LogQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端「操作日志查询模块」控制器（Lzw）
 * <p>
 * 对应《教师端接口文档》六、操作日志查询模块（6.1）。
 * 路径 {@code /teacher/logs}，权限码 {@code log:view}。
 */
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherLogController {

    private final TeacherLogService teacherLogService;

    @GetMapping("/logs")
    public ApiResult<PageResult<LogItem>> listLogs(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "logLevel", required = false) Integer logLevel,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "relatedType", required = false) String relatedType,
            @RequestParam(value = "relatedId", required = false) Long relatedId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        LogQuery query = LogQuery.builder()
                .operatorId(operatorId)
                .action(action)
                .module(module)
                .logLevel(logLevel)
                .startTime(startTime)
                .endTime(endTime)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .grade(grade)
                .keyword(keyword)
                .build();
        return ApiResult.success(teacherLogService.listLogs(userId, query, pageParam));
    }
}