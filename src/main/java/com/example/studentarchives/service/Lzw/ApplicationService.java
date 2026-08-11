package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.archive.*;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.ArchiveTypeConfig;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.version.ModelVersion;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.ArchiveTypeEnum;
import com.example.studentarchives.enums.AttachmentBizTypeEnum;
import com.example.studentarchives.enums.FileStatusEnum;
import com.example.studentarchives.enums.ModelVersionModelTypeEnum;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 个人档案信息申报服务
 * <p>
 * 提供 10 种档案类型的申报：学科竞赛、奖学金、创新创业、学术研究、
 * 荣誉证书、实习经历、组织履历、实训项目、社会实践、图书心得。
 * <p>
 * 复用 ProfileCareerPlanService 中的 bindFile / writeModelVersion 模式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ArchiveRepository archiveRepository;
    private final UserRepository userRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final ArchiveTypeConfigRepository archiveTypeConfigRepository;
    private final DuplicateDetectionRuleRepository duplicateDetectionRuleRepository;

    // 10 个扩展表 Repository
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

    private final ObjectMapper objectMapper;

    // ==================== 1. 学科竞赛 ====================

    @Transactional
    public ArchiveSubmitResponse submitCompetition(CompetitionSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.ACADEMIC_COMPETITION.getValue(),
                req.getCompetitionName(), req.getSemesterId(), req.getObtainTime(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveCompetition ext = new ArchiveCompetition();
        ext.setArchiveId(archive.getId());
        ext.setCompetitionName(req.getCompetitionName());
        ext.setCompetitionType(req.getCompetitionType());
        ext.setAwardLevel(req.getAwardLevel());
        ext.setParticipantRole(req.getParticipantRole());
        competitionRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 2. 奖学金 ====================

    @Transactional
    public ArchiveSubmitResponse submitScholarship(ScholarshipSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.SCHOLARSHIP.getValue(),
                req.getScholarshipName(), req.getSemesterId(), req.getObtainTime(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveScholarship ext = new ArchiveScholarship();
        ext.setArchiveId(archive.getId());
        ext.setScholarshipName(req.getScholarshipName());
        ext.setScholarshipCategory(req.getScholarshipCategory());
        ext.setAwardLevel(req.getAwardLevel());
        scholarshipRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 3. 创新创业 ====================

    @Transactional
    public ArchiveSubmitResponse submitInnovation(InnovationSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.INNOVATION_ENTREPRENEURSHIP.getValue(),
                req.getCompanyName(), req.getSemesterId(), req.getRegisteredTime(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveInnovation ext = new ArchiveInnovation();
        ext.setArchiveId(archive.getId());
        ext.setCompanyName(req.getCompanyName());
        ext.setIndustryType(req.getIndustryType());
        ext.setProjectType(req.getProjectType());
        ext.setParticipantRole(req.getParticipantRole());
        ext.setRegisteredAt(req.getRegisteredTime());
        innovationRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 4. 学术研究 ====================

    @Transactional
    public ArchiveSubmitResponse submitResearch(ResearchSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.ACADEMIC_RESEARCH.getValue(),
                req.getProjectName(), req.getSemesterId(), req.getStartDate(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveResearch ext = new ArchiveResearch();
        ext.setArchiveId(archive.getId());
        ext.setProjectName(req.getProjectName());
        ext.setProjectLevel(req.getProjectLevel());
        ext.setProjectType(req.getProjectType());
        ext.setParticipantRole(req.getParticipantRole());
        ext.setStartDate(req.getStartDate());
        ext.setEndDate(req.getEndDate());
        researchRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 5. 荣誉证书 ====================

    @Transactional
    public ArchiveSubmitResponse submitCertificate(CertificateSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.HONOR_CERTIFICATE.getValue(),
                req.getCertificateName(), req.getSemesterId(), req.getObtainTime(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveCertificate ext = new ArchiveCertificate();
        ext.setArchiveId(archive.getId());
        ext.setCertificateType(req.getCertificateType());
        ext.setCertificateName(req.getCertificateName());
        ext.setCertificateNo(req.getCertificateNo());
        ext.setIssuingUnit(req.getIssuingUnit());
        ext.setValidUntil(req.getValidUntil());
        certificateRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 6. 实习经历 ====================

    @Transactional
    public ArchiveSubmitResponse submitInternship(InternshipSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.INTERNSHIP.getValue(),
                req.getCompanyName(), req.getSemesterId(), req.getStartDate(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveInternship ext = new ArchiveInternship();
        ext.setArchiveId(archive.getId());
        ext.setCompanyName(req.getCompanyName());
        ext.setLocation(req.getLocation());
        ext.setPosition(req.getPosition());
        ext.setStartDate(req.getStartDate());
        ext.setEndDate(req.getEndDate());
        internshipRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 7. 组织履历 ====================

    @Transactional
    public ArchiveSubmitResponse submitOrganization(OrganizationSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.ORGANIZATION.getValue(),
                req.getPositionTitle(), req.getSemesterId(), req.getStartDate(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveOrganization ext = new ArchiveOrganization();
        ext.setArchiveId(archive.getId());
        ext.setOrgLevel(req.getOrgLevel());
        ext.setDepartment(req.getDepartment());
        ext.setPositionTitle(req.getPositionTitle());
        ext.setStartDate(req.getStartDate());
        ext.setEndDate(req.getEndDate());
        organizationRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 8. 实训项目 ====================

    @Transactional
    public ArchiveSubmitResponse submitTraining(TrainingSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.TRAINING_PROJECT.getValue(),
                req.getProjectName(), req.getSemesterId(), req.getStartDate(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveTrainingProject ext = new ArchiveTrainingProject();
        ext.setArchiveId(archive.getId());
        ext.setProjectName(req.getProjectName());
        ext.setProjectContent(req.getProjectContent());
        ext.setStartDate(req.getStartDate());
        ext.setEndDate(req.getEndDate());
        trainingProjectRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 9. 社会实践 ====================

    @Transactional
    public ArchiveSubmitResponse submitPractice(PracticeSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.SOCIAL_PRACTICE.getValue(),
                req.getActivityName(), req.getSemesterId(), req.getStartDate(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveSocialPractice ext = new ArchiveSocialPractice();
        ext.setArchiveId(archive.getId());
        ext.setActivityName(req.getActivityName());
        ext.setPracticeLocation(req.getPracticeLocation());
        ext.setPracticeUnit(req.getPracticeUnit());
        ext.setParticipantRole(req.getParticipantRole());
        ext.setStartDate(req.getStartDate());
        ext.setEndDate(req.getEndDate());
        ext.setVolunteerHours(req.getVolunteerHours());
        socialPracticeRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 10. 图书心得 ====================

    @Transactional
    public ArchiveSubmitResponse submitBookReview(BookReviewSubmitRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(req.getIsDraft());
        LocalDateTime now = LocalDateTime.now();

        Archive archive = createArchiveBase(schoolId, userId, ArchiveTypeEnum.BOOK_REVIEW.getValue(),
                req.getBookName(), req.getSemesterId(), req.getReadMonth(), draft, now);
        archive = archiveRepository.save(archive);

        ArchiveBookReview ext = new ArchiveBookReview();
        ext.setArchiveId(archive.getId());
        ext.setBookName(req.getBookName());
        ext.setReadMonth(req.getReadMonth());
        ext.setReviewContent(req.getReviewContent());
        bookReviewRepository.save(ext);

        bindFiles(req.getEvidenceFileIds(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 自动保存草稿 ====================

    @Transactional
    public AutosaveResponse autosave(Long archiveId, Map<String, Object> body, Long userId) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案不存在"));
        checkOwnership(archive, userId);
        checkEditable(archive);

        LocalDateTime now = LocalDateTime.now();

        // 更新基表字段（仅更新 body 中提供的字段）
        if (body.containsKey("title")) {
            archive.setTitle((String) body.get("title"));
        }
        if (body.containsKey("semesterId")) {
            archive.setSemesterId(toLong(body.get("semesterId")));
        }
        if (body.containsKey("obtainTime")) {
            archive.setObtainedAt(parseDate((String) body.get("obtainTime")));
        }
        ArchiveAuditInfo audit = archive.getAuditInfo();
        if (audit == null) {
            audit = new ArchiveAuditInfo();
            archive.setAuditInfo(audit);
        }
        audit.setDraftSavedAt(now);

        // 更新扩展表（仅更新 body 中提供的字段）
        updateExtension(archive.getArchiveType(), archiveId, body);

        // 如果从草稿提交，更新状态
        boolean submit = body.containsKey("isDraft") && toInt(body.get("isDraft")) == 0;
        if (submit) {
            archive.setStatus(ApplyStatusEnum.PENDING.getValue());
            audit.setSubmittedAt(LocalDateTime.now());
            audit.setCurrentVersion(audit.getCurrentVersion() != null ? audit.getCurrentVersion() + 1 : 1);
            audit.setSubmitCount(audit.getSubmitCount() != null ? audit.getSubmitCount() + 1 : 1);
            archive = archiveRepository.save(archive);
            return AutosaveResponse.builder()
                    .archiveId(archive.getId())
                    .status(archive.getStatus())
                    .statusLabel(ApplyStatusEnum.of(archive.getStatus()).getLabel())
                    .currentVersion(audit.getCurrentVersion())
                    .submitCount(audit.getSubmitCount())
                    .build();
        }

        archive = archiveRepository.save(archive);
        return AutosaveResponse.builder()
                .archiveId(archive.getId())
                .status(archive.getStatus())
                .statusLabel(ApplyStatusEnum.of(archive.getStatus()).getLabel())
                .savedAt(toIso(now))
                .build();
    }

    // ==================== 重复检测 ====================

    @Transactional(readOnly = true)
    public DuplicateCheckResponse duplicateCheck(DuplicateCheckRequest req, Long userId) {
        User user = loadUser(userId);
        Long schoolId = schoolId(user);

        DuplicateDetectionRule rule = duplicateDetectionRuleRepository
                .findFirstBySchoolIdAndArchiveTypeAndStatus(schoolId, req.getArchiveType(), 1)
                .orElse(null);

        if (rule == null) {
            return DuplicateCheckResponse.builder()
                    .hasDuplicate(false)
                    .similarItems(Collections.emptyList())
                    .suggestion("当前学校未配置该类型的重复检测规则")
                    .build();
        }

        // 解析检测字段
        List<String> detectFields;
        try {
            detectFields = objectMapper.readValue(rule.getDetectFields(),
                    new TypeReference<List<String>>() {});
        } catch (Exception e) {
            detectFields = Collections.emptyList();
        }

        // 查询同类型、同用户的已通过档案做简易比对
        List<Archive> sameTypeArchives = archiveRepository.findByUserIdAndArchiveType(userId, req.getArchiveType());
        List<DuplicateCheckResponse.SimilarItem> similarItems = new ArrayList<>();

        for (Archive a : sameTypeArchives) {
            if (a.getStatus() == null || a.getStatus() != ApplyStatusEnum.APPROVED.getValue()) continue;
            // 简易精确匹配
            boolean match = false;
            if (req.getFields() != null && !detectFields.isEmpty()) {
                for (String field : detectFields) {
                    Object inputVal = req.getFields().get(field);
                    if (inputVal != null && inputVal.toString().equalsIgnoreCase(
                            String.valueOf(a.getTitle() != null ? a.getTitle() : ""))) {
                        match = true;
                        break;
                    }
                }
            }
            if (match) {
                similarItems.add(DuplicateCheckResponse.SimilarItem.builder()
                        .archiveId(a.getId())
                        .title(a.getTitle())
                        .status(a.getStatus())
                        .statusLabel(ApplyStatusEnum.of(a.getStatus()).getLabel())
                        .similarity(rule.getSimilarityThreshold() != null
                                ? rule.getSimilarityThreshold() : BigDecimal.ONE)
                        .build());
            }
        }

        boolean hasDuplicate = !similarItems.isEmpty();
        return DuplicateCheckResponse.builder()
                .hasDuplicate(hasDuplicate)
                .similarItems(similarItems)
                .suggestion(hasDuplicate ? "检测到疑似重复申报，请确认是否继续提交" : "未检测到重复申报")
                .build();
    }

    // ==================== 评选说明 ====================

    @Transactional(readOnly = true)
    public GuideResponse getGuide(String type) {
        ArchiveTypeEnum typeEnum = ArchiveTypeEnum.of(type);
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的档案类型：" + type);
        }
        ArchiveTypeConfig config = archiveTypeConfigRepository
                .findByArchiveTypeAndStatus(type, 1)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "该档案类型的配置不存在"));

        return GuideResponse.builder()
                .archiveType(config.getArchiveType())
                .typeName(config.getTypeName())
                .evaluateDesc(config.getEvaluateDesc())
                .evaluateRequirements(config.getEvaluateRequirements())
                .evaluateNotes(config.getEvaluateNotes())
                .applyDesc(config.getApplyDesc())
                .icon(config.getIcon())
                .build();
    }

    // ==================== 版本历史 ====================

    @Transactional(readOnly = true)
    public VersionHistoryResponse getVersions(Long archiveId, Long userId) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案不存在"));
        checkOwnership(archive, userId);

        List<ModelVersion> versions = modelVersionRepository
                .findByModelTypeAndModelIdOrderByVersionAsc(
                        ModelVersionModelTypeEnum.ARCHIVE.getValue(), archiveId);

        Integer currentVersion = archive.getAuditInfo() != null ? archive.getAuditInfo().getCurrentVersion() : null;
        List<VersionItemResponse> list = new ArrayList<>();
        for (ModelVersion v : versions) {
            list.add(VersionItemResponse.builder()
                    .version(v.getVersion())
                    .title(v.getTitle())
                    .status(v.getStatus())
                    .statusLabel(v.getStatus() != null ? ApplyStatusEnum.of(v.getStatus()).getLabel() : null)
                    .rejectedReason(v.getChangeSummary())
                    .createdAt(toIso(v.getCreatedAt()))
                    .build());
        }
        return VersionHistoryResponse.builder()
                .currentVersion(currentVersion)
                .versions(list)
                .build();
    }

    // ==================== 更正已通过记录 ====================

    @Transactional
    public ArchiveSubmitResponse correction(Long archiveId, Map<String, Object> body, Long userId) {
        Archive original = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案不存在"));
        checkOwnership(original, userId);

        if (original.getStatus() == null || original.getStatus() != ApplyStatusEnum.APPROVED.getValue()) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅已通过状态的档案可以更正");
        }

        String correctionReason = (String) body.get("correctionReason");

        // 创建新版本档案
        User user = loadUser(userId);
        Long schoolId = schoolId(user);
        boolean draft = isDraft(body.containsKey("isDraft") ? toInt(body.get("isDraft")) : 1);
        LocalDateTime now = LocalDateTime.now();

        Archive archive = new Archive();
        archive.setSchoolId(schoolId);
        archive.setUserId(userId);
        archive.setArchiveType(original.getArchiveType());
        archive.setTitle(body.containsKey("title") ? (String) body.get("title") : original.getTitle());
        archive.setSemesterId(body.containsKey("semesterId") ? toLong(body.get("semesterId")) : original.getSemesterId());
        archive.setObtainedAt(body.containsKey("obtainTime")
                ? parseDate((String) body.get("obtainTime")) : original.getObtainedAt());
        archive.setDuplicateCheckStatus(0);

        ArchiveAuditInfo audit = new ArchiveAuditInfo();
        ArchiveAuditInfo origAudit = original.getAuditInfo();
        audit.setCurrentVersion(origAudit != null && origAudit.getCurrentVersion() != null
                ? origAudit.getCurrentVersion() + 1 : 2);
        audit.setSubmitCount(draft ? 0 : 1);
        if (draft) {
            audit.setDraftSavedAt(now);
            archive.setStatus(ApplyStatusEnum.DRAFT.getValue());
        } else {
            audit.setSubmittedAt(now);
            archive.setStatus(ApplyStatusEnum.PENDING.getValue());
        }
        archive.setAuditInfo(audit);
        archive = archiveRepository.save(archive);

        // 复制并合并扩展表数据
        copyAndMergeExtension(original.getArchiveType(), original.getId(), archive.getId(), body);

        // 标记原始档案的更正原因
        original.setCorrectionReason(correctionReason);
        archiveRepository.save(original);

        bindFiles(body.containsKey("evidenceFileIds")
                ? toLongList(body.get("evidenceFileIds"))
                : Collections.emptyList(), userId, archive.getId());
        writeArchiveVersion(archive, userId);

        return buildResponse(archive);
    }

    // ==================== 私有工具方法 ====================

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

    private Archive createArchiveBase(Long schoolId, Long userId, String archiveType,
                                       String title, Long semesterId, LocalDate obtainedAt,
                                       boolean draft, LocalDateTime now) {
        Archive archive = new Archive();
        archive.setSchoolId(schoolId);
        archive.setUserId(userId);
        archive.setArchiveType(archiveType);
        archive.setTitle(title);
        archive.setSemesterId(semesterId);
        archive.setObtainedAt(obtainedAt);
        archive.setDuplicateCheckStatus(0);

        ArchiveAuditInfo audit = new ArchiveAuditInfo();
        audit.setCurrentVersion(1);
        audit.setSubmitCount(draft ? 0 : 1);
        if (draft) {
            archive.setStatus(ApplyStatusEnum.DRAFT.getValue());
            audit.setDraftSavedAt(now);
        } else {
            archive.setStatus(ApplyStatusEnum.PENDING.getValue());
            audit.setSubmittedAt(now);
        }
        archive.setAuditInfo(audit);
        return archive;
    }

    private void checkOwnership(Archive archive, Long userId) {
        if (!userId.equals(archive.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限");
        }
    }

    private void checkEditable(Archive archive) {
        Integer status = archive.getStatus();
        if (status == null || (status != ApplyStatusEnum.DRAFT.getValue()
                && status != ApplyStatusEnum.REJECTED.getValue())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅草稿或已退回状态可编辑");
        }
    }

    private void bindFiles(List<Long> fileIds, Long userId, Long archiveId) {
        if (fileIds == null || fileIds.isEmpty()) return;
        for (Long fileId : fileIds) {
            bindFile(fileId, userId, AttachmentBizTypeEnum.ARCHIVE.getValue(), archiveId);
        }
    }

    private AttachmentRelation bindFile(Long fileId, Long userId, String bizType, Long bizId) {
        AttachmentRelation relation = attachmentRelationRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));
        if (relation.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在");
        }
        if (relation.getFileStatus() == null || FileStatusEnum.of(relation.getFileStatus()) == null
                || !FileStatusEnum.of(relation.getFileStatus()).isBindable()) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "文件已关联，不能重复绑定");
        }
        relation.setBizType(bizType);
        relation.setBizId(bizId);
        relation.setFileStatus(FileStatusEnum.BOUND.getValue());
        relation.setTempExpireAt(null);
        return attachmentRelationRepository.save(relation);
    }

    private void writeArchiveVersion(Archive archive, Long userId) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", archive.getId());
        snapshot.put("archiveType", archive.getArchiveType());
        snapshot.put("title", archive.getTitle());
        snapshot.put("semesterId", archive.getSemesterId());
        snapshot.put("obtainedAt", archive.getObtainedAt() != null ? archive.getObtainedAt().format(DATE_FMT) : null);
        snapshot.put("status", archive.getStatus());

        ArchiveAuditInfo audit = archive.getAuditInfo();
        ModelVersion mv = new ModelVersion();
        mv.setModelType(ModelVersionModelTypeEnum.ARCHIVE.getValue());
        mv.setModelId(archive.getId());
        mv.setVersion(audit != null && audit.getCurrentVersion() != null ? audit.getCurrentVersion() : 1);
        mv.setTitle(archive.getTitle());
        mv.setDataSnapshot(writeJson(snapshot));
        mv.setStatus(archive.getStatus());
        mv.setCreatedBy(userId);
        modelVersionRepository.save(mv);
    }

    @SuppressWarnings("unchecked")
    private void updateExtension(String archiveType, Long archiveId, Map<String, Object> body) {
        switch (archiveType) {
            case "academic_competition" -> {
                competitionRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("competitionName")) ext.setCompetitionName((String) body.get("competitionName"));
                    if (body.containsKey("competitionType")) ext.setCompetitionType((String) body.get("competitionType"));
                    if (body.containsKey("awardLevel")) ext.setAwardLevel((String) body.get("awardLevel"));
                    if (body.containsKey("participantRole")) ext.setParticipantRole((String) body.get("participantRole"));
                    competitionRepository.save(ext);
                });
            }
            case "scholarship" -> {
                scholarshipRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("scholarshipName")) ext.setScholarshipName((String) body.get("scholarshipName"));
                    if (body.containsKey("scholarshipCategory")) ext.setScholarshipCategory((String) body.get("scholarshipCategory"));
                    if (body.containsKey("awardLevel")) ext.setAwardLevel((String) body.get("awardLevel"));
                    scholarshipRepository.save(ext);
                });
            }
            case "innovation_entrepreneurship" -> {
                innovationRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("companyName")) ext.setCompanyName((String) body.get("companyName"));
                    if (body.containsKey("industryType")) ext.setIndustryType((String) body.get("industryType"));
                    if (body.containsKey("projectType")) ext.setProjectType((String) body.get("projectType"));
                    if (body.containsKey("participantRole")) ext.setParticipantRole((String) body.get("participantRole"));
                    if (body.containsKey("registeredTime")) ext.setRegisteredAt(parseDate((String) body.get("registeredTime")));
                    innovationRepository.save(ext);
                });
            }
            case "academic_research" -> {
                researchRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("projectName")) ext.setProjectName((String) body.get("projectName"));
                    if (body.containsKey("projectLevel")) ext.setProjectLevel((String) body.get("projectLevel"));
                    if (body.containsKey("projectType")) ext.setProjectType((String) body.get("projectType"));
                    if (body.containsKey("participantRole")) ext.setParticipantRole((String) body.get("participantRole"));
                    if (body.containsKey("startDate")) ext.setStartDate(parseDate((String) body.get("startDate")));
                    if (body.containsKey("endDate")) ext.setEndDate(parseDate((String) body.get("endDate")));
                    researchRepository.save(ext);
                });
            }
            case "honor_certificate" -> {
                certificateRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("certificateType")) ext.setCertificateType((String) body.get("certificateType"));
                    if (body.containsKey("certificateName")) ext.setCertificateName((String) body.get("certificateName"));
                    if (body.containsKey("certificateNo")) ext.setCertificateNo((String) body.get("certificateNo"));
                    if (body.containsKey("issuingUnit")) ext.setIssuingUnit((String) body.get("issuingUnit"));
                    if (body.containsKey("validUntil")) ext.setValidUntil(parseDate((String) body.get("validUntil")));
                    certificateRepository.save(ext);
                });
            }
            case "internship" -> {
                internshipRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("companyName")) ext.setCompanyName((String) body.get("companyName"));
                    if (body.containsKey("location")) ext.setLocation((String) body.get("location"));
                    if (body.containsKey("position")) ext.setPosition((String) body.get("position"));
                    if (body.containsKey("startDate")) ext.setStartDate(parseDate((String) body.get("startDate")));
                    if (body.containsKey("endDate")) ext.setEndDate(parseDate((String) body.get("endDate")));
                    internshipRepository.save(ext);
                });
            }
            case "organization" -> {
                organizationRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("orgLevel")) ext.setOrgLevel((String) body.get("orgLevel"));
                    if (body.containsKey("department")) ext.setDepartment((String) body.get("department"));
                    if (body.containsKey("positionTitle")) ext.setPositionTitle((String) body.get("positionTitle"));
                    if (body.containsKey("startDate")) ext.setStartDate(parseDate((String) body.get("startDate")));
                    if (body.containsKey("endDate")) ext.setEndDate(parseDate((String) body.get("endDate")));
                    organizationRepository.save(ext);
                });
            }
            case "training_project" -> {
                trainingProjectRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("projectName")) ext.setProjectName((String) body.get("projectName"));
                    if (body.containsKey("projectContent")) ext.setProjectContent((String) body.get("projectContent"));
                    if (body.containsKey("startDate")) ext.setStartDate(parseDate((String) body.get("startDate")));
                    if (body.containsKey("endDate")) ext.setEndDate(parseDate((String) body.get("endDate")));
                    trainingProjectRepository.save(ext);
                });
            }
            case "social_practice" -> {
                socialPracticeRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("activityName")) ext.setActivityName((String) body.get("activityName"));
                    if (body.containsKey("practiceLocation")) ext.setPracticeLocation((String) body.get("practiceLocation"));
                    if (body.containsKey("practiceUnit")) ext.setPracticeUnit((String) body.get("practiceUnit"));
                    if (body.containsKey("participantRole")) ext.setParticipantRole((String) body.get("participantRole"));
                    if (body.containsKey("startDate")) ext.setStartDate(parseDate((String) body.get("startDate")));
                    if (body.containsKey("endDate")) ext.setEndDate(parseDate((String) body.get("endDate")));
                    if (body.containsKey("volunteerHours")) ext.setVolunteerHours(toBigDecimal(body.get("volunteerHours")));
                    socialPracticeRepository.save(ext);
                });
            }
            case "book_review" -> {
                bookReviewRepository.findByArchiveId(archiveId).ifPresent(ext -> {
                    if (body.containsKey("bookName")) ext.setBookName((String) body.get("bookName"));
                    if (body.containsKey("readMonth")) ext.setReadMonth(parseDate((String) body.get("readMonth")));
                    if (body.containsKey("reviewContent")) ext.setReviewContent((String) body.get("reviewContent"));
                    bookReviewRepository.save(ext);
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void copyAndMergeExtension(String archiveType, Long oldArchiveId, Long newArchiveId,
                                        Map<String, Object> body) {
        switch (archiveType) {
            case "academic_competition" -> {
                ArchiveCompetition old = competitionRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveCompetition ext = new ArchiveCompetition();
                ext.setArchiveId(newArchiveId);
                ext.setCompetitionName(body.containsKey("competitionName") ? (String) body.get("competitionName")
                        : old != null ? old.getCompetitionName() : null);
                ext.setCompetitionType(body.containsKey("competitionType") ? (String) body.get("competitionType")
                        : old != null ? old.getCompetitionType() : null);
                ext.setAwardLevel(body.containsKey("awardLevel") ? (String) body.get("awardLevel")
                        : old != null ? old.getAwardLevel() : null);
                ext.setParticipantRole(body.containsKey("participantRole") ? (String) body.get("participantRole")
                        : old != null ? old.getParticipantRole() : null);
                competitionRepository.save(ext);
            }
            case "scholarship" -> {
                ArchiveScholarship old = scholarshipRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveScholarship ext = new ArchiveScholarship();
                ext.setArchiveId(newArchiveId);
                ext.setScholarshipName(body.containsKey("scholarshipName") ? (String) body.get("scholarshipName")
                        : old != null ? old.getScholarshipName() : null);
                ext.setScholarshipCategory(body.containsKey("scholarshipCategory") ? (String) body.get("scholarshipCategory")
                        : old != null ? old.getScholarshipCategory() : null);
                ext.setAwardLevel(body.containsKey("awardLevel") ? (String) body.get("awardLevel")
                        : old != null ? old.getAwardLevel() : null);
                scholarshipRepository.save(ext);
            }
            case "innovation_entrepreneurship" -> {
                ArchiveInnovation old = innovationRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveInnovation ext = new ArchiveInnovation();
                ext.setArchiveId(newArchiveId);
                ext.setCompanyName(body.containsKey("companyName") ? (String) body.get("companyName")
                        : old != null ? old.getCompanyName() : null);
                ext.setIndustryType(body.containsKey("industryType") ? (String) body.get("industryType")
                        : old != null ? old.getIndustryType() : null);
                ext.setProjectType(body.containsKey("projectType") ? (String) body.get("projectType")
                        : old != null ? old.getProjectType() : null);
                ext.setParticipantRole(body.containsKey("participantRole") ? (String) body.get("participantRole")
                        : old != null ? old.getParticipantRole() : null);
                ext.setRegisteredAt(body.containsKey("registeredTime") ? parseDate((String) body.get("registeredTime"))
                        : old != null ? old.getRegisteredAt() : null);
                innovationRepository.save(ext);
            }
            case "academic_research" -> {
                ArchiveResearch old = researchRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveResearch ext = new ArchiveResearch();
                ext.setArchiveId(newArchiveId);
                ext.setProjectName(body.containsKey("projectName") ? (String) body.get("projectName")
                        : old != null ? old.getProjectName() : null);
                ext.setProjectLevel(body.containsKey("projectLevel") ? (String) body.get("projectLevel")
                        : old != null ? old.getProjectLevel() : null);
                ext.setProjectType(body.containsKey("projectType") ? (String) body.get("projectType")
                        : old != null ? old.getProjectType() : null);
                ext.setParticipantRole(body.containsKey("participantRole") ? (String) body.get("participantRole")
                        : old != null ? old.getParticipantRole() : null);
                ext.setStartDate(body.containsKey("startDate") ? parseDate((String) body.get("startDate"))
                        : old != null ? old.getStartDate() : null);
                ext.setEndDate(body.containsKey("endDate") ? parseDate((String) body.get("endDate"))
                        : old != null ? old.getEndDate() : null);
                researchRepository.save(ext);
            }
            case "honor_certificate" -> {
                ArchiveCertificate old = certificateRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveCertificate ext = new ArchiveCertificate();
                ext.setArchiveId(newArchiveId);
                ext.setCertificateType(body.containsKey("certificateType") ? (String) body.get("certificateType")
                        : old != null ? old.getCertificateType() : null);
                ext.setCertificateName(body.containsKey("certificateName") ? (String) body.get("certificateName")
                        : old != null ? old.getCertificateName() : null);
                ext.setCertificateNo(body.containsKey("certificateNo") ? (String) body.get("certificateNo")
                        : old != null ? old.getCertificateNo() : null);
                ext.setIssuingUnit(body.containsKey("issuingUnit") ? (String) body.get("issuingUnit")
                        : old != null ? old.getIssuingUnit() : null);
                ext.setValidUntil(body.containsKey("validUntil") ? parseDate((String) body.get("validUntil"))
                        : old != null ? old.getValidUntil() : null);
                certificateRepository.save(ext);
            }
            case "internship" -> {
                ArchiveInternship old = internshipRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveInternship ext = new ArchiveInternship();
                ext.setArchiveId(newArchiveId);
                ext.setCompanyName(body.containsKey("companyName") ? (String) body.get("companyName")
                        : old != null ? old.getCompanyName() : null);
                ext.setLocation(body.containsKey("location") ? (String) body.get("location")
                        : old != null ? old.getLocation() : null);
                ext.setPosition(body.containsKey("position") ? (String) body.get("position")
                        : old != null ? old.getPosition() : null);
                ext.setStartDate(body.containsKey("startDate") ? parseDate((String) body.get("startDate"))
                        : old != null ? old.getStartDate() : null);
                ext.setEndDate(body.containsKey("endDate") ? parseDate((String) body.get("endDate"))
                        : old != null ? old.getEndDate() : null);
                internshipRepository.save(ext);
            }
            case "organization" -> {
                ArchiveOrganization old = organizationRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveOrganization ext = new ArchiveOrganization();
                ext.setArchiveId(newArchiveId);
                ext.setOrgLevel(body.containsKey("orgLevel") ? (String) body.get("orgLevel")
                        : old != null ? old.getOrgLevel() : null);
                ext.setDepartment(body.containsKey("department") ? (String) body.get("department")
                        : old != null ? old.getDepartment() : null);
                ext.setPositionTitle(body.containsKey("positionTitle") ? (String) body.get("positionTitle")
                        : old != null ? old.getPositionTitle() : null);
                ext.setStartDate(body.containsKey("startDate") ? parseDate((String) body.get("startDate"))
                        : old != null ? old.getStartDate() : null);
                ext.setEndDate(body.containsKey("endDate") ? parseDate((String) body.get("endDate"))
                        : old != null ? old.getEndDate() : null);
                organizationRepository.save(ext);
            }
            case "training_project" -> {
                ArchiveTrainingProject old = trainingProjectRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveTrainingProject ext = new ArchiveTrainingProject();
                ext.setArchiveId(newArchiveId);
                ext.setProjectName(body.containsKey("projectName") ? (String) body.get("projectName")
                        : old != null ? old.getProjectName() : null);
                ext.setProjectContent(body.containsKey("projectContent") ? (String) body.get("projectContent")
                        : old != null ? old.getProjectContent() : null);
                ext.setStartDate(body.containsKey("startDate") ? parseDate((String) body.get("startDate"))
                        : old != null ? old.getStartDate() : null);
                ext.setEndDate(body.containsKey("endDate") ? parseDate((String) body.get("endDate"))
                        : old != null ? old.getEndDate() : null);
                trainingProjectRepository.save(ext);
            }
            case "social_practice" -> {
                ArchiveSocialPractice old = socialPracticeRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveSocialPractice ext = new ArchiveSocialPractice();
                ext.setArchiveId(newArchiveId);
                ext.setActivityName(body.containsKey("activityName") ? (String) body.get("activityName")
                        : old != null ? old.getActivityName() : null);
                ext.setPracticeLocation(body.containsKey("practiceLocation") ? (String) body.get("practiceLocation")
                        : old != null ? old.getPracticeLocation() : null);
                ext.setPracticeUnit(body.containsKey("practiceUnit") ? (String) body.get("practiceUnit")
                        : old != null ? old.getPracticeUnit() : null);
                ext.setParticipantRole(body.containsKey("participantRole") ? (String) body.get("participantRole")
                        : old != null ? old.getParticipantRole() : null);
                ext.setStartDate(body.containsKey("startDate") ? parseDate((String) body.get("startDate"))
                        : old != null ? old.getStartDate() : null);
                ext.setEndDate(body.containsKey("endDate") ? parseDate((String) body.get("endDate"))
                        : old != null ? old.getEndDate() : null);
                ext.setVolunteerHours(body.containsKey("volunteerHours") ? toBigDecimal(body.get("volunteerHours"))
                        : old != null ? old.getVolunteerHours() : null);
                socialPracticeRepository.save(ext);
            }
            case "book_review" -> {
                ArchiveBookReview old = bookReviewRepository.findByArchiveId(oldArchiveId).orElse(null);
                ArchiveBookReview ext = new ArchiveBookReview();
                ext.setArchiveId(newArchiveId);
                ext.setBookName(body.containsKey("bookName") ? (String) body.get("bookName")
                        : old != null ? old.getBookName() : null);
                ext.setReadMonth(body.containsKey("readMonth") ? parseDate((String) body.get("readMonth"))
                        : old != null ? old.getReadMonth() : null);
                ext.setReviewContent(body.containsKey("reviewContent") ? (String) body.get("reviewContent")
                        : old != null ? old.getReviewContent() : null);
                bookReviewRepository.save(ext);
            }
        }
    }

    private ArchiveSubmitResponse buildResponse(Archive archive) {
        ArchiveAuditInfo audit = archive.getAuditInfo();
        return ArchiveSubmitResponse.builder()
                .archiveId(archive.getId())
                .status(archive.getStatus())
                .statusLabel(ApplyStatusEnum.of(archive.getStatus()).getLabel())
                .currentVersion(audit != null ? audit.getCurrentVersion() : null)
                .submitCount(audit != null ? audit.getSubmitCount() : null)
                .build();
    }

    // ==================== 类型转换工具 ====================

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "日期格式必须为yyyy-MM-dd");
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object value) {
        if (value == null) return Collections.emptyList();
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::toLong)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return Collections.emptyList();
    }

    private String toIso(LocalDateTime dt) {
        return dt != null ? dt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE) : null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    // ==================== 内嵌 POJO：请求体 ====================

    @Data
    public static class CompetitionSubmitRequest {
        private Long semesterId;
        private LocalDate obtainTime;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String competitionName;
        private String competitionType;
        private String awardLevel;
        private String participantRole;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class ScholarshipSubmitRequest {
        private Long semesterId;
        private LocalDate obtainTime;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String scholarshipName;
        private String scholarshipCategory;
        private String awardLevel;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class InnovationSubmitRequest {
        private Long semesterId;
        private LocalDate registeredTime;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String companyName;
        private String industryType;
        private String projectType;
        private String participantRole;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class ResearchSubmitRequest {
        private Long semesterId;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String projectName;
        private String projectLevel;
        private String projectType;
        private String participantRole;
        private LocalDate startDate;
        private LocalDate endDate;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class CertificateSubmitRequest {
        private Long semesterId;
        private LocalDate obtainTime;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String certificateType;
        private String certificateName;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class InternshipSubmitRequest {
        private Long semesterId;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String companyName;
        private String location;
        private String position;
        private LocalDate startDate;
        private LocalDate endDate;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class OrganizationSubmitRequest {
        private Long semesterId;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String orgLevel;
        private String department;
        private String positionTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class TrainingSubmitRequest {
        private Long semesterId;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String projectName;
        private String projectContent;
        private LocalDate startDate;
        private LocalDate endDate;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class PracticeSubmitRequest {
        private Long semesterId;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String activityName;
        private String practiceLocation;
        private String practiceUnit;
        private String participantRole;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal volunteerHours;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    @Data
    public static class BookReviewSubmitRequest {
        private Long semesterId;
        private List<Long> evidenceFileIds;
        private Integer isDraft;
        private String bookName;
        private LocalDate readMonth;
        private String reviewContent;
        private String certificateNo;
        private String issuingUnit;
        private LocalDate validUntil;
    }

    // ==================== 内嵌 POJO：响应体 ====================

    @Data
    @Builder
    public static class ArchiveSubmitResponse {
        private Long archiveId;
        private Integer status;
        private String statusLabel;
        private Integer currentVersion;
        private Integer submitCount;
    }

    @Data
    @Builder
    public static class AutosaveResponse {
        private Long archiveId;
        private Integer status;
        private String statusLabel;
        private String savedAt;
        private Integer currentVersion;
        private Integer submitCount;
    }

    @Data
    @Builder
    public static class VersionHistoryResponse {
        private Integer currentVersion;
        private List<VersionItemResponse> versions;
    }

    @Data
    @Builder
    public static class DuplicateCheckRequest {
        private String archiveType;
        private Map<String, Object> fields;
    }

    @Data
    @Builder
    public static class DuplicateCheckResponse {
        private boolean hasDuplicate;
        private List<SimilarItem> similarItems;
        private String suggestion;

        @Data
        @Builder
        public static class SimilarItem {
            private Long archiveId;
            private String title;
            private Integer status;
            private String statusLabel;
            private BigDecimal similarity;
        }
    }

    @Data
    @Builder
    public static class GuideResponse {
        private String archiveType;
        private String typeName;
        private String evaluateDesc;
        private String evaluateRequirements;
        private String evaluateNotes;
        private String applyDesc;
        private String icon;
    }

    @Data
    @Builder
    public static class VersionItemResponse {
        private Integer version;
        private String title;
        private Integer status;
        private String statusLabel;
        private String rejectedReason;
        private String createdAt;
    }
}
