package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.guideconfig.request.GuideConfigUpdateRequest;
import com.example.studentarchives.dto.Fmy.guideconfig.response.GuideConfigItem;
import com.example.studentarchives.dto.Fmy.guideconfig.response.GuideConfigListResponse;
import com.example.studentarchives.service.Fmy.AdminGuideConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端评选说明配置控制器
 * <p>
 * 对应《管理端接口文档》十八、评选说明配置模块，统一前缀 /admin/guide-configs。
 * 评选说明按学校独立配置：schoolId 由 Service 从当前登录管理员所属学校解析，不接受前端传入。
 * 所有接口需通过 {@link AdminGuideConfigService} 校验 admin 角色或 guide:manage 权限码，
 * 越权返回 20005 无访问权限。写操作写入 audit_log 审计日志（module=guide_config）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/guide-configs")
@RequiredArgsConstructor
public class AdminGuideConfigController {

    private final AdminGuideConfigService adminGuideConfigService;

    // ==================== 18.1 获取评选说明配置列表 ====================

    /**
     * 获取本校评选说明配置列表
     *
     * @param userId  当前登录用户 ID
     * @param kind    archive=档案类型，award=奖项类型
     * @param status  0=禁用 1=启用，不传返回全部
     * @param keyword 按类型中文名/编码模糊搜索
     * @return 配置列表
     */
    @GetMapping
    public ApiResult<GuideConfigListResponse> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam("kind") String kind,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResult.success(adminGuideConfigService.list(userId, kind, status, keyword));
    }

    // ==================== 18.2 更新（或新增）评选说明配置 ====================

    /**
     * 保存本校指定类型的评选说明配置（不存在时自动新增）
     *
     * @param userId  当前登录用户 ID
     * @param kind    archive=档案类型，award=奖项类型
     * @param type    类型编码，必须属于 ArchiveTypeEnum / AwardTypeEnum
     * @param request 保存请求（局部更新语义）
     * @return 保存后的配置项
     */
    @AuditLog(module = "guide_config", action = "update",
            description = "保存评选说明配置: #kind/#type", logResult = true)
    @PutMapping("/{kind}/{type}")
    public ApiResult<GuideConfigItem> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable String kind,
            @PathVariable String type,
            @Valid @RequestBody GuideConfigUpdateRequest request) {
        return ApiResult.success("保存成功", adminGuideConfigService.upsert(userId, kind, type, request));
    }
}
