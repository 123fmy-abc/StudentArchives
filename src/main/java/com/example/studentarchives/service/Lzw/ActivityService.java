package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Lzw.activity.request.ActivityListRequest;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityDetailResponse;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityListItemResponse;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityStatusResponse;
import com.example.studentarchives.entity.archive.*;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.version.ModelVersion;
import com.example.studentarchives.enums.ActivityTypeEnum;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.ArchiveTypeEnum;
import com.example.studentarchives.enums.AwardTypeEnum;
import com.example.studentarchives.enums.ModelVersionModelTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ModelVersionRepository modelVersionRepository;
    private final UserRepository userRepository;
    private final DictionaryRepository dictionaryRepository;

    // Archive extension repositories
    private final ArchiveCompetitionRepository competitionRepository;
    private final ArchiveScholarshipRepository scholarshipRepository;
    private final ArchiveInnovationRepository innovationRepository;
    private final ArchiveResearchRepository researchRepository;
    private final ArchiveCertificateRepository certificateRepository;
    private final ArchiveInternshipRepository internshipRepository;
    private final ArchiveOrganizationRepository organizationRepository;
    private final ArchiveTrainingProjectRepository trainingProjectRepository;
    private final ArchiveSocialPracticeRepository socialPracticeRepository;
    private final ArchiveBookReviewRepository bookReviewRepository;

    // Award extension repositories
    private final AwardCompetitionStarRepository competitionStarRepository;
    private final AwardInnovationStarRepository innovationStarRepository;
    private final AwardResearchStarRepository researchStarRepository;
    private final AwardResearchProjectRepository researchProjectRepository;
    private final AwardSoftwareCopyrightRepository softwareCopyrightRepository;
    private final AwardPublishedPaperRepository publishedPaperRepository;

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
                a.setStatus(ApplyStatusEnum.REVOKED.getValue());
                a.getAuditInfo().setRevokedAt(LocalDateTime.now());
                archiveRepository.save(a);
                yield ActivityStatusResponse.builder()
                        .id(a.getId()).type("archive")
                        .status(ApplyStatusEnum.REVOKED.getValue()).statusLabel("已撤销")
                        .currentVersion(a.getAuditInfo().getCurrentVersion())
                        .submitCount(a.getAuditInfo().getSubmitCount()).build();
            }
            case AWARD -> {
                AwardApplication a = awardApplicationRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
                checkOwnership(a.getUserId(), userId);
                checkWithdrawable(a.getStatus());
                a.setStatus(ApplyStatusEnum.REVOKED.getValue());
                a.getAuditInfo().setRevokedAt(LocalDateTime.now());
                awardApplicationRepository.save(a);
                yield ActivityStatusResponse.builder()
                        .id(a.getId()).type("award")
                        .status(ApplyStatusEnum.REVOKED.getValue()).statusLabel("已撤销")
                        .currentVersion(a.getAuditInfo().getCurrentVersion())
                        .submitCount(a.getAuditInfo().getSubmitCount()).build();
            }
            case CAREER_PLAN -> {
                CareerPlan p = careerPlanRepository.findById(activityId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
                checkOwnership(p.getUserId(), userId);
                checkWithdrawable(p.getStatus());
                p.setStatus(ApplyStatusEnum.REVOKED.getValue());
                p.getAuditInfo().setRevokedAt(LocalDateTime.now());
                careerPlanRepository.save(p);
                yield ActivityStatusResponse.builder()
                        .id(p.getId()).type("career_plan")
                        .status(ApplyStatusEnum.REVOKED.getValue()).statusLabel("已撤销")
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
        var ai = nullSafe(a.getAuditInfo());
        ArchiveTypeEnum at = ArchiveTypeEnum.of(a.getArchiveType());
        return ActivityListItemResponse.builder()
                .id(a.getId()).type("archive")
                .archiveType(a.getArchiveType())
                .archiveTypeLabel(at != null ? at.getLabel() : null)
                .title(a.getTitle())
                .content(buildContent("archive", a.getArchiveType()))
                .status(a.getStatus()).statusLabel(s.getLabel())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .submitTime(format(ai.getSubmittedAt()))
                .currentVersion(ai.getCurrentVersion())
                .submitCount(ai.getSubmitCount())
                .canEdit(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canDelete(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED || s == ApplyStatusEnum.REVOKED)
                .canWithdraw(s == ApplyStatusEnum.PENDING)
                .build();
    }

    private ActivityListItemResponse toListItem(AwardApplication a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        var ai = nullSafe(a.getAuditInfo());
        AwardTypeEnum at = AwardTypeEnum.of(a.getAwardType());

        // 科研之星按子表 primary_category 归一到前端子页签过滤别名，
        // 否则 research_star 无法匹配前端 research_project/software_copyright/published_paper 三个页签。
        String archiveType = a.getAwardType();
        String archiveTypeLabel = at != null ? at.getLabel() : null;
        if (at == AwardTypeEnum.RESEARCH_STAR) {
            String cat = researchStarRepository.findByApplicationId(a.getId())
                    .map(r -> r.getPrimaryCategory()).orElse(null);
            archiveType = researchStarArchiveType(cat);
            archiveTypeLabel = researchStarLabel(cat);
        }

        return ActivityListItemResponse.builder()
                .id(a.getId()).type("award")
                .archiveType(archiveType)
                .archiveTypeLabel(archiveTypeLabel)
                .title(a.getTitle())
                .content("奖项报名已提交")
                .status(a.getStatus()).statusLabel(s.getLabel())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .submitTime(format(ai.getSubmittedAt()))
                .currentVersion(ai.getCurrentVersion())
                .submitCount(ai.getSubmitCount())
                .canEdit(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canDelete(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED || s == ApplyStatusEnum.REVOKED)
                .canWithdraw(s == ApplyStatusEnum.PENDING)
                .build();
    }

    private ActivityListItemResponse toListItem(CareerPlan p) {
        ApplyStatusEnum s = ApplyStatusEnum.of(p.getStatus());
        var ai = nullSafe(p.getAuditInfo());
        return ActivityListItemResponse.builder()
                .id(p.getId()).type("career_plan")
                .title(p.getTitle())
                .content("职业规划已提交")
                .status(p.getStatus()).statusLabel(s.getLabel())
                .semesterId(p.getSemesterId())
                .semesterName(lookupSemesterName(p.getSemesterId()))
                .submitTime(format(ai.getSubmittedAt()))
                .currentVersion(ai.getCurrentVersion())
                .submitCount(ai.getSubmitCount())
                .canEdit(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED)
                .canDelete(s == ApplyStatusEnum.DRAFT || s == ApplyStatusEnum.REJECTED || s == ApplyStatusEnum.REVOKED)
                .canWithdraw(s == ApplyStatusEnum.PENDING)
                .build();
    }

    /** 科研之星 primary_category → 前端子页签过滤别名（archiveType） */
    private static String researchStarArchiveType(String primaryCategory) {
        return switch (primaryCategory == null ? "" : primaryCategory) {
            case "project" -> "research_project";
            case "software_copyright" -> "software_copyright";
            case "published_paper" -> "published_paper";
            default -> AwardTypeEnum.RESEARCH_STAR.getValue();
        };
    }

    /** 科研之星 primary_category → 显示标签 */
    private static String researchStarLabel(String primaryCategory) {
        return switch (primaryCategory == null ? "" : primaryCategory) {
            case "project" -> "科研项目";
            case "software_copyright" -> "软件著作权";
            case "published_paper" -> "发表论文";
            default -> AwardTypeEnum.RESEARCH_STAR.getLabel();
        };
    }

    // ==================== 私有：实体 → 详情 DTO ====================

    private ActivityDetailResponse toDetail(Archive a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        ArchiveTypeEnum at = ArchiveTypeEnum.of(a.getArchiveType());
        return ActivityDetailResponse.builder()
                .id(a.getId()).type("archive")
                .archiveType(a.getArchiveType())
                .archiveTypeLabel(at != null ? at.getLabel() : null)
                .title(a.getTitle())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .status(a.getStatus()).statusLabel(s.getLabel())
                .submitTime(format(a.getAuditInfo() != null ? a.getAuditInfo().getSubmittedAt() : null))
                .auditedAt(format(a.getAuditInfo() != null ? a.getAuditInfo().getAuditedAt() : null))
                .auditorId(a.getAuditInfo() != null ? a.getAuditInfo().getAuditorId() : null)
                .auditorName(resolveAuditorName(a.getAuditInfo()))
                .rejectedReason(a.getAuditInfo() != null ? a.getAuditInfo().getRejectedReason() : null)
                .currentVersion(a.getAuditInfo() != null ? a.getAuditInfo().getCurrentVersion() : null)
                .submitCount(a.getAuditInfo() != null ? a.getAuditInfo().getSubmitCount() : null)
                .detail(buildArchiveDetail(a))
                .evidenceFiles(queryFiles("archive", a.getId()))
                .approvalHistory(Collections.emptyList())
                .versionHistory(queryVersionHistory(ModelVersionModelTypeEnum.ARCHIVE.getValue(), a.getId()))
                .build();
    }

    private ActivityDetailResponse toDetail(AwardApplication a) {
        ApplyStatusEnum s = ApplyStatusEnum.of(a.getStatus());
        AwardTypeEnum at = AwardTypeEnum.of(a.getAwardType());
        return ActivityDetailResponse.builder()
                .id(a.getId()).type("award")
                .archiveType(a.getAwardType())
                .archiveTypeLabel(at != null ? at.getLabel() : null)
                .title(a.getTitle())
                .semesterId(a.getSemesterId())
                .semesterName(lookupSemesterName(a.getSemesterId()))
                .status(a.getStatus()).statusLabel(s.getLabel())
                .submitTime(format(a.getAuditInfo() != null ? a.getAuditInfo().getSubmittedAt() : null))
                .auditedAt(format(a.getAuditInfo() != null ? a.getAuditInfo().getAuditedAt() : null))
                .auditorId(a.getAuditInfo() != null ? a.getAuditInfo().getAuditorId() : null)
                .auditorName(resolveAuditorName(a.getAuditInfo()))
                .rejectedReason(a.getAuditInfo() != null ? a.getAuditInfo().getRejectedReason() : null)
                .currentVersion(a.getAuditInfo() != null ? a.getAuditInfo().getCurrentVersion() : null)
                .submitCount(a.getAuditInfo() != null ? a.getAuditInfo().getSubmitCount() : null)
                .detail(buildAwardDetail(a))
                .evidenceFiles(queryFiles("award", a.getId()))
                .approvalHistory(Collections.emptyList())
                .versionHistory(queryVersionHistory(ModelVersionModelTypeEnum.AWARD_APPLICATION.getValue(), a.getId()))
                .build();
    }

    private ActivityDetailResponse toDetail(CareerPlan p) {
        ApplyStatusEnum s = ApplyStatusEnum.of(p.getStatus());
        return ActivityDetailResponse.builder()
                .id(p.getId()).type("career_plan")
                .title(p.getTitle())
                .semesterId(p.getSemesterId())
                .semesterName(lookupSemesterName(p.getSemesterId()))
                .status(p.getStatus()).statusLabel(s.getLabel())
                .submitTime(format(p.getAuditInfo() != null ? p.getAuditInfo().getSubmittedAt() : null))
                .auditedAt(format(p.getAuditInfo() != null ? p.getAuditInfo().getAuditedAt() : null))
                .auditorId(p.getAuditInfo() != null ? p.getAuditInfo().getAuditorId() : null)
                .auditorName(resolveAuditorName(p.getAuditInfo()))
                .rejectedReason(p.getAuditInfo() != null ? p.getAuditInfo().getRejectedReason() : null)
                .currentVersion(p.getAuditInfo() != null ? p.getAuditInfo().getCurrentVersion() : null)
                .submitCount(p.getAuditInfo() != null ? p.getAuditInfo().getSubmitCount() : null)
                .detail(buildCareerPlanDetail(p))
                .evidenceFiles(queryFiles("career_plan", p.getId()))
                .approvalHistory(Collections.emptyList())
                .versionHistory(queryVersionHistory(ModelVersionModelTypeEnum.CAREER_PLAN.getValue(), p.getId()))
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
        if (status != 0 && status != 3 && status != ApplyStatusEnum.REVOKED.getValue()) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅草稿、已退回或已撤销状态的活动可删除");
        }
    }

    private void checkWithdrawable(Integer status) {
        if (status != 1) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅待审批状态的活动可撤回");
        }
    }

    // ==================== 私有：工具方法 ====================

    /** 空安全获取 auditInfo，防止 Hibernate 将 @Embedded 置 null 导致 NPE */
    private static ArchiveAuditInfo nullSafe(ArchiveAuditInfo ai) {
        return ai != null ? ai : new ArchiveAuditInfo();
    }

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

    // ==================== 私有：详情构建 ====================

    /** 根据档案类型构建 detail 对象（查扩展表 + 字典标签） */
    private Map<String, Object> buildArchiveDetail(Archive a) {
        Map<String, Object> detail = new LinkedHashMap<>();
        String type = a.getArchiveType();
        if (type == null) return detail;

        // 通用字段
        if (a.getObtainedAt() != null) detail.put("obtainTime", formatDate(a.getObtainedAt()));
        if (a.getCourseCode() != null) detail.put("courseCode", a.getCourseCode());

        // 查扩展表
        switch (type) {
            case "academic_competition" -> competitionRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("competitionName", ext.getCompetitionName());
                detail.put("competitionType", ext.getCompetitionType());
                detail.put("competitionTypeLabel", resolveLabel("competition_type", ext.getCompetitionType()));
                detail.put("awardLevel", ext.getAwardLevel());
                detail.put("awardLevelLabel", resolveLabel("award_level", ext.getAwardLevel()));
                detail.put("participantRole", ext.getParticipantRole());
            });
            case "scholarship" -> scholarshipRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("scholarshipName", ext.getScholarshipName());
                detail.put("scholarshipCategory", ext.getScholarshipCategory());
                detail.put("scholarshipCategoryLabel", resolveLabel("scholarship_category", ext.getScholarshipCategory()));
                detail.put("awardLevel", ext.getAwardLevel());
                detail.put("awardLevelLabel", resolveLabel("award_level", ext.getAwardLevel()));
            });
            case "innovation_entrepreneurship" -> innovationRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("companyName", ext.getCompanyName());
                detail.put("industryType", ext.getIndustryType());
                detail.put("industryTypeLabel", resolveLabel("industry_type", ext.getIndustryType()));
                detail.put("projectType", ext.getProjectType());
                detail.put("projectTypeLabel", resolveLabel("project_type", ext.getProjectType()));
                detail.put("participantRole", ext.getParticipantRole());
                if (ext.getRegisteredAt() != null) detail.put("registeredAt", formatDate(ext.getRegisteredAt()));
            });
            case "academic_research" -> researchRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("projectName", ext.getProjectName());
                detail.put("projectLevel", ext.getProjectLevel());
                detail.put("projectLevelLabel", resolveLabel("project_level", ext.getProjectLevel()));
                detail.put("projectType", ext.getProjectType());
                detail.put("projectTypeLabel", resolveLabel("project_type", ext.getProjectType()));
                detail.put("participantRole", ext.getParticipantRole());
                if (ext.getStartDate() != null) detail.put("startDate", formatDate(ext.getStartDate()));
                if (ext.getEndDate() != null) detail.put("endDate", formatDate(ext.getEndDate()));
            });
            case "honor_certificate" -> certificateRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("certificateType", ext.getCertificateType());
                detail.put("certificateTypeLabel", resolveLabel("certificate_type", ext.getCertificateType()));
                detail.put("certificateName", ext.getCertificateName());
                detail.put("certificateNo", ext.getCertificateNo());
                detail.put("issuingUnit", ext.getIssuingUnit());
                if (ext.getValidUntil() != null) detail.put("validUntil", formatDate(ext.getValidUntil()));
            });
            case "internship" -> internshipRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("companyName", ext.getCompanyName());
                detail.put("location", ext.getLocation());
                detail.put("position", ext.getPosition());
                if (ext.getStartDate() != null) detail.put("startDate", formatDate(ext.getStartDate()));
                if (ext.getEndDate() != null) detail.put("endDate", formatDate(ext.getEndDate()));
            });
            case "organization" -> organizationRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("orgLevel", ext.getOrgLevel());
                detail.put("orgLevelLabel", resolveLabel("org_level", ext.getOrgLevel()));
                detail.put("department", ext.getDepartment());
                detail.put("positionTitle", ext.getPositionTitle());
                if (ext.getStartDate() != null) detail.put("startDate", formatDate(ext.getStartDate()));
                if (ext.getEndDate() != null) detail.put("endDate", formatDate(ext.getEndDate()));
            });
            case "training_project" -> trainingProjectRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("projectName", ext.getProjectName());
                detail.put("projectContent", ext.getProjectContent());
                if (ext.getStartDate() != null) detail.put("startDate", formatDate(ext.getStartDate()));
                if (ext.getEndDate() != null) detail.put("endDate", formatDate(ext.getEndDate()));
            });
            case "social_practice" -> socialPracticeRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("activityName", ext.getActivityName());
                detail.put("practiceLocation", ext.getPracticeLocation());
                detail.put("practiceUnit", ext.getPracticeUnit());
                detail.put("participantRole", ext.getParticipantRole());
                if (ext.getStartDate() != null) detail.put("startDate", formatDate(ext.getStartDate()));
                if (ext.getEndDate() != null) detail.put("endDate", formatDate(ext.getEndDate()));
                if (ext.getVolunteerHours() != null) detail.put("volunteerHours", ext.getVolunteerHours());
            });
            case "book_review" -> bookReviewRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("bookName", ext.getBookName());
                if (ext.getReadMonth() != null) detail.put("readMonth", formatDate(ext.getReadMonth()));
                detail.put("reviewContent", ext.getReviewContent());
            });
        }
        return detail;
    }

    private Map<String, Object> buildAwardDetail(AwardApplication a) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("awardType", a.getAwardType());
        AwardTypeEnum at = AwardTypeEnum.of(a.getAwardType());
        if (at != null) detail.put("awardTypeLabel", at.getLabel());

        // 通用字段（证书编号、颁发单位、有效期、角色）
        if (a.getCertificateNo() != null) detail.put("certificateNo", a.getCertificateNo());
        if (a.getIssuingUnit() != null) detail.put("issuingUnit", a.getIssuingUnit());
        if (a.getValidUntil() != null) detail.put("validUntil", formatDate(a.getValidUntil()));
        if (a.getParticipantRole() != null) detail.put("participantRole", a.getParticipantRole());

        // 各星类专属子表字段回填（与提交/编辑契约 body key 一致，round-trip）
        if (at != null) {
            switch (at) {
                case COMPETITION_STAR -> competitionStarRepository.findByApplicationId(a.getId()).ifPresent(ext -> {
                    if (ext.getCompetitionName() != null) detail.put("competitionName", ext.getCompetitionName());
                    if (ext.getParticipatedAt() != null) detail.put("participatedTime", formatDate(ext.getParticipatedAt()));
                    if (ext.getCompetitionLevel() != null) detail.put("competitionLevel", ext.getCompetitionLevel());
                    if (ext.getAwardLevel() != null) detail.put("awardLevel", ext.getAwardLevel());
                });
                case INNOVATION_STAR -> innovationStarRepository.findByApplicationId(a.getId()).ifPresent(ext -> {
                    if (ext.getCompanyName() != null) detail.put("companyName", ext.getCompanyName());
                    if (ext.getIndustryType() != null) detail.put("industryType", ext.getIndustryType());
                    if (ext.getApplicantRank() != null) detail.put("applicantRank", ext.getApplicantRank());
                    if (ext.getRegisteredAt() != null) detail.put("registeredTime", formatDate(ext.getRegisteredAt()));
                });
                case RESEARCH_STAR -> researchStarRepository.findByApplicationId(a.getId()).ifPresent(rs -> {
                    String cat = rs.getPrimaryCategory();
                    if ("project".equals(cat)) {
                        researchProjectRepository.findByResearchStarId(rs.getId()).stream().findFirst().ifPresent(p -> {
                            if (p.getProjectName() != null) detail.put("projectName", p.getProjectName());
                            if (p.getProjectLevel() != null) detail.put("projectLevel", p.getProjectLevel());
                            if (p.getRankTotal() != null) detail.put("rankTotal", p.getRankTotal());
                            if (p.getEstablishedAt() != null) detail.put("establishedTime", formatDate(p.getEstablishedAt()));
                        });
                    } else if ("software_copyright".equals(cat)) {
                        softwareCopyrightRepository.findByResearchStarId(rs.getId()).stream().findFirst().ifPresent(sw -> {
                            if (sw.getSoftwareName() != null) detail.put("softwareName", sw.getSoftwareName());
                            if (sw.getRankTotal() != null) detail.put("rankTotal", sw.getRankTotal());
                            if (sw.getApprovedAt() != null) detail.put("approvedTime", formatDate(sw.getApprovedAt()));
                        });
                    } else if ("published_paper".equals(cat)) {
                        publishedPaperRepository.findByResearchStarId(rs.getId()).stream().findFirst().ifPresent(pp -> {
                            if (pp.getJournalName() != null) detail.put("journalName", pp.getJournalName());
                            if (pp.getPaperTitle() != null) detail.put("paperTitle", pp.getPaperTitle());
                            if (pp.getRankTotal() != null) detail.put("rankTotal", pp.getRankTotal());
                            if (pp.getPublishedAt() != null) detail.put("publishedTime", formatDate(pp.getPublishedAt()));
                        });
                    }
                });
            }
        }
        return detail;
    }

    private Map<String, Object> buildCareerPlanDetail(CareerPlan p) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (p.getContent() != null) detail.put("content", p.getContent());
        if (p.getRequirement() != null) detail.put("requirement", p.getRequirement());
        if (p.getProgressRate() != null) detail.put("progressRate", p.getProgressRate());
        if (p.getSource() != null) detail.put("source", p.getSource());
        if (p.getAiSuggestionId() != null) detail.put("aiSuggestionId", p.getAiSuggestionId());
        if (p.getRequireConfirm() != null) detail.put("requireConfirm", p.getRequireConfirm());
        return detail;
    }

    /** 查审核人姓名 */
    private String resolveAuditorName(ArchiveAuditInfo audit) {
        if (audit == null || audit.getAuditorId() == null) return null;
        return userRepository.findById(audit.getAuditorId())
                .map(u -> u.getName())
                .orElse(null);
    }

    /** 查版本历史 */
    private List<ActivityDetailResponse.VersionHistoryItem> queryVersionHistory(String modelType, Long modelId) {
        List<ModelVersion> versions = modelVersionRepository
                .findByModelTypeAndModelIdOrderByVersionAsc(modelType, modelId);
        if (versions.isEmpty()) return Collections.emptyList();
        return versions.stream()
                .map(v -> ActivityDetailResponse.VersionHistoryItem.builder()
                        .version(v.getVersion())
                        .title(v.getTitle())
                        .status(v.getStatus())
                        .statusLabel(v.getStatus() != null ? ApplyStatusEnum.of(v.getStatus()).getLabel() : null)
                        .createdAt(format(v.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    /** 字典标签解析（dict_type → dict_code → dict_name） */
    private String resolveLabel(String dictType, String dictCode) {
        if (dictType == null || dictCode == null) return null;
        return dictionaryRepository.findActiveByDictType(dictType).stream()
                .filter(d -> dictCode.equals(d.getDictCode()))
                .findFirst()
                .map(Dictionary::getDictName)
                .orElse(null);
    }

    /** 列表 content 生成 */
    private String buildContent(String type, String archiveType) {
        if ("archive".equals(type)) {
            ArchiveTypeEnum at = ArchiveTypeEnum.of(archiveType);
            return (at != null ? at.getLabel() : "档案") + "申报已提交";
        }
        if ("award".equals(type)) return "奖项报名已提交";
        if ("career_plan".equals(type)) return "职业规划已提交";
        return null;
    }

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static String format(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.atZone(java.time.ZoneId.of("Asia/Shanghai")).format(DTF);
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
