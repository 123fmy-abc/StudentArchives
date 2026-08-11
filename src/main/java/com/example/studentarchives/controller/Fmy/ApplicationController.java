package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.service.Fmy.ApplicationService;
import com.example.studentarchives.service.Fmy.ApplicationService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个人档案信息申报控制器
 * <p>
 * 支持 10 种档案类型的新增/保存、草稿增量保存、重复检测、
 * 评选说明查询、版本历史查询、已通过记录的更正。
 */
@Slf4j
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ==================== 类型化提交端点（10 种） ====================

    /** 学科竞赛 */
    @PostMapping("/competition")
    public ApiResult<ArchiveSubmitResponse> submitCompetition(
            @RequestBody CompetitionSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitCompetition(body, userId));
    }

    /** 奖学金 */
    @PostMapping("/scholarship")
    public ApiResult<ArchiveSubmitResponse> submitScholarship(
            @RequestBody ScholarshipSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitScholarship(body, userId));
    }

    /** 创新创业 */
    @PostMapping("/innovation")
    public ApiResult<ArchiveSubmitResponse> submitInnovation(
            @RequestBody InnovationSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitInnovation(body, userId));
    }

    /** 学术研究 */
    @PostMapping("/research")
    public ApiResult<ArchiveSubmitResponse> submitResearch(
            @RequestBody ResearchSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitResearch(body, userId));
    }

    /** 荣誉证书 */
    @PostMapping("/certificate")
    public ApiResult<ArchiveSubmitResponse> submitCertificate(
            @RequestBody CertificateSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitCertificate(body, userId));
    }

    /** 实习经历 */
    @PostMapping("/internship")
    public ApiResult<ArchiveSubmitResponse> submitInternship(
            @RequestBody InternshipSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitInternship(body, userId));
    }

    /** 组织履历 */
    @PostMapping("/organization")
    public ApiResult<ArchiveSubmitResponse> submitOrganization(
            @RequestBody OrganizationSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitOrganization(body, userId));
    }

    /** 实训项目 */
    @PostMapping("/training")
    public ApiResult<ArchiveSubmitResponse> submitTraining(
            @RequestBody TrainingSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitTraining(body, userId));
    }

    /** 社会实践 */
    @PostMapping("/practice")
    public ApiResult<ArchiveSubmitResponse> submitPractice(
            @RequestBody PracticeSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitPractice(body, userId));
    }

    /** 图书心得 */
    @PostMapping("/book-review")
    public ApiResult<ArchiveSubmitResponse> submitBookReview(
            @RequestBody BookReviewSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("提交成功", applicationService.submitBookReview(body, userId));
    }

    // ==================== 通用端点（5 个） ====================

    /** 自动保存草稿（增量更新） */
    @PutMapping("/{archiveId}/autosave")
    public ApiResult<AutosaveResponse> autosave(
            @PathVariable Long archiveId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("草稿已保存", applicationService.autosave(archiveId, body, userId));
    }

    /** 重复检测 */
    @PostMapping("/duplicate-check")
    public ApiResult<DuplicateCheckResponse> duplicateCheck(
            @RequestBody DuplicateCheckRequest body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(applicationService.duplicateCheck(body, userId));
    }

    /** 评选说明 */
    @GetMapping("/{type}/guide")
    public ApiResult<GuideResponse> getGuide(@PathVariable String type) {
        return ApiResult.success(applicationService.getGuide(type));
    }

    /** 版本历史 */
    @GetMapping("/{archiveId}/versions")
    public ApiResult<VersionHistoryResponse> getVersions(
            @PathVariable Long archiveId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(applicationService.getVersions(archiveId, userId));
    }

    /** 更正已通过记录（生成新版本） */
    @PostMapping("/{archiveId}/correction")
    public ApiResult<ArchiveSubmitResponse> correction(
            @PathVariable Long archiveId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success("更正申请已提交", applicationService.correction(archiveId, body, userId));
    }
}
