package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.service.Lzw.AwardService;
import com.example.studentarchives.service.Lzw.AwardService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 奖项报名控制器
 * <p>
 * 支持 3 种奖项类型：竞赛之星、科研之星、双创之星。
 * 科研之星采用"主记录 + 子项目"模式，先创建主记录，再添加子项目，最后提交。
 */
@Slf4j
@RestController
@RequestMapping("/awards")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    // ==================== 8.1 奖项总览统计 ====================

    @GetMapping("/overview")
    public ApiResult<AwardOverviewResponse> getOverview(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(awardService.getOverview(userId));
    }

    // ==================== 8.1.1 奖项草稿自动保存 ====================

    @PutMapping("/{applicationId}/autosave")
    public ApiResult<AwardAutosaveResponse> autosave(
            @PathVariable Long applicationId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Long userId) {
        AwardAutosaveResponse resp = awardService.autosave(applicationId, body, userId);
        boolean isSubmit = resp.getSavedAt() == null;
        return ApiResult.success(isSubmit ? "提交成功" : "草稿已保存", resp);
    }

    // ==================== 8.1.2 奖项重复申报检测 ====================

    @PostMapping("/duplicate-check")
    public ApiResult<AwardDuplicateCheckResponse> duplicateCheck(
            @RequestBody AwardDuplicateCheckRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(awardService.duplicateCheck(body, userId));
    }

    // ==================== 8.1.3 获取奖项评选说明 ====================

    @GetMapping("/{type}/guide")
    public ApiResult<AwardGuideResponse> getGuide(@PathVariable String type) {
        return ApiResult.success(awardService.getGuide(type));
    }

    // ==================== 8.2 竞赛之星报名 ====================

    @PostMapping("/competition-star")
    public ApiResult<AwardSubmitResponse> submitCompetitionStar(
            @RequestBody CompetitionStarRequest body,
            @AuthenticationPrincipal Long userId) {
        boolean isDraft = body.getIsDraft() != null && body.getIsDraft() == 1;
        return ApiResult.success(
                isDraft ? "草稿已保存" : "提交成功",
                awardService.submitCompetitionStar(body, userId));
    }

    // ==================== 8.3 科研之星 ====================

    @PostMapping("/research-star")
    public ApiResult<ResearchStarCreateResponse> createResearchStar(
            @RequestBody ResearchStarCreateRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(awardService.createResearchStar(body, userId));
    }

    @PostMapping("/research-star/{researchStarId}/projects")
    public ApiResult<AwardSubmitResponse> addProject(
            @PathVariable Long researchStarId,
            @RequestBody ResearchProjectRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("子项目已添加", awardService.addProject(researchStarId, body, userId));
    }

    @PostMapping("/research-star/{researchStarId}/software")
    public ApiResult<AwardSubmitResponse> addSoftware(
            @PathVariable Long researchStarId,
            @RequestBody SoftwareCopyrightRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("软著已添加", awardService.addSoftware(researchStarId, body, userId));
    }

    @PostMapping("/research-star/{researchStarId}/papers")
    public ApiResult<AwardSubmitResponse> addPaper(
            @PathVariable Long researchStarId,
            @RequestBody PublishedPaperRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("论文已添加", awardService.addPaper(researchStarId, body, userId));
    }

    @PostMapping("/research-star/{researchStarId}/submit")
    public ApiResult<AwardSubmitResponse> submitResearchStar(
            @PathVariable Long researchStarId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", awardService.submitResearchStar(researchStarId, userId));
    }

    // ==================== 8.4 双创之星报名 ====================

    @PostMapping("/innovation-star")
    public ApiResult<AwardSubmitResponse> submitInnovationStar(
            @RequestBody InnovationStarRequest body,
            @AuthenticationPrincipal Long userId) {
        boolean isDraft = body.getIsDraft() != null && body.getIsDraft() == 1;
        return ApiResult.success(
                isDraft ? "草稿已保存" : "提交成功",
                awardService.submitInnovationStar(body, userId));
    }

    // ==================== 8.5 获取奖项版本历史 ====================

    @GetMapping("/{applicationId}/versions")
    public ApiResult<AwardVersionHistoryResponse> getVersions(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(awardService.getVersions(applicationId, userId));
    }
}
