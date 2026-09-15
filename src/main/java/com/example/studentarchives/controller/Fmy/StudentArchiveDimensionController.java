package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveDimensionResponse;
import com.example.studentarchives.service.Fmy.StudentArchiveDimensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生多维度能力画像 Controller（《学生端接口文档》4.1.7）
 * <p>
 * 补齐前端已在调用、后端此前缺失的 {@code GET /archive/dimensions}
 * （此前后端没有任何 {@code /archive/**} 控制器，与 {@code /admin/ability-dimensions} 不是一回事）。
 * <p>
 * 鉴权：{@code /archive/**} 由 SecurityConfig 兜底要求登录；只返回当前登录人本人的画像，
 * 无权限码（学生端个人数据，与 {@code /profile/**} 同口径）。
 */
@Slf4j
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class StudentArchiveDimensionController {

    private final StudentArchiveDimensionService studentArchiveDimensionService;

    /**
     * 获取多维度能力画像（GET /archive/dimensions，《学生端接口文档》4.1.7）
     *
     * @param semesterId 学期 ID，不传表示全部学期累计（与 GET /messages/unified 的 archive 参数口径一致）
     * @param userId     当前登录用户 ID
     */
    @GetMapping("/dimensions")
    public ApiResult<ArchiveDimensionResponse> getDimensions(
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(studentArchiveDimensionService.getDimensions(userId, semesterId));
    }
}
