package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityListItemResponse;
import com.example.studentarchives.service.Lzw.StudentManageService;
import com.example.studentarchives.service.Lzw.StudentManageService.StudentItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端「学生管理模块」控制器（Lzw）
 * <p>
 * 对应《教师端接口文档》七、学生管理模块（7.1 / 7.2）。
 * 路径 {@code /teacher/students}，需认证；数据范围按 {@code role_scopes} 限制。
 */
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherStudentManageController {

    private final StudentManageService studentManageService;

    @GetMapping("/students")
    public ApiResult<PageResult<StudentItem>> listStudents(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "scopeType", required = false) Integer scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(studentManageService.listStudents(userId, scopeType, scopeId,
                semesterId, grade, keyword, pageParam));
    }

    @GetMapping("/students/{studentId}/activities")
    public ApiResult<PageResult<ActivityListItemResponse>> listActivities(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long studentId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "10") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(studentManageService.listActivities(userId, studentId,
                type, status, semesterId, pageParam));
    }
}