package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse;
import com.example.studentarchives.dto.Fmy.profile.response.TeacherStudentProfileResponse;
import com.example.studentarchives.service.Fmy.TeacherStudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端学生管理控制器
 * <p>
 * 提供教师端学生管理模块接口（《教师端接口文档》十六、学生管理模块），统一前缀 /teacher/students：
 * <ul>
 *   <li>档案详情：复用 {@link com.example.studentarchives.service.Fmy.ProfileService#getProfileInfo}
 *       组装档案主体，教师侧 {@code role_scopes} 范围校验后附加 isInScope 标记；</li>
 *   <li>成长时间轴：复用 {@code ProfileService#getGrowthTimeline}（学生端 4.2 同一服务）；</li>
 *   <li>职业规划详情：复用 {@code ProfileCareerPlanService#getPlanDetail}（学生端 4.6 同一服务）。</li>
 * </ul>
 * 越权（学生不在教师授权范围内）返回 20005 无访问权限。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/students")
@RequiredArgsConstructor
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;

    /**
     * 获取学生档案详情（GET /teacher/students/{studentId}/profile）
     *
     * @param userId    当前登录用户 ID（由 JWT 过滤器注入）
     * @param studentId 目标学生用户 ID
     * @return 学生档案详情（教师视角含 isInScope 标记）
     */
    @GetMapping("/{studentId:[0-9]+}/profile")
    public ApiResult<TeacherStudentProfileResponse> getStudentProfile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long studentId) {
        return ApiResult.success(teacherStudentService.getStudentProfile(userId, studentId));
    }

    /**
     * 获取学生成长时间轴（GET /teacher/students/{studentId}/growth-timeline）
     *
     * @param userId     当前登录用户 ID
     * @param studentId  目标学生用户 ID
     * @param semesterId 学期 ID 筛选
     * @param eventType  事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升
     * @param status     审核状态筛选
     * @return 成长时间轴响应
     */
    @GetMapping("/{studentId:[0-9]+}/growth-timeline")
    public ApiResult<GrowthTimelineResponse> getGrowthTimeline(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long studentId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "eventType", required = false) Integer eventType,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(
                teacherStudentService.getGrowthTimeline(userId, studentId, semesterId, eventType, status));
    }

    /**
     * 获取学生职业规划详情（GET /teacher/students/{studentId}/career-plans/{planId}）
     *
     * @param userId    当前登录用户 ID
     * @param studentId 目标学生用户 ID
     * @param planId    规划 ID
     * @return 规划详情
     */
    @GetMapping("/{studentId:[0-9]+}/career-plans/{planId:[0-9]+}")
    public ApiResult<CareerPlanDetailResponse> getCareerPlanDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long studentId,
            @PathVariable Long planId) {
        return ApiResult.success(teacherStudentService.getCareerPlanDetail(userId, studentId, planId));
    }
}
