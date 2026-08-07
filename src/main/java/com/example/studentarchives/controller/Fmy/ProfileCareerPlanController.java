package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.profile.request.AiPlanCreateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionAddRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionFileRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionStatusRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerGoalAddRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerGoalUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerMilestoneAddRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerMilestoneUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerPlanCopyRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerPlanCreateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerReflectionAddRequest;
import com.example.studentarchives.dto.Fmy.profile.response.AiPlanCreateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerActionFileResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerActionStatusResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanCopyResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanCreateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanIdResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanListItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanPreviewResponse;
import com.example.studentarchives.service.Fmy.ProfileCareerPlanService;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;

/**
 * 个人中心职业规划控制器
 * <p>
 * 提供学生端职业规划接口（《学生端接口文档》四、4.3~4.15），
 * 所有接口需携带 Bearer Token 认证，数据按当前登录学生归属校验。
 */
@Slf4j
@RestController
@RequestMapping("/profile/career-plans")
@RequiredArgsConstructor
public class ProfileCareerPlanController {

    private final ProfileCareerPlanService profileCareerPlanService;

    /**
     * 获取职业规划列表（4.3）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID 筛选，不传返回全部学期
     * @param page       页码，默认1
     * @param perPage    每页条数，默认10
     * @return 分页列表
     */
    @GetMapping
    public ApiResult<PageResult<CareerPlanListItem>> listPlans(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "10") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return profileCareerPlanService.listPlans(userId, semesterId, pageParam).toApiResult();
    }

    /**
     * 新增/提交职业规划（4.4）
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping
    public ApiResult<CareerPlanCreateResponse> createPlan(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CareerPlanCreateRequest request) {
        CareerPlanCreateResponse response = profileCareerPlanService.createPlan(userId, request);
        boolean draft = request.getIsDraft() != null && request.getIsDraft() == 1;
        return ApiResult.success(draft ? "保存成功" : "提交成功", response);
    }

    /**
     * 复制上一学期计划（4.7）
     *
     * @param userId  当前登录用户 ID
     * @param request 复制请求
     * @return 复制结果
     */
    @PostMapping("/copy")
    public ApiResult<CareerPlanCopyResponse> copyPlan(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CareerPlanCopyRequest request) {
        return ApiResult.success("复制成功", profileCareerPlanService.copyPlan(userId, request));
    }

    /**
     * AI建议一键添加为计划（4.15）
     *
     * @param userId  当前登录用户 ID
     * @param request AI建议创建请求
     * @return 创建结果
     */
    @PostMapping("/ai-add")
    public ApiResult<AiPlanCreateResponse> aiAddPlan(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AiPlanCreateRequest request) {
        return ApiResult.success(profileCareerPlanService.aiAddPlan(userId, request));
    }

    /**
     * 获取职业规划详情（4.6）
     *
     * @param userId 当前登录用户 ID
     * @param planId 规划 ID
     * @return 规划详情
     */
    @GetMapping("/{planId}")
    public ApiResult<CareerPlanDetailResponse> getPlanDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId) {
        return ApiResult.success(profileCareerPlanService.getPlanDetail(userId, planId));
    }

    /**
     * 下载职业规划文件（4.5）
     * <p>
     * 校验归属后重定向至 OSS 签名 URL，浏览器自动下载。
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param purpose  导出用途：internal（内部查看，默认）/ external（外部投递，不添加屏幕水印）
     * @param response HTTP 响应（用于重定向）
     */
    @GetMapping("/{planId}/download")
    public void downloadPlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @RequestParam(value = "purpose", required = false, defaultValue = "internal") String purpose,
            HttpServletResponse response) throws IOException {
        String url = profileCareerPlanService.getCareerPlanDownloadUrl(userId, planId, purpose);
        response.sendRedirect(url);
    }

    /**
     * 预览职业规划文件（4.5.1）
     * <p>
     * 与下载共用同一份生成/缓存文件，返回 OSS 签名 inline 预览 URL
     * （response-content-disposition=inline），前端可在新标签页内嵌渲染 PDF，确认版式后再下载。
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param purpose 导出用途：internal（内部预览，带屏幕水印）/ external（外部投递，无水印）
     * @return 预览信息
     */
    @GetMapping("/{planId}/preview")
    public ApiResult<CareerPlanPreviewResponse> previewPlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @RequestParam(value = "purpose", required = false, defaultValue = "internal") String purpose) {
        return ApiResult.success(profileCareerPlanService.getCareerPlanPreviewUrl(userId, planId, purpose));
    }

    /**
     * 添加目标（4.8）
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param request 目标创建请求
     * @return 新目标 ID
     */
    @PostMapping("/{planId}/goals")
    public ApiResult<CareerPlanIdResponse> addGoal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody CareerGoalAddRequest request) {
        return ApiResult.success("添加成功", profileCareerPlanService.addGoal(userId, planId, request));
    }

    /**
     * 添加行动（4.9）
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param goalId  目标 ID（行动归属的目标）
     * @param request 行动创建请求
     * @return 新行动 ID
     */
    @PostMapping("/{planId}/goals/{goalId}/actions")
    public ApiResult<CareerPlanIdResponse> addAction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long goalId,
            @Valid @RequestBody CareerActionAddRequest request) {
        return ApiResult.success("添加成功", profileCareerPlanService.addAction(userId, planId, goalId, request));
    }

    /**
     * 添加里程碑（4.10）
     *
     * @param userId      当前登录用户 ID
     * @param planId      规划 ID
     * @param actionId    行动 ID（里程碑归属的行动）
     * @param request     里程碑创建请求
     * @return 新里程碑 ID
     */
    @PostMapping("/{planId}/actions/{actionId}/milestones")
    public ApiResult<CareerPlanIdResponse> addMilestone(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long actionId,
            @Valid @RequestBody CareerMilestoneAddRequest request) {
        return ApiResult.success("添加成功",
                profileCareerPlanService.addMilestone(userId, planId, actionId, request));
    }

    /**
     * 更新目标（4.8.1）
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param goalId  目标 ID
     * @param request 更新请求（goalTitle 必填，其余字段不传保留原值，传空字符串清空）
     * @return 目标 ID
     */
    @PutMapping("/{planId}/goals/{goalId}")
    public ApiResult<CareerPlanIdResponse> updateGoal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long goalId,
            @Valid @RequestBody CareerGoalUpdateRequest request) {
        return ApiResult.success("更新成功", profileCareerPlanService.updateGoal(userId, planId, goalId, request));
    }

    /**
     * 更新行动（4.9.1）
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param actionId 行动 ID
     * @param request  更新请求（actionTitle 必填，其余字段不传保留原值，传空字符串清空）
     * @return 行动 ID
     */
    @PutMapping("/{planId}/actions/{actionId}")
    public ApiResult<CareerPlanIdResponse> updateAction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long actionId,
            @Valid @RequestBody CareerActionUpdateRequest request) {
        return ApiResult.success("更新成功",
                profileCareerPlanService.updateAction(userId, planId, actionId, request));
    }

    /**
     * 更新里程碑（4.10.1）
     *
     * @param userId      当前登录用户 ID
     * @param planId      规划 ID
     * @param milestoneId 里程碑 ID
     * @param request     更新请求（milestoneTitle 必填，其余字段不传保留原值）
     * @return 里程碑 ID
     */
    @PutMapping("/{planId}/milestones/{milestoneId}")
    public ApiResult<CareerPlanIdResponse> updateMilestone(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody CareerMilestoneUpdateRequest request) {
        return ApiResult.success("更新成功",
                profileCareerPlanService.updateMilestone(userId, planId, milestoneId, request));
    }

    /**
     * 删除目标（4.8.2）
     * <p>
     * 级联软删其行动、里程碑与行动成果文件，并重算规划进度。
     *
     * @param userId 当前登录用户 ID
     * @param planId 规划 ID
     * @param goalId 目标 ID
     * @return 操作结果
     */
    @DeleteMapping("/{planId}/goals/{goalId}")
    public ApiResult<Void> deleteGoal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long goalId) {
        profileCareerPlanService.deleteGoal(userId, planId, goalId);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 删除行动（4.9.2）
     * <p>
     * 级联软删其里程碑与行动成果文件，并重算目标状态与规划进度。
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param actionId 行动 ID
     * @return 操作结果
     */
    @DeleteMapping("/{planId}/actions/{actionId}")
    public ApiResult<Void> deleteAction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long actionId) {
        profileCareerPlanService.deleteAction(userId, planId, actionId);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 删除里程碑（4.10.2）
     *
     * @param userId      当前登录用户 ID
     * @param planId      规划 ID
     * @param milestoneId 里程碑 ID
     * @return 操作结果
     */
    @DeleteMapping("/{planId}/milestones/{milestoneId}")
    public ApiResult<Void> deleteMilestone(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long milestoneId) {
        profileCareerPlanService.deleteMilestone(userId, planId, milestoneId);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 更新行动状态（4.12）
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param actionId 行动 ID
     * @param request  状态请求
     * @return 更新后的行动状态
     */
    @PutMapping("/{planId}/actions/{actionId}/status")
    public ApiResult<CareerActionStatusResponse> updateActionStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long actionId,
            @Valid @RequestBody CareerActionStatusRequest request) {
        return ApiResult.success("更新成功",
                profileCareerPlanService.updateActionStatus(userId, planId, actionId, request));
    }

    /**
     * 上传行动成果文件（4.13）
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param actionId 行动 ID
     * @param request  文件绑定请求
     * @return 文件信息
     */
    @PostMapping("/{planId}/actions/{actionId}/files")
    public ApiResult<CareerActionFileResponse> bindActionFile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @PathVariable Long actionId,
            @Valid @RequestBody CareerActionFileRequest request) {
        return ApiResult.success("上传成功",
                profileCareerPlanService.bindActionFile(userId, planId, actionId, request));
    }

    /**
     * 添加阶段反思（4.14）
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param request 反思内容请求
     * @return 新反思 ID
     */
    @PostMapping("/{planId}/reflections")
    public ApiResult<CareerPlanIdResponse> addReflection(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody CareerReflectionAddRequest request) {
        return ApiResult.success("添加成功", profileCareerPlanService.addReflection(userId, planId, request));
    }
}
