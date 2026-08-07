package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Lzw.activity.request.ActivityListRequest;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityDetailResponse;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityListItemResponse;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityStatusResponse;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.enums.ActivityTypeEnum;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 动态记录 Service
 * <p>
 * 聚合 archives / award_applications / career_plans 三张表的数据，
 * 提供统一的列表、详情、编辑、删除、撤回功能。
 * <p>
 * 所有操作均校验 userId 归属权，禁止越权访问他人记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final SemesterRepository semesterRepository;

    /** 允许的排序字段白名单 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("submit_time", "created_at", "updated_at", "id");

    // ==================== 列表 ====================

    /**
     * 获取动态记录列表（跨 3 表聚合 + 内存筛选排序分页）
     *
     * @param request 筛选 + 分页参数
     * @param userId  当前登录用户 ID
     * @return 分页结果
     */
    public PageResult<ActivityListItemResponse> list(ActivityListRequest request, Long userId) {
        List<ActivityListItemResponse> allItems = new ArrayList<>();

        String filterType = request.getType();
        boolean fetchAll = (filterType == null || filterType.isEmpty());

        // 1. 查询各源表
        if (fetchAll || "archive".equals(filterType)) {
            List<Archive> archives = archiveRepository.findByUserId(userId);
            for (Archive a : archives) {
                if (matchesFilter(a.getStatus(), a.getArchiveType(), a.getSemesterId(), a.getTitle(), request)) {
                    allItems.add(toListItem(a));
                }
            }
        }

        if (fetchAll || "award".equals(filterType)) {
            List<AwardApplication> awards = awardApplicationRepository.findByUserId(userId);
            for (AwardApplication a : awards) {
                if (matchesFilter(a.getStatus(), a.getAwardType(), a.getSemesterId(), a.getTitle(), request)) {
                    allItems.add(toListItem(a));
                }
            }
        }

        if (fetchAll || "career_plan".equals(filterType)) {
            List<CareerPlan> plans = careerPlanRepository.findByUserId(userId);
            for (CareerPlan p : plans) {
                if (matchesFilter(p.getStatus(), null, p.getSemesterId(), p.getTitle(), request)) {
                    allItems.add(toListItem(p));
                }
            }
        }

        // 2. 排序：按 submitTime 倒序（nulls last），再按 id 倒序
        allItems.sort(Comparator
                .comparing(ActivityListItemResponse::getSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ActivityListItemResponse::getId, Comparator.reverseOrder()));

        // 3. 分页
        int total = allItems.size();
        int fromIndex = request.getOffset();
        int toIndex = Math.min(fromIndex + request.getPerPage(), total);

        List<ActivityListItemResponse> page = (fromIndex < total)
                ? allItems.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return PageResult.of(page, total, request);
    }

    // ==================== 详情 ====================

    /**
     * 获取动态记录详情（含佐证材料、版本历史）
     */
    public ActivityDetailResponse getDetail(ActivityTypeEnum type, Long activityId, Long userId) {
        return switch (type) {
            case ARCHIVE -> {
                Archive a = archiveRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
                checkOwnership(a.getUserId(), userId);
                yield toDetail(a);
            }
            case AWARD -> {
                AwardApplication a = awardApplicationRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
                checkOwnership(a.getUserId(), userId);
                yield toDetail(a);
            }
            case CAREER_PLAN -> {
                CareerPlan p = careerPlanRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
                checkOwnership(p.getUserId(), userId);
                yield toDetail(p);
            }
        };
    }

    // ==================== 编辑 ====================

    /**
     * 编辑动态记录（仅限草稿/已退回状态）
     * <p>
     * 编辑后状态自动变为待审批(1)，版本号 +1，提交次数 +1。
     */
    @Transactional
    public ActivityStatusResponse edit(ActivityTypeEnum type, Long activityId, ActivityEditBody body, Long userId) {
        return switch (type) {
            case ARCHIVE -> editArchive(activityId, body, userId);
            case AWARD -> editAward(activityId, body, userId);
            case CAREER_PLAN -> editCareerPlan(activityId, body, userId);
        };
    }

    // ==================== 删除 ====================

    /**
     * 软删除动态记录（仅限草稿/已退回状态）
     */
    @Transactional
    public void delete(ActivityTypeEnum type, Long activityId, Long userId) {
        switch (type) {
            case ARCHIVE -> {
                Archive a = archiveRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
                checkOwnership(a.getUserId(), userId);
                checkDeletable(a.getStatus());
                archiveRepository.softDeleteById(activityId, LocalDateTime.now());
            }
            case AWARD -> {
                AwardApplication a = awardApplicationRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
                checkOwnership(a.getUserId(), userId);
                checkDeletable(a.getStatus());
                awardApplicationRepository.softDeleteById(activityId, LocalDateTime.now());
            }
            case CAREER_PLAN -> {
                CareerPlan p = careerPlanRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
                checkOwnership(p.getUserId(), userId);
                checkDeletable(p.getStatus());
                careerPlanRepository.softDeleteById(activityId, LocalDateTime.now());
            }
        }
    }

    // ==================== 撤回 ====================

    /**
     * 撤回申报（仅限待审批状态 → 草稿）
     */
    @Transactional
    public ActivityStatusResponse withdraw(ActivityTypeEnum type, Long activityId, Long userId) {
        return switch (type) {
            case ARCHIVE -> {
                Archive a = archiveRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
                checkOwnership(a.getUserId(), userId);
                checkWithdrawable(a.getStatus());
                a.setStatus(0);
                a.getAuditInfo().setRevokedAt(LocalDateTime.now());
                archiveRepository.save(a);
                yield ActivityStatusResponse.builder()
                        .id(a.getId()).type("archive")
                        .status(0).statusLabel("草稿")
                        .currentVersion(a.getAuditInfo().getCurrentVersion())
                        .submitCount(a.getAuditInfo().getSubmitCount()).build();
            }
            case AWARD -> {
                AwardApplication a = awardApplicationRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
                checkOwnership(a.getUserId(), userId);
                checkWithdrawable(a.getStatus());
                a.setStatus(0);
                a.getAuditInfo().setRevokedAt(LocalDateTime.now());
                awardApplicationRepository.save(a);
                yield ActivityStatusResponse.builder()
                        .id(a.getId()).type("award")
                        .status(0).statusLabel("草稿")
                        .currentVersion(a.getAuditInfo().getCurrentVersion())
                        .submitCount(a.getAuditInfo().getSubmitCount()).build();
            }
            case CAREER_PLAN -> {
                CareerPlan p = careerPlanRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
                checkOwnership(p.getUserId(), userId);
                checkWithdrawable(p.getStatus());
                p.setStatus(0);
                p.getAuditInfo().setRevokedAt(LocalDateTime.now());
                careerPlanRepository.save(p);
                yield ActivityStatusResponse.builder()
                        .id(p.getId()).type("career_plan")
                        .status(0).statusLabel("草稿")
                        .currentVersion(p.getAuditInfo().getCurrentVersion())
                        .submitCount(p.getAuditInfo().getSubmitCount()).build();
            }
        };
    }

    // ==================== 私有：筛选匹配 ====================

    private boolean matchesFilter(Integer status, String typeCode, Long semesterId, String title, ActivityListRequest req) {
        if (req.getStatus() != null && !req.getStatus().equals(status)) return false;
        if (req.getArchiveType() != null && !req.getArchiveType().isEmpty()
                && !req.getArchiveType().equals(typeCode)) return false;
        if (req.getSemesterId() != null && !req.getSemesterId().equals(semesterId)) return false;
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()
                && (title == null || !title.toLowerCase().contains(req.getKeyword().toLowerCase()))) return false;
        return true;
    }

    // ==================== 私有：实体 → 列表项 DTO ====================

    private ActivityListItemResponse toListItem(Archive a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        return ActivityListItemResponse.builder()
                .id(a.getId()).type("archive")
                .archiveType(a.getArchiveType())
                .title(a.getTitle())
                .status(a.getStatus()).statusLabel(s.getLabel())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .submitTime(format(a.getAuditInfo().getSubmittedAt()))
                .currentVersion(a.getAuditInfo().getCurrentVersion())
                .submitCount(a.getAuditInfo().getSubmitCount())
                .canEdit(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canDelete(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canWithdraw(s == ApplyStatusEnum.PENDING)
                .build();
    }

    private ActivityListItemResponse toListItem(AwardApplication a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        return ActivityListItemResponse.builder()
                .id(a.getId()).type("award")
                .archiveType(a.getAwardType())
                .title(a.getTitle())
                .status(a.getStatus()).statusLabel(s.getLabel())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .submitTime(format(a.getAuditInfo().getSubmittedAt()))
                .currentVersion(a.getAuditInfo().getCurrentVersion())
                .submitCount(a.getAuditInfo().getSubmitCount())
                .canEdit(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canDelete(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canWithdraw(s == ApplyStatusEnum.PENDING)
                .build();
    }

    private ActivityListItemResponse toListItem(CareerPlan p) {
        ApplyStatusEnum s = ApplyStatusEnum.of(p.getStatus());
        return ActivityListItemResponse.builder()
                .id(p.getId()).type("career_plan")
                .title(p.getTitle())
                .status(p.getStatus()).statusLabel(s.getLabel())
                .semesterId(p.getSemesterId())
                .semesterName(lookupSemesterName(p.getSemesterId()))
                .submitTime(format(p.getAuditInfo().getSubmittedAt()))
                .currentVersion(p.getAuditInfo().getCurrentVersion())
                .submitCount(p.getAuditInfo().getSubmitCount())
                .canEdit(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canDelete(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canWithdraw(s == ApplyStatusEnum.PENDING)
                .build();
    }

    // ==================== 私有：实体 → 详情 DTO ====================

    private ActivityDetailResponse toDetail(Archive a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        return ActivityDetailResponse.builder()
                .id(a.getId()).type("archive")
                .title(a.getTitle())
                .status(a.getStatus()).statusLabel(s.getLabel())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .createdAt(format(a.getCreatedAt()))
                .updatedAt(format(a.getUpdatedAt()))
                .archiveType(a.getArchiveType())
                .courseCode(a.getCourseCode())
                .obtainedAt(formatDate(a.getObtainedAt()))
                .duplicateCheckStatus(a.getDuplicateCheckStatus())
                .duplicateInfo(a.getDuplicateInfo())
                .correctionReason(a.getCorrectionReason())
                .submitTime(format(a.getAuditInfo().getSubmittedAt()))
                .auditedAt(format(a.getAuditInfo().getAuditedAt()))
                .auditorId(a.getAuditInfo().getAuditorId())
                .rejectedReason(a.getAuditInfo().getRejectedReason())
                .returnedAt(format(a.getAuditInfo().getReturnedAt()))
                .passedAt(format(a.getAuditInfo().getPassedAt()))
                .revokedAt(format(a.getAuditInfo().getRevokedAt()))
                .currentVersion(a.getAuditInfo().getCurrentVersion())
                .submitCount(a.getAuditInfo().getSubmitCount())
                .evidenceFiles(queryFiles("archive", a.getId()))
                .build();
    }

    private ActivityDetailResponse toDetail(AwardApplication a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        return ActivityDetailResponse.builder()
                .id(a.getId()).type("award")
                .title(a.getTitle())
                .status(a.getStatus()).statusLabel(s.getLabel())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .createdAt(format(a.getCreatedAt()))
                .updatedAt(format(a.getUpdatedAt()))
                .awardType(a.getAwardType())
                .certificateNo(a.getCertificateNo())
                .issuingUnit(a.getIssuingUnit())
                .validUntil(formatDate(a.getValidUntil()))
                .participantRole(a.getParticipantRole())
                .submitTime(format(a.getAuditInfo().getSubmittedAt()))
                .auditedAt(format(a.getAuditInfo().getAuditedAt()))
                .auditorId(a.getAuditInfo().getAuditorId())
                .rejectedReason(a.getAuditInfo().getRejectedReason())
                .returnedAt(format(a.getAuditInfo().getReturnedAt()))
                .passedAt(format(a.getAuditInfo().getPassedAt()))
                .revokedAt(format(a.getAuditInfo().getRevokedAt()))
                .currentVersion(a.getAuditInfo().getCurrentVersion())
                .submitCount(a.getAuditInfo().getSubmitCount())
                .evidenceFiles(queryFiles("award", a.getId()))
                .build();
    }

    private ActivityDetailResponse toDetail(CareerPlan p) {
        ApplyStatusEnum s = ApplyStatusEnum.of(p.getStatus());
        return ActivityDetailResponse.builder()
                .id(p.getId()).type("career_plan")
                .title(p.getTitle())
                .status(p.getStatus()).statusLabel(s.getLabel())
                .semesterId(p.getSemesterId())
                .semesterName(lookupSemesterName(p.getSemesterId()))
                .createdAt(format(p.getCreatedAt()))
                .updatedAt(format(p.getUpdatedAt()))
                .content(p.getContent())
                .requirement(p.getRequirement())
                .progressRate(p.getProgressRate())
                .source(p.getSource())
                .aiSuggestionId(p.getAiSuggestionId())
                .requireConfirm(p.getRequireConfirm())
                .submitTime(format(p.getAuditInfo().getSubmittedAt()))
                .auditedAt(format(p.getAuditInfo().getAuditedAt()))
                .auditorId(p.getAuditInfo().getAuditorId())
                .rejectedReason(p.getAuditInfo().getRejectedReason())
                .returnedAt(format(p.getAuditInfo().getReturnedAt()))
                .passedAt(format(p.getAuditInfo().getPassedAt()))
                .revokedAt(format(p.getAuditInfo().getRevokedAt()))
                .currentVersion(p.getAuditInfo().getCurrentVersion())
                .submitCount(p.getAuditInfo().getSubmitCount())
                .evidenceFiles(queryFiles("career_plan", p.getId()))
                .build();
    }

    // ==================== 私有：编辑逻辑 ====================

    @Transactional
    private ActivityStatusResponse editArchive(Long activityId, ActivityEditBody body, Long userId) {
        Archive a = archiveRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
        checkOwnership(a.getUserId(), userId);
        checkEditable(a.getStatus());

        if (body.getTitle() != null) a.setTitle(body.getTitle());
        if (body.getSemesterId() != null) a.setSemesterId(body.getSemesterId());
        if (body.getCourseCode() != null) a.setCourseCode(body.getCourseCode());
        if (body.getObtainedAt() != null) a.setObtainedAt(body.getObtainedAt());
        if (body.getCorrectionReason() != null) a.setCorrectionReason(body.getCorrectionReason());

        a.setStatus(1); // 编辑后 → 待审批
        a.getAuditInfo().setSubmittedAt(LocalDateTime.now());
        a.getAuditInfo().setSubmitCount(a.getAuditInfo().getSubmitCount() + 1);
        a.getAuditInfo().setCurrentVersion(a.getAuditInfo().getCurrentVersion() + 1);
        archiveRepository.save(a);

        return ActivityStatusResponse.builder()
                .id(a.getId()).type("archive")
                .status(1).statusLabel("待审批")
                .currentVersion(a.getAuditInfo().getCurrentVersion())
                .submitCount(a.getAuditInfo().getSubmitCount()).build();
    }

    @Transactional
    private ActivityStatusResponse editAward(Long activityId, ActivityEditBody body, Long userId) {
        AwardApplication a = awardApplicationRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
        checkOwnership(a.getUserId(), userId);
        checkEditable(a.getStatus());

        if (body.getTitle() != null) a.setTitle(body.getTitle());
        if (body.getSemesterId() != null) a.setSemesterId(body.getSemesterId());
        if (body.getAwardType() != null) a.setAwardType(body.getAwardType());
        if (body.getCertificateNo() != null) a.setCertificateNo(body.getCertificateNo());
        if (body.getIssuingUnit() != null) a.setIssuingUnit(body.getIssuingUnit());
        if (body.getValidUntil() != null) a.setValidUntil(body.getValidUntil());
        if (body.getParticipantRole() != null) a.setParticipantRole(body.getParticipantRole());

        a.setStatus(1);
        a.getAuditInfo().setSubmittedAt(LocalDateTime.now());
        a.getAuditInfo().setSubmitCount(a.getAuditInfo().getSubmitCount() + 1);
        a.getAuditInfo().setCurrentVersion(a.getAuditInfo().getCurrentVersion() + 1);
        awardApplicationRepository.save(a);

        return ActivityStatusResponse.builder()
                .id(a.getId()).type("award")
                .status(1).statusLabel("待审批")
                .currentVersion(a.getAuditInfo().getCurrentVersion())
                .submitCount(a.getAuditInfo().getSubmitCount()).build();
    }

    @Transactional
    private ActivityStatusResponse editCareerPlan(Long activityId, ActivityEditBody body, Long userId) {
        CareerPlan p = careerPlanRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
        checkOwnership(p.getUserId(), userId);
        checkEditable(p.getStatus());

        if (body.getTitle() != null) p.setTitle(body.getTitle());
        if (body.getSemesterId() != null) p.setSemesterId(body.getSemesterId());
        if (body.getContent() != null) p.setContent(body.getContent());
        if (body.getRequirement() != null) p.setRequirement(body.getRequirement());
        if (body.getProgressRate() != null) p.setProgressRate(body.getProgressRate());

        p.setStatus(1);
        p.getAuditInfo().setSubmittedAt(LocalDateTime.now());
        p.getAuditInfo().setSubmitCount(p.getAuditInfo().getSubmitCount() + 1);
        p.getAuditInfo().setCurrentVersion(p.getAuditInfo().getCurrentVersion() + 1);
        careerPlanRepository.save(p);

        return ActivityStatusResponse.builder()
                .id(p.getId()).type("career_plan")
                .status(1).statusLabel("待审批")
                .currentVersion(p.getAuditInfo().getCurrentVersion())
                .submitCount(p.getAuditInfo().getSubmitCount()).build();
    }

    // ==================== 私有：权限与状态校验 ====================

    private void checkOwnership(Long entityUserId, Long currentUserId) {
        if (!entityUserId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该活动");
        }
    }

    private void checkEditable(Integer status) {
        if (status != 0 && status != 3) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅草稿或已退回状态的活动可编辑");
        }
    }

    private void checkDeletable(Integer status) {
        if (status != 0 && status != 3) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅草稿或已退回状态的活动可删除");
        }
    }

    private void checkWithdrawable(Integer status) {
        if (status != 1) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅待审批状态的活动可撤回");
        }
    }

    // ==================== 私有：工具方法 ====================

    private String lookupSemesterName(Long semesterId) {
        if (semesterId == null) return null;
        return semesterRepository.findById(semesterId)
                .map(s -> s.getName())
                .orElse(null);
    }

    private List<ActivityDetailResponse.FileItem> queryFiles(String bizType, Long bizId) {
        List<AttachmentRelation> files = attachmentRelationRepository.findByBizTypeAndBizId(bizType, bizId);
        if (files.isEmpty()) return null;
        return files.stream().map(f -> ActivityDetailResponse.FileItem.builder()
                .fileId(f.getId())
                .fileName(f.getOriginalName())
                .fileSize(f.getFileSize())
                .build()).toList();
    }

    private static String format(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    private static String formatDate(java.time.LocalDate d) {
        if (d == null) return null;
        return d.toString();
    }

    // ==================== 编辑请求体（内嵌 POJO） ====================

    /**
     * 编辑请求体：只包含可编辑字段，Controller 用 @RequestBody 绑定。
     * 服务端按 type 选择相关字段，null 字段不覆盖原值。
     */
    @lombok.Data
    public static class ActivityEditBody {
        private String title;
        private Long semesterId;
        // Archive 专用
        private String courseCode;
        private java.time.LocalDate obtainedAt;
        private String correctionReason;
        // Award 专用
        private String awardType;
        private String certificateNo;
        private String issuingUnit;
        private java.time.LocalDate validUntil;
        private String participantRole;
        // CareerPlan 专用
        private String content;
        private String requirement;
        private Integer progressRate;
    }
}
