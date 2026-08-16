package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.export.request.ExportTemplateCreateRequest;
import com.example.studentarchives.dto.Fmy.export.request.ExportTemplateUpdateRequest;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateCreateResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateDefaultResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateDeleteResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateDetailResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateItem;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplatePreviewImageResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateStatusResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateUpdateResponse;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.ScopeTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AdminExportTemplateRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端导出模板服务
 * <p>
 * 对应《管理端接口文档》五、数据导出模块（5.3~5.10 导出模板管理），统一前缀 /admin/export-templates：
 * <ul>
 *   <li>5.3 GET /admin/export-templates：模板列表（分页，按学校/导出类型/状态筛选）；</li>
 *   <li>5.4 GET /admin/export-templates/{templateId}：模板详情（含 JSON 配置列）；</li>
 *   <li>5.5 POST /admin/export-templates：创建模板（version 初始 1，is_default=0）；</li>
 *   <li>5.6 PUT /admin/export-templates/{templateId}：更新模板（version 自动 +1）；</li>
 *   <li>5.7 DELETE /admin/export-templates/{templateId}：软删除模板（默认模板不允许删除）；</li>
 *   <li>5.8 PUT /admin/export-templates/{templateId}/default：设置默认模板（同校同类型唯一）；</li>
 *   <li>5.9 PATCH /admin/export-templates/{templateId}/status：修改模板状态（不触发 version 自增）；</li>
 *   <li>5.10 POST /admin/export-templates/{templateId}/preview-image：上传模板预览图（回写 preview_image，不触发 version 自增）。</li>
 * </ul>
 * 所有接口需校验 admin 角色或 export:template:manage 权限码，越权返回 20005 无访问权限。
 * 删除采用逻辑删除（deleted_at 置位），历史导出任务不受影响。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExportTemplateService {

    /** 导出模板管理权限码（《管理端接口文档》关键权限码） */
    private static final String PERMISSION = "export:template:manage";

    /** 导出类型中文标签 */
    private static final Map<String, String> EXPORT_TYPE_LABELS = Map.of(
            "student_archive", "学生成长档案",
            "career_plan", "职业规划",
            "resume", "个人简历");

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final AdminExportTemplateRepository adminExportTemplateRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final OssFileService ossFileService;
    private final ObjectMapper objectMapper;

    // ==================== 5.3 获取导出模板列表 ====================

    /**
     * 获取导出模板列表（GET /admin/export-templates，文档 5.3）
     * <p>
     * 按学校（必填）/导出类型/启用状态筛选，按更新时间倒序分页；列表项不含
     * templateContent、headerHtml、footerHtml 等大字段（需调详情接口）。
     *
     * @param userId     当前登录用户 ID
     * @param schoolId   学校 ID
     * @param exportType 导出类型（可选）
     * @param status     0=禁用 1=启用（可选，不传返回全部）
     * @param pageParam  分页参数
     * @return 分页的模板列表
     */
    public PageResult<ExportTemplateItem> listTemplates(Long userId, Long schoolId, String exportType,
                                                        Integer status, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        if (schoolId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "schoolId 不能为空");
        }
        List<ExportTemplate> all = adminExportTemplateRepository.findAll().stream()
                .filter(t -> Objects.equals(t.getSchoolId(), schoolId))
                .filter(t -> exportType == null || exportType.isBlank() || Objects.equals(t.getExportType(), exportType))
                .filter(t -> status == null || Objects.equals(t.getStatus(), status))
                .sorted(Comparator.comparing(ExportTemplate::getUpdatedAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = all.size();
        int offset = pageParam.getOffset();
        List<ExportTemplate> pageItems = offset >= all.size()
                ? List.of()
                : all.subList(offset, Math.min(offset + pageParam.getPerPage(), all.size()));

        // 批量加载创建人姓名
        Map<Long, String> creatorName = userRepository.findByIdIn(
                        pageItems.stream().map(ExportTemplate::getCreatedBy).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        List<ExportTemplateItem> list = pageItems.stream()
                .map(t -> toListItem(t, creatorName.get(t.getCreatedBy())))
                .collect(Collectors.toList());
        return PageResult.of(list, total, pageParam);
    }

    // ==================== 5.4 获取导出模板详情 ====================

    /**
     * 获取导出模板详情（GET /admin/export-templates/{templateId}，文档 5.4）
     * <p>
     * 返回模板完整配置：fieldsConfig/filterConditions/pageConfig/marginConfig/watermarkConfig/fontConfig
     * 解析为 JSON 节点，并携带 templateContent、headerHtml、footerHtml 等大字段。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 模板完整配置
     */
    public ExportTemplateDetailResponse getTemplateDetail(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ExportTemplate template = requireTemplate(templateId);
        String creatorName = userRepository.findById(template.getCreatedBy())
                .map(User::getName).orElse(null);
        return toDetailResponse(template, creatorName);
    }

    // ==================== 5.5 创建导出模板 ====================

    /**
     * 创建导出模板（POST /admin/export-templates，文档 5.5）
     * <p>
     * 模板编码同校内唯一（重复返回 10002 参数错误）；version 初始 1，is_default=0。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    public ExportTemplateCreateResponse createTemplate(Long userId, ExportTemplateCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);

        schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));
        checkTemplateCodeUnique(request.getSchoolId(), request.getTemplateCode(), null);
        // 校验渲染模式对应的内容：1=字段列表模式必填 fieldsConfig，2=自由模板模式必填 templateContent
        boolean freeLayout = request.getTemplateMode() != null && request.getTemplateMode() == 2;
        if (freeLayout) {
            if (request.getTemplateContent() == null || request.getTemplateContent().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "templateContent 不能为空");
            }
        } else {
            if (request.getFieldsConfig() == null) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "fieldsConfig 不能为空");
            }
        }

        ExportTemplate template = new ExportTemplate();
        template.setSchoolId(request.getSchoolId());
        template.setTemplateName(request.getTemplateName());
        template.setTemplateCode(request.getTemplateCode());
        template.setExportType(request.getExportType());
        template.setScopeType(request.getScopeType() != null ? request.getScopeType() : 1);
        template.setFieldsConfig(writeJson(request.getFieldsConfig()));
        template.setFilterConditions(writeJson(request.getFilterConditions()));
        template.setTemplateContent(request.getTemplateContent());
        template.setTemplateMode(request.getTemplateMode() != null ? request.getTemplateMode() : 1);
        template.setEngineType(request.getEngineType() != null && !request.getEngineType().isBlank()
                ? request.getEngineType() : "puppeteer");
        template.setPageConfig(writeJson(request.getPageConfig()));
        template.setPaperSize(request.getPaperSize() != null && !request.getPaperSize().isBlank()
                ? request.getPaperSize() : "A4");
        template.setOrientation(request.getOrientation() != null ? request.getOrientation() : 1);
        template.setMarginConfig(writeJson(request.getMarginConfig()));
        template.setHeaderHtml(request.getHeaderHtml());
        template.setFooterHtml(request.getFooterHtml());
        template.setWatermarkConfig(writeJson(request.getWatermarkConfig()));
        template.setFontConfig(writeJson(request.getFontConfig()));
        template.setVersion(1);
        template.setIsDefault(0);
        template.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        template.setCreatedBy(userId);
        template = adminExportTemplateRepository.save(template);

        return ExportTemplateCreateResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .templateName(template.getTemplateName())
                .templateCode(template.getTemplateCode())
                .exportType(template.getExportType())
                .templateMode(template.getTemplateMode())
                .version(template.getVersion())
                .isDefault(template.getIsDefault())
                .status(template.getStatus())
                .createdBy(template.getCreatedBy())
                .createdAt(toIso(template.getCreatedAt()))
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 5.6 更新导出模板 ====================

    /**
     * 更新导出模板（PUT /admin/export-templates/{templateId}，文档 5.6）
     * <p>
     * 全部字段可选，未传（null）表示不修改；更新成功时 version 自动 +1。
     * 修改 templateCode 时校验同校唯一。
     * status 字段不受支持（传值返回 10001），状态变更请调用 5.9 状态接口，避免 version 自增。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param request    更新请求
     * @return 更新结果
     */
    public ExportTemplateUpdateResponse updateTemplate(Long userId, Long templateId, ExportTemplateUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ExportTemplate template = requireTemplate(templateId);

        // status 状态变更已拆分为独立接口（PATCH /admin/export-templates/{templateId}/status，文档 5.9），
        // 避免状态开关触发 version 自增污染导出任务追溯的模板内容版本；本接口传入 status 返回 10001
        if (request.getStatus() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "status 请使用状态变更接口 PATCH /admin/export-templates/{templateId}/status");
        }

        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getTemplateCode() != null && !Objects.equals(request.getTemplateCode(), template.getTemplateCode())) {
            checkTemplateCodeUnique(template.getSchoolId(), request.getTemplateCode(), templateId);
            template.setTemplateCode(request.getTemplateCode());
        }
        if (request.getScopeType() != null) {
            template.setScopeType(request.getScopeType());
        }
        if (request.getFieldsConfig() != null) {
            template.setFieldsConfig(writeJson(request.getFieldsConfig()));
        }
        if (request.getFilterConditions() != null) {
            template.setFilterConditions(writeJson(request.getFilterConditions()));
        }
        if (request.getTemplateContent() != null) {
            template.setTemplateContent(request.getTemplateContent());
        }
        if (request.getTemplateMode() != null) {
            template.setTemplateMode(request.getTemplateMode());
        }
        if (request.getEngineType() != null) {
            template.setEngineType(request.getEngineType());
        }
        if (request.getPageConfig() != null) {
            template.setPageConfig(writeJson(request.getPageConfig()));
        }
        if (request.getPaperSize() != null) {
            template.setPaperSize(request.getPaperSize());
        }
        if (request.getOrientation() != null) {
            template.setOrientation(request.getOrientation());
        }
        if (request.getMarginConfig() != null) {
            template.setMarginConfig(writeJson(request.getMarginConfig()));
        }
        if (request.getHeaderHtml() != null) {
            template.setHeaderHtml(request.getHeaderHtml());
        }
        if (request.getFooterHtml() != null) {
            template.setFooterHtml(request.getFooterHtml());
        }
        if (request.getWatermarkConfig() != null) {
            template.setWatermarkConfig(writeJson(request.getWatermarkConfig()));
        }
        if (request.getFontConfig() != null) {
            template.setFontConfig(writeJson(request.getFontConfig()));
        }
        // 校验渲染模式与内容的配套关系（文档 5.6 规则：切换 templateMode 时需配套更新 fieldsConfig 或 templateContent）
        if (Objects.equals(template.getTemplateMode(), 2)) {
            if (template.getTemplateContent() == null || template.getTemplateContent().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "自由模板模式必须提供 templateContent");
            }
        } else if (template.getFieldsConfig() == null || template.getFieldsConfig().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "字段列表模式必须提供 fieldsConfig");
        }
        // 版本自动 +1，便于导出任务追溯使用的模板版本
        template.setVersion((template.getVersion() != null ? template.getVersion() : 1) + 1);
        template = adminExportTemplateRepository.save(template);

        return ExportTemplateUpdateResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .templateName(template.getTemplateName())
                .templateCode(template.getTemplateCode())
                .exportType(template.getExportType())
                .templateMode(template.getTemplateMode())
                .version(template.getVersion())
                .isDefault(template.getIsDefault())
                .status(template.getStatus())
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 5.7 删除导出模板 ====================

    /**
     * 删除导出模板（DELETE /admin/export-templates/{templateId}，文档 5.7）
     * <p>
     * 软删除（deleted_at 置位）；当前为默认模板（is_default=1）时不允许删除，返回 4 操作失败。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 删除结果
     */
    @Transactional
    public ExportTemplateDeleteResponse deleteTemplate(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ExportTemplate template = requireTemplate(templateId);
        if (Objects.equals(template.getIsDefault(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "默认模板需先取消默认再删除");
        }
        LocalDateTime deletedAt = LocalDateTime.now();
        int updated = adminExportTemplateRepository.softDelete(templateId, deletedAt);
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "模板不存在");
        }
        return ExportTemplateDeleteResponse.builder()
                .id(templateId)
                .deletedAt(toIso(deletedAt))
                .build();
    }

    // ==================== 5.8 设置默认导出模板 ====================

    /**
     * 设置默认导出模板（PUT /admin/export-templates/{templateId}/default，文档 5.8）
     * <p>
     * 将指定模板设为某学校、某导出类型下的默认模板，同时取消同学校、同导出类型下
     * 其他模板的默认状态（is_default=0）。被设置模板必须处于启用状态（status=1）。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 设置结果
     */
    public ExportTemplateDefaultResponse setDefaultTemplate(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ExportTemplate template = requireTemplate(templateId);
        if (!Objects.equals(template.getStatus(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "禁用状态模板不能设为默认");
        }
        // 取消同学校、同导出类型下其他模板的默认状态
        List<ExportTemplate> sameSchoolType = adminExportTemplateRepository
                .findBySchoolIdAndExportType(template.getSchoolId(), template.getExportType());
        boolean changed = false;
        for (ExportTemplate other : sameSchoolType) {
            if (!Objects.equals(other.getId(), templateId) && Objects.equals(other.getIsDefault(), 1)) {
                other.setIsDefault(0);
                adminExportTemplateRepository.save(other);
                changed = true;
            }
        }
        template.setIsDefault(1);
        template = adminExportTemplateRepository.save(template);
        log.info("设置默认导出模板 templateId={}, schoolId={}, exportType={}{}",
                templateId, template.getSchoolId(), template.getExportType(),
                changed ? "（已取消其他模板默认）" : "");
        return ExportTemplateDefaultResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .exportType(template.getExportType())
                .templateName(template.getTemplateName())
                .isDefault(template.getIsDefault())
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 5.9 修改导出模板状态 ====================

    /**
     * 修改导出模板状态（PATCH /admin/export-templates/{templateId}/status，文档 5.9）
     * <p>
     * 仅变更启用/禁用状态，不触发 version 自增（version 记录模板内容版本，供导出任务追溯）；
     * 禁用默认模板（is_default=1）不允许（与 5.7 删除规则一致），返回 4 操作失败。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param status     0=禁用 1=启用
     * @return 修改后的状态
     */
    public ExportTemplateStatusResponse updateTemplateStatus(Long userId, Long templateId, Integer status) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 仅支持 0=禁用 1=启用");
        }
        ExportTemplate template = requireTemplate(templateId);
        checkDisableAllowed(template, status);
        template.setStatus(status);
        template = adminExportTemplateRepository.save(template);
        log.info("修改导出模板状态 templateId={}, status={}", templateId, status);
        return ExportTemplateStatusResponse.builder()
                .id(template.getId())
                .status(template.getStatus())
                .statusLabel(statusLabel(template.getStatus()))
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 5.10 上传导出模板预览图 ====================

    /**
     * 上传导出模板预览图（POST /admin/export-templates/{templateId}/preview-image，文档 5.10）
     * <p>
     * 直接上传至 OSS 正式目录 export-template-preview/{uuid}.{ext} 并回写模板 preview_image 字段。
     * 首次调用为赋值预览图，再次调用即覆盖（upsert）；不触发 version 自增（预览图仅供管理端展示，
     * 导出任务渲染不消费，避免重复上传导致版本噪音，与 5.9 状态接口拆分逻辑一致）。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param file       预览图文件（仅 jpg/jpeg/png/gif/webp/bmp，最大 2MB）
     * @return 上传结果
     */
    public ExportTemplatePreviewImageResponse uploadPreviewImage(Long userId, Long templateId, MultipartFile file) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ExportTemplate template = requireTemplate(templateId);

        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 2. 校验文件扩展名（仅允许图片格式）
        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);
        if (ext == null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "文件缺少扩展名");
        }
        Set<String> allowedImageExts = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
        if (!allowedImageExts.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                    "仅支持 jpg/png/gif/webp/bmp 格式的图片，当前文件格式: " + ext);
        }

        // 3. 校验文件大小（预览图限制 2MB）
        long maxPreviewSize = 2 * 1024 * 1024L;
        if (file.getSize() > maxPreviewSize) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "预览图文件过大，最大允许 2MB，当前文件大小: " + (file.getSize() / 1024) + "KB");
        }

        // 4. 上传到 OSS 正式目录（export-template-preview/{uuid}.{ext}）
        String objectKey;
        try {
            objectKey = ossFileService.uploadTemplatePreview(file);
        } catch (Exception e) {
            log.error("OSS 模板预览图上传失败: templateId={}, filename={}, size={}, error={}",
                    templateId, originalFilename, file.getSize(), e.getMessage(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED,
                    "预览图上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }

        // 5. 生成预览图访问 URL（30 天有效，生产环境建议预览图目录开启公共读后改用不带签名的 URL）
        String previewImage = ossFileService.generatePresignedUrl(objectKey, 30 * 24 * 60);

        // 6. 回写模板 preview_image 字段（不触发 version 自增）
        template.setPreviewImage(previewImage);
        template = adminExportTemplateRepository.save(template);

        log.info("导出模板预览图上传成功: templateId={}, objectKey={}", templateId, objectKey);

        return ExportTemplatePreviewImageResponse.builder()
                .id(template.getId())
                .previewImage(previewImage)
                .objectKey(objectKey)
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** 按 ID 加载模板，不存在返回 30001 */
    private ExportTemplate requireTemplate(Long templateId) {
        return adminExportTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "模板不存在"));
    }

    /**
     * 校验目标状态允许禁用：禁用默认模板（is_default=1）会导致渲染静默回退
     * （is_default 仍挂在禁用模板上，实际取首个启用模板），故与删除规则一致不允许，
     * 需先设置其他模板为默认（文档 5.9 业务规则）。
     */
    private void checkDisableAllowed(ExportTemplate template, Integer targetStatus) {
        if (Objects.equals(targetStatus, 0) && Objects.equals(template.getIsDefault(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "默认模板不能禁用，请先设置其他模板为默认");
        }
    }

    /** 校验模板编码同校唯一（excludeId 为当前更新模板 ID，创建时传 null），重复返回 10002 参数错误 */
    private void checkTemplateCodeUnique(Long schoolId, String templateCode, Long excludeId) {
        boolean duplicate = adminExportTemplateRepository.findBySchoolIdAndTemplateCode(schoolId, templateCode)
                .stream().anyMatch(t -> excludeId == null || !Objects.equals(t.getId(), excludeId));
        if (duplicate) {
            throw new BusinessException(10002, "模板编码已存在");
        }
    }

    /** 模板 → 列表项 DTO */
    private ExportTemplateItem toListItem(ExportTemplate t, String creatorName) {
        return ExportTemplateItem.builder()
                .id(t.getId())
                .schoolId(t.getSchoolId())
                .templateName(t.getTemplateName())
                .templateCode(t.getTemplateCode())
                .exportType(t.getExportType())
                .exportTypeLabel(exportTypeLabel(t.getExportType()))
                .scopeType(t.getScopeType())
                .scopeTypeLabel(scopeTypeLabel(t.getScopeType()))
                .templateMode(t.getTemplateMode())
                .engineType(t.getEngineType())
                .paperSize(t.getPaperSize())
                .orientation(t.getOrientation())
                .orientationLabel(orientationLabel(t.getOrientation()))
                .version(t.getVersion())
                .isDefault(t.getIsDefault())
                .status(t.getStatus())
                .statusLabel(statusLabel(t.getStatus()))
                .previewImage(t.getPreviewImage())
                .createdBy(t.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(toIso(t.getCreatedAt()))
                .updatedAt(toIso(t.getUpdatedAt()))
                .build();
    }

    /** 模板 → 详情 DTO（JSON 配置列解析为 JsonNode） */
    private ExportTemplateDetailResponse toDetailResponse(ExportTemplate t, String creatorName) {
        return ExportTemplateDetailResponse.builder()
                .id(t.getId())
                .schoolId(t.getSchoolId())
                .templateName(t.getTemplateName())
                .templateCode(t.getTemplateCode())
                .exportType(t.getExportType())
                .exportTypeLabel(exportTypeLabel(t.getExportType()))
                .scopeType(t.getScopeType())
                .scopeTypeLabel(scopeTypeLabel(t.getScopeType()))
                .fieldsConfig(parseJson(t.getFieldsConfig()))
                .filterConditions(parseJson(t.getFilterConditions()))
                .templateMode(t.getTemplateMode())
                .templateModeLabel(templateModeLabel(t.getTemplateMode()))
                .templateContent(t.getTemplateContent())
                .engineType(t.getEngineType())
                .pageConfig(parseJson(t.getPageConfig()))
                .paperSize(t.getPaperSize())
                .orientation(t.getOrientation())
                .orientationLabel(orientationLabel(t.getOrientation()))
                .marginConfig(parseJson(t.getMarginConfig()))
                .headerHtml(t.getHeaderHtml())
                .footerHtml(t.getFooterHtml())
                .watermarkConfig(parseJson(t.getWatermarkConfig()))
                .fontConfig(parseJson(t.getFontConfig()))
                .previewImage(t.getPreviewImage())
                .version(t.getVersion())
                .isDefault(t.getIsDefault())
                .status(t.getStatus())
                .statusLabel(statusLabel(t.getStatus()))
                .createdBy(t.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(toIso(t.getCreatedAt()))
                .updatedAt(toIso(t.getUpdatedAt()))
                .build();
    }

    // ==================== 标签映射 ====================

    private String exportTypeLabel(String exportType) {
        return exportType != null ? EXPORT_TYPE_LABELS.get(exportType) : null;
    }

    private String scopeTypeLabel(Integer scopeType) {
        ScopeTypeEnum e = ScopeTypeEnum.of(scopeType);
        return e != null ? e.getLabel() : null;
    }

    /** 方向：1=纵向 2=横向 */
    private String orientationLabel(Integer orientation) {
        if (orientation == null) {
            return null;
        }
        return orientation == 2 ? "横向" : "纵向";
    }

    /** 状态：0=禁用 1=启用 */
    private String statusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "启用" : "禁用";
    }

    /** 模板渲染模式：1=字段列表模式 2=自由模板模式 */
    private String templateModeLabel(Integer templateMode) {
        if (templateMode == null) {
            return null;
        }
        return templateMode == 2 ? "自由模板模式" : "字段列表模式";
    }

    // ==================== JSON 工具 ====================

    /** 序列化对象到 JSON 字符串（null 原样返回，供 JSON 列写入） */
    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    /** 解析 JSON 字符串为 JsonNode（空白返回 null，供详情接口返回 JSON 列） */
    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    /** 提取文件扩展名（不含点号，小写） */
    private static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1).toLowerCase()
                : null;
    }
}
