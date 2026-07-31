package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.Fmy.OssProperties;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.AuditActionEnum;
import com.example.studentarchives.enums.EventTypeEnum;
import com.example.studentarchives.enums.GenderEnum;
import com.example.studentarchives.enums.RoleLevelEnum;
import com.example.studentarchives.enums.ScopeTypeEnum;
import com.example.studentarchives.dto.Fmy.common.response.DictItemResponse;
import com.example.studentarchives.dto.Fmy.common.response.FilePreviewResponse;
import com.example.studentarchives.dto.Fmy.common.response.FileUploadResponse;
import com.example.studentarchives.dto.Fmy.common.response.IndicatorTreeResponse;
import com.example.studentarchives.dto.Fmy.common.response.SemesterItemResponse;
import com.example.studentarchives.dto.Fmy.common.response.AvatarUploadResponse;
import com.example.studentarchives.entity.file.AttachmentLimit;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.foundation.IndicatorRuleVersion;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveTypeConfigRepository;
import com.example.studentarchives.repository.AttachmentLimitRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.EvaluationIndicatorRepository;
import com.example.studentarchives.repository.IndicatorRuleVersionRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用接口服务
 * <p>
 * 提供文件上传/预览/下载/删除、学期列表、字典数据、指标树查询等通用能力。
 * 文件存储基于阿里云 OSS，由 {@link OssFileService} 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonService {

    private final OssFileService ossFileService;
    private final OssProperties ossProperties;

    private final SemesterRepository semesterRepository;
    private final DictionaryRepository dictionaryRepository;
    private final EvaluationIndicatorRepository evaluationIndicatorRepository;
    private final IndicatorRuleVersionRepository indicatorRuleVersionRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final AttachmentLimitRepository attachmentLimitRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final ArchiveTypeConfigRepository archiveTypeConfigRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * 接口传入 type → 数据库 file_category 映射
     */
    private static final Map<String, String> TYPE_TO_CATEGORY = Map.of(
            "evidence", "proof",
            "certificate", "certificate",
            "plan", "other",
            "avatar", "photo",
            "photo", "photo",
            "proof", "proof",
            "other", "other"
    );

    /** 可直接预览的 MIME 类型前缀 */
    private static final Set<String> PREVIEWABLE_MIME_PREFIXES = Set.of("image/", "application/pdf");

    /** 管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";

    private static DictItemResponse item(String value, String label, int sort) {
        return DictItemResponse.builder().value(value).label(label).sort(sort).build();
    }

    // ==================== 文件上传 ====================

    /**
     * 上传文件到 OSS（先存临时目录）
     * <p>
     * 1. 校验文件类型、大小（根据 attachment_limits 配置）
     * 2. 上传到 OSS 临时目录
     * 3. 写入 file_uploads 表（file_status=1 暂存）
     *
     * @param file   上传的文件
     * @param type   文件类型（evidence/certificate/plan/avatar 等）
     * @param module 所属业务模块（competition/scholarship/practice 等）
     * @param userId 当前用户 ID
     * @return 上传响应
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, String type, String module, Long userId) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 2. 转换文件类别
        String fileCategory = TYPE_TO_CATEGORY.get(type);
        if (fileCategory == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型: " + type);
        }

        // 3. 文件扩展名校验
        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);
        if (ext == null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "文件缺少扩展名");
        }

        // 4. 根据 attachment_limits 校验（如果存在对应配置）
        AttachmentLimit limit = attachmentLimitRepository
                .findBySchoolIdAndArchiveType(0L, module)
                .orElse(null);
        if (limit != null) {
            // 校验扩展名
            String allowedExts = limit.getAllowedExtensions();
            if (allowedExts != null && !allowedExts.isEmpty()) {
                List<String> allowedList = parseJsonArray(allowedExts);
                if (!allowedList.isEmpty() && allowedList.stream().noneMatch(ext::equalsIgnoreCase)) {
                    throw new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                            "不支持的文件格式，允许: " + String.join(", ", allowedList));
                }
            }
            // 校验文件大小
            Long maxSize = limit.getMaxFileSize();
            if (maxSize != null && maxSize > 0 && file.getSize() > maxSize) {
                throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                        "文件过大，最大允许 " + (maxSize / 1024 / 1024) + "MB");
            }
        } else {
            // 默认校验：文件大小不超过 maxFileSize
            long maxFileSize = ossProperties.getMaxFileSize();
            if (file.getSize() > maxFileSize) {
                throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                        "文件过大，最大允许 " + (maxFileSize / 1024 / 1024) + "MB");
            }
        }

        // 5. 上传到 OSS 临时目录
        String objectKey;
        try {
            objectKey = ossFileService.uploadTempFile(file);
        } catch (Exception e) {
            log.error("OSS 临时文件上传失败: type={}, module={}, filename={}, size={}, error={}",
                    type, module, file.getOriginalFilename(), file.getSize(), e.getMessage(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED,
                    "文件上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }

        // 6. 生成临时签名 URL
        String fileUrl = ossFileService.generatePresignedUrl(objectKey, ossProperties.getUrlExpireMinutes());

        // 7. 写入 file_uploads 表
        AttachmentRelation relation = new AttachmentRelation();
        relation.setUserId(userId);
        relation.setBizType(module);
        relation.setFileCategory(fileCategory);
        relation.setOriginalName(originalFilename);
        relation.setFilePath(objectKey);
        relation.setFileSize(file.getSize());
        relation.setMimeType(file.getContentType());
        relation.setDisk("oss");
        relation.setFileStatus(1); // 暂存
        relation.setTempExpireAt(LocalDateTime.now().plusHours(ossProperties.getTempExpireHours()));
        relation.setDownloadExpireAt(LocalDateTime.now().plusMinutes(ossProperties.getUrlExpireMinutes()));
        relation.setConvertStatus(0);
        relation.setSortOrder(0);
        attachmentRelationRepository.save(relation);

        log.info("文件上传成功: fileId={}, originalName={}, objectKey={}, userId={}",
                relation.getId(), originalFilename, objectKey, userId);

        // 8. 构建响应
        return FileUploadResponse.builder()
                .fileId(relation.getId())
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .objectKey(objectKey)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
    }

    // ==================== 文件预览 ====================

    /**
     * 获取文件预览信息
     * <p>
     * 校验当前用户是否为文件所有者，管理员可预览任意文件。
     *
     * @param fileId 文件 ID
     * @param userId 当前用户 ID
     * @return 文件预览响应
     */
    @Transactional(readOnly = true)
    public FilePreviewResponse previewFile(Long fileId, Long userId) {
        AttachmentRelation relation = attachmentRelationRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));

        // 权限校验：文件所有者或管理员可预览
        if (!Objects.equals(relation.getUserId(), userId) && !isAdmin(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }

        String fileUrl = ossFileService.getFileUrl(relation.getFilePath());
        String mimeType = relation.getMimeType();
        boolean canPreview = mimeType != null && PREVIEWABLE_MIME_PREFIXES.stream().anyMatch(mimeType::startsWith);

        return FilePreviewResponse.builder()
                .fileId(relation.getId())
                .fileName(relation.getOriginalName())
                .fileUrl(fileUrl)
                .objectKey(relation.getFilePath())
                .fileType(mimeType)
                .previewUrl(canPreview ? fileUrl : null)
                .canPreview(canPreview)
                .build();
    }

    // ==================== 文件下载 ====================

    /**
     * 生成文件下载 URL
     * <p>
     * 校验当前用户是否为文件所有者，管理员可下载任意文件。
     *
     * @param fileId 文件 ID
     * @param userId 当前用户 ID
     * @return 下载 URL
     */
    @Transactional(readOnly = true)
    public String downloadFile(Long fileId, Long userId) {
        AttachmentRelation relation = attachmentRelationRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));

        // 权限校验：文件所有者或管理员可下载
        if (!Objects.equals(relation.getUserId(), userId) && !isAdmin(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }

        // 校验下载有效期：download_expire_at 非空且已过期时拒绝下载
        if (relation.getDownloadExpireAt() != null
                && LocalDateTime.now().isAfter(relation.getDownloadExpireAt())) {
            throw new BusinessException(ResultCode.DATA_LOCKED, "文件下载链接已过期");
        }

        String fileUrl = ossFileService.getFileUrl(relation.getFilePath());
        if (fileUrl == null) {
            throw new BusinessException(ResultCode.DATA_LOCKED, "文件下载链接已过期");
        }

        return fileUrl;
    }

    // ==================== 删除文件 ====================

    /**
     * 删除未提交附件（软删除 + 移除 OSS 文件）
     * <p>
     * 仅允许删除当前登录用户上传、且满足以下条件之一的附件：
     * - 未关联业务记录（biz_id IS NULL）
     * - 关联的业务记录处于草稿或已退回状态
     *
     * @param fileId 文件 ID
     * @param userId 当前用户 ID
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        AttachmentRelation relation = attachmentRelationRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));

        // 校验文件状态：仅允许删除 file_status=1（暂存）的记录
        // 已关联业务（file_status=2）的记录需由业务层判断状态流转
        if (relation.getFileStatus() != 1) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "当前文件状态不可删除，请先取消关联");
        }

        // 删除 OSS 物理文件
        try {
            ossFileService.deleteFile(relation.getFilePath());
        } catch (Exception e) {
            log.warn("OSS 文件删除失败（可能已被清理）: objectKey={}", relation.getFilePath(), e);
        }

        // 软删除数据库记录（native update 绕过 updatable=false 限制）
        attachmentRelationRepository.softDeleteById(fileId, LocalDateTime.now(), userId);

        log.info("文件删除成功: fileId={}, objectKey={}, userId={}", fileId, relation.getFilePath(), userId);
    }

    // ==================== 头像上传 ====================

    /**
     * 上传头像（直接存储到 OSS 正式目录 + 更新 user_contact_infos.avatar）
     * <p>
     * 与普通文件上传不同，头像上传不走临时目录（temp/），
     * 直接上传至 OSS 正式目录 avatar/{uuid}.{ext}，
     * 并在上传成功后立即更新用户联系信息的头像字段。
     *
     * @param file   上传的头像文件（仅允许 jpg/png/gif/webp/bmp，最大 2MB）
     * @param userId 当前登录用户 ID
     * @return 头像上传响应（含完整访问 URL）
     */
    @Transactional
    public AvatarUploadResponse uploadAvatar(MultipartFile file, Long userId) {
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

        // 3. 校验文件大小（头像限制 2MB）
        long maxAvatarSize = 2 * 1024 * 1024L;
        if (file.getSize() > maxAvatarSize) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "头像文件过大，最大允许 2MB，当前文件大小: " + (file.getSize() / 1024) + "KB");
        }

        // 4. 上传到 OSS 头像目录（avatar/{uuid}.{ext}）
        String objectKey;
        try {
            objectKey = ossFileService.uploadAvatar(file);
        } catch (Exception e) {
            log.error("OSS 头像上传失败: filename={}, size={}, error={}",
                    originalFilename, file.getSize(), e.getMessage(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED,
                    "头像上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }

        // 5. 生成头像访问 URL（30 天有效，生产环境建议 OSS 头像目录开启公共读后改用不带签名的 URL）
        String avatarUrl = ossFileService.generatePresignedUrl(objectKey, 30 * 24 * 60);

        // 6. 更新 user_contact_infos.avatar 字段
        UserContactInfo contactInfo = userContactInfoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserContactInfo newInfo = new UserContactInfo();
                    newInfo.setUserId(userId);
                    return newInfo;
                });
        contactInfo.setAvatar(avatarUrl);
        contactInfo.setUpdatedBy(userId);
        userContactInfoRepository.save(contactInfo);

        log.info("头像上传成功: userId={}, objectKey={}", userId, objectKey);

        return AvatarUploadResponse.builder()
                .avatarUrl(avatarUrl)
                .objectKey(objectKey)
                .build();
    }

    // ==================== 学期下拉选项 ====================

    /**
     * 获取学期下拉选项列表
     * <p>
     * 数据来源：semesters 表，status=1 且 deleted_at IS NULL，按 start_date 倒序。
     *
     * @param schoolId 学校 ID
     * @return 学期列表
     */
    @Transactional(readOnly = true)
    public List<SemesterItemResponse> getSemesters(Long schoolId) {
        return semesterRepository.findActiveBySchoolId(schoolId)
                .stream()
                .map(s -> SemesterItemResponse.builder()
                        .value(Math.toIntExact(s.getId()))
                        .label(generateSemesterLabel(s.getName()))
                        .name(s.getName())
                        .isCurrent(s.getIsCurrent())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 字典数据 ====================

    // ==================== 枚举查询 ====================

    /**
     * 枚举值映射（int value → label），供前端做下拉/展示使用。
     * <p>
     * 与 {@link #getDict(String)} 的区别：
     * <ul>
     *   <li>{@code getDict()} 返回 {@code dictionaries} 表数据（string dict_code → label）</li>
     *   <li>{@code getEnum()} 返回代码级枚举数据（int value → label）</li>
     * </ul>
     * 适用场景：DB 字段存 int 值（如 gender 存 0/1/2），
     * 但字典表 dict_code 是 string 编码（unknown/male/female），
     * 前端无法直接匹配时调用本接口获取 int→label 映射。
     */
    private static final Map<String, List<DictItemResponse>> ENUM_VALUES = buildEnumValues();

    private static Map<String, List<DictItemResponse>> buildEnumValues() {
        Map<String, List<DictItemResponse>> map = new HashMap<>();
        map.put("gender", Arrays.stream(GenderEnum.values())
                .map(e -> item(String.valueOf(e.getValue()), e.getLabel(), e.getValue()))
                .collect(Collectors.toList()));
        map.put("apply_status", Arrays.stream(ApplyStatusEnum.values())
                .map(e -> item(String.valueOf(e.getValue()), e.getLabel(), e.getValue()))
                .collect(Collectors.toList()));
        map.put("scope_type", Arrays.stream(ScopeTypeEnum.values())
                .map(e -> item(String.valueOf(e.getValue()), e.getLabel(), e.getValue()))
                .collect(Collectors.toList()));
        map.put("audit_action", Arrays.stream(AuditActionEnum.values())
                .map(e -> item(String.valueOf(e.getValue()), e.getLabel(), e.getValue()))
                .collect(Collectors.toList()));
        map.put("event_type", Arrays.stream(EventTypeEnum.values())
                .map(e -> item(String.valueOf(e.getValue()), e.getLabel(), e.getValue()))
                .collect(Collectors.toList()));
        map.put("role_level", Arrays.stream(RoleLevelEnum.values())
                .map(e -> item(String.valueOf(e.getValue()), e.getLabel(), e.getValue()))
                .collect(Collectors.toList()));
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取枚举数据（int value → label 映射）
     * <p>
     * 用于前端下拉筛选等场景，返回代码级枚举的 int 值与对应 label。
     *
     * @param enumType 枚举类型名称
     * @return 枚举项列表
     */
    public List<DictItemResponse> getEnum(String enumType) {
        List<DictItemResponse> result = ENUM_VALUES.get(enumType);
        if (result == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的枚举类型: " + enumType);
        }
        return result;
    }

    /**
     * 获取字典数据（下拉选项）
     * <p>
     * 数据来源：dictionaries 表（archive_category 除外，来源为 archive_type_configs 表），
     * status=1 且 deleted_at IS NULL，按 sort 正序。
     * 使用 Caffeine 缓存（5 分钟过期），降低字典表频繁查询压力。
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dict", key = "#dictType", unless = "#result.isEmpty()")
    public List<DictItemResponse> getDict(String dictType) {
        // 特殊处理：archive_category 从 archive_type_configs 表获取
        if ("archive_category".equals(dictType)) {
            return archiveTypeConfigRepository.findAllActive()
                    .stream()
                    .map(c -> DictItemResponse.builder()
                            .value(c.getArchiveType())
                            .label(c.getTypeName())
                            .sort(c.getSort())
                            .build())
                    .collect(Collectors.toList());
        }

        // 普通字典从 dictionaries 表获取
        List<DictItemResponse> result = dictionaryRepository.findActiveByDictType(dictType)
                .stream()
                .map(d -> DictItemResponse.builder()
                        .value(d.getDictCode())
                        .label(d.getDictName())
                        .sort(d.getSort())
                        .build())
                .collect(Collectors.toList());

        return result;
    }

    // ==================== 指标树查询 ====================

    /**
     * 获取指标树
     * <p>
     * 数据来源：evaluation_indicators、indicator_rule_versions、ability_dimensions 表，
     * 仅返回 status=1 且 deleted_at IS NULL 的指标。
     * 使用 Caffeine 缓存（5 分钟过期），避免频繁查询指标树。
     *
     * @param versionId 指定指标版本 ID（null 则返回当前生效版本）
     * @param schoolId  学校 ID
     * @return 指标树响应
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "indicators", key = "#versionId != null ? #versionId.toString().concat('-').concat(#schoolId.toString()) : 'latest-'.concat(#schoolId.toString())")
    public IndicatorTreeResponse getIndicators(Long versionId, Long schoolId) {
        IndicatorRuleVersion version;

        if (versionId != null) {
            // 指定版本
            version = indicatorRuleVersionRepository.findById(versionId)
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "指标版本不存在"));
        } else {
            // 当前生效版本
            version = indicatorRuleVersionRepository.findCurrentEffective(schoolId)
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "未找到当前生效的指标版本"));
        }

        // 查询该版本下所有启用指标
        List<EvaluationIndicator> allIndicators = evaluationIndicatorRepository
                .findActiveByVersion(version.getVersion());

        // 构建维度名称映射（dimensionCode → dimensionName）
        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive()
                .stream()
                .collect(Collectors.toMap(
                        com.example.studentarchives.entity.foundation.AbilityDimension::getDimensionCode,
                        com.example.studentarchives.entity.foundation.AbilityDimension::getDimensionName,
                        (a, b) -> a));

        // 构建树结构
        List<IndicatorTreeResponse.IndicatorNode> tree = buildIndicatorTree(allIndicators, null, dimensionNameMap);

        return IndicatorTreeResponse.builder()
                .versionId(version.getId())
                .versionName(version.getVersionName())
                .effectiveAt(version.getEffectiveAt() != null ? version.getEffectiveAt().toString() : null)
                .indicators(tree)
                .build();
    }

    /**
     * 递归构建指标树
     *
     * @param allIndicators 全部指标列表
     * @param parentId      父级 ID（null 表示一级节点）
     * @param dimensionNameMap 维度编码 → 维度名称映射
     * @return 指标节点列表
     */
    private List<IndicatorTreeResponse.IndicatorNode> buildIndicatorTree(
            List<EvaluationIndicator> allIndicators, Long parentId, Map<String, String> dimensionNameMap) {

        return allIndicators.stream()
                .filter(e -> Objects.equals(e.getParentId(), parentId))
                .map(e -> {
                    List<IndicatorTreeResponse.IndicatorNode> children = buildIndicatorTree(allIndicators, e.getId(), dimensionNameMap);
                    return IndicatorTreeResponse.IndicatorNode.builder()
                            .indicatorId(e.getId())
                            .indicatorCode(e.getIndicatorCode())
                            .indicatorName(e.getIndicatorName())
                            .level(e.getLevel())
                            .weight(e.getWeight())
                            .dimensionCode(e.getDimensionCode())
                            .dimensionName(e.getDimensionCode() != null ? dimensionNameMap.get(e.getDimensionCode()) : null)
                            .children(children.isEmpty() ? null : children)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== 角色权限校验 ====================

    /**
     * 判断当前用户是否拥有管理员角色
     * <p>
     * 用于文件预览/下载等操作的管理员绕过权限校验。
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) return false;
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        return roleRepository.findByIdIn(roleIds).stream()
                .anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getCode()));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据学期 name 生成展示 label
     * 如 "2022-2023-1" → "2022-2023第一学期"
     */
    private String generateSemesterLabel(String name) {
        if (name == null || name.isEmpty()) return name;
        int lastDash = name.lastIndexOf('-');
        if (lastDash <= 0 || lastDash >= name.length() - 1) return name;

        String prefix = name.substring(0, lastDash);
        String suffix = name.substring(lastDash + 1);
        String semesterChinese;
        try {
            int semesterNum = Integer.parseInt(suffix);
            semesterChinese = switch (semesterNum) {
                case 1 -> "第一学期";
                case 2 -> "第二学期";
                case 3 -> "第三学期";
                default -> "第" + semesterNum + "学期";
            };
        } catch (NumberFormatException e) {
            semesterChinese = suffix;
        }
        return prefix + semesterChinese;
    }

    /**
     * 提取文件扩展名（不含点号）
     */
    private static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1).toLowerCase()
                : null;
    }

    /**
     * 解析 JSON 数组字符串为 List
     * 如 "[\"pdf\",\"jpg\",\"png\"]" → ["pdf", "jpg", "png"]
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            // 简单解析：去掉 [] 和引号，按逗号分隔
            String trimmed = json.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            if (trimmed.isEmpty()) return Collections.emptyList();
            return Arrays.stream(trimmed.split(","))
                    .map(s -> s.trim().replaceAll("^\"|\"$", "").toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
