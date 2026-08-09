package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.profile.request.BasicInfoUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.ContactUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.ExportRequest;
import com.example.studentarchives.dto.Fmy.profile.request.InterestUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.SelfEvaluationUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.StudentStatusUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.response.BasicInfoUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ContactUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.StudentStatusUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.DataCompletenessResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ExportPreviewResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ExportSubmitResponse;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse;
import com.example.studentarchives.dto.Fmy.profile.response.InterestUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ScoreDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ScoreListResponse;
import com.example.studentarchives.dto.Fmy.profile.response.SelfEvaluationResponse;
import com.example.studentarchives.service.Fmy.ProfileExportService;
import com.example.studentarchives.service.Fmy.ProfileService;
import com.example.studentarchives.service.Fmy.ResumeExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心控制器
 * <p>
 * 提供学生端个人中心模块接口（《学生端接口文档》四、个人中心模块），
 * 所有接口需携带 Bearer Token 认证。
 */
@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileExportService profileExportService;
    private final ResumeExportService resumeExportService;

    /**
     * 获取个人档案信息
     * <p>
     * 聚合当前学生真实数据返回：学籍信息、联系信息、志愿时长、
     * 画像分数、兴趣标签、学期成绩、个人奖项汇总、短板分析。
     *
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @return 个人档案信息
     */
    @GetMapping("/info")
    public ApiResult<ProfileInfoResponse> getProfileInfo(@AuthenticationPrincipal Long userId) {
        ProfileInfoResponse response = profileService.getProfileInfo(userId);
        return ApiResult.success(response);
    }

    /**
     * 更新个人联系信息（4.1.1）
     *
     * @param userId  当前登录用户 ID
     * @param request 联系信息更新请求
     * @return 更新后的联系信息
     */
    @PutMapping("/contact")
    public ApiResult<ContactUpdateResponse> updateContact(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ContactUpdateRequest request) {
        return ApiResult.success("更新成功", profileService.updateContact(userId, request));
    }

    /**
     * 获取画像分数列表（4.1.2）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID，不传返回当前学期
     * @return 画像分数列表
     */
    @GetMapping("/scores")
    public ApiResult<ScoreListResponse> getScores(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        return ApiResult.success(profileService.getScores(userId, semesterId));
    }

    /**
     * 获取分数计算说明（4.1.3）
     *
     * @param userId        当前登录用户 ID
     * @param calculationId 评分计算批次 ID
     * @return 分数计算明细
     */
    @GetMapping("/scores/{calculationId}/details")
    public ApiResult<ScoreDetailResponse> getScoreDetails(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long calculationId) {
        return ApiResult.success(profileService.getScoreDetails(userId, calculationId));
    }

    /**
     * 获取数据完整度（4.1.4）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID，不传返回当前学期
     * @return 数据完整度
     */
    @GetMapping("/data-completeness")
    public ApiResult<DataCompletenessResponse> getDataCompleteness(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        return ApiResult.success(profileService.getDataCompleteness(userId, semesterId));
    }

    /**
     * 更新政治面貌（4.1.5）
     *
     * @param userId  当前登录用户 ID
     * @param request 政治面貌更新请求
     * @return 更新后的政治面貌
     */
    @PutMapping("/political-status")
    public ApiResult<BasicInfoUpdateResponse> updateBasicInfo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BasicInfoUpdateRequest request) {
        return ApiResult.success("更新成功", profileService.updateBasicInfo(userId, request));
    }

    /**
     * 更新学生状态（4.1.5.1）
     *
     * @param userId  当前登录用户 ID
     * @param request 学生状态更新请求
     * @return 更新后的学生状态
     */
    @PutMapping("/student-status")
    public ApiResult<StudentStatusUpdateResponse> updateStudentStatus(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudentStatusUpdateRequest request) {
        return ApiResult.success("更新成功", profileService.updateStudentStatus(userId, request));
    }

    /**
     * 更新自我评价（4.1.6）
     *
     * @param userId  当前登录用户 ID
     * @param request 自我评价更新请求
     * @return 更新后的自我评价
     */
    @PutMapping("/self-evaluation")
    public ApiResult<SelfEvaluationResponse> updateSelfEvaluation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SelfEvaluationUpdateRequest request) {
        return ApiResult.success("更新成功", profileService.updateSelfEvaluation(userId, request));
    }

    /**
     * 更新个人兴趣标签（4.11）
     *
     * @param userId  当前登录用户 ID
     * @param request 兴趣标签更新请求
     * @return 更新/新增条数
     */
    @PutMapping("/interests")
    public ApiResult<InterestUpdateResponse> updateInterests(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InterestUpdateRequest request) {
        return ApiResult.success("更新成功", profileService.updateInterests(userId, request));
    }

    /**
     * 删除个人兴趣标签（4.11.1）
     *
     * @param userId     当前登录用户 ID
     * @param interestId 兴趣标签 ID
     * @return 删除结果
     */
    @DeleteMapping("/interests/{id}")
    public ApiResult<Void> deleteInterest(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long interestId) {
        profileService.deleteInterest(userId, interestId);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 获取成长时间轴（4.2）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID 筛选
     * @param eventType  事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升
     * @param status     审核状态：0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销
     * @param viewType   视图类型：list（默认）/ tree / ring
     * @return 成长时间轴响应（按 viewType 返回 list / tree / ring 结构）
     */
    @GetMapping("/growth-timeline")
    public ApiResult<GrowthTimelineResponse> getGrowthTimeline(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "eventType", required = false) Integer eventType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "viewType", required = false) String viewType) {
        return ApiResult.success(profileService.getGrowthTimeline(userId, semesterId, eventType, status, viewType));
    }

    /**
     * 获取档案导出预览（4.16）
     *
     * @param userId 当前登录用户 ID
     * @return 导出预览（可选栏目）
     */
    @GetMapping("/export/preview")
    public ApiResult<ExportPreviewResponse> getExportPreview(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(profileExportService.getExportPreview(userId));
    }

    /**
     * 提交档案导出（4.17）
     * <p>
     * 同步生成学生成长档案 PDF 并上传，写审计日志并通知学生。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 导出任务结果
     */
    @PostMapping("/export")
    public ApiResult<ExportSubmitResponse> submitExport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ExportRequest request) {
        return ApiResult.success("导出任务已创建", profileExportService.submitExport(userId, request));
    }

    /**
     * 获取简历导出预览
     *
     * @param userId 当前登录用户 ID
     * @return 简历导出预览（可选栏目）
     */
    @GetMapping("/resume/export/preview")
    public ApiResult<ExportPreviewResponse> getResumeExportPreview(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(resumeExportService.getExportPreview(userId));
    }

    /**
     * 提交简历导出
     * <p>
     * 同步生成个人简历 PDF 并上传，写审计日志并通知学生。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 导出任务结果
     */
    @PostMapping("/resume/export")
    public ApiResult<ExportSubmitResponse> submitResumeExport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ExportRequest request) {
        return ApiResult.success("简历导出任务已创建", resumeExportService.submitExport(userId, request));
    }
}
