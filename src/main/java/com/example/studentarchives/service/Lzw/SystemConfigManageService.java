package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.foundation.SystemSetting;
import com.example.studentarchives.entity.message.Announcement;
import com.example.studentarchives.enums.AnnouncementTargetTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AnnouncementRepository;
import com.example.studentarchives.repository.SystemSettingRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端系统配置与公告管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》十二、系统配置与公告管理模块（12.1 ~ 12.6）。
 * 数据来源：system_settings、announcements。
 * <p>
 * 权限：关键权限码表未列出系统配置/公告相关权限码，故均要求 admin 角色（越权返回 20005）。
 * 数据隔离：公告按操作人所属学校隔离（school_id 不再由前端传入）；系统配置无学校维度，全局共享。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigManageService {

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final SystemSettingRepository systemSettingRepository;
    private final AnnouncementRepository announcementRepository;
    private final AdminAuthService adminAuthService;

    // ==================== 12.1 获取系统配置列表 ====================

    @Transactional(readOnly = true)
    public PageResult<SettingItem> listSettings(Long operatorId, String group, String keyword, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);

        final String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        Specification<SystemSetting> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (group != null && !group.isBlank()) {
                predicates.add(cb.equal(root.get("settingGroup"), group.trim()));
            }
            if (kw != null) {
                predicates.add(cb.like(cb.lower(root.get("settingKey")), "%" + kw + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "settingGroup").and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<SystemSetting> page = systemSettingRepository.findAll(spec, pageable);

        List<SettingItem> items = page.getContent().stream().map(s -> SettingItem.builder()
                .settingKey(s.getSettingKey())
                .settingName(s.getSettingName())
                .settingValue(s.getSettingValue())
                .group(s.getSettingGroup())
                .description(s.getDescription())
                .updatedAt(toIso(s.getUpdatedAt()))
                .build()).toList();

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 12.2 更新系统配置 ====================

    @Transactional
    public void updateSetting(Long operatorId, SettingUpdateRequest body) {
        adminAuthService.requireAdmin(operatorId);

        String settingKey = requireNotBlank(body.getSettingKey(), "settingKey 不能为空");
        String settingValue = body.getSettingValue();
        if (settingValue == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "settingValue 不能为空");
        }

        SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "系统配置不存在"));
        setting.setSettingValue(settingValue);
        systemSettingRepository.save(setting);
    }

    // ==================== 12.3 获取公告列表 ====================

    @Transactional(readOnly = true)
    public PageResult<AnnouncementItem> listAnnouncements(Long operatorId, String targetType, Long targetId,
                                                          Integer status, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        Specification<Announcement> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schoolId"), schoolId));
            if (targetType != null && !targetType.isBlank()) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<Announcement> page = announcementRepository.findAll(spec, pageable);

        List<AnnouncementItem> items = page.getContent().stream().map(a -> AnnouncementItem.builder()
                .announcementId(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .targetType(a.getTargetType())
                .status(a.getStatus())
                .statusLabel(announcementStatusLabel(a.getStatus()))
                .publishedAt(toIso(a.getPublishedAt()))
                .publisherId(a.getPublisherId())
                .createdAt(toIso(a.getCreatedAt()))
                .build()).toList();

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 12.4 发布公告 ====================

    @Transactional
    public AnnouncementIdResponse createAnnouncement(Long operatorId, AnnouncementCreateRequest body) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        String title = requireNotBlank(body.getTitle(), "title 不能为空");
        String content = requireNotBlank(body.getContent(), "content 不能为空");
        String targetType = validateTargetType(body.getTargetType());
        Integer status = body.getStatus() != null ? body.getStatus() : 1;
        validateStatus(status);

        // targetType=all 时无需目标对象，强制清空 targetId
        Long targetId = AnnouncementTargetTypeEnum.ALL.getValue().equals(targetType) ? null : body.getTargetId();

        LocalDateTime publishedAt = body.getPublishedAt() != null
                ? parseDateTime(body.getPublishedAt(), "publishedAt")
                : LocalDateTime.now();

        Announcement a = new Announcement();
        a.setSchoolId(schoolId);
        a.setTitle(title);
        a.setContent(content);
        a.setPublisherId(operatorId);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setStatus(status);
        a.setPublishedAt(publishedAt);
        announcementRepository.save(a);

        return AnnouncementIdResponse.builder().announcementId(a.getId()).build();
    }

    // ==================== 12.5 更新公告 ====================

    @Transactional
    public void updateAnnouncement(Long operatorId, Long announcementId, AnnouncementUpdateRequest body) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        Announcement a = announcementRepository.findById(announcementId)
                .filter(x -> schoolId.equals(x.getSchoolId()))
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "公告不存在"));

        if (body.getTitle() != null) {
            a.setTitle(requireNotBlank(body.getTitle(), "title 不能为空"));
        }
        if (body.getContent() != null) {
            a.setContent(requireNotBlank(body.getContent(), "content 不能为空"));
        }
        if (body.getTargetType() != null) {
            a.setTargetType(validateTargetType(body.getTargetType()));
        }
        if (body.getStatus() != null) {
            validateStatus(body.getStatus());
            a.setStatus(body.getStatus());
        }
        if (body.getPublishedAt() != null) {
            a.setPublishedAt(parseDateTime(body.getPublishedAt(), "publishedAt"));
        }

        // targetId 单独处理：目标类型为 all 时置空，否则仅在显式传入时更新
        String effectiveType = a.getTargetType();
        if (AnnouncementTargetTypeEnum.ALL.getValue().equals(effectiveType)) {
            a.setTargetId(null);
        } else if (body.getTargetId() != null) {
            a.setTargetId(body.getTargetId());
        }

        announcementRepository.save(a);
    }

    // ==================== 12.6 删除公告 ====================

    @Transactional
    public void deleteAnnouncement(Long operatorId, Long announcementId) {
        adminAuthService.requireAdmin(operatorId);
        Long schoolId = adminAuthService.getOperatorSchoolId(operatorId);

        Announcement a = announcementRepository.findById(announcementId)
                .filter(x -> schoolId.equals(x.getSchoolId()))
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "公告不存在"));

        int updated = announcementRepository.softDeleteById(a.getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "公告不存在");
        }
    }

    // ==================== 通用辅助 ====================

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, message);
        }
        return value.trim();
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(禁用) 或 1(启用)");
        }
    }

    private String validateTargetType(String targetType) {
        String type = requireNotBlank(targetType, "targetType 不能为空");
        if (AnnouncementTargetTypeEnum.of(type) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "targetType 只能为 all/college/major/class");
        }
        return type;
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        String v = value.trim();
        try {
            return OffsetDateTime.parse(v).toLocalDateTime();
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(v);
            } catch (DateTimeParseException e2) {
                throw new BusinessException(ResultCode.PARAM_ERROR, fieldName + " 格式错误");
            }
        }
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    private String announcementStatusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "已发布" : "未发布";
    }

    // ==================== 内嵌 POJO ====================

    /** 12.1 系统配置列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SettingItem {
        private String settingKey;
        private String settingName;
        private String settingValue;
        private String group;
        private String description;
        private String updatedAt;
    }

    /** 12.2 更新系统配置请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingUpdateRequest {
        private String settingKey;
        private String settingValue;
    }

    /** 12.3 公告列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnnouncementItem {
        private Long announcementId;
        private String title;
        private String content;
        private String targetType;
        private Integer status;
        private String statusLabel;
        private String publishedAt;
        private Long publisherId;
        private String createdAt;
    }

    /** 12.4 发布公告请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementCreateRequest {
        private String title;
        private String content;
        private String targetType;
        private Long targetId;
        private Integer status;
        private String publishedAt;
    }

    /** 12.5 更新公告请求（同 12.4，部分更新） */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementUpdateRequest {
        private String title;
        private String content;
        private String targetType;
        private Long targetId;
        private Integer status;
        private String publishedAt;
    }

    /** 12.4 发布公告响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementIdResponse {
        private Long announcementId;
    }
}