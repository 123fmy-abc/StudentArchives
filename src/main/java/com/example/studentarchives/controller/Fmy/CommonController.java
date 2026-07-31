package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.common.response.DictItemResponse;
import com.example.studentarchives.dto.Fmy.common.response.AvatarUploadResponse;
import com.example.studentarchives.dto.Fmy.common.response.FilePreviewResponse;
import com.example.studentarchives.dto.Fmy.common.response.FileUploadResponse;
import com.example.studentarchives.dto.Fmy.common.response.IndicatorTreeResponse;
import com.example.studentarchives.dto.Fmy.common.response.SemesterItemResponse;
import com.example.studentarchives.service.Fmy.CommonService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 通用接口控制器
 * <p>
 * 提供文件上传/预览/下载/删除、学期下拉、字典数据、指标树查询等通用接口。
 * 所有接口需携带 Bearer Token 认证。
 */
@Slf4j
@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class CommonController {

    private final CommonService commonService;

    // ==================== 文件上传 ====================

    /**
     * 文件上传
     * <p>
     * 上传文件到阿里云 OSS（临时目录），返回文件 ID 和签名 URL。
     * 首次上传写入 file_uploads 表（file_status=1 暂存），
     * 关联业务提交时自动拷贝至正式目录。
     *
     * @param file   文件对象
     * @param type   文件类型：evidence/certificate/plan/avatar 等
     * @param module 所属业务模块：competition/scholarship/practice/research/career_plan 等
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @return 上传结果
     */
    @AuditLog(module = "file", action = "upload", description = "上传文件: #type + '/' + #module", logResult = true)
    @PostMapping("/upload")
    public ApiResult<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("module") String module,
            @AuthenticationPrincipal Long userId) {

        FileUploadResponse response = commonService.uploadFile(file, type, module, userId);
        return ApiResult.success("上传成功", response);
    }

    // ==================== 文件预览 ====================

    /**
     * 文件预览
     * <p>
     * 校验权限后返回 OSS 签名 URL。图片/PDF 可直接预览，
     * 其他类型仅返回文件元数据。
     *
     * @param fileId 文件 ID
     * @param userId 当前登录用户 ID
     * @return 文件预览信息
     */
    @GetMapping("/files/{fileId}/preview")
    public ApiResult<FilePreviewResponse> previewFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal Long userId) {

        FilePreviewResponse response = commonService.previewFile(fileId, userId);
        return ApiResult.success(response);
    }

    // ==================== 文件下载 ====================

    /**
     * 文件下载
     * <p>
     * 校验权限后重定向至 OSS 签名 URL，浏览器自动下载文件。
     *
     * @param fileId   文件 ID
     * @param userId   当前登录用户 ID
     * @param response HTTP 响应（用于重定向）
     */
    @GetMapping("/files/{fileId}/download")
    public void downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) throws IOException {

        String downloadUrl = commonService.downloadFile(fileId, userId);
        response.sendRedirect(downloadUrl);
    }

    // ==================== 删除文件 ====================

    /**
     * 删除未提交附件
     * <p>
     * 仅允许删除当前登录用户上传、且 file_status=1（暂存）的附件。
     * 删除时同时对 OSS 做物理删除和数据库软删除。
     *
     * @param fileId 附件文件 ID
     * @param userId 当前登录用户 ID
     * @return 操作结果
     */
    @AuditLog(module = "file", action = "delete", description = "删除文件: #fileId")
    @DeleteMapping("/files/{fileId}")
    public ApiResult<Void> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal Long userId) {

        commonService.deleteFile(fileId, userId);
        return ApiResult.success("附件已删除", null);
    }

    // ==================== 头像上传 ====================

    /**
     * 上传头像
     * <p>
     * 与普通文件上传不同，头像上传不走临时存储流程：
     * 直接上传到 OSS 正式目录（avatar/{uuid}.{ext}），
     * 并更新 user_contact_infos.avatar 字段。
     *
     * @param file   头像文件（仅支持 jpg/png/gif/webp/bmp，最大 2MB）
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @return 头像 URL
     */
    @AuditLog(module = "file", action = "upload_avatar", description = "上传头像", logResult = true)
    @PostMapping("/upload/avatar")
    public ApiResult<AvatarUploadResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {

        AvatarUploadResponse response = commonService.uploadAvatar(file, userId);
        return ApiResult.success("头像上传成功", response);
    }

    // ==================== 学期下拉选项 ====================

    /**
     * 获取学期下拉选项
     * <p>
     * 数据来源：semesters 表，status=1 且 deleted_at IS NULL，按 start_date 倒序。
     *
     * @param userId 当前登录用户 ID（用于获取所属学校）
     * @return 学期列表
     */
    @GetMapping("/semesters")
    public ApiResult<List<SemesterItemResponse>> getSemesters(@AuthenticationPrincipal Long userId) {
        // 默认使用学校 ID=1（单学校场景），后续可扩展为从用户信息中获取
        List<SemesterItemResponse> list = commonService.getSemesters(1L);
        return ApiResult.success(list);
    }

    // ==================== 字典数据 ====================

    /**
     * 获取字典数据（下拉选项）
     * <p>
     * 数据来源：dictionaries 表（配置型数据，string dict_code 存储在业务表字段中）。
     * 支持的字典类型：competition_level、award_level、scholarship_level、
     * scholarship_award_level、company_type、industry_type、research_type、
     * project_level、organization_level、archive_category（来自 archive_type_configs 表）、
     * message_category、political_status、competition_type 等。
     * <p>
     * 性别（gender）和申报状态（apply_status）DB 存储为 int 值，
     * 请使用 {@link #getEnums(String)} 接口获取 int→label 映射。
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @GetMapping("/dict")
    public ApiResult<List<DictItemResponse>> getDict(@RequestParam("dictType") String dictType) {
        List<DictItemResponse> list = commonService.getDict(dictType);
        return ApiResult.success(list);
    }

    // ==================== 枚举查询 ====================

    /**
     * 获取枚举数据（int value → label 映射）
     * <p>
     * 与 {@code /common/dict} 的区别：
     * - /common/dict 返回 dictionaries 表数据（string dict_code → label）
     * - /common/enums 返回代码级枚举数据（int value → label）
     * <p>
     * 支持的枚举类型：gender、apply_status、scope_type、audit_action、event_type、role_level。
     * <p>
     * 适用场景：
     * - gender、apply_status：DB 字段存储 int 值（如 users.gender = 0/1/2），
     *   前端下拉列表应使用本接口而非 /common/dict，确保值类型匹配。
     * - scope_type、audit_action、event_type、role_level：后端业务逻辑依赖枚举值，
     *   前端展示标签时使用本接口。
     *
     * @param enumType 枚举类型
     * @return 枚举项列表
     */
    @GetMapping("/enums")
    public ApiResult<List<DictItemResponse>> getEnums(@RequestParam("enumType") String enumType) {
        List<DictItemResponse> list = commonService.getEnum(enumType);
        return ApiResult.success(list);
    }

    // ==================== 指标树查询 ====================

    /**
     * 获取指标树
     * <p>
     * 数据来源：evaluation_indicators、indicator_rule_versions、ability_dimensions 表，
     * 仅返回 status=1 且 deleted_at IS NULL 的指标。
     *
     * @param versionId 指定指标版本 ID（可选，不传则返回当前生效版本）
     * @param userId    当前登录用户 ID（用于获取所属学校）
     * @return 指标树
     */
    @GetMapping("/indicators")
    public ApiResult<IndicatorTreeResponse> getIndicators(
            @RequestParam(value = "versionId", required = false) Long versionId,
            @AuthenticationPrincipal Long userId) {
        // 默认使用学校 ID=1（单学校场景）
        IndicatorTreeResponse response = commonService.getIndicators(versionId, 1L);
        return ApiResult.success(response);
    }
}
