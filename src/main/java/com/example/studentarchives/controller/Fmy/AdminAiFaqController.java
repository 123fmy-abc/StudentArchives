package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.aifaq.request.AdminAiFaqCreateRequest;
import com.example.studentarchives.dto.Fmy.aifaq.request.AdminAiFaqUpdateRequest;
import com.example.studentarchives.dto.Fmy.aifaq.response.AiFaqQuestionResponse;
import com.example.studentarchives.service.Fmy.AdminAiFaqService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 AI 常见问题配置控制器
 * <p>
 * 统一前缀 /admin/ai-faqs。所有接口需通过 {@link AdminAiFaqService}
 * 校验 admin 角色或 ai:faq:manage 权限码，越权返回 20005 无访问权限。
 * 写操作写入 audit_log 审计日志（module=ai_faq）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai-faqs")
@RequiredArgsConstructor
public class AdminAiFaqController {

    private final AdminAiFaqService adminAiFaqService;

    /**
     * 获取常见问题列表（含禁用）
     */
    @GetMapping
    public ApiResult<List<AiFaqQuestionResponse>> list(@AuthenticationPrincipal Long userId) {
        List<AiFaqQuestionResponse> list = adminAiFaqService.list(userId);
        return ApiResult.success(list);
    }

    /**
     * 创建常见问题
     */
    @AuditLog(module = "ai_faq", action = "create",
            description = "创建AI常见问题: #request.question", logResult = true)
    @PostMapping
    public ApiResult<AiFaqQuestionResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AdminAiFaqCreateRequest request) {
        AiFaqQuestionResponse response = adminAiFaqService.create(userId, request);
        return ApiResult.success("创建成功", response);
    }

    /**
     * 更新常见问题
     */
    @AuditLog(module = "ai_faq", action = "update",
            description = "更新AI常见问题: #id", logResult = true)
    @PutMapping("/{id}")
    public ApiResult<AiFaqQuestionResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AdminAiFaqUpdateRequest request) {
        AiFaqQuestionResponse response = adminAiFaqService.update(userId, id, request);
        return ApiResult.success("更新成功", response);
    }

    /**
     * 删除常见问题（软删除）
     */
    @AuditLog(module = "ai_faq", action = "delete", description = "删除AI常见问题: #id")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        adminAiFaqService.delete(userId, id);
        return ApiResult.success("删除成功", null);
    }
}
