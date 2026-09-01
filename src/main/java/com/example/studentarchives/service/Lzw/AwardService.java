package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.award.*;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.AwardTypeConfig;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.version.ModelVersion;
import com.example.studentarchives.enums.*;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 奖项报名服务
 * <p>
 * 支持 3 种奖项类型：竞赛之星、科研之星、双创之星。
 * 科研之星采用"主记录 + 子项目（科研项目/软件著作权/发表论文）"模式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardService {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AwardApplicationRepository awardApplicationRepository;
    private final AwardCompetitionStarRepository competitionStarRepository;
    private final AwardResearchStarRepository researchStarRepository;
    private final AwardResearchProjectRepository researchProjectRepository;
    private final AwardSoftwareCopyrightRepository softwareCopyrightRepository;
    private final AwardPublishedPaperRepository publishedPaperRepository;
    private final AwardInnovationStarRepository innovationStarRepository;
    private final UserRepository userRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final AwardTypeConfigRepository awardTypeConfigRepository;
    private final ObjectMapper objectMapper;
    private final ApprovalSubmitService approvalSubmitService;

    // ==================== 8.1 奖项总览统计 ====================

    @Transactional(readOnly = true)
    public AwardOverviewResponse getOverview(Long userId) {
        List<AwardApplication> all = awardApplicationRepository.findByUserId(userId);

        long total = all.size();
        long pending = all.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
        long approved = all.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count();

        Long latestSemesterId = all.stream()
                .map(AwardApplication::getSemesterId)
                .filter(Objects::nonNull)
                .max(Long::compareTo).orElse(null);
        long newThisSemester = latestSemesterId != null
                ? all.stream().filter(a -> latestSemesterId.equals(a.getSemesterId())).count()
                : 0;

        Map<String, Long> typeDist = all.stream()
                .collect(Collectors.groupingBy(a -> a.getAwardType() != null ? a.getAwardType() : "unknown", Collectors.counting()));

        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("draft", all.stream().filter(a -> a.getStatus() != null && a.getStatus() == 0).count());
        statusDist.put("pending", all.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count());
        statusDist.put("approved", all.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count());
        statusDist.put("rejected", all.stream().filter(a -> a.getStatus() != null && a.getStatus() == 3).count());

        Map<Long, Long> semesterCounts = all.stream()
                .filter(a -> a.getSemesterId() != null)
                .collect(Collectors.groupingBy(AwardApplication::getSemesterId, Collectors.counting()));
        List<SemesterTrendItem> semesterTrend = semesterCounts.entrySet().stream()
                .map(e -> SemesterTrendItem.builder()
                        .semesterId(e.getKey())
                        .semesterName("学期" + e.getKey())
                        .count(e.getValue().intValue())
                        .build())
                .collect(Collectors.toList());

        List<RecentActivityItem> recentActivities = all.stream()
                .sorted(Comparator.comparing(a -> {
                    ArchiveAuditInfo ai = a.getAuditInfo();
                    return ai != null && ai.getSubmittedAt() != null ? ai.getSubmittedAt() : LocalDateTime.MIN;
                }, Comparator.reverseOrder()))
                .limit(5)
                .map(a -> {
                    User u = userRepository.findById(a.getUserId()).orElse(null);
                    AwardTypeEnum at = AwardTypeEnum.of(a.getAwardType());
                    return RecentActivityItem.builder()
                            .id(a.getId())
                            .title(a.getTitle())
                            .applicant(u != null ? u.getName() : null)
                            .type(a.getAwardType())
                            .typeLabel(at != null ? at.getLabel() : null)
                            .submitTime(a.getAuditInfo() != null && a.getAuditInfo().getSubmittedAt() != null
                                    ? a.getAuditInfo().getSubmittedAt().format(DATE_FMT) : null)
                            .status(a.getStatus())
                            .statusLabel(ApplyStatusEnum.of(a.getStatus()).getLabel())
                            .build();
                })
                .collect(Collectors.toList());

        return AwardOverviewResponse.builder()
                .totalSubmissions((int) total)
                .pendingReview((int) pending)
                .approved((int) approved)
                .newThisSemester((int) newThisSemester)
                .typeDistribution(typeDist)
                .statusDistribution(statusDist)
                .semesterTrend(semesterTrend)
                .recentActivities(recentActivities)
                .build();
    }

    // ==================== 8.1.1 奖项草稿自动保存 ====================

    @Transactional
    public AwardAutosaveResponse autosave(Long applicationId, Map<String, Object> body, Long userId) {
        AwardApplication app = awardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项报名不存在"));
        checkOwnership(app, userId);
        checkEditable(app);

        LocalDateTime now = LocalDateTime.now();

        if (body.containsKey("title")) app.setTitle((String) body.get("title"));
        if (body.containsKey("semesterId")) app.setSemesterId(toLong(body.get("semesterId")));
        if (body.containsKey("certificateNo")) app.setCertificateNo((String) body.get("certificateNo"));
        if (body.containsKey("issuingUnit")) app.setIssuingUnit((String) body.get("issuingUnit"));
        if (body.containsKey("validUntil")) app.setValidUntil(parseDate((String) body.get("validUntil")));
        if (body.containsKey("participantRole")) app.setParticipantRole((String) body.get("participantRole"));

        ArchiveAuditInfo audit = app.getAuditInfo();
        if (audit == null) { audit = new ArchiveAuditInfo(); app.setAuditInfo(audit); }
        audit.setDraftSavedAt(now);

        updateExtension(app.getAwardType(), applicationId, body);

        boolean submit = body.containsKey("isDraft") && toInt(body.get("isDraft")) == 0;
        if (submit) {
            app.setStatus(ApplyStatusEnum.PENDING.getValue());
            audit.setSubmittedAt(LocalDateTime.now());
            audit.setCurrentVersion(audit.getCurrentVersion() != null ? audit.getCurrentVersion() + 1 : 1);
            audit.setSubmitCount(audit.getSubmitCount() != null ? audit.getSubmitCount() + 1 : 1);
            app = awardApplicationRepository.save(app);
            generatePendingApprovalIfSubmitted(app);
            return AwardAutosaveResponse.builder()
                    .applicationId(app.getId()).status(app.getStatus())
                    .statusLabel(ApplyStatusEnum.of(app.getStatus()).getLabel())
                    .currentVersion(audit.getCurrentVersion())
                    .submitCount(audit.getSubmitCount())
                    .build();
        }

        app = awardApplicationRepository.save(app);
        return AwardAutosaveResponse.builder()
                .applicationId(app.getId()).status(app.getStatus())
                .statusLabel(ApplyStatusEnum.of(app.getStatus()).getLabel())
                .savedAt(toIso(now))
                .build();
    }

    // ==================== 8.1.2 奖项重复申报检测 ====================

    @Transactional(readOnly = true)
    public AwardDuplicateCheckResponse duplicateCheck(AwardDuplicateCheckRequest req, Long userId) {
        List<AwardApplication> all = awardApplicationRepository.findByUserId(userId);
        List<AwardApplication> sameType = all.stream()
                .filter(a -> req.getAwardType().equals(a.getAwardType()))
                .toList();

        List<DuplicateRecord> records = new ArrayList<>();
        for (AwardApplication a : sameType) {
            if (a.getStatus() == null || a.getStatus() != ApplyStatusEnum.APPROVED.getValue()) continue;
            double similarity = 0.0;
            if (req.getCertificateNo() != null && req.getCertificateNo().equals(a.getCertificateNo())) {
                similarity = 1.0;
            } else if (req.getTitle() != null && a.getTitle() != null && a.getTitle().contains(req.getTitle())) {
                similarity = 0.7;
            }
            if (similarity > 0.5) {
                records.add(DuplicateRecord.builder()
                        .applicationId(a.getId()).title(a.getTitle())
                        .status(a.getStatus()).statusLabel(ApplyStatusEnum.of(a.getStatus()).getLabel())
                        .similarity(similarity).build());
            }
        }

        boolean isDuplicate = !records.isEmpty();
        return AwardDuplicateCheckResponse.builder()
                .isDuplicate(isDuplicate).duplicateRecords(records)
                .suggestion(isDuplicate ? "检测到疑似重复报名，请确认是否为同一成果。" : "未检测到重复报名")
                .build();
    }

    // ==================== 8.1.3 获取奖项评选说明 ====================

    @Transactional(readOnly = true)
    public AwardGuideResponse getGuide(String type) {
        AwardTypeEnum typeEnum = AwardTypeEnum.of(type);
        if (typeEnum == null) throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的奖项类型：" + type);

        AwardTypeConfig config = awardTypeConfigRepository
                .findByAwardTypeAndStatus(type, 1)
                .orElse(null);

        List<AwardGuideResponse.Requirement> requirements = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        if (config != null) {
            if (config.getEvaluateRequirements() != null) {
                try {
                    requirements = objectMapper.readValue(config.getEvaluateRequirements(),
                            new TypeReference<List<AwardGuideResponse.Requirement>>() {});
                } catch (Exception ignored) {}
            }
            if (config.getEvaluateNotes() != null) {
                try {
                    notes = objectMapper.readValue(config.getEvaluateNotes(),
                            new TypeReference<List<String>>() {});
                } catch (Exception ignored) {}
            }
        }

        return AwardGuideResponse.builder()
                .type(type).typeLabel(typeEnum.getLabel())
                .title(typeEnum.getLabel() + "评选说明")
                .content(config != null ? config.getEvaluateDesc() : null)
                .requirements(requirements).notes(notes)
                .updatedAt(config != null && config.getUpdatedAt() != null ? toIso(config.getUpdatedAt()) : null)
                .build();
    }

    // ==================== 8.2 竞赛之星报名 ====================

    @Transactional
    public AwardSubmitResponse submitCompetitionStar(CompetitionStarRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        String title = "竞赛之星-" + req.getCompetitionName();
        AwardApplication app = createAwardBase(schoolId, userId, AwardTypeEnum.COMPETITION_STAR.getValue(),
                title, req.getSemesterId(), draft, now);
        app.setCertificateNo(req.getCertificateNo());
        app.setIssuingUnit(req.getIssuingUnit());
        app.setValidUntil(req.getValidUntil());
        app.setParticipantRole(req.getParticipantRole());
        app = awardApplicationRepository.save(app);

        AwardCompetitionStar ext = new AwardCompetitionStar();
        ext.setApplicationId(app.getId());
        ext.setCompetitionName(req.getCompetitionName());
        ext.setParticipatedAt(req.getParticipatedTime());
        ext.setCompetitionLevel(req.getCompetitionLevel());
        ext.setAwardLevel(req.getAwardLevel());
        competitionStarRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, app.getId());
        writeAwardVersion(app, userId);
        generatePendingApprovalIfSubmitted(app);

        return buildSubmitResponse(app);
    }

    // ==================== 8.3 科研之星 ====================

    @Transactional
    public ResearchStarCreateResponse createResearchStar(ResearchStarCreateRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = true;
        LocalDateTime now = LocalDateTime.now();

        AwardApplication app = createAwardBase(schoolId, userId, AwardTypeEnum.RESEARCH_STAR.getValue(),
                "科研之星", req.getSemesterId(), draft, now);
        app.setCertificateNo(req.getCertificateNo());
        app.setIssuingUnit(req.getIssuingUnit());
        app.setValidUntil(req.getValidUntil());
        app.setParticipantRole(req.getParticipantRole());
        app = awardApplicationRepository.save(app);

        AwardResearchStar rs = new AwardResearchStar();
        rs.setApplicationId(app.getId());
        rs.setPrimaryCategory(null);
        rs = researchStarRepository.save(rs);

        return ResearchStarCreateResponse.builder()
                .applicationId(app.getId()).researchStarId(rs.getId())
                .status(app.getStatus()).statusLabel(ApplyStatusEnum.of(app.getStatus()).getLabel())
                .build();
    }

    @Transactional
    public AwardSubmitResponse addProject(Long researchStarId, ResearchProjectRequest req, Long userId) {
        AwardResearchStar rs = loadResearchStar(researchStarId, userId);
        AwardApplication app = loadApplication(rs.getApplicationId(), userId);

        AwardResearchProject project = new AwardResearchProject();
        project.setResearchStarId(rs.getId());
        project.setProjectName(req.getProjectName());
        project.setProjectLevel(req.getProjectLevel());
        project.setRankTotal(req.getRankTotal());
        project.setEstablishedAt(req.getEstablishedTime());
        researchProjectRepository.save(project);

        if (rs.getPrimaryCategory() == null || rs.getPrimaryCategory().isEmpty()) {
            rs.setPrimaryCategory("project");
            researchStarRepository.save(rs);
        }

        bindFiles(req.getEvidenceFileIds(), userId, app.getId());
        return buildSubmitResponse(app);
    }

    @Transactional
    public AwardSubmitResponse addSoftware(Long researchStarId, SoftwareCopyrightRequest req, Long userId) {
        AwardResearchStar rs = loadResearchStar(researchStarId, userId);
        AwardApplication app = loadApplication(rs.getApplicationId(), userId);

        AwardSoftwareCopyright sw = new AwardSoftwareCopyright();
        sw.setResearchStarId(rs.getId());
        sw.setSoftwareName(req.getSoftwareName());
        sw.setRankTotal(req.getRankTotal());
        sw.setApprovedAt(req.getApprovedTime());
        softwareCopyrightRepository.save(sw);

        if (rs.getPrimaryCategory() == null || rs.getPrimaryCategory().isEmpty()) {
            rs.setPrimaryCategory("software_copyright");
            researchStarRepository.save(rs);
        }

        bindFiles(req.getEvidenceFileIds(), userId, app.getId());
        return buildSubmitResponse(app);
    }

    @Transactional
    public AwardSubmitResponse addPaper(Long researchStarId, PublishedPaperRequest req, Long userId) {
        AwardResearchStar rs = loadResearchStar(researchStarId, userId);
        AwardApplication app = loadApplication(rs.getApplicationId(), userId);

        AwardPublishedPaper paper = new AwardPublishedPaper();
        paper.setResearchStarId(rs.getId());
        paper.setJournalName(req.getJournalName());
        paper.setPaperTitle(req.getPaperTitle());
        paper.setRankTotal(req.getRankTotal());
        paper.setPublishedAt(req.getPublishedTime());
        publishedPaperRepository.save(paper);

        if (rs.getPrimaryCategory() == null || rs.getPrimaryCategory().isEmpty()) {
            rs.setPrimaryCategory("published_paper");
            researchStarRepository.save(rs);
        }

        bindFiles(req.getEvidenceFileIds(), userId, app.getId());
        return buildSubmitResponse(app);
    }

    @Transactional
    public AwardSubmitResponse submitResearchStar(Long researchStarId, Long userId) {
        AwardResearchStar rs = loadResearchStar(researchStarId, userId);
        AwardApplication app = loadApplication(rs.getApplicationId(), userId);

        if (rs.getPrimaryCategory() == null || rs.getPrimaryCategory().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少添加一条子项目后再提交");
        }

        String catLabel = switch (rs.getPrimaryCategory()) {
            case "project" -> "科研项目";
            case "software_copyright" -> "软件著作权";
            case "published_paper" -> "发表论文";
            default -> "科研之星";
        };
        // 追加学生在子表填写的名称，便于区分同类多条申报
        String subName = researchSubName(rs);
        app.setTitle((subName != null && !subName.isBlank())
                ? "科研之星-" + catLabel + "-" + subName
                : "科研之星-" + catLabel);

        ArchiveAuditInfo audit = app.getAuditInfo();
        if (audit == null) { audit = new ArchiveAuditInfo(); app.setAuditInfo(audit); }
        audit.setSubmittedAt(LocalDateTime.now());
        audit.setCurrentVersion(audit.getCurrentVersion() != null ? audit.getCurrentVersion() + 1 : 1);
        audit.setSubmitCount(audit.getSubmitCount() != null ? audit.getSubmitCount() + 1 : 1);
        app.setStatus(ApplyStatusEnum.PENDING.getValue());
        app = awardApplicationRepository.save(app);
        generatePendingApprovalIfSubmitted(app);

        writeAwardVersion(app, userId);
        return buildSubmitResponse(app);
    }

    /** 科研之星子表名称：按 primary_category 取对应子记录的首条名称 */
    private String researchSubName(AwardResearchStar rs) {
        String cat = rs.getPrimaryCategory();
        if (cat == null) {
            return null;
        }
        return switch (cat) {
            case "project" -> researchProjectRepository.findByResearchStarId(rs.getId()).stream()
                    .findFirst().map(p -> p.getProjectName()).orElse(null);
            case "software_copyright" -> softwareCopyrightRepository.findByResearchStarId(rs.getId()).stream()
                    .findFirst().map(p -> p.getSoftwareName()).orElse(null);
            case "published_paper" -> publishedPaperRepository.findByResearchStarId(rs.getId()).stream()
                    .findFirst().map(p -> p.getPaperTitle()).orElse(null);
            default -> null;
        };
    }

    // ==================== 8.4 双创之星报名 ====================

    @Transactional
    public AwardSubmitResponse submitInnovationStar(InnovationStarRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        String title = "双创之星-" + req.getCompanyName();
        AwardApplication app = createAwardBase(schoolId, userId, AwardTypeEnum.INNOVATION_STAR.getValue(),
                title, req.getSemesterId(), draft, now);
        app.setCertificateNo(req.getCertificateNo());
        app.setIssuingUnit(req.getIssuingUnit());
        app.setValidUntil(req.getValidUntil());
        app.setParticipantRole(req.getParticipantRole());
        app = awardApplicationRepository.save(app);

        AwardInnovationStar ext = new AwardInnovationStar();
        ext.setApplicationId(app.getId());
        ext.setCompanyName(req.getCompanyName());
        ext.setIndustryType(req.getIndustryType());
        ext.setApplicantRank(req.getApplicantRank());
        ext.setRegisteredAt(req.getRegisteredTime());
        innovationStarRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, app.getId());
        writeAwardVersion(app, userId);
        generatePendingApprovalIfSubmitted(app);

        return buildSubmitResponse(app);
    }

    // ==================== 8.5 获取奖项版本历史 ====================

    @Transactional(readOnly = true)
    public AwardVersionHistoryResponse getVersions(Long applicationId, Long userId) {
        AwardApplication app = awardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项报名不存在"));
        checkOwnership(app, userId);

        List<ModelVersion> versions = modelVersionRepository
                .findByModelTypeAndModelIdOrderByVersionAsc(
                        ModelVersionModelTypeEnum.AWARD_APPLICATION.getValue(), applicationId);

        Integer currentVersion = app.getAuditInfo() != null ? app.getAuditInfo().getCurrentVersion() : null;
        List<AwardVersionItem> list = new ArrayList<>();
        for (ModelVersion v : versions) {
            list.add(AwardVersionItem.builder()
                    .version(v.getVersion()).title(v.getTitle())
                    .status(v.getStatus())
                    .statusLabel(v.getStatus() != null ? ApplyStatusEnum.of(v.getStatus()).getLabel() : null)
                    .rejectedReason(v.getChangeSummary())
                    .createdAt(toIso(v.getCreatedAt()))
                    .build());
        }
        return AwardVersionHistoryResponse.builder().currentVersion(currentVersion).versions(list).build();
    }

    // ==================== 私有工具方法 ====================

    /**
     * 提交后生成教师端待审核任务（纯内部联动，失败不阻塞提交）。
     * 仅当申报状态为待审批(1)时触发；草稿(0)不触发。
     */
    private void generatePendingApprovalIfSubmitted(AwardApplication app) {
        if (app.getStatus() == null || app.getStatus() != ApplyStatusEnum.PENDING.getValue()) {
            return;
        }
        try {
            User user = loadUser(app.getUserId());
            AwardTypeEnum typeEnum = AwardTypeEnum.of(app.getAwardType());
            String categoryLabel = typeEnum != null ? typeEnum.getLabel() : app.getAwardType();
            LocalDateTime submittedAt = app.getAuditInfo() != null ? app.getAuditInfo().getSubmittedAt() : null;
            approvalSubmitService.createOnSubmit(
                    app.getSchoolId(), "AwardApplication", app.getAwardType(), app.getId(),
                    app.getUserId(), user.getName(), user.getUserNo(), app.getTitle(),
                    categoryLabel, submittedAt);
        } catch (Exception e) {
            log.warn("生成待审核任务失败（不阻塞提交）: awardApplicationId={}, err={}", app.getId(), e.getMessage());
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
    }

    private Long schoolId(User user) {
        return user.getSchoolId() != null ? user.getSchoolId() : 1L;
    }

    private boolean isDraft(Integer isDraft) {
        return isDraft != null && isDraft == 1;
    }

    private AwardApplication createAwardBase(Long schoolId, Long userId, String awardType,
                                              String title, Long semesterId, boolean draft, LocalDateTime now) {
        AwardApplication app = new AwardApplication();
        app.setSchoolId(schoolId);
        app.setUserId(userId);
        app.setAwardType(awardType);
        app.setTitle(title);
        app.setSemesterId(semesterId);

        ArchiveAuditInfo audit = new ArchiveAuditInfo();
        audit.setCurrentVersion(1);
        audit.setSubmitCount(draft ? 0 : 1);
        if (draft) {
            app.setStatus(ApplyStatusEnum.DRAFT.getValue());
            audit.setDraftSavedAt(now);
        } else {
            app.setStatus(ApplyStatusEnum.PENDING.getValue());
            audit.setSubmittedAt(now);
        }
        app.setAuditInfo(audit);
        return app;
    }

    private void checkOwnership(AwardApplication app, Long userId) {
        if (!userId.equals(app.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限");
        }
    }

    private void checkEditable(AwardApplication app) {
        Integer status = app.getStatus();
        if (status == null || (status != ApplyStatusEnum.DRAFT.getValue()
                && status != ApplyStatusEnum.REJECTED.getValue())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅草稿或已退回状态可编辑");
        }
    }

    private AwardResearchStar loadResearchStar(Long researchStarId, Long userId) {
        AwardResearchStar rs = researchStarRepository.findById(researchStarId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "科研之星记录不存在"));
        AwardApplication app = awardApplicationRepository.findById(rs.getApplicationId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项报名不存在"));
        checkOwnership(app, userId);
        return rs;
    }

    private AwardApplication loadApplication(Long applicationId, Long userId) {
        AwardApplication app = awardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项报名不存在"));
        checkOwnership(app, userId);
        return app;
    }

    private void bindFiles(List<Long> fileIds, Long userId, Long applicationId) {
        if (fileIds == null || fileIds.isEmpty()) return;
        for (Long fileId : fileIds) {
            bindFile(fileId, userId, applicationId);
        }
    }

    private void bindFile(Long fileId, Long userId, Long bizId) {
        AttachmentRelation relation = attachmentRelationRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));
        if (relation.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在");
        }
        if (relation.getFileStatus() == null || FileStatusEnum.of(relation.getFileStatus()) == null
                || !FileStatusEnum.of(relation.getFileStatus()).isBindable()) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "文件已关联，不能重复绑定");
        }
        relation.setBizType(AttachmentBizTypeEnum.AWARD.getValue());
        relation.setBizId(bizId);
        relation.setFileStatus(FileStatusEnum.BOUND.getValue());
        relation.setTempExpireAt(null);
        attachmentRelationRepository.save(relation);
    }

    private void writeAwardVersion(AwardApplication app, Long userId) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", app.getId());
        snapshot.put("awardType", app.getAwardType());
        snapshot.put("title", app.getTitle());
        snapshot.put("semesterId", app.getSemesterId());
        snapshot.put("status", app.getStatus());

        ArchiveAuditInfo audit = app.getAuditInfo();
        ModelVersion mv = new ModelVersion();
        mv.setModelType(ModelVersionModelTypeEnum.AWARD_APPLICATION.getValue());
        mv.setModelId(app.getId());
        mv.setVersion(audit != null && audit.getCurrentVersion() != null ? audit.getCurrentVersion() : 1);
        mv.setTitle(app.getTitle());
        mv.setDataSnapshot(writeJson(snapshot));
        mv.setStatus(app.getStatus());
        mv.setCreatedBy(userId);
        modelVersionRepository.save(mv);
    }

    @SuppressWarnings("unchecked")
    private void updateExtension(String awardType, Long applicationId, Map<String, Object> body) {
        switch (awardType) {
            case "competition_star" -> competitionStarRepository.findByApplicationId(applicationId).ifPresent(ext -> {
                if (body.containsKey("competitionName")) ext.setCompetitionName((String) body.get("competitionName"));
                if (body.containsKey("participatedTime")) ext.setParticipatedAt(parseDate((String) body.get("participatedTime")));
                if (body.containsKey("competitionLevel")) ext.setCompetitionLevel((String) body.get("competitionLevel"));
                if (body.containsKey("awardLevel")) ext.setAwardLevel((String) body.get("awardLevel"));
                competitionStarRepository.save(ext);
            });
            case "innovation_star" -> innovationStarRepository.findByApplicationId(applicationId).ifPresent(ext -> {
                if (body.containsKey("companyName")) ext.setCompanyName((String) body.get("companyName"));
                if (body.containsKey("industryType")) ext.setIndustryType((String) body.get("industryType"));
                if (body.containsKey("applicantRank")) ext.setApplicantRank((String) body.get("applicantRank"));
                if (body.containsKey("registeredTime")) ext.setRegisteredAt(parseDate((String) body.get("registeredTime")));
                innovationStarRepository.save(ext);
            });
        }
    }

    private AwardSubmitResponse buildSubmitResponse(AwardApplication app) {
        ArchiveAuditInfo audit = app.getAuditInfo();
        return AwardSubmitResponse.builder()
                .applicationId(app.getId())
                .status(app.getStatus())
                .statusLabel(ApplyStatusEnum.of(app.getStatus()).getLabel())
                .currentVersion(audit != null ? audit.getCurrentVersion() : null)
                .submitCount(audit != null ? audit.getSubmitCount() : null)
                .build();
    }

    // ==================== 类型转换工具 ====================

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value, DATE_FMT); }
        catch (DateTimeParseException e) { throw new BusinessException(ResultCode.PARAM_ERROR, "日期格式必须为yyyy-MM-dd"); }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private String toIso(LocalDateTime dt) {
        return dt != null ? dt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE) : null;
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { log.warn("序列化 JSON 失败", e); return null; }
    }

    // ==================== 内嵌 POJO：请求体 ====================

    @Data
    public static class CompetitionStarRequest {
        private Long semesterId;
        private String competitionName;
        private LocalDate participatedTime;
        private String competitionLevel;
        private String awardLevel;
        private Integer isDraft;
        private List<Long> evidenceFileIds;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
        private String participantRole;
    }

    @Data
    public static class ResearchStarCreateRequest {
        private Long semesterId;
        private Integer isDraft;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
        private String participantRole;
    }

    @Data
    public static class ResearchProjectRequest {
        private String projectName;
        private String projectLevel;
        private String rankTotal;
        private LocalDate establishedTime;
        private List<Long> evidenceFileIds;
    }

    @Data
    public static class SoftwareCopyrightRequest {
        private String softwareName;
        private String rankTotal;
        private LocalDate approvedTime;
        private List<Long> evidenceFileIds;
    }

    @Data
    public static class PublishedPaperRequest {
        private String journalName;
        private String paperTitle;
        private String rankTotal;
        private LocalDate publishedTime;
        private List<Long> evidenceFileIds;
    }

    @Data
    public static class InnovationStarRequest {
        private Long semesterId;
        private String companyName;
        private String industryType;
        private String applicantRank;
        private LocalDate registeredTime;
        private Integer isDraft;
        private List<Long> evidenceFileIds;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
        private String participantRole;
    }

    @Data
    public static class AwardDuplicateCheckRequest {
        private String awardType;
        private String certificateNo;
        private String title;
        private String participatedTime;
    }

    // ==================== 内嵌 POJO：响应体 ====================

    @Data @Builder
    public static class AwardOverviewResponse {
        private int totalSubmissions;
        private int pendingReview;
        private int approved;
        private int newThisSemester;
        private Map<String, Long> typeDistribution;
        private Map<String, Long> statusDistribution;
        private List<SemesterTrendItem> semesterTrend;
        private List<RecentActivityItem> recentActivities;
    }

    @Data @Builder
    public static class SemesterTrendItem {
        private Long semesterId;
        private String semesterName;
        private int count;
    }

    @Data @Builder
    public static class RecentActivityItem {
        private Long id;
        private String title;
        private String applicant;
        private String type;
        private String typeLabel;
        private String submitTime;
        private Integer status;
        private String statusLabel;
    }

    @Data @Builder
    public static class AwardAutosaveResponse {
        private Long applicationId;
        private Integer status;
        private String statusLabel;
        private String savedAt;
        private Integer currentVersion;
        private Integer submitCount;
    }

    @Data @Builder
    public static class AwardDuplicateCheckResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("isDuplicate")
        private boolean isDuplicate;
        private List<DuplicateRecord> duplicateRecords;
        private String suggestion;
    }

    @Data @Builder
    public static class DuplicateRecord {
        private Long applicationId;
        private String title;
        private Integer status;
        private String statusLabel;
        private double similarity;
    }

    @Data @Builder
    public static class AwardGuideResponse {
        private String type;
        private String typeLabel;
        private String title;
        private String content;
        private List<Requirement> requirements;
        private List<String> notes;
        private String updatedAt;

        @Data @Builder
        public static class Requirement {
            private String field;
            private String label;
            private boolean required;
            private String description;
        }
    }

    @Data @Builder
    public static class AwardSubmitResponse {
        private Long applicationId;
        private Integer status;
        private String statusLabel;
        private Integer currentVersion;
        private Integer submitCount;
    }

    @Data @Builder
    public static class ResearchStarCreateResponse {
        private Long applicationId;
        private Long researchStarId;
        private Integer status;
        private String statusLabel;
    }

    @Data @Builder
    public static class AwardVersionHistoryResponse {
        private Integer currentVersion;
        private List<AwardVersionItem> versions;
    }

    @Data @Builder
    public static class AwardVersionItem {
        private Integer version;
        private String title;
        private Integer status;
        private String statusLabel;
        private String rejectedReason;
        private String createdAt;
    }
}
