package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.career.request.TeacherCareerFeedbackRequest;
import com.example.studentarchives.dto.Fmy.career.response.TeacherCareerFeedbackResponse;
import com.example.studentarchives.service.Fmy.TeacherCareerPlanService;
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
 * 教师端职业规划控制器（《教师端接口文档》八、职业规划反馈模块）
 * <p>
 * 提供教师对学生的职业规划进行反馈点评的接口，统一前缀 /teacher/career-plans。
 * 需登录访问，数据范围按教师授权校验（越权返回 20005）。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/career-plans")
@RequiredArgsConstructor
public class TeacherCareerPlanController {

    private final TeacherCareerPlanService teacherCareerPlanService;

    /**
     * 提交职业规划反馈（POST /teacher/career-plans/{planId}/feedbacks，教师端文档 8.1）
     *
     * @param userId  当前登录用户 ID（由 JWT 过滤器注入）
     * @param planId  职业规划 ID（career_plans.id）
     * @param request 反馈内容与建议项
     * @return 反馈 ID 与提交时间
     */
    @PostMapping("/{planId}/feedbacks")
    public ApiResult<TeacherCareerFeedbackResponse> submitFeedback(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody TeacherCareerFeedbackRequest request) {
        return ApiResult.success("反馈已提交",
                teacherCareerPlanService.submitFeedback(userId, planId, request));
    }
}
