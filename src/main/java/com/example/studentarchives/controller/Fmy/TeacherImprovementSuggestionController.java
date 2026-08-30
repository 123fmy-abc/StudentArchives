package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.weakness.request.TeacherImprovementSuggestionRequest;
import com.example.studentarchives.dto.Fmy.weakness.response.TeacherImprovementSuggestionResponse;
import com.example.studentarchives.service.Fmy.TeacherImprovementSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端改进建议控制器（《教师端接口文档》十、短板识别与改进建议模块）
 * <p>
 * 提供教师对目标学生新增改进建议的接口，统一前缀 /teacher/students。
 * 需登录访问，数据范围按教师授权校验（越权返回 20005）。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/students")
@RequiredArgsConstructor
public class TeacherImprovementSuggestionController {

    private final TeacherImprovementSuggestionService teacherImprovementSuggestionService;

    /**
     * 新增改进建议（POST /teacher/students/{studentId}/improvement-suggestions，教师端文档 10.1）
     *
     * @param userId    当前登录用户 ID（由 JWT 过滤器注入）
     * @param studentId 目标学生用户 ID
     * @param request   建议内容
     * @return 建议 ID 与创建时间
     */
    @PostMapping("/{studentId:[0-9]+}/improvement-suggestions")
    public ApiResult<TeacherImprovementSuggestionResponse> addSuggestion(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long studentId,
            @Valid @RequestBody TeacherImprovementSuggestionRequest request) {
        return ApiResult.success("建议已添加",
                teacherImprovementSuggestionService.addSuggestion(userId, studentId, request));
    }
}
