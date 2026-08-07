package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.profile.request.ExportRequest;
import com.example.studentarchives.dto.Fmy.profile.response.ExportPreviewResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ExportPreviewResponse.SectionItem;
import com.example.studentarchives.dto.Fmy.profile.response.ExportSubmitResponse;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.evaluation.AwardSummary;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.export.ExportJob;
import com.example.studentarchives.entity.export.ExportOperationLog;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.enums.AttachmentBizTypeEnum;
import com.example.studentarchives.enums.ExportTaskStatusEnum;
import com.example.studentarchives.enums.FileStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.AwardSummaryRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.ExportJobRepository;
import com.example.studentarchives.repository.ExportOperationLogRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 档案导出服务
 * <p>
 * 提供学生端个人中心档案导出接口（《学生端接口文档》四、4.16~4.17）：
 * 导出预览、提交导出（同步生成 PDF 上传 OSS，写审计日志并通知学生）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileExportService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 导出文件名日期格式：20260701 */
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 支持导出的栏目编码 */
    private static final Set<String> ALLOWED_SECTIONS = Set.of(
            "academicInfo", "dimensionScores", "awards", "practices", "careerPlans");

    /** 档案默认导出栏目（不传 sections 时全选，顺序与导出预览一致） */
    private static final List<String> DEFAULT_SECTIONS = List.of(
            "academicInfo", "dimensionScores", "awards", "practices", "careerPlans");

    private final ExportJobRepository exportJobRepository;
    private final ExportOperationLogRepository exportOperationLogRepository;
    private final ExportTemplateRenderService exportTemplateRenderService;
    private final SchoolRepository schoolRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final UserMessageRepository userMessageRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final SemesterRepository semesterRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final AwardSummaryRepository awardSummaryRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final ArchiveRepository archiveRepository;
    private final OssFileService ossFileService;
    private final ObjectMapper objectMapper;

    // ==================== 导出预览（4.16） ====================

    /**
     * 获取档案导出预览（GET /profile/export/preview）
     * <p>
     * 返回可选导出栏目（配置类常量，非伪造数据）。
     */
    @Transactional(readOnly = true)
    public ExportPreviewResponse getExportPreview(Long userId) {
        List<SectionItem> sections = Arrays.asList(
                SectionItem.builder().code("academicInfo").name("学籍信息").selected(true).disabled(true).build(),
                SectionItem.builder().code("dimensionScores").name("画像分数").selected(true).disabled(false).build(),
                SectionItem.builder().code("awards").name("获奖记录").selected(true).disabled(false).build(),
                SectionItem.builder().code("practices").name("实践经历").selected(true).disabled(false).build(),
                SectionItem.builder().code("careerPlans").name("成长规划").selected(true).disabled(false).build()
        );
        return ExportPreviewResponse.builder()
                .sections(sections)
                .dataVersion(buildDataVersion(userId))
                .generatedAt(toIso(LocalDateTime.now()))
                .build();
    }

    /**
     * 根据当前学期画像评分的指标规则版本生成数据版本标识（如 "20260701-v3"）
     */
    private String buildDataVersion(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        Long schoolId = user != null && user.getSchoolId() != null ? user.getSchoolId() : 1L;
        Semester semester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);
        if (semester == null) {
            return "v1-" + LocalDate.now().format(FILE_DATE_FORMAT);
        }
        Integer ruleVersion = portraitEvaluationScoreRepository
                .findByUserIdAndSemesterId(userId, semester.getId()).stream()
                .map(PortraitEvaluationScore::getRuleVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(1);
        return "v" + ruleVersion + "-" + LocalDate.now().format(FILE_DATE_FORMAT);
    }

    // ==================== 提交导出（4.17） ====================

    /**
     * 提交档案导出（POST /profile/export）
     * <p>
     * 同步生成学生成长档案 PDF：创建 export_jobs 任务 → OpenPDF 渲染 →
     * 上传 OSS 并绑定 file_uploads → 更新任务完成 → 写 export_operation_logs 审计 →
     * 通过 user_messages 通知学生。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 导出任务结果（按文档返回待处理形态）
     */
    @Transactional
    public ExportSubmitResponse submitExport(Long userId, ExportRequest request) {
        // sections 不传或为空时默认全栏目导出
        List<String> sections = (request.getSections() == null || request.getSections().isEmpty())
                ? DEFAULT_SECTIONS : request.getSections();
        // 校验栏目与文件类型
        List<String> invalidSections = sections.stream()
                .filter(s -> !ALLOWED_SECTIONS.contains(s))
                .collect(Collectors.toList());
        if (!invalidSections.isEmpty()) {
            log.warn("档案导出栏目校验失败 userId={} sections={} invalid={}",
                    userId, sections, invalidSections);
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "导出栏目不存在：" + String.join("、", invalidSections)
                            + "，支持：" + String.join("、", ALLOWED_SECTIONS));
        }
        String fileType = request.getFileType() != null ? request.getFileType() : "pdf";
        if (!"pdf".equals(fileType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "暂仅支持PDF格式导出");
        }
        // 档案导出支持 purpose 选择是否带水印：internal 屏幕水印+打印隐藏，external（默认）屏幕与打印均无水印
        String purpose = request.getPurpose() != null ? request.getPurpose() : "external";
        if (!"internal".equals(purpose) && !"external".equals(purpose)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用途参数只能是 internal 或 external");
        }
        boolean watermarkEnabled = "internal".equals(purpose);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
        LocalDateTime now = LocalDateTime.now();
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;

        // 解析默认导出模板（无模板时回退旧版 OpenPDF 渲染）
        ExportTemplate template = exportTemplateRenderService.resolveDefaultTemplate(schoolId, "student_archive");

        // 创建导出任务
        ExportJob job = new ExportJob();
        job.setSchoolId(schoolId);
        job.setTemplateId(template != null ? template.getId() : null);
        job.setOperatorId(userId);
        job.setExportType("student_archive");
        job.setScopeType(1);
        job.setScopeId(schoolId);
        job.setStatus(ExportTaskStatusEnum.PENDING.getValue());
        job.setTotalCount(sections.size());
        job.setSuccessCount(0);
        job.setExpireAt(now.plusDays(7));
        job.setFilterConditions(writeJson(Map.of("sections", sections, "fileType", fileType, "purpose", purpose)));
        job = exportJobRepository.save(job);

        // 渲染 PDF（external 无水印；无模板时回退 OpenPDF 渲染，该路径不含水印）
        byte[] pdfBytes;
        try {
            if (template != null) {
                pdfBytes = exportTemplateRenderService.renderTemplate(template,
                        buildArchiveContext(userId, sections), watermarkEnabled);
            } else {
                pdfBytes = renderStudentArchivePdf(userId, sections);
            }
        } catch (Exception e) {
            log.error("导出PDF生成失败 userId={} jobId={}", userId, job.getId(), e);
            job.setStatus(ExportTaskStatusEnum.FAILED.getValue());
            job.setErrorMsg(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "档案导出生成失败");
        }

        // 上传 OSS
        String originalName = "学生成长档案-" + LocalDate.now().format(FILE_DATE_FORMAT) + ".pdf";
        String objectKey;
        try {
            objectKey = ossFileService.uploadBytes(pdfBytes, "application/pdf",
                    AttachmentBizTypeEnum.STUDENT_ARCHIVE_EXPORT.getValue(), "pdf", originalName);
        } catch (Exception e) {
            log.error("导出文件上传OSS失败 userId={} jobId={}", userId, job.getId(), e);
            job.setStatus(ExportTaskStatusEnum.FAILED.getValue());
            job.setErrorMsg(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
            throw new BusinessException(ResultCode.THIRD_OSS_FAILED, "导出文件上传失败");
        }

        // 绑定 file_uploads
        AttachmentRelation relation = new AttachmentRelation();
        relation.setUserId(userId);
        relation.setBizType(AttachmentBizTypeEnum.STUDENT_ARCHIVE_EXPORT.getValue());
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

        // 更新任务完成
        job.setStatus(ExportTaskStatusEnum.COMPLETED.getValue());
        job.setSuccessCount(1);
        job.setFileId(relation.getId());
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);

        // 导出操作日志（action=1 创建）
        ExportOperationLog opLog = new ExportOperationLog();
        opLog.setSchoolId(schoolId);
        opLog.setOperatorId(userId);
        opLog.setExportType("student_archive");
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

        // 消息通知（分类对齐接口文档 5.1 枚举，避免消息列表漏显）
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setSenderType(1);
        message.setCategory("system_notice");
        message.setTitle("档案导出完成");
        message.setContent("您的学生成长档案已生成，可立即下载（7 天内有效）。");
        message.setRelatedType("student_archive");
        message.setRelatedId(job.getId());
        message.setJumpUrl("/profile/export");
        message.setIsRead(0);
        message.setIsArchived(0);
        message.setIsImportant(0);
        userMessageRepository.save(message);

        // 导出为同步生成：直接返回已完成状态与可立即下载的签名链接
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
     * 构建模板渲染上下文（与旧版渲染方法数据口径一致，实践经历取已审核通过数据）。
     * 未选择的栏目不装配数据（列表置空），由模板 {{#showXxx}} + {{^list}} 分支控制显隐。
     */
    private Map<String, Object> buildArchiveContext(Long userId, List<String> sections) {
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

        Set<String> sectionSet = Set.copyOf(sections);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studentName", user != null ? user.getName() : null);
        context.put("userNo", user != null ? user.getUserNo() : null);
        context.put("schoolName", school != null ? school.getName() : null);
        context.put("college", college != null ? college.getName() : null);
        context.put("major", major != null ? major.getName() : null);
        context.put("clazz", clazz != null ? clazz.getName() : null);
        context.put("grade", clazz != null ? clazz.getGrade() : null);
        context.put("politicalStatus", profile != null ? profile.getPoliticalStatus() : null);
        context.put("email", contact != null ? contact.getEmail() : null);
        context.put("phone", contact != null ? contact.getPhone() : null);
        context.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        context.put("dataVersion", buildDataVersion(userId));

        context.put("showAcademicInfo", sectionSet.contains("academicInfo"));
        context.put("showDimensionScores", sectionSet.contains("dimensionScores"));
        context.put("showAwards", sectionSet.contains("awards"));
        context.put("showPractices", sectionSet.contains("practices"));
        context.put("showCareerPlans", sectionSet.contains("careerPlans"));

        // 画像分数（当前学期）
        if (sectionSet.contains("dimensionScores")) {
            Semester semester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);
            List<PortraitEvaluationScore> scores = (semester != null)
                    ? portraitEvaluationScoreRepository.findByUserIdAndSemesterId(userId, semester.getId())
                    : Collections.emptyList();
            Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                    .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                            AbilityDimension::getDimensionName, (a, b) -> a));
            List<Map<String, Object>> scoreItems = scores.stream().map(s -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("dimensionName", dimensionNameMap.getOrDefault(s.getDimensionCode(), s.getDimensionCode()));
                item.put("score", cleanDecimal(s.getScore()));
                item.put("targetScore", cleanDecimal(s.getTargetScore()));
                item.put("gap", cleanDecimal(s.getGap()));
                return item;
            }).collect(Collectors.toList());
            context.put("scores", scoreItems);
        } else {
            context.put("scores", Collections.emptyList());
        }

        // 获奖记录
        if (sectionSet.contains("awards")) {
            List<Map<String, Object>> awardItems = awardSummaryRepository.findByUserId(userId).stream().map(a -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("category", a.getCategory());
                item.put("totalCount", a.getTotalCount());
                item.put("maxLevel", a.getMaxLevel());
                item.put("latestAt", a.getLatestAt() != null ? String.valueOf(a.getLatestAt()) : null);
                return item;
            }).collect(Collectors.toList());
            context.put("awards", awardItems);
        } else {
            context.put("awards", Collections.emptyList());
        }

        // 实践经历（仅已审核通过，status==2）
        if (sectionSet.contains("practices")) {
            List<Map<String, Object>> practiceItems = archiveRepository.findByUserId(userId).stream()
                    .filter(a -> a.getStatus() != null && a.getStatus() == 2)
                    .map(a -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("title", a.getTitle());
                        item.put("archiveType", a.getArchiveType());
                        item.put("obtainedAt", a.getObtainedAt() != null ? String.valueOf(a.getObtainedAt()) : null);
                        return item;
                    })
                    .collect(Collectors.toList());
            context.put("practices", practiceItems);
        } else {
            context.put("practices", Collections.emptyList());
        }

        // 成长规划
        if (sectionSet.contains("careerPlans")) {
            List<Map<String, Object>> planItems = careerPlanRepository.findByUserId(userId).stream().map(p -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", p.getTitle());
                item.put("progressRate", p.getProgressRate() != null ? p.getProgressRate() : 0);
                return item;
            }).collect(Collectors.toList());
            context.put("careerPlans", planItems);
        } else {
            context.put("careerPlans", Collections.emptyList());
        }
        return context;
    }

    // ==================== PDF 渲染 ====================

    /**
     * 渲染学生成长档案 PDF（仅包含所选栏目，实践经历取已审核通过数据）
     */
    private byte[] renderStudentArchivePdf(Long userId, List<String> sections) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("学生成长档案", pdfFont(18, Font.BOLD)));
        document.add(new Paragraph("导出时间：" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), pdfFont(10)));
        document.add(new Paragraph(" "));

        Set<String> sectionSet = Set.copyOf(sections);
        if (sectionSet.contains("academicInfo")) {
            renderAcademicInfo(document, userId);
        }
        if (sectionSet.contains("dimensionScores")) {
            renderDimensionScores(document, userId);
        }
        if (sectionSet.contains("awards")) {
            renderAwards(document, userId);
        }
        if (sectionSet.contains("practices")) {
            renderPractices(document, userId);
        }
        if (sectionSet.contains("careerPlans")) {
            renderCareerPlans(document, userId);
        }

        document.close();
        return out.toByteArray();
    }

    private void renderAcademicInfo(Document document, Long userId) throws Exception {
        document.add(new Paragraph("一、学籍信息", pdfFont(14, Font.BOLD)));
        User user = userRepository.findById(userId).orElse(null);
        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
        Clazz clazz = (profile != null && profile.getClassId() != null)
                ? clazzRepository.findById(profile.getClassId()).orElse(null) : null;
        Major major = (clazz != null && clazz.getMajorId() != null)
                ? majorRepository.findById(clazz.getMajorId()).orElse(null) : null;
        College college = (major != null && major.getCollegeId() != null)
                ? collegeRepository.findById(major.getCollegeId()).orElse(null) : null;
        UserContactInfo contact = userContactInfoRepository.findByUserId(userId).orElse(null);

        addLine(document, "姓名", user != null ? user.getName() : null);
        addLine(document, "学号", user != null ? user.getUserNo() : null);
        addLine(document, "学院", college != null ? college.getName() : null);
        addLine(document, "专业", major != null ? major.getName() : null);
        addLine(document, "班级", clazz != null ? clazz.getName() : null);
        addLine(document, "年级", clazz != null ? clazz.getGrade() : null);
        addLine(document, "政治面貌", profile != null ? profile.getPoliticalStatus() : null);
        addLine(document, "邮箱", contact != null ? contact.getEmail() : null);
        addLine(document, "手机", contact != null ? contact.getPhone() : null);
        document.add(new Paragraph(" "));
    }

    private void renderDimensionScores(Document document, Long userId) throws Exception {
        document.add(new Paragraph("二、画像分数", pdfFont(14, Font.BOLD)));
        User user = userRepository.findById(userId).orElse(null);
        Long schoolId = user != null && user.getSchoolId() != null ? user.getSchoolId() : 1L;
        Semester semester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);
        List<PortraitEvaluationScore> scores = (semester != null)
                ? portraitEvaluationScoreRepository.findByUserIdAndSemesterId(userId, semester.getId())
                : Collections.emptyList();
        Map<String, String> dimensionNameMap = abilityDimensionRepository.findAllActive().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));
        if (scores.isEmpty()) {
            document.add(new Paragraph("暂无画像分数数据", pdfFont(12)));
        } else {
            for (PortraitEvaluationScore s : scores) {
                addLine(document, dimensionNameMap.getOrDefault(s.getDimensionCode(), s.getDimensionCode()),
                        "得分 " + cleanDecimal(s.getScore()) + " / 目标 " + cleanDecimal(s.getTargetScore())
                                + "（差距 " + cleanDecimal(s.getGap()) + "）");
            }
        }
        document.add(new Paragraph(" "));
    }

    private void renderAwards(Document document, Long userId) throws Exception {
        document.add(new Paragraph("三、获奖记录", pdfFont(14, Font.BOLD)));
        List<AwardSummary> awards = awardSummaryRepository.findByUserId(userId);
        if (awards.isEmpty()) {
            document.add(new Paragraph("暂无获奖记录", pdfFont(12)));
        } else {
            for (AwardSummary award : awards) {
                addLine(document, award.getCategory(),
                        award.getTotalCount() + " 项 / 最高" + award.getMaxLevel()
                                + (award.getLatestAt() != null ? " / 最近 " + award.getLatestAt() : ""));
            }
        }
        document.add(new Paragraph(" "));
    }

    private void renderPractices(Document document, Long userId) throws Exception {
        document.add(new Paragraph("四、实践经历（已审核通过）", pdfFont(14, Font.BOLD)));
        List<com.example.studentarchives.entity.archive.Archive> archives = archiveRepository.findByUserId(userId)
                .stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 2)
                .collect(Collectors.toList());
        if (archives.isEmpty()) {
            document.add(new Paragraph("暂无已通过的实践经历", pdfFont(12)));
        } else {
            for (com.example.studentarchives.entity.archive.Archive a : archives) {
                addLine(document, a.getTitle(),
                        a.getArchiveType() + (a.getObtainedAt() != null ? " / " + a.getObtainedAt() : ""));
            }
        }
        document.add(new Paragraph(" "));
    }

    private void renderCareerPlans(Document document, Long userId) throws Exception {
        document.add(new Paragraph("五、成长规划", pdfFont(14, Font.BOLD)));
        List<CareerPlan> plans = careerPlanRepository.findByUserId(userId);
        if (plans.isEmpty()) {
            document.add(new Paragraph("暂无成长规划", pdfFont(12)));
        } else {
            for (CareerPlan plan : plans) {
                addLine(document, plan.getTitle(),
                        "进度 " + (plan.getProgressRate() != null ? plan.getProgressRate() : 0) + "%");
            }
        }
        document.add(new Paragraph(" "));
    }

    /**
     * 输出 "标签：值" 行
     */
    private void addLine(Document document, String label, String value) throws Exception {
        document.add(new Paragraph((label != null ? label : "") + "：" + (value != null ? value : ""), pdfFont(12)));
    }

    /**
     * 中文字体（STSong-Light），失败时回退 Helvetica
     */
    private Font pdfFont(float size) {
        return pdfFont(size, Font.NORMAL);
    }

    /**
     * 中文字体（STSong-Light），失败时回退 Helvetica
     */
    private Font pdfFont(float size, int style) {
        try {
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            log.warn("加载中文字体失败，回退 Helvetica: {}", e.getMessage());
            return new Font(Font.HELVETICA, size, style);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 序列化 JSON
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    /**
     * 清理 BigDecimal 尾部零
     */
    private BigDecimal cleanDecimal(BigDecimal value) {
        if (value == null) return null;
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    /**
     * LocalDateTime → ISO 8601 带时区字符串
     */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
