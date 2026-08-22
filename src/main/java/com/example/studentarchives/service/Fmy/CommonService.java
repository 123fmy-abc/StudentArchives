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
import com.example.studentarchives.dto.Fmy.indicator.response.AdminIndicatorTreeResponse;
import com.example.studentarchives.entity.export.ExportOperationLog;
import com.example.studentarchives.entity.file.AttachmentLimit;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import com.example.studentarchives.entity.foundation.IndicatorRuleVersion;
import com.example.studentarchives.entity.career.CareerAction;
import com.example.studentarchives.entity.career.CareerGoal;
import com.example.studentarchives.entity.career.CareerMilestone;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ArchiveTypeConfigRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.CareerActionRepository;
import com.example.studentarchives.repository.CareerGoalRepository;
import com.example.studentarchives.repository.CareerMilestoneRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.AttachmentLimitRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.ExportOperationLogRepository;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.EvaluationIndicatorRepository;
import com.example.studentarchives.repository.IndicatorRuleVersionRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.RoleScopeRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final CareerActionRepository careerActionRepository;
    private final CareerGoalRepository careerGoalRepository;
    private final CareerMilestoneRepository careerMilestoneRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final RoleScopeRepository roleScopeRepository;
    private final ExportOperationLogRepository exportOperationLogRepository;
    private final ObjectMapper objectMapper;

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
     * 权限校验：文件所有者、管理员、或其授权范围覆盖该学生的教师可预览。
     *
     * @param fileId 文件 ID
     * @param userId 当前用户 ID
     * @return 文件预览响应
     */
    @Transactional(readOnly = true)
    public FilePreviewResponse previewFile(Long fileId, Long userId) {
        AttachmentRelation relation = attachmentRelationRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));

        // 权限校验：文件所有者、管理员、或其授权范围覆盖该学生的教师可预览
        if (!canAccessFile(userId, relation)) {
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
     * 权限校验：文件所有者、管理员、或其授权范围覆盖该学生的教师可下载。
     * 研究数据导出文件（biz_type=research_export）下载时补写 export_operation_logs(action=2) 下载审计。
     *
     * @param fileId 文件 ID
     * @param userId 当前用户 ID
     * @return 下载 URL
     */
    @Transactional
    public String downloadFile(Long fileId, Long userId) {
        AttachmentRelation relation = attachmentRelationRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));

        // 权限校验：文件所有者、管理员、或其授权范围覆盖该学生的教师可下载
        if (!canAccessFile(userId, relation)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }

        // 校验下载有效期：download_expire_at 非空且已过期时拒绝下载
        if (relation.getDownloadExpireAt() != null
                && LocalDateTime.now().isAfter(relation.getDownloadExpireAt())) {
            throw new BusinessException(ResultCode.DATA_LOCKED, "文件下载链接已过期");
        }

        // 生成 OSS 签名 URL：对象不存在（已物理清理/缺失）时返回 null，与有效期过期区分报错
        String fileUrl = ossFileService.getFileUrl(relation.getFilePath(), relation.getOriginalName());
        if (fileUrl == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "文件不存在或已失效");
        }

        // 研究数据导出下载审计（export_operation_logs action=2），以创建记录（action=1）为模板
        recordExportDownloadAudit(relation, userId);

        return fileUrl;
    }

    /**
     * 记录导出文件的下载审计（export_operation_logs action=2）。
     * <p>
     * 仅对 biz_type=research_export（研究数据导出）与 student_archive（学生档案导出，
     * 含管理端一键导出与学生端个人导出）的文件生效；以该文件创建时的审计记录（action=1）为模板
     * 复制范围、筛选条件、记录数、匿名化、数据版本与字段说明快照，operator 记为下载者。
     */
    private void recordExportDownloadAudit(AttachmentRelation relation, Long userId) {
        String bizType = relation.getBizType();
        if (!AdminExportService.FILE_BIZ_TYPE.equals(bizType)
                && !AdminExportService.FILE_BIZ_TYPE_ARCHIVE.equals(bizType)) {
            return;
        }
        exportOperationLogRepository.findTopByFileIdAndActionOrderByCreatedAtDesc(relation.getId(), 1)
                .ifPresent(created -> {
                    ExportOperationLog opLog = new ExportOperationLog();
                    opLog.setSchoolId(created.getSchoolId());
                    opLog.setOperatorId(userId);
                    opLog.setExportType(created.getExportType());
                    opLog.setAction(2);
                    opLog.setScopeType(created.getScopeType());
                    opLog.setScopeId(created.getScopeId());
                    opLog.setFilterConditions(created.getFilterConditions());
                    opLog.setRecordCount(created.getRecordCount());
                    opLog.setIsAnonymized(created.getIsAnonymized());
                    opLog.setDataVersion(created.getDataVersion());
                    opLog.setFieldDescription(created.getFieldDescription());
                    opLog.setFileId(relation.getId());
                    opLog.setStatus(1);
                    exportOperationLogRepository.save(opLog);
                });
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
     * 已发布版本优先读取 {@code indicator_rule_versions.tree_snapshot}（发布时点的完整指标树快照，
     * 历史版本可精确回溯，且不包含发布后未发布的草稿改动）；
     * 快照落地前已发布的旧数据回退到按 {@code evaluation_indicators.version} 查询。
     * 使用 Caffeine 缓存（5 分钟过期），避免频繁查询指标树。
     *
     * @param versionId 指定指标版本 ID（null 则返回当前生效版本）
     * @param schoolId  学校 ID
     * @return 指标树响应
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "indicators", key = "#versionId != null ? #versionId.toString().concat('-').concat(#schoolId.toString()) : 'latest-'.concat(#schoolId.toString())")
    public IndicatorTreeResponse getIndicators(Long versionId, Long schoolId) {
        IndicatorRuleVersion version = resolveRuleVersion(versionId, schoolId);

        List<IndicatorTreeResponse.IndicatorNode> tree;
        if (version.getTreeSnapshot() != null && !version.getTreeSnapshot().isBlank()) {
            // 已发布版本：直接读取发布时点的完整指标树快照，历史版本可精确回溯
            tree = toStudentNodes(readSnapshot(version.getTreeSnapshot()));
        } else {
            // 兼容快照落地前已发布的旧数据：按版本号查当前指标表（历史版本将返回空树）
            List<EvaluationIndicator> allIndicators = evaluationIndicatorRepository.findActiveByVersion(version.getVersion());
            Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                    .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                            AbilityDimension::getDimensionName, (a, b) -> a));
            tree = buildIndicatorTree(allIndicators, null, dimensionNameMap);
        }

        return IndicatorTreeResponse.builder()
                .versionId(version.getId())
                .versionName(version.getVersionName())
                .effectiveAt(version.getEffectiveAt() != null ? version.getEffectiveAt().toString() : null)
                .indicators(tree)
                .build();
    }

    /**
     * 解析指标规则版本：versionId 指定时按 ID 查询，否则取当前生效版本。
     */
    private IndicatorRuleVersion resolveRuleVersion(Long versionId, Long schoolId) {
        if (versionId != null) {
            return indicatorRuleVersionRepository.findById(versionId)
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "指标版本不存在"));
        }
        return indicatorRuleVersionRepository.findCurrentEffective(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "未找到当前生效的指标版本"));
    }

    /**
     * 反序列化指标树快照 JSON（发布时写入的完整节点结构）。
     */
    private List<AdminIndicatorTreeResponse.IndicatorNode> readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson,
                    new TypeReference<List<AdminIndicatorTreeResponse.IndicatorNode>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "指标树快照解析失败");
        }
    }

    /**
     * 管理端完整节点 → 学生端精简节点（字段子集映射，递归）。
     */
    private List<IndicatorTreeResponse.IndicatorNode> toStudentNodes(List<AdminIndicatorTreeResponse.IndicatorNode> nodes) {
        if (nodes == null) {
            return null;
        }
        return nodes.stream()
                .map(n -> IndicatorTreeResponse.IndicatorNode.builder()
                        .indicatorId(n.getId())
                        .indicatorCode(n.getIndicatorCode())
                        .indicatorName(n.getIndicatorName())
                        .level(n.getLevel())
                        .weight(n.getWeight())
                        .dimensionCode(n.getDimensionCode())
                        .dimensionName(n.getDimensionName())
                        .children(toStudentNodes(n.getChildren()))
                        .build())
                .collect(Collectors.toList());
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
     * 判断当前用户是否有权访问附件
     * <p>
     * 权限规则（满足任一即可）：
     * 1. 当前用户是附件上传者本人；
     * 2. 当前用户是管理员（admin 角色）；
     * 3. 附件归属学生（业务记录属主，兜底上传者）为当前用户本人；
     * 4. 当前用户是教师/辅导员等，且其 {@code role_scopes} 授权范围覆盖归属学生：
     *    学校(1)/学院(2)/专业(3)/班级(4) 范围与学生的组织归属逐级匹配，
     *    且范围在生效期内（valid_from/valid_until）且学期维度（semester_id）一致。
     *    课程(5)/年级(6) 等范围暂无可直接映射的组织归属，不据此放行。
     * <p>
     * 归属学生解析（见 {@link #resolveFileOwner}）：优先按附件绑定的业务记录
     * （archive/award/career_plan 及行动/里程碑链）解析属主学生与记录学期；
     * 未绑定记录或记录无学生属主（如管理端批量导出）时，上传者为学生本人则以上传者
     * 为属主，否则视为非学生个人数据——仅上传者本人与管理员可访问。
     *
     * @param operatorId 当前操作者用户 ID
     * @param relation   附件关系记录
     * @return 有权访问返回 true
     */
    private boolean canAccessFile(Long operatorId, AttachmentRelation relation) {
        Long uploaderId = relation.getUserId();
        // 1. 上传者本人
        if (Objects.equals(operatorId, uploaderId)) {
            return true;
        }
        // 2. 管理员
        if (isAdmin(operatorId)) {
            return true;
        }
        if (operatorId == null || uploaderId == null) {
            return false;
        }

        // 3. 解析附件归属学生（业务记录属主优先，兜底上传者；非学生数据返回 null）
        FileOwnerContext owner = resolveFileOwner(relation);
        if (owner == null || owner.studentId() == null) {
            // 非学生个人数据（如管理端批量导出）：仅上传者本人与管理员可访问
            return false;
        }
        Long ownerId = owner.studentId();
        // 4. 归属学生本人（含教师/辅导员代传后学生查看自身数据）
        if (Objects.equals(operatorId, ownerId)) {
            return true;
        }

        // 5. 解析归属学生的组织归属：users.school_id → student_profiles.class_id
        //    → classes.major_id → majors.college_id（school_id 取 users 表即可）
        User ownerUser = userRepository.findById(ownerId).orElse(null);
        if (ownerUser == null) {
            return false;
        }
        Long schoolId = ownerUser.getSchoolId();
        Long classId = null;
        Long majorId = null;
        Long collegeId = null;
        StudentProfile profile = studentProfileRepository.findByUserId(ownerId).orElse(null);
        if (profile != null && profile.getClassId() != null) {
            classId = profile.getClassId();
            Clazz clazz = clazzRepository.findById(profile.getClassId()).orElse(null);
            if (clazz != null && clazz.getMajorId() != null) {
                majorId = clazz.getMajorId();
                Major major = majorRepository.findById(clazz.getMajorId()).orElse(null);
                if (major != null) {
                    collegeId = major.getCollegeId();
                }
            }
        }

        // 6. 授权范围匹配：仅启用状态 + 生效期内 + 学期维度一致的 role_scopes
        LocalDate today = LocalDate.now();
        List<RoleScope> scopes = roleScopeRepository.findByUserIdAndStatus(operatorId, 1);
        for (RoleScope scope : scopes) {
            if (!isScopeInEffect(scope, today)) {
                continue;
            }
            if (!isScopeSemesterMatched(scope, owner.semesterId())) {
                continue;
            }
            Long scopeId = scope.getScopeId();
            Integer scopeType = scope.getScopeType();
            if (scopeId == null || scopeType == null) {
                continue;
            }
            switch (scopeType) {
                case 1 -> { // 学校
                    if (scopeId.equals(schoolId)) return true;
                }
                case 2 -> { // 学院
                    if (scopeId.equals(collegeId)) return true;
                }
                case 3 -> { // 专业
                    if (scopeId.equals(majorId)) return true;
                }
                case 4 -> { // 班级
                    if (scopeId.equals(classId)) return true;
                }
                default -> { // 课程(5)/年级(6) 暂不据此放行
                }
            }
        }
        return false;
    }

    /** 附件归属学生上下文：数据所有者学生 + 所属记录学期（用于教师范围学期维度匹配） */
    private record FileOwnerContext(Long studentId, Long semesterId) {}

    /**
     * 解析附件归属学生：优先按业务记录（biz_type+biz_id）解析属主学生与记录学期；
     * 未绑定记录或记录无学生属主（如管理端批量导出）时，上传者为学生本人则以上传者
     * 为属主，否则返回 null（非学生个人数据，不适用教师范围放行）。
     */
    private FileOwnerContext resolveFileOwner(AttachmentRelation relation) {
        String bizType = relation.getBizType();
        Long bizId = relation.getBizId();
        if (bizType != null && bizId != null) {
            FileOwnerContext recordOwner = resolveRecordOwner(bizType, bizId);
            if (recordOwner != null) {
                return recordOwner;
            }
        }
        // 兜底：上传者本人（仅当其为学生）
        Long uploaderId = relation.getUserId();
        if (uploaderId != null && studentProfileRepository.findByUserId(uploaderId).isPresent()) {
            return new FileOwnerContext(uploaderId, null);
        }
        return null;
    }

    /**
     * 按业务记录解析归属学生与学期（archive/award/career_plan 及其行动/里程碑链）。
     * 返回 null 表示该业务类型无学生属主（如管理端导出、公告等）。
     */
    private FileOwnerContext resolveRecordOwner(String bizType, Long bizId) {
        switch (bizType) {
            case "archive" -> {
                return archiveRepository.findById(bizId)
                        .map(a -> new FileOwnerContext(a.getUserId(), a.getSemesterId()))
                        .orElse(null);
            }
            case "award" -> {
                return awardApplicationRepository.findById(bizId)
                        .map(a -> new FileOwnerContext(a.getUserId(), a.getSemesterId()))
                        .orElse(null);
            }
            case "career_plan", "career_plan_export", "career_plan_export_external" -> {
                return careerPlanRepository.findById(bizId)
                        .map(p -> new FileOwnerContext(p.getUserId(), p.getSemesterId()))
                        .orElse(null);
            }
            case "career_action" -> {
                return resolveCareerActionOwner(bizId);
            }
            case "career_milestone" -> {
                return resolveCareerMilestoneOwner(bizId);
            }
            default -> {
                return null;
            }
        }
    }

    /** 职业规划行动归属：action → goal → plan，解析属主学生与学期 */
    private FileOwnerContext resolveCareerActionOwner(Long actionId) {
        CareerAction action = careerActionRepository.findById(actionId).orElse(null);
        if (action == null || action.getGoalId() == null) {
            return null;
        }
        CareerGoal goal = careerGoalRepository.findById(action.getGoalId()).orElse(null);
        if (goal == null || goal.getCareerPlanId() == null) {
            return null;
        }
        return careerPlanRepository.findById(goal.getCareerPlanId())
                .map(p -> new FileOwnerContext(p.getUserId(), p.getSemesterId()))
                .orElse(null);
    }

    /** 职业规划里程碑归属：milestone → action → goal → plan，解析属主学生与学期 */
    private FileOwnerContext resolveCareerMilestoneOwner(Long milestoneId) {
        CareerMilestone milestone = careerMilestoneRepository.findById(milestoneId).orElse(null);
        if (milestone == null || milestone.getActionId() == null) {
            return null;
        }
        return resolveCareerActionOwner(milestone.getActionId());
    }

    /** 授权生效期校验：valid_from/valid_until 未设置视为永久有效 */
    private boolean isScopeInEffect(RoleScope scope, LocalDate today) {
        if (scope.getValidFrom() != null && today.isBefore(scope.getValidFrom())) {
            return false;
        }
        if (scope.getValidUntil() != null && today.isAfter(scope.getValidUntil())) {
            return false;
        }
        return true;
    }

    /** 学期维度校验：范围未限定学期，或与附件归属记录学期一致 */
    private boolean isScopeSemesterMatched(RoleScope scope, Long recordSemesterId) {
        Long scopeSemesterId = scope.getSemesterId();
        if (scopeSemesterId == null) {
            return true;
        }
        return scopeSemesterId.equals(recordSemesterId);
    }

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
