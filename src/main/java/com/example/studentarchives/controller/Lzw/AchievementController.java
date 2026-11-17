package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementDetailResponse;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementDownloadTicketResponse;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementListResponse;
import com.example.studentarchives.service.Lzw.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 成果墙控制器
 * <p>
 * 以学生档案（archives）为数据源提供只读的成果墙视图：列表、详情、附件下载凭证。
 * 学生自传证书上传尚未实现。
 */
@Slf4j
@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /**
     * 成果墙列表
     *
     * @param category   honor / contest / practice / research / completion / other，all 或不传表示全部
     * @param status     archived / missing / pending，all 或不传表示全部
     * @param tag        实践二级标签：volunteer / activity / internship / social_practice
     * @param semesterId 学期 ID
     * @param keyword    标题关键词
     * @param sort       date_desc（默认）/ date_asc / updated_desc
     */
    @GetMapping
    public ApiResult<AchievementListResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "8") int perPage,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(achievementService.list(
                category, status, tag, semesterId, keyword, sort, page, perPage, userId));
    }

    /**
     * 成果详情
     */
    @GetMapping("/{id}")
    public ApiResult<AchievementDetailResponse> detail(
            @PathVariable String id,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(achievementService.detail(id, userId));
    }

    /**
     * 成果附件下载凭证（短时效签名地址）
     */
    @GetMapping("/{id}/files/{fileId}/download")
    public ApiResult<AchievementDownloadTicketResponse> download(
            @PathVariable String id,
            @PathVariable String fileId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(achievementService.downloadTicket(id, fileId, userId));
    }
}
