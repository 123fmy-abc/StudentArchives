package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.profile.request.ExportRequest;
import com.example.studentarchives.dto.Fmy.profile.response.ExportPreviewResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ExportPreviewResponse.SectionItem;
import com.example.studentarchives.dto.Fmy.profile.response.ExportSubmitResponse;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.archive.ArchiveCertificate;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.award.AwardCompetitionStar;
import com.example.studentarchives.entity.award.AwardInnovationStar;
import com.example.studentarchives.entity.award.AwardResearchProject;
import com.example.studentarchives.entity.award.AwardResearchStar;
import com.example.studentarchives.entity.export.ExportJob;
import com.example.studentarchives.entity.export.ExportOperationLog;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.user.UserInterest;
import com.example.studentarchives.enums.AttachmentBizTypeEnum;
import com.example.studentarchives.enums.DegreeTypeEnum;
import com.example.studentarchives.enums.ExportTaskStatusEnum;
import com.example.studentarchives.enums.FileStatusEnum;
import com.example.studentarchives.enums.StudentStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveCertificateRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.AwardCompetitionStarRepository;
import com.example.studentarchives.repository.AwardInnovationStarRepository;
import com.example.studentarchives.repository.AwardResearchProjectRepository;
import com.example.studentarchives.repository.AwardResearchStarRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.ExportJobRepository;
import com.example.studentarchives.repository.ExportOperationLogRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterGpaSummaryRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserInterestRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人简历导出服务
 * <p>
 * 提供学生端个人简历导出接口：
 * - GET /profile/resume/export/preview  预览可导出栏目
 * - POST /profile/resume/export         同步生成简历 PDF 并上传 OSS
 * <p>
 * 数据来自学生学籍、联系信息、学期成绩、奖项申请、兴趣标签、实践档案、证书及自我评价，
 * 渲染优先使用 export_templates 中 export_type=resume 的默认模板，无模板时抛出异常。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExportService {

    /** ISO 8601 带时区格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 文件名日期格式 */
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 简历支持的栏目编码 */
    private static final Set<String> ALLOWED_SECTIONS = Set.of(
            "education", "awards", "skills", "practices", "certificates", "selfEvaluation");

    /** 简历默认导出栏目（不传 sections 时全选，顺序与导出预览一致） */
    private static final List<String> DEFAULT_SECTIONS = List.of(
            "education", "awards", "skills", "practices", "certificates", "selfEvaluation");

    /** 荣誉证书档案类型编码 */
    private static final String CERTIFICATE_ARCHIVE_TYPE = "honor_certificate";

    /** 兴趣熟练度标签 */
    private static final Map<Integer, String> PROFICIENCY_LABELS = Map.of(
            1, "入门",
            2, "一般",
            3, "熟练",
            4, "精通"
    );

    private final ExportJobRepository exportJobRepository;
    private final ExportOperationLogRepository exportOperationLogRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final ExportTemplateRenderService exportTemplateRenderService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final SchoolRepository schoolRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final SemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final SemesterRepository semesterRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final AwardCompetitionStarRepository awardCompetitionStarRepository;
    private final AwardInnovationStarRepository awardInnovationStarRepository;
    private final AwardResearchStarRepository awardResearchStarRepository;
    private final AwardResearchProjectRepository awardResearchProjectRepository;
    private final UserInterestRepository userInterestRepository;
    private final ArchiveRepository archiveRepository;
    private final ArchiveCertificateRepository archiveCertificateRepository;
    private final UserMessageRepository userMessageRepository;
    private final OssFileService ossFileService;
    private final ObjectMapper objectMapper;

    // ==================== 导出预览 ====================

    /**
     * 获取简历导出预览（GET /profile/resume/export/preview）
     */
    @Transactional(readOnly = true)
    public ExportPreviewResponse getExportPreview(Long userId) {
        List<SectionItem> sections = Arrays.asList(
                SectionItem.builder().code("education").name("教育背景").selected(true).disabled(true).build(),
                SectionItem.builder().code("awards").name("获奖情况").selected(true).disabled(false).build(),
                SectionItem.builder().code("skills").name("技能与兴趣").selected(true).disabled(false).build(),
                SectionItem.builder().code("practices").name("实践经历").selected(true).disabled(false).build(),
                SectionItem.builder().code("certificates").name("证书").selected(true).disabled(false).build(),
                SectionItem.builder().code("selfEvaluation").name("自我评价").selected(true).disabled(false).build()
        );
        return ExportPreviewResponse.builder()
                .sections(sections)
                .dataVersion(buildDataVersion(userId))
                .generatedAt(toIso(LocalDateTime.now()))
                .build();
    }

    // ==================== 提交导出 ====================

    /**
     * 提交简历导出（POST /profile/resume/export）
     */
    @Transactional
    public ExportSubmitResponse submitExport(Long userId, ExportRequest request) {
        // sections 不传或为空时默认全栏目导出
        List<String> sections = (request.getSections() == null || request.getSections().isEmpty())
                ? DEFAULT_SECTIONS : request.getSections();
        List<String> invalidSections = sections.stream()
                .filter(s -> !ALLOWED_SECTIONS.contains(s))
                .collect(Collectors.toList());
        if (!invalidSections.isEmpty()) {
            log.warn("简历导出栏目校验失败 userId={} sections={} invalid={}",
                    userId, sections, invalidSections);
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "导出栏目不存在：" + String.join("、", invalidSections)
                            + "，支持：" + String.join("、", ALLOWED_SECTIONS));
        }
        String fileType = request.getFileType() != null ? request.getFileType() : "pdf";
        if (!"pdf".equals(fileType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "暂仅支持PDF格式导出");
        }
        String purpose = request.getPurpose() != null ? request.getPurpose() : "external";
        if (!"internal".equals(purpose) && !"external".equals(purpose)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用途参数只能是 internal 或 external");
        }
        boolean watermarkEnabled = "internal".equals(purpose);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
        LocalDateTime now = LocalDateTime.now();
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;

        ExportTemplate template = exportTemplateRenderService.resolveDefaultTemplate(schoolId, "resume");
        if (template == null) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "未配置简历导出模板，请联系管理员");
        }

        ExportJob job = new ExportJob();
        job.setSchoolId(schoolId);
        job.setTemplateId(template.getId());
        job.setOperatorId(userId);
        job.setExportType("resume");
        job.setScopeType(1);
        job.setScopeId(schoolId);
        job.setStatus(ExportTaskStatusEnum.PENDING.getValue());
        job.setTotalCount(sections.size());
        job.setSuccessCount(0);
        job.setExpireAt(now.plusDays(7));
        job.setFilterConditions(writeJson(Map.of("sections", sections, "fileType", fileType, "purpose", purpose)));
        job = exportJobRepository.save(job);

        byte[] pdfBytes;
        try {
            pdfBytes = exportTemplateRenderService.renderTemplate(template,
                    buildResumeContext(userId, sections, purpose), watermarkEnabled);
        } catch (Exception e) {
            log.error("简历PDF生成失败 userId={} jobId={}", userId, job.getId(), e);
            job.setStatus(ExportTaskStatusEnum.FAILED.getValue());
            job.setErrorMsg(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "简历导出生成失败");
        }

        String originalName = "个人简历.pdf";
        String objectKey;
        try {
            objectKey = ossFileService.uploadBytes(pdfBytes, "application/pdf",
                    AttachmentBizTypeEnum.RESUME_EXPORT.getValue(), "pdf", originalName);
        } catch (Exception e) {
            log.error("简历导出文件上传OSS失败 userId={} jobId={}", userId, job.getId(), e);
            job.setStatus(ExportTaskStatusEnum.FAILED.getValue());
            job.setErrorMsg(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
            throw new BusinessException(ResultCode.THIRD_OSS_FAILED, "导出文件上传失败");
        }

        AttachmentRelation relation = new AttachmentRelation();
        relation.setUserId(userId);
        relation.setBizType(AttachmentBizTypeEnum.RESUME_EXPORT.getValue());
        relation.setBizId(job.getId());
        relation.setFileCategory("pdf");
        relation.setOriginalName(originalName);
        relation.setFilePath(objectKey);
        relation.setFileSize((long) pdfBytes.length);
        relation.setMimeType("application/pdf");
        relation.setDisk("oss");
        relation.setConvertStatus(0);
        relation.setSortOrder(0);
        relation.setFileStatus(FileStatusEnum.BOUND.getValue());
        // 下载有效期与 export_jobs.expire_at 对齐（7 天），通用下载端点将据此校验过期
        relation.setDownloadExpireAt(job.getExpireAt());
        relation = attachmentRelationRepository.save(relation);

        job.setStatus(ExportTaskStatusEnum.COMPLETED.getValue());
        job.setSuccessCount(1);
        job.setFileId(relation.getId());
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);

        ExportOperationLog opLog = new ExportOperationLog();
        opLog.setSchoolId(schoolId);
        opLog.setOperatorId(userId);
        opLog.setExportType("resume");
        opLog.setAction(1);
        opLog.setScopeType(1);
        opLog.setScopeId(schoolId);
        opLog.setFilterConditions(job.getFilterConditions());
        opLog.setRecordCount(1);
        opLog.setIsAnonymized(0);
        opLog.setDataVersion(1);
        opLog.setFileId(relation.getId());
        opLog.setStatus(1);
        exportOperationLogRepository.save(opLog);

        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setSenderType(1);
        message.setCategory("system_notice");
        message.setTitle("简历导出完成");
        message.setContent("您的个人简历已生成，可立即下载（7 天内有效）。");
        message.setRelatedType("resume");
        message.setRelatedId(job.getId());
        message.setJumpUrl("/profile/resume/export");
        message.setIsRead(0);
        message.setIsArchived(0);
        message.setIsImportant(0);
        userMessageRepository.save(message);

        String downloadUrl = ossFileService.getFileUrl(relation.getFilePath(), originalName);
        return ExportSubmitResponse.builder()
                .exportJobId(job.getId())
                .status(ExportTaskStatusEnum.COMPLETED.getValue())
                .statusLabel(ExportTaskStatusEnum.COMPLETED.getLabel())
                .fileId(relation.getId())
                .downloadUrl(downloadUrl)
                .originalName(originalName)
                .expireAt(toIso(job.getExpireAt()))
                .build();
    }

    // ==================== 模板渲染上下文 ====================

    /**
     * 构建简历模板渲染上下文
     *
     * @param purpose 导出用途：internal 内部预览（电话号码脱敏、加水印），external 外发（完整号码、无水印）
     */
    private Map<String, Object> buildResumeContext(Long userId, List<String> sections, String purpose) {
        User user = userRepository.findById(userId).orElse(null);
        StudentProfile profile = (user != null) ? studentProfileRepository.findByUserId(userId).orElse(null) : null;
        Clazz clazz = (profile != null && profile.getClassId() != null)
                ? clazzRepository.findById(profile.getClassId()).orElse(null) : null;
        Major major = (clazz != null && clazz.getMajorId() != null)
                ? majorRepository.findById(clazz.getMajorId()).orElse(null) : null;
        College college = (major != null && major.getCollegeId() != null)
                ? collegeRepository.findById(major.getCollegeId()).orElse(null) : null;
        UserContactInfo contact = userContactInfoRepository.findByUserId(userId).orElse(null);
        Long schoolId = user != null && user.getSchoolId() != null ? user.getSchoolId() : 1L;
        School school = schoolRepository.findById(schoolId).orElse(null);

        String majorName = major != null ? major.getName() : null;
        String gradeName = clazz != null ? clazz.getGrade() : null;
        String degreeType = profile != null ? profile.getDegreeType() : null;
        String studentStatus = profile != null ? profile.getStudentStatus() : null;
        boolean internal = "internal".equals(purpose);

        Set<String> sectionSet = Set.copyOf(sections);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studentName", user != null ? user.getName() : null);
        context.put("userNo", user != null ? user.getUserNo() : null);
        context.put("title", buildResumeTitle(studentStatus, majorName));
        String phone = contact != null ? contact.getPhone() : null;
        context.put("phone", internal ? maskPhone(phone) : phone);
        context.put("email", contact != null ? contact.getEmail() : null);
        context.put("grade", gradeName);
        context.put("clazz", clazz != null ? clazz.getName() : null);
        context.put("schoolName", school != null ? school.getName() : null);
        context.put("college", college != null ? college.getName() : null);
        context.put("major", majorName);
        context.put("degreeType", degreeType);
        context.put("degreeTypeLabel", labelOfDegreeType(degreeType));
        context.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        context.put("dataVersion", buildDataVersion(userId));

        context.put("showEducation", sectionSet.contains("education"));
        context.put("showAwards", sectionSet.contains("awards"));
        context.put("showSkills", sectionSet.contains("skills"));
        context.put("showPractices", sectionSet.contains("practices"));
        context.put("showCertificates", sectionSet.contains("certificates"));
        context.put("showSelfEvaluation", sectionSet.contains("selfEvaluation"));

        // 教育背景：学期 GPA
        if (sectionSet.contains("education")) {
            context.put("gpas", buildGpaItems(userId));
        } else {
            context.put("gpas", Collections.emptyList());
        }

        // 获奖情况
        if (sectionSet.contains("awards")) {
            context.put("awards", buildAwardItems(userId));
        } else {
            context.put("awards", Collections.emptyList());
        }

        // 技能与兴趣
        if (sectionSet.contains("skills")) {
            context.put("skillCategories", buildSkillCategories(userId));
        } else {
            context.put("skillCategories", Collections.emptyList());
        }

        // 实践经历（已审核通过）
        if (sectionSet.contains("practices")) {
            context.put("practices", buildPracticeItems(userId));
        } else {
            context.put("practices", Collections.emptyList());
        }

        // 证书
        if (sectionSet.contains("certificates")) {
            context.put("certificates", buildCertificateItems(userId));
        } else {
            context.put("certificates", Collections.emptyList());
        }

        // 自我评价
        if (sectionSet.contains("selfEvaluation")) {
            context.put("selfEvaluation", profile != null ? profile.getSelfEvaluation() : null);
        } else {
            context.put("selfEvaluation", null);
        }

        return context;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 拼接简历副标题：学生状态 + 专业，如"应届毕业生 · 计算机科学与技术"。
     */
    private String buildResumeTitle(String studentStatus, String majorName) {
        String statusLabel = labelOfStudentStatus(studentStatus);
        if (statusLabel != null && majorName != null) {
            return statusLabel + " · " + majorName;
        }
        return majorName != null ? majorName : "";
    }

    private String labelOfDegreeType(String degreeType) {
        DegreeTypeEnum e = DegreeTypeEnum.of(degreeType);
        return e != null ? e.getLabel() : null;
    }

    private String labelOfStudentStatus(String studentStatus) {
        StudentStatusEnum e = StudentStatusEnum.of(studentStatus);
        return e != null ? e.getLabel() : null;
    }

    private List<Map<String, Object>> buildGpaItems(Long userId) {
        List<SemesterGpaSummary> summaries = semesterGpaSummaryRepository.findByUserId(userId);
        if (summaries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> semesterNameMap = summaries.stream()
                .map(SemesterGpaSummary::getSemesterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                        sid -> sid,
                        sid -> semesterRepository.findById(sid).map(Semester::getName).orElse(""),
                        (a, b) -> a));
        return summaries.stream()
                .filter(s -> s.getSemesterId() != null)
                .sorted(java.util.Comparator.comparingLong(SemesterGpaSummary::getSemesterId))
                .map(s -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    String semesterName = generateSemesterLabel(semesterNameMap.get(s.getSemesterId()));
                    item.put("semesterName", semesterName);
                    item.put("gpa", cleanDecimal(s.getWeightedGpa()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAwardItems(Long userId) {
        List<AwardApplication> applications = awardApplicationRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 2)
                .collect(Collectors.toList());
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> applicationIds = applications.stream().map(AwardApplication::getId).collect(Collectors.toList());
        Map<Long, AwardCompetitionStar> competitionMap = awardCompetitionStarRepository
                .findByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(AwardCompetitionStar::getApplicationId, s -> s, (a, b) -> a));
        Map<Long, AwardInnovationStar> innovationMap = awardInnovationStarRepository
                .findByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(AwardInnovationStar::getApplicationId, s -> s, (a, b) -> a));
        Map<Long, AwardResearchStar> researchMap = awardResearchStarRepository
                .findByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(AwardResearchStar::getApplicationId, s -> s, (a, b) -> a));

        List<Long> researchStarIds = researchMap.values().stream()
                .map(AwardResearchStar::getId).collect(Collectors.toList());
        Map<Long, AwardResearchProject> projectMap = researchStarIds.isEmpty() ? Collections.emptyMap()
                : awardResearchProjectRepository.findByResearchStarIdIn(researchStarIds).stream()
                .collect(Collectors.toMap(AwardResearchProject::getResearchStarId, p -> p, (a, b) -> a));

        return applications.stream().map(app -> {
            LocalDate passedAt = app.getAuditInfo() != null && app.getAuditInfo().getPassedAt() != null
                    ? app.getAuditInfo().getPassedAt().toLocalDate() : null;
            AwardCompetitionStar competition = competitionMap.get(app.getId());
            if (competition != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", isNotBlank(competition.getCompetitionName())
                        ? competition.getCompetitionName() : app.getTitle());
                item.put("level", toEnglishCompetitionLevel(competition.getCompetitionLevel()));
                item.put("awardName", competition.getAwardLevel());
                item.put("awardLevel", competition.getAwardLevel());
                item.put("date", competition.getParticipatedAt() != null
                        ? competition.getParticipatedAt().toString()
                        : (passedAt != null ? passedAt.toString() : null));
                return item;
            }
            AwardInnovationStar innovation = innovationMap.get(app.getId());
            if (innovation != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", isNotBlank(innovation.getCompanyName())
                        ? innovation.getCompanyName() : app.getTitle());
                item.put("level", null);
                item.put("awardName", innovation.getApplicantRank());
                item.put("awardLevel", null);
                item.put("date", innovation.getRegisteredAt() != null
                        ? innovation.getRegisteredAt().toString()
                        : (passedAt != null ? passedAt.toString() : null));
                return item;
            }
            AwardResearchStar research = researchMap.get(app.getId());
            AwardResearchProject project = research != null ? projectMap.get(research.getId()) : null;
            if (research != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", app.getTitle());
                item.put("level", project != null ? toEnglishCompetitionLevel(project.getProjectLevel()) : null);
                item.put("awardName", project != null ? project.getProjectName() : null);
                item.put("awardLevel", null);
                item.put("date", project != null && project.getEstablishedAt() != null
                        ? project.getEstablishedAt().toString()
                        : (passedAt != null ? passedAt.toString() : null));
                return item;
            }
            // 扩展表未关联明细时仅输出报名基表字段，不构造占位等级/获奖级别
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", app.getTitle());
            item.put("level", null);
            item.put("awardName", null);
            item.put("awardLevel", null);
            item.put("date", passedAt != null ? passedAt.toString() : null);
            return item;
        }).collect(Collectors.toList());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 将竞赛/奖项等级（校级/省级/国家级等）转换为英文代码，用于简历模板 badge 展示。
     * 若已是英文或无法识别则原样返回。
     */
    private String toEnglishCompetitionLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        String normalized = level.trim().toLowerCase();
        if (normalized.contains("international") || normalized.contains("国际")) {
            return "international";
        }
        if (normalized.contains("national") || normalized.contains("国家")) {
            return "national";
        }
        if (normalized.contains("provincial") || normalized.contains("省级") || normalized.contains("省")) {
            return "provincial";
        }
        if (normalized.contains("municipal") || normalized.contains("city") || normalized.contains("市级")
                || normalized.contains("市")) {
            return "municipal";
        }
        if (normalized.contains("school") || normalized.contains("校级") || normalized.contains("校")) {
            return "school";
        }
        if (normalized.contains("academy") || normalized.contains("college") || normalized.contains("院级")
                || normalized.contains("院")) {
            return "academy";
        }
        return level;
    }

    private List<Map<String, Object>> buildSkillCategories(Long userId) {
        List<UserInterest> interests = userInterestRepository.findByUserIdOrderBySortAsc(userId);
        if (interests.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 tagName 合并同类兴趣，明细项去重，熟练度取最高等级
        Map<String, List<UserInterest>> grouped = interests.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getTagName() != null ? i.getTagName().trim() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grouped.entrySet().stream()
                .filter(e -> !e.getKey().isEmpty())
                .map(e -> {
                    String category = e.getKey();
                    List<UserInterest> list = e.getValue();
                    Set<String> itemNames = new LinkedHashSet<>();
                    int maxProficiencyLevel = list.stream()
                            .mapToInt(i -> i.getProficiencyLevel() != null ? i.getProficiencyLevel() : 2)
                            .max().orElse(2);
                    String proficiency = PROFICIENCY_LABELS.getOrDefault(maxProficiencyLevel, "一般");

                    for (UserInterest i : list) {
                        if (i.getDetailContent() != null && !i.getDetailContent().isBlank()) {
                            Arrays.stream(i.getDetailContent().split("[,，、]"))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .forEach(itemNames::add);
                        } else {
                            itemNames.add(i.getTagName());
                        }
                    }

                    List<Map<String, String>> items = itemNames.stream()
                            .map(name -> Map.of("name", name, "proficiency", proficiency))
                            .collect(Collectors.toList());
                    String joinedItems = String.join("、", itemNames);
                    String display = category + " " + joinedItems + " " + proficiency;

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("category", category);
                    result.put("proficiency", proficiency);
                    result.put("items", items);
                    result.put("joinedItems", joinedItems);
                    result.put("display", display);
                    return result;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPracticeItems(Long userId) {
        List<Archive> approvedArchives = archiveRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 2)
                .collect(Collectors.toList());
        Set<Long> certificateArchiveIds = certificateArchiveIds(approvedArchives);
        return approvedArchives.stream()
                .filter(a -> !CERTIFICATE_ARCHIVE_TYPE.equals(a.getArchiveType()))
                .filter(a -> !certificateArchiveIds.contains(a.getId()))
                .filter(a -> !looksLikeCertificate(a))
                .sorted((a, b) -> {
                    LocalDate da = a.getObtainedAt();
                    LocalDate db = b.getObtainedAt();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .map(a -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("title", a.getTitle());
                    item.put("date", a.getObtainedAt() != null ? a.getObtainedAt().toString() : null);
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCertificateItems(Long userId) {
        List<Archive> approvedArchives = archiveRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 2)
                .collect(Collectors.toList());
        Map<Long, ArchiveCertificate> certificateMap = certificateMap(approvedArchives);
        return approvedArchives.stream()
                .filter(a -> CERTIFICATE_ARCHIVE_TYPE.equals(a.getArchiveType())
                        || certificateMap.containsKey(a.getId())
                        || looksLikeCertificate(a))
                .map(a -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    ArchiveCertificate certificate = certificateMap.get(a.getId());
                    String name = (certificate != null && certificate.getCertificateName() != null
                            && !certificate.getCertificateName().isBlank())
                            ? certificate.getCertificateName() : a.getTitle();
                    String type = (certificate != null && certificate.getCertificateType() != null
                            && !certificate.getCertificateType().isBlank())
                            ? certificate.getCertificateType() : a.getArchiveType();
                    item.put("name", name);
                    item.put("type", type);
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据标题兜底识别证书类档案（如“大学英语六级证书”），避免仅依赖 archive_type 或扩展表记录时漏分。
     */
    private boolean looksLikeCertificate(Archive archive) {
        if (archive == null || archive.getTitle() == null) {
            return false;
        }
        String title = archive.getTitle().trim();
        return title.contains("证书") || title.contains("资格证") || title.contains("CET")
                || title.contains("普通话") || title.contains("计算机等级");
    }

    private Set<Long> certificateArchiveIds(List<Archive> archives) {
        if (archives.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> archiveIds = archives.stream().map(Archive::getId).collect(Collectors.toList());
        return archiveCertificateRepository.findByArchiveIdIn(archiveIds).stream()
                .map(ArchiveCertificate::getArchiveId)
                .collect(Collectors.toSet());
    }

    private Map<Long, ArchiveCertificate> certificateMap(List<Archive> archives) {
        if (archives.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> archiveIds = archives.stream().map(Archive::getId).collect(Collectors.toList());
        return archiveCertificateRepository.findByArchiveIdIn(archiveIds).stream()
                .collect(Collectors.toMap(ArchiveCertificate::getArchiveId, c -> c, (a, b) -> a));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据当前学期画像评分规则版本生成数据版本标识
     */
    private String buildDataVersion(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        Long schoolId = user != null && user.getSchoolId() != null ? user.getSchoolId() : 1L;
        return "v1-" + LocalDate.now().format(FILE_DATE_FORMAT);
    }

    /**
     * 学期名称转展示标签，如 "2022-2023-1" → "2022-2023第一学期"
     */
    private String generateSemesterLabel(String name) {
        if (name == null || name.isBlank()) return name;
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    private BigDecimal cleanDecimal(BigDecimal value) {
        if (value == null) return null;
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
