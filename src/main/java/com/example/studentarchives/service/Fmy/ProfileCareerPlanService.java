package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.DefaultTemplateHtml;
import com.example.studentarchives.config.Fmy.OssProperties;
import com.example.studentarchives.dto.Fmy.profile.request.AiPlanCreateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionAddRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionFileRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionStatusRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerActionUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerGoalAddRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerGoalUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerMilestoneAddRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerMilestoneUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerPlanCopyRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerPlanCreateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.CareerReflectionAddRequest;
import com.example.studentarchives.dto.Fmy.profile.response.AiPlanCreateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerActionFileResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerActionStatusResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanCopyResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanCreateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.ActionDetail;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.FeedbackItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.FileItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.GoalDetail;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.MilestoneItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.ReflectionItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse.VersionItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanIdResponse;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanListItem;
import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanPreviewResponse;
import com.example.studentarchives.entity.career.CareerAction;
import com.example.studentarchives.entity.career.CareerGoal;
import com.example.studentarchives.entity.career.CareerMilestone;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.career.CareerPlanFeedback;
import com.example.studentarchives.entity.career.CareerReflection;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.version.ModelVersion;
import com.example.studentarchives.entity.weakness.ImprovementSuggestion;
import com.example.studentarchives.entity.weakness.WeaknessAnalysis;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.AttachmentBizTypeEnum;
import com.example.studentarchives.enums.CareerProgressStatusEnum;
import com.example.studentarchives.enums.FileStatusEnum;
import com.example.studentarchives.enums.ModelVersionModelTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.CareerActionRepository;
import com.example.studentarchives.repository.CareerGoalRepository;
import com.example.studentarchives.repository.CareerMilestoneRepository;
import com.example.studentarchives.repository.CareerPlanFeedbackRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.CareerReflectionRepository;
import com.example.studentarchives.repository.ImprovementSuggestionRepository;
import com.example.studentarchives.repository.ModelVersionRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.WeaknessAnalysisRepository;
import com.example.studentarchives.service.Lzw.ApprovalSubmitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 职业规划服务
 * <p>
 * 提供学生端个人中心职业规划（《学生端接口文档》四、4.3~4.15）全部接口：
 * 列表、新增/提交、详情、下载、复制、添加目标/行动/里程碑、更新行动状态、
 * 上传行动成果、添加阶段反思、AI建议一键添加。
 * <p>
 * 数据口径：
 * - career_plans / career_goals / career_actions / career_milestones
 * - file_uploads（biz_type='career_plan' 规划附件、biz_type='career_action' 行动成果）
 * - career_reflections / career_plan_feedbacks / model_versions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileCareerPlanService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 日期格式：2005-03-15 */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 职业规划导出模板业务类型（export_templates.export_type） */
    private static final String EXPORT_TYPE_CAREER_PLAN = "career_plan";

    /** 职业规划导出文件名（固定，不含日期，避免下载名随日期变化） */
    private static final String EXPORT_FILE_NAME = "职业规划文件.pdf";

    /** 规划来源标签（source：1=手动创建 2=AI建议添加） */
    private static final Map<Integer, String> PLAN_SOURCE_LABELS = Map.of(
            1, "手动创建",
            2, "AI建议添加"
    );

    /** 行动状态权重（progressRate 等权计算） */
    private static final double STATUS_WEIGHT_NOT_STARTED = 0.0;
    private static final double STATUS_WEIGHT_IN_PROGRESS = 0.5;
    private static final double STATUS_WEIGHT_COMPLETED = 1.0;

    private final CareerPlanRepository careerPlanRepository;
    private final CareerGoalRepository careerGoalRepository;
    private final CareerActionRepository careerActionRepository;
    private final CareerMilestoneRepository careerMilestoneRepository;
    private final CareerReflectionRepository careerReflectionRepository;
    private final CareerPlanFeedbackRepository careerPlanFeedbackRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final ImprovementSuggestionRepository improvementSuggestionRepository;
    private final WeaknessAnalysisRepository weaknessAnalysisRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final OssFileService ossFileService;
    private final OssProperties ossProperties;
    private final ObjectMapper objectMapper;
    private final ExportTemplateRenderService exportTemplateRenderService;
    private final ApprovalSubmitService approvalSubmitService;

    // ==================== 列表（4.3） ====================

    /**
     * 获取职业规划列表（GET /profile/career-plans）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID 筛选，不传返回全部学期
     * @param pageParam  分页参数
     * @return 分页列表
     */
    @Transactional(readOnly = true)
    public PageResult<CareerPlanListItem> listPlans(Long userId, Long semesterId, PageParam pageParam) {
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        List<CareerPlan> plans;
        long total;
        if (semesterId != null) {
            plans = careerPlanRepository.findByUserIdAndSemesterId(userId, semesterId, pageable);
            total = careerPlanRepository.countByUserIdAndSemesterId(userId, semesterId);
        } else {
            plans = careerPlanRepository.findByUserId(userId, pageable);
            total = careerPlanRepository.countByUserId(userId);
        }

        Map<Long, String> auditorNameMap = buildAuditorNameMap(plans);
        Map<Long, String> semesterNameMap = buildSemesterNameMap(plans);
        List<CareerPlanListItem> items = plans.stream()
                .map(p -> toListItem(p, auditorNameMap, semesterNameMap))
                .collect(Collectors.toList());
        return PageResult.of(items, total, pageParam);
    }

    // ==================== 新增/提交（4.4） ====================

    /**
     * 新增/提交职业规划（POST /profile/career-plans）
     * <p>
     * 级联创建 goals→actions→milestones，绑定 evidenceFileIds 附件，
     * 写入 model_versions 版本记录（model_type='career_plan'）。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @Transactional
    public CareerPlanCreateResponse createPlan(Long userId, CareerPlanCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
        semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));

        boolean draft = request.getIsDraft() != null && request.getIsDraft() == 1;
        LocalDateTime now = LocalDateTime.now();

        CareerPlan plan = new CareerPlan();
        plan.setSchoolId(user.getSchoolId() != null ? user.getSchoolId() : 1L);
        plan.setUserId(userId);
        plan.setSemesterId(request.getSemesterId());
        plan.setTitle(request.getTitle());
        plan.setContent(request.getContent());
        plan.setRequirement(request.getRequirement());
        plan.setSource(1);
        // 手动创建即直接添加，无需学生二次确认（对齐文档 4.6 示例 requireConfirm=0）
        plan.setRequireConfirm(0);
        plan.setProgressRate(0);
        plan.setStatus(draft ? 0 : 1);
        ArchiveAuditInfo audit = new ArchiveAuditInfo();
        audit.setCurrentVersion(1);
        audit.setSubmitCount(draft ? 0 : 1);
        if (draft) {
            audit.setDraftSavedAt(now);
        } else {
            audit.setSubmittedAt(now);
        }
        plan.setAuditInfo(audit);
        plan = careerPlanRepository.save(plan);

        // 级联创建目标/行动/里程碑
        createNestedStructures(plan.getId(), request.getGoals());

        // 绑定规划附件
        if (request.getEvidenceFileIds() != null) {
            for (Long fileId : request.getEvidenceFileIds()) {
                bindFile(fileId, userId, AttachmentBizTypeEnum.CAREER_PLAN.getValue(), plan.getId());
            }
        }

        // 写入版本记录
        writeModelVersion(plan, userId);

        // 提交（非草稿）后生成待审核任务（教师端「待审核任务模块」4.1）
        if (!draft) {
            generatePendingApproval(plan, user);
        }

        return CareerPlanCreateResponse.builder()
                .planId(plan.getId())
                .status(plan.getStatus())
                .statusLabel(ApplyStatusEnum.of(plan.getStatus()).getLabel())
                .currentVersion(audit.getCurrentVersion())
                .submitCount(audit.getSubmitCount())
                .build();
    }

    /** 提交（非草稿）后生成待审核任务，失败不抛出、不影响提交。 */
    private void generatePendingApproval(CareerPlan plan, User user) {
        try {
            LocalDateTime submittedAt = plan.getAuditInfo() != null ? plan.getAuditInfo().getSubmittedAt() : null;
            approvalSubmitService.createOnSubmit(
                    plan.getSchoolId(), "CareerPlan", null, plan.getId(),
                    plan.getUserId(), user.getName(), user.getUserNo(), plan.getTitle(),
                    "职业生涯规划", submittedAt);
        } catch (Exception e) {
            log.warn("生成待审核任务失败（不阻塞提交）: careerPlanId={}, err={}", plan.getId(), e.getMessage());
        }
    }

    // ==================== 下载（4.5） ====================

    /**
     * 获取职业规划下载 URL（GET /profile/career-plans/{planId}/download）
     * <p>
     * 惰性生成并缓存职业规划独立文档（不再返回上传佐证）：优先取已生成的主文件
     * （career_plans.file_id），规划被编辑/审批状态变化（updatedAt 晚于文件更新时间）后
     * 自动重新生成，保证文档反映最新状态。
     * <p>
     * external 用途用于外部投递，不添加屏幕水印，且不缓存复用，确保每次拿到干净文件。
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param purpose 导出用途：internal（内部查看，屏幕水印+打印隐藏）/ external（外部投递，默认，屏幕和打印均无水印）
     * @return OSS 签名下载 URL
     */
    @Transactional
    public String getCareerPlanDownloadUrl(Long userId, Long planId, String purpose) {
        return downloadUrl(resolveExportFile(userId, planId, purpose));
    }

    /**
     * 获取职业规划文件预览信息（GET /profile/career-plans/{planId}/preview）
     * <p>
     * 与下载共用 {@link #resolveExportFile} 解析同一份生成/缓存文件，保证「预览即所见即所得」；
     * 返回 OSS 签名 inline 预览 URL（response-content-disposition=inline），供前端新标签页内嵌渲染 PDF。
     * external 用途返回无水印版本，便于投递前确认版式。
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param purpose 导出用途：internal（内部预览，屏幕水印+打印隐藏）/ external（外部投递，默认，无水印）
     * @return 预览信息
     */
    @Transactional
    public CareerPlanPreviewResponse getCareerPlanPreviewUrl(Long userId, Long planId, String purpose) {
        AttachmentRelation relation = resolveExportFile(userId, planId, purpose);
        if (relation == null || relation.getFilePath() == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "未找到规划文件");
        }
        String previewUrl = ossFileService.getPreviewUrl(relation.getFilePath(), relation.getOriginalName());
        if (previewUrl == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "规划文件不存在");
        }
        return CareerPlanPreviewResponse.builder()
                .previewUrl(previewUrl)
                .fileName(relation.getOriginalName())
                .purpose(purpose != null ? purpose : "external")
                .generatedAt(toIso(LocalDateTime.now()))
                .build();
    }

    /**
     * 解析职业规划导出文件：internal 优先复用未过期的缓存文件（复用前刷新固定文件名），
     * 否则惰性生成。下载与预览共用此逻辑，保证两份 URL 指向同一份文件。
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param purpose 导出用途：internal / external
     * @return 已生成并绑定的导出附件关系
     */
    private AttachmentRelation resolveExportFile(Long userId, Long planId, String purpose) {
        CareerPlan plan = getOwnedPlan(userId, planId);

        String normalizedPurpose = purpose != null ? purpose : "external";
        if (!"internal".equals(normalizedPurpose) && !"external".equals(normalizedPurpose)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用途参数只能是 internal 或 external");
        }
        boolean watermarkEnabled = "internal".equals(normalizedPurpose);

        // external 用途不缓存，必须重新生成以保证无水印
        if (watermarkEnabled) {
            // 1) 已有主文件且未过期、OSS 对象仍存在 → 直接复用（同步刷新文件名，覆盖旧缓存里的日期文件名）
            AttachmentRelation main = resolveOwnedRelation(plan.getExportFileId(), userId);
            if (main != null && main.getFilePath() != null
                    && !isStale(plan, main) && ossFileService.exists(main.getFilePath())) {
                return refreshExportFileName(main);
            }

            // 2) file_id 丢失时的兜底：查内部用途已绑定的导出文件并补写 file_id
            AttachmentRelation exportFile = findExportRelation(plan.getId(), userId,
                    AttachmentBizTypeEnum.CAREER_PLAN_EXPORT.getValue());
            if (exportFile != null && exportFile.getFilePath() != null
                    && !isStale(plan, exportFile) && ossFileService.exists(exportFile.getFilePath())) {
                plan.setExportFileId(exportFile.getId());
                careerPlanRepository.save(plan);
                return refreshExportFileName(exportFile);
            }
        }

        // 3) 惰性生成（首次下载 / 主文件丢失 / 规划已变更 / external 用途）
        return generateCareerPlanFile(userId, plan, watermarkEnabled);
    }

    // ==================== 职业规划文档生成（4.5 下载） ====================

    /**
     * 惰性生成职业规划独立文档：按规划详情渲染 PDF → 上传 OSS → 绑定导出附件
     * （internal 用 {@code career_plan_export} 并写入 {@code career_plans.file_id}；
     * external 用独立的 {@code career_plan_export_external}，不写 file_id）。
     * <p>
     * 模板优先取 {@code export_templates} 中 {@code career_plan} 类型的默认模板，
     * 未播种时用内存兜底模板（同一套默认配置）保证单渲染路径。
     * <p>
     * internal / external 使用各自独立的附件行，互不复用：外部投递的无水印文件
     * 不会覆盖内部水印缓存，internal 的复用也不会命中外部文件。
     *
     * @param userId           当前登录用户 ID
     * @param plan             归属当前用户的规划
     * @param watermarkEnabled 是否添加屏幕可见、打印隐藏的水印
     * @return 绑定后的导出附件关系（下载/预览据此生成各自 URL）
     */
    private AttachmentRelation generateCareerPlanFile(Long userId, CareerPlan plan, boolean watermarkEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
        Long schoolId = user.getSchoolId() != null ? user.getSchoolId() : 1L;
        ExportTemplate template = exportTemplateRenderService.resolveDefaultTemplate(schoolId, EXPORT_TYPE_CAREER_PLAN);
        if (template == null) {
            template = buildFallbackTemplate(schoolId, user.getId());
        }

        byte[] pdfBytes;
        try {
            pdfBytes = exportTemplateRenderService.renderTemplate(template, buildPlanDocumentContext(plan), watermarkEnabled);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("职业规划文件生成失败 userId={} planId={}", userId, plan.getId(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "职业规划文件生成失败");
        }

        String originalName = EXPORT_FILE_NAME;
        // 内部/外部使用各自独立的附件行：internal 缓存于 career_plans.file_id（带水印），
        // external 每次重新生成（无水印），二者不复用同一行，避免相互覆盖污染。
        String bizType = watermarkEnabled
                ? AttachmentBizTypeEnum.CAREER_PLAN_EXPORT.getValue()
                : AttachmentBizTypeEnum.CAREER_PLAN_EXPORT_EXTERNAL.getValue();
        String objectKey;
        try {
            objectKey = ossFileService.uploadBytes(pdfBytes, "application/pdf", bizType, "pdf", originalName);
        } catch (Exception e) {
            log.error("职业规划文件上传OSS失败 userId={} planId={}", userId, plan.getId(), e);
            throw new BusinessException(ResultCode.THIRD_OSS_FAILED, "职业规划文件上传失败");
        }

        // 仅复用同用途既有导出关系（替换 OSS 旧对象，避免孤儿文件）：
        // internal 优先 file_id 主文件；external 不触碰 file_id，只查外部投递行。
        AttachmentRelation relation = watermarkEnabled ? resolveOwnedRelation(plan.getExportFileId(), userId) : null;
        if (relation == null) {
            relation = findExportRelation(plan.getId(), userId, bizType);
        }
        if (relation != null) {
            ossFileService.deleteFile(relation.getFilePath());
            relation.setOriginalName(originalName);
            relation.setFilePath(objectKey);
            relation.setFileSize((long) pdfBytes.length);
            relation.setMimeType("application/pdf");
            relation.setFileStatus(FileStatusEnum.BOUND.getValue());
        } else {
            relation = new AttachmentRelation();
            relation.setUserId(userId);
            relation.setBizType(bizType);
            relation.setBizId(plan.getId());
            relation.setFileCategory("pdf");
            relation.setOriginalName(originalName);
            relation.setFilePath(objectKey);
            relation.setFileSize((long) pdfBytes.length);
            relation.setMimeType("application/pdf");
            relation.setDisk("oss");
            relation.setConvertStatus(0);
            relation.setSortOrder(0);
            relation.setFileStatus(FileStatusEnum.BOUND.getValue());
        }
        // 先保存关系行（file_id 外键需要行存在），再写规划主文件
        relation = attachmentRelationRepository.save(relation);
        // 仅 internal 写入 career_plans.file_id 作为水印缓存；external 不缓存，每次重新生成
        if (watermarkEnabled) {
            plan.setExportFileId(relation.getId());
            careerPlanRepository.save(plan);
        }

        return relation;
    }

    /**
     * 构建职业规划文档的 Mustache 渲染上下文：封面（姓名/学号/学期）+ 规划信息 +
     * 目标→行动→里程碑 + 阶段反思 + 教师反馈。
     */
    private Map<String, Object> buildPlanDocumentContext(CareerPlan plan) {
        Long userId = plan.getUserId();
        User user = userRepository.findById(userId).orElse(null);
        ArchiveAuditInfo audit = plan.getAuditInfo();
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studentName", user != null ? user.getName() : null);
        context.put("userNo", user != null ? user.getUserNo() : null);
        context.put("semesterName", semesterName(plan.getSemesterId()));
        context.put("title", plan.getTitle());
        context.put("content", plan.getContent());
        context.put("requirement", plan.getRequirement());
        context.put("progressRate", plan.getProgressRate() != null ? plan.getProgressRate() : 0);
        context.put("statusLabel", ApplyStatusEnum.of(plan.getStatus()).getLabel());
        context.put("submittedAt", audit != null ? toIso(audit.getSubmittedAt()) : null);
        context.put("auditedAt", audit != null ? toIso(audit.getAuditedAt()) : null);
        context.put("auditorName", audit != null && audit.getAuditorId() != null
                ? buildAuditorNameMap(Collections.singletonList(plan)).get(audit.getAuditorId()) : null);
        context.put("rejectedReason", audit != null ? audit.getRejectedReason() : null);

        // 目标 → 行动 → 里程碑
        List<Map<String, Object>> goalItems = new ArrayList<>();
        for (CareerGoal goal : careerGoalRepository.findByCareerPlanIdOrderBySortAsc(plan.getId())) {
            Map<String, Object> goalItem = new LinkedHashMap<>();
            goalItem.put("goalTitle", goal.getGoalTitle());
            goalItem.put("goalDesc", goal.getGoalDesc());
            goalItem.put("targetDate", formatDate(goal.getTargetDate()));
            goalItem.put("statusLabel", CareerProgressStatusEnum.of(goal.getStatus()).getLabel());
            List<Map<String, Object>> actionItems = new ArrayList<>();
            for (CareerAction action : careerActionRepository.findByGoalIdOrderBySortAsc(goal.getId())) {
                Map<String, Object> actionItem = new LinkedHashMap<>();
                actionItem.put("actionTitle", action.getActionTitle());
                actionItem.put("actionDesc", action.getActionDesc());
                actionItem.put("statusLabel", CareerProgressStatusEnum.of(action.getStatus()).getLabel());
                actionItem.put("timeRange", formatTimeRange(action.getStartDate(), action.getEndDate()));
                actionItem.put("completionRate", action.getCompletionRate() != null ? action.getCompletionRate() : 0);
                List<Map<String, Object>> milestoneItems = new ArrayList<>();
                for (CareerMilestone milestone
                        : careerMilestoneRepository.findByActionIdOrderBySortAsc(action.getId())) {
                    Map<String, Object> milestoneItem = new LinkedHashMap<>();
                    milestoneItem.put("milestoneTitle", milestone.getMilestoneTitle());
                    milestoneItem.put("milestoneDate", formatDate(milestone.getMilestoneDate()));
                    milestoneItem.put("achievedLabel",
                            milestone.getIsAchieved() != null && milestone.getIsAchieved() == 1 ? "已完成" : "未达成");
                    milestoneItems.add(milestoneItem);
                }
                actionItem.put("milestones", milestoneItems);
                actionItems.add(actionItem);
            }
            goalItem.put("actions", actionItems);
            goalItems.add(goalItem);
        }
        context.put("goals", goalItems);

        // 阶段反思
        List<Map<String, Object>> reflectionItems = careerReflectionRepository
                .findByCareerPlanIdOrderByCreatedAtAsc(plan.getId())
                .stream()
                .map(r -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("reflectionContent", r.getReflectionContent());
                    item.put("createdAt", toIso(r.getCreatedAt()));
                    return item;
                })
                .collect(Collectors.toList());
        context.put("reflections", reflectionItems);

        // 教师反馈 + 教师姓名
        List<CareerPlanFeedback> feedbacks = careerPlanFeedbackRepository.findByCareerPlanIdOrderByCreatedAtAsc(plan.getId());
        List<Long> teacherIds = feedbacks.stream()
                .map(CareerPlanFeedback::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> teacherNameMap = teacherIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findByIdIn(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        List<Map<String, Object>> feedbackItems = feedbacks.stream().map(f -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("teacherName", teacherNameMap.get(f.getTeacherId()));
            item.put("feedbackContent", f.getFeedbackContent());
            item.put("createdAt", toIso(f.getCreatedAt()));
            return item;
        }).collect(Collectors.toList());
        context.put("feedbacks", feedbackItems);

        context.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return context;
    }

    /**
     * 模板未播种时的内存兜底（与种子器共享同一套默认配置），保持单渲染路径。
     */
    private ExportTemplate buildFallbackTemplate(Long schoolId, Long createdBy) {
        ExportTemplate template = new ExportTemplate();
        template.setSchoolId(schoolId);
        template.setTemplateName("职业规划默认模板");
        template.setTemplateCode("career_plan_default");
        template.setExportType(EXPORT_TYPE_CAREER_PLAN);
        template.setScopeType(1);
        template.setFieldsConfig("{}");
        template.setFilterConditions("{}");
        template.setTemplateMode(2);
        template.setTemplateContent(DefaultTemplateHtml.CAREER_PLAN);
        template.setEngineType("html");
        template.setPaperSize("A4");
        template.setOrientation(1);
        template.setHeaderHtml("职业规划");
        template.setFooterHtml(DefaultTemplateHtml.DEFAULT_FOOTER);
        template.setMarginConfig(writeJson(DefaultTemplateHtml.DEFAULT_MARGIN));
        template.setFontConfig(writeJson(DefaultTemplateHtml.DEFAULT_FONT));
        template.setWatermarkConfig(writeJson(DefaultTemplateHtml.DEFAULT_WATERMARK));
        template.setPageConfig(writeJson(DefaultTemplateHtml.DEFAULT_PAGE));
        template.setVersion(1);
        template.setIsDefault(1);
        template.setStatus(1);
        template.setCreatedBy(createdBy);
        return template;
    }

    /**
     * 解析归属当前用户的附件关系（file_id 指向），越权/不存在返回 null。
     */
    private AttachmentRelation resolveOwnedRelation(Long fileId, Long userId) {
        if (fileId == null) {
            return null;
        }
        AttachmentRelation relation = attachmentRelationRepository.findById(fileId).orElse(null);
        if (relation == null || !userId.equals(relation.getUserId())) {
            return null;
        }
        return relation;
    }

    /**
     * 查询规划已绑定的导出文件（排除已软删），file_id 丢失时兜底使用。
     * 按用途 bizType 区分：internal 用 career_plan_export、external 用 career_plan_export_external，
     * 避免外部无水印文件被 internal 缓存复用。
     */
    private AttachmentRelation findExportRelation(Long planId, Long userId, String bizType) {
        return attachmentRelationRepository
                .findByBizTypeAndBizIdOrderBySortOrderAsc(bizType, planId)
                .stream()
                .filter(r -> userId.equals(r.getUserId()))
                .filter(r -> r.getFileStatus() == null || FileStatusEnum.of(r.getFileStatus()) == FileStatusEnum.BOUND)
                .findFirst()
                .orElse(null);
    }

    /**
     * 文档是否过期：规划最新更新晚于已生成文件（规划被编辑/审批状态变化后需重新生成）。
     */
    private boolean isStale(CareerPlan plan, AttachmentRelation relation) {
        LocalDateTime planUpdated = plan.getUpdatedAt();
        LocalDateTime fileUpdated = relation.getUpdatedAt();
        return planUpdated != null && fileUpdated != null && planUpdated.isAfter(fileUpdated);
    }

    /**
     * 复用已缓存文件时刷新下载文件名：旧版本生成的附件行可能存有
     * "职业规划-20260806.pdf" 这类日期文件名，改为固定名后需一并更新，
     * 使签名 URL 的 response-content-disposition 携带新文件名（无需重生成 PDF）。
     */
    private AttachmentRelation refreshExportFileName(AttachmentRelation relation) {
        if (relation != null && !EXPORT_FILE_NAME.equals(relation.getOriginalName())) {
            relation.setOriginalName(EXPORT_FILE_NAME);
            return attachmentRelationRepository.save(relation);
        }
        return relation;
    }

    /**
     * 校验并返回下载 URL，文件缺失/不存在时抛业务异常。
     */
    private String downloadUrl(AttachmentRelation relation) {
        if (relation == null || relation.getFilePath() == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "未找到规划文件");
        }
        // 显式携带中文文件名（response-content-disposition 覆盖），避免回退到 OSS 对象上可能旧乱码的元数据
        String url = ossFileService.getFileUrl(relation.getFilePath(), relation.getOriginalName());
        if (url == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "规划文件不存在");
        }
        return url;
    }

    /**
     * 起止日期拼接："2005-03-15 ~ 2005-06-30"，仅一端存在时返回该端，两端为空返回空串。
     */
    private String formatTimeRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "";
        }
        if (start == null) {
            return formatDate(end);
        }
        if (end == null) {
            return formatDate(start);
        }
        return formatDate(start) + " ~ " + formatDate(end);
    }

    // ==================== 详情（4.6） ====================

    /**
     * 获取职业规划详情（GET /profile/career-plans/{planId}）
     *
     * @param userId 当前登录用户 ID
     * @param planId 规划 ID
     * @return 规划详情
     */
    @Transactional(readOnly = true)
    public CareerPlanDetailResponse getPlanDetail(Long userId, Long planId) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ArchiveAuditInfo audit = plan.getAuditInfo();
        Map<Long, String> userNameMap = buildAuditorNameMap(Collections.singletonList(plan));

        // goals → actions → milestones + files
        List<CareerGoal> goals = careerGoalRepository.findByCareerPlanIdOrderBySortAsc(planId);
        Map<Long, List<CareerAction>> actionsByGoal = new HashMap<>();
        List<Long> actionIds = new ArrayList<>();
        for (CareerGoal goal : goals) {
            List<CareerAction> actions = careerActionRepository.findByGoalIdOrderBySortAsc(goal.getId());
            actionsByGoal.put(goal.getId(), actions);
            actions.forEach(a -> actionIds.add(a.getId()));
        }
        Map<Long, List<CareerMilestone>> milestonesByAction = new HashMap<>();
        for (Long actionId : actionIds) {
            milestonesByAction.put(actionId, careerMilestoneRepository.findByActionIdOrderBySortAsc(actionId));
        }
        Map<Long, List<AttachmentRelation>> filesByAction = actionIds.isEmpty() ? Collections.emptyMap()
                : attachmentRelationRepository
                        .findByBizTypeAndBizIdIn(AttachmentBizTypeEnum.CAREER_ACTION.getValue(), actionIds)
                        .stream()
                        .collect(Collectors.groupingBy(AttachmentRelation::getBizId));
        // 批量加载各里程碑的成果证明文件（避免逐条查询 N+1）
        Map<Long, AttachmentRelation> proofFiles = collectMilestoneProofFiles(milestonesByAction);

        List<GoalDetail> goalDetails = goals.stream()
                .map(g -> GoalDetail.builder()
                        .id(g.getId())
                        .goalTitle(g.getGoalTitle())
                        .goalDesc(g.getGoalDesc())
                        .targetDate(formatDate(g.getTargetDate()))
                        .status(g.getStatus())
                        .statusLabel(CareerProgressStatusEnum.of(g.getStatus()).getLabel())
                        .actions(actionsByGoal.getOrDefault(g.getId(), Collections.emptyList()).stream()
                                .map(a -> ActionDetail.builder()
                                        .id(a.getId())
                                        .actionTitle(a.getActionTitle())
                                        .actionDesc(a.getActionDesc())
                                        .status(a.getStatus())
                                        .statusLabel(CareerProgressStatusEnum.of(a.getStatus()).getLabel())
                                        .startDate(formatDate(a.getStartDate()))
                                        .endDate(formatDate(a.getEndDate()))
                                        .completionRate(a.getCompletionRate())
                                        .files(filesByAction.getOrDefault(a.getId(), Collections.emptyList()).stream()
                                                .map(f -> FileItem.builder()
                                                        .fileId(f.getId())
                                                        .fileName(f.getOriginalName())
                                                        .fileUrl(ossFileService.getFileUrl(f.getFilePath()))
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .milestones(milestonesByAction.getOrDefault(a.getId(), Collections.emptyList())
                                                .stream()
                                                .map(m -> {
                                                    AttachmentRelation proof = proofFiles.get(m.getProofFileId());
                                                    return MilestoneItem.builder()
                                                            .id(m.getId())
                                                            .milestoneTitle(m.getMilestoneTitle())
                                                            .milestoneDate(formatDate(m.getMilestoneDate()))
                                                            .isAchieved(m.getIsAchieved())
                                                            .proofFileId(m.getProofFileId())
                                                            .proofFileName(proof != null ? proof.getOriginalName() : null)
                                                            .proofFileUrl(proof != null
                                                                    ? ossFileService.getFileUrl(proof.getFilePath()) : null)
                                                            .build();
                                                })
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        // 阶段反思
        List<ReflectionItem> reflections = careerReflectionRepository.findByCareerPlanIdOrderByCreatedAtAsc(planId)
                .stream()
                .map(r -> ReflectionItem.builder()
                        .id(r.getId())
                        .reflectionContent(r.getReflectionContent())
                        .createdAt(toIso(r.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        // 教师反馈 + 教师姓名
        List<CareerPlanFeedback> feedbacks = careerPlanFeedbackRepository.findByCareerPlanIdOrderByCreatedAtAsc(planId);
        List<Long> teacherIds = feedbacks.stream()
                .map(CareerPlanFeedback::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> teacherNameMap = teacherIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findByIdIn(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        List<FeedbackItem> feedbackItems = feedbacks.stream()
                .map(f -> FeedbackItem.builder()
                        .id(f.getId())
                        .teacherName(teacherNameMap.get(f.getTeacherId()))
                        .feedbackContent(f.getFeedbackContent())
                        .createdAt(toIso(f.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        // 版本历史
        List<VersionItem> versionHistory = modelVersionRepository
                .findByModelTypeAndModelIdOrderByVersionAsc(ModelVersionModelTypeEnum.CAREER_PLAN.getValue(), planId)
                .stream()
                .map(v -> VersionItem.builder()
                        .version(v.getVersion())
                        .status(v.getStatus())
                        .statusLabel(v.getStatus() != null ? ApplyStatusEnum.of(v.getStatus()).getLabel() : null)
                        .createdAt(toIso(v.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        return CareerPlanDetailResponse.builder()
                .id(plan.getId())
                .semesterId(plan.getSemesterId())
                .semesterName(semesterName(plan.getSemesterId()))
                .title(plan.getTitle())
                .content(plan.getContent())
                .requirement(plan.getRequirement())
                .progressRate(plan.getProgressRate())
                .status(plan.getStatus())
                .statusLabel(ApplyStatusEnum.of(plan.getStatus()).getLabel())
                .submittedAt(audit != null ? toIso(audit.getSubmittedAt()) : null)
                .auditedAt(audit != null ? toIso(audit.getAuditedAt()) : null)
                .auditorName(audit != null && audit.getAuditorId() != null
                        ? userNameMap.get(audit.getAuditorId()) : null)
                .rejectedReason(audit != null ? audit.getRejectedReason() : null)
                .copyFromId(plan.getCopyFromId())
                .source(plan.getSource())
                .sourceLabel(PLAN_SOURCE_LABELS.getOrDefault(plan.getSource(), null))
                .aiSuggestionId(plan.getAiSuggestionId())
                .requireConfirm(plan.getRequireConfirm())
                .goals(goalDetails)
                .reflections(reflections)
                .feedbacks(feedbackItems)
                .versionHistory(versionHistory)
                .build();
    }

    // ==================== 复制（4.7） ====================

    /**
     * 复制上一学期计划（POST /profile/career-plans/copy）
     * <p>
     * 深拷贝源学期规划及其目标/行动/里程碑，状态重置为草稿，文件不复制。
     *
     * @param userId  当前登录用户 ID
     * @param request 复制请求
     * @return 复制结果
     */
    @Transactional
    public CareerPlanCopyResponse copyPlan(Long userId, CareerPlanCopyRequest request) {
        CareerPlan source = careerPlanRepository
                .findFirstByUserIdAndSemesterIdOrderByIdDesc(userId, request.getSourceSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "未找到源学期规划"));
        semesterRepository.findById(request.getTargetSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "目标学期不存在"));

        CareerPlan copy = new CareerPlan();
        copy.setSchoolId(source.getSchoolId());
        copy.setUserId(userId);
        copy.setSemesterId(request.getTargetSemesterId());
        copy.setTitle(request.getTitle() != null ? request.getTitle() : source.getTitle() + "(复制)");
        copy.setContent(source.getContent());
        copy.setRequirement(source.getRequirement());
        copy.setCopyFromId(source.getId());
        copy.setSource(source.getSource() != null ? source.getSource() : 1);
        copy.setAiSuggestionId(source.getAiSuggestionId());
        // 复制后为直接可编辑的草稿，无需二次确认（与手动创建语义一致）
        copy.setRequireConfirm(0);
        copy.setProgressRate(0);
        copy.setStatus(0);
        ArchiveAuditInfo audit = new ArchiveAuditInfo();
        audit.setCurrentVersion(1);
        audit.setSubmitCount(0);
        audit.setDraftSavedAt(LocalDateTime.now());
        copy.setAuditInfo(audit);
        copy = careerPlanRepository.save(copy);

        // 深拷贝目标/行动/里程碑（状态重置为草稿/未开始）
        for (CareerGoal sourceGoal : careerGoalRepository.findByCareerPlanIdOrderBySortAsc(source.getId())) {
            CareerGoal goal = new CareerGoal();
            goal.setCareerPlanId(copy.getId());
            goal.setGoalTitle(sourceGoal.getGoalTitle());
            goal.setGoalDesc(sourceGoal.getGoalDesc());
            goal.setTargetDate(sourceGoal.getTargetDate());
            goal.setSource(sourceGoal.getSource());
            goal.setStatus(0);
            goal.setSort(sourceGoal.getSort());
            goal = careerGoalRepository.save(goal);

            for (CareerAction sourceAction : careerActionRepository.findByGoalIdOrderBySortAsc(sourceGoal.getId())) {
                CareerAction action = new CareerAction();
                action.setGoalId(goal.getId());
                action.setActionTitle(sourceAction.getActionTitle());
                action.setActionDesc(sourceAction.getActionDesc());
                action.setStatus(0);
                action.setSource(sourceAction.getSource());
                action.setStartDate(sourceAction.getStartDate());
                action.setEndDate(sourceAction.getEndDate());
                action.setCompletionRate(0);
                action.setSort(sourceAction.getSort());
                action = careerActionRepository.save(action);

                for (CareerMilestone sourceMilestone
                        : careerMilestoneRepository.findByActionIdOrderBySortAsc(sourceAction.getId())) {
                    CareerMilestone milestone = new CareerMilestone();
                    milestone.setActionId(action.getId());
                    milestone.setMilestoneTitle(sourceMilestone.getMilestoneTitle());
                    milestone.setMilestoneDate(sourceMilestone.getMilestoneDate());
                    milestone.setIsAchieved(0);
                    milestone.setSort(sourceMilestone.getSort());
                    careerMilestoneRepository.save(milestone);
                }
            }
        }

        return CareerPlanCopyResponse.builder()
                .planId(copy.getId())
                .copyFromId(copy.getCopyFromId())
                .status(0)
                .statusLabel(ApplyStatusEnum.of(0).getLabel())
                .build();
    }

    // ==================== 添加目标/行动/里程碑（4.8~4.10） ====================

    /**
     * 添加目标（POST /profile/career-plans/{planId}/goals）
     */
    @Transactional
    public CareerPlanIdResponse addGoal(Long userId, Long planId, CareerGoalAddRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        int sort = (request.getSort() != null) ? request.getSort()
                : careerGoalRepository.findTopByCareerPlanIdOrderBySortDesc(planId)
                        .map(g -> g.getSort() + 1).orElse(0);
        CareerGoal goal = new CareerGoal();
        goal.setCareerPlanId(planId);
        goal.setGoalTitle(request.getGoalTitle());
        goal.setGoalDesc(request.getGoalDesc());
        goal.setTargetDate(parseDate(request.getTargetDate()));
        goal.setSource(1);
        goal.setStatus(0);
        goal.setSort(sort);
        goal = careerGoalRepository.save(goal);
        recomputePlanProgress(planId);
        return CareerPlanIdResponse.builder().goalId(goal.getId()).build();
    }

    /**
     * 添加行动（POST /profile/career-plans/{planId}/goals/{goalId}/actions）
     * <p>
     * 归属目标经路径 goalId 指定，校验目标属于当前规划后创建。
     */
    @Transactional
    public CareerPlanIdResponse addAction(Long userId, Long planId, Long goalId, CareerActionAddRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        careerGoalRepository.findByIdAndCareerPlanId(goalId, planId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "目标不存在"));
        int sort = (request.getSort() != null) ? request.getSort()
                : careerActionRepository.findTopByGoalIdOrderBySortDesc(goalId)
                        .map(a -> a.getSort() + 1).orElse(0);
        CareerAction action = new CareerAction();
        action.setGoalId(goalId);
        action.setActionTitle(request.getActionTitle());
        action.setActionDesc(request.getActionDesc());
        action.setStatus(0);
        action.setSource(1);
        action.setStartDate(parseDate(request.getStartDate()));
        action.setEndDate(parseDate(request.getEndDate()));
        action.setCompletionRate(0);
        action.setSort(sort);
        action = careerActionRepository.save(action);
        // 新增未开始行动会稀释进度、可能使已完成目标回落，需同步重算目标状态与规划进度
        updateGoalStatus(goalId);
        recomputePlanProgress(planId);
        return CareerPlanIdResponse.builder().actionId(action.getId()).build();
    }

    /**
     * 添加里程碑（POST /profile/career-plans/{planId}/actions/{actionId}/milestones）
     * <p>
     * 归属行动经路径 actionId 指定，校验行动属于当前规划后创建。
     */
    @Transactional
    public CareerPlanIdResponse addMilestone(Long userId, Long planId, Long actionId, CareerMilestoneAddRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        requireActionInPlan(actionId, planId);
        int sort = careerMilestoneRepository.findTopByActionIdOrderBySortDesc(actionId)
                .map(m -> m.getSort() + 1).orElse(0);
        CareerMilestone milestone = new CareerMilestone();
        milestone.setActionId(actionId);
        milestone.setMilestoneTitle(request.getMilestoneTitle());
        milestone.setMilestoneDate(parseDate(request.getMilestoneDate()));
        milestone.setIsAchieved(0);
        milestone.setSort(sort);
        milestone = careerMilestoneRepository.save(milestone);
        // 新增未达成里程碑后，若行动已有里程碑则按达成占比重算状态与完成率
        syncActionProgress(actionId);
        return CareerPlanIdResponse.builder().milestoneId(milestone.getId()).build();
    }

    // ==================== 更新/删除目标、行动、里程碑（4.8.1~4.10.2） ====================

    /**
     * 更新目标（PUT /profile/career-plans/{planId}/goals/{goalId}）
     * <p>
     * 部分更新语义：goalTitle 必填，其余字段不传（null）保留原值，传空字符串清空。
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param goalId  目标 ID
     * @param request 更新请求
     * @return 目标 ID
     */
    @Transactional
    public CareerPlanIdResponse updateGoal(Long userId, Long planId, Long goalId, CareerGoalUpdateRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        CareerGoal goal = careerGoalRepository.findByIdAndCareerPlanId(goalId, planId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "目标不存在"));
        goal.setGoalTitle(request.getGoalTitle());
        if (request.getGoalDesc() != null) {
            goal.setGoalDesc(request.getGoalDesc());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(parseDate(request.getTargetDate()));
        }
        if (request.getSort() != null) {
            goal.setSort(request.getSort());
        }
        careerGoalRepository.save(goal);
        return CareerPlanIdResponse.builder().goalId(goal.getId()).build();
    }

    /**
     * 更新行动（PUT /profile/career-plans/{planId}/actions/{actionId}）
     * <p>
     * 部分更新语义：actionTitle 必填，其余字段不传（null）保留原值，传空字符串清空。
     * 状态仍走 4.12 更新行动状态接口，本接口不接收 status。
     * 归属经 行动→目标→规划 链路校验。
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param actionId 行动 ID
     * @param request  更新请求
     * @return 行动 ID
     */
    @Transactional
    public CareerPlanIdResponse updateAction(Long userId, Long planId, Long actionId,
                                             CareerActionUpdateRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        CareerAction action = requireActionInPlan(actionId, planId);
        action.setActionTitle(request.getActionTitle());
        if (request.getActionDesc() != null) {
            action.setActionDesc(request.getActionDesc());
        }
        if (request.getStartDate() != null) {
            action.setStartDate(parseDate(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            action.setEndDate(parseDate(request.getEndDate()));
        }
        if (request.getSort() != null) {
            action.setSort(request.getSort());
        }
        careerActionRepository.save(action);
        return CareerPlanIdResponse.builder().actionId(action.getId()).build();
    }

    /**
     * 更新里程碑（PUT /profile/career-plans/{planId}/milestones/{milestoneId}）
     * <p>
     * 部分更新语义：milestoneTitle 必填，其余字段不传（null）保留原值，传空字符串清空。
     * isAchieved=1 时写入 achieved_at，回退为 0 时清空。
     * 归属经 里程碑→行动→目标→规划 链路校验。
     *
     * @param userId      当前登录用户 ID
     * @param planId      规划 ID
     * @param milestoneId 里程碑 ID
     * @param request     更新请求
     * @return 里程碑 ID
     */
    @Transactional
    public CareerPlanIdResponse updateMilestone(Long userId, Long planId, Long milestoneId,
                                                CareerMilestoneUpdateRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        CareerMilestone milestone = careerMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "里程碑不存在"));
        requireActionInPlan(milestone.getActionId(), planId);
        milestone.setMilestoneTitle(request.getMilestoneTitle());
        if (request.getMilestoneDate() != null) {
            milestone.setMilestoneDate(parseDate(request.getMilestoneDate()));
        }
        if (request.getIsAchieved() != null) {
            boolean achieved = request.getIsAchieved() == 1;
            milestone.setIsAchieved(request.getIsAchieved());
            milestone.setAchievedAt(achieved ? LocalDateTime.now() : null);
        }
        // 成果证明材料：0=清空解绑（并清理原证明），>0=重新绑定（先释放旧证明再绑定新文件）
        Long proofFileId = request.getProofFileId();
        if (proofFileId != null) {
            if (proofFileId == 0L) {
                unbindMilestoneProof(milestone.getProofFileId(), userId);
                milestone.setProofFileId(null);
            } else if (milestone.getProofFileId() == null || !milestone.getProofFileId().equals(proofFileId)) {
                unbindMilestoneProof(milestone.getProofFileId(), userId);
                AttachmentRelation relation = bindFile(proofFileId, userId,
                        AttachmentBizTypeEnum.CAREER_MILESTONE.getValue(), milestoneId);
                milestone.setProofFileId(relation.getId());
            }
        }
        careerMilestoneRepository.save(milestone);
        // 达成状态/里程碑变化后联动重算行动完成率与状态（存在里程碑时）
        syncActionProgress(milestone.getActionId());
        return CareerPlanIdResponse.builder().milestoneId(milestone.getId()).build();
    }

    /**
     * 删除目标（DELETE /profile/career-plans/{planId}/goals/{goalId}）
     * <p>
     * 级联软删其行动、里程碑与行动成果文件，并重算规划进度。
     *
     * @param userId 当前登录用户 ID
     * @param planId 规划 ID
     * @param goalId 目标 ID
     */
    @Transactional
    public void deleteGoal(Long userId, Long planId, Long goalId) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        careerGoalRepository.findByIdAndCareerPlanId(goalId, planId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "目标不存在"));
        for (CareerAction action : careerActionRepository.findByGoalIdOrderBySortAsc(goalId)) {
            deleteActionCascade(action.getId(), userId);
        }
        careerGoalRepository.softDeleteById(goalId, LocalDateTime.now());
        recomputePlanProgress(planId);
    }

    /**
     * 删除行动（DELETE /profile/career-plans/{planId}/actions/{actionId}）
     * <p>
     * 级联软删其里程碑与行动成果文件，并重算目标状态与规划进度。
     *
     * @param userId   当前登录用户 ID
     * @param planId   规划 ID
     * @param actionId 行动 ID
     */
    @Transactional
    public void deleteAction(Long userId, Long planId, Long actionId) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        CareerAction action = requireActionInPlan(actionId, planId);
        deleteActionCascade(actionId, userId);
        updateGoalStatus(action.getGoalId());
        recomputePlanProgress(planId);
    }

    /**
     * 删除里程碑（DELETE /profile/career-plans/{planId}/milestones/{milestoneId}）
     * <p>
     * 软删里程碑及其成果证明文件；删除后若行动仍存在其他里程碑，
     * 按剩余里程碑达成占比重算行动状态与完成率。
     *
     * @param userId      当前登录用户 ID
     * @param planId      规划 ID
     * @param milestoneId 里程碑 ID
     */
    @Transactional
    public void deleteMilestone(Long userId, Long planId, Long milestoneId) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        ensureEditable(plan);
        CareerMilestone milestone = careerMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "里程碑不存在"));
        requireActionInPlan(milestone.getActionId(), planId);
        unbindMilestoneProof(milestone.getProofFileId(), userId);
        int affected = careerMilestoneRepository.softDeleteById(milestoneId, LocalDateTime.now());
        if (affected == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "里程碑不存在");
        }
        syncActionProgress(milestone.getActionId());
    }

    // ==================== 更新行动状态（4.12） ====================

    /**
     * 更新行动状态并重算规划进度（PUT /profile/career-plans/{planId}/actions/{actionId}/status）
     *
     * @param userId  当前登录用户 ID
     * @param planId  规划 ID
     * @param actionId 行动 ID
     * @param request 状态请求（0=未开始 1=进行中 2=已完成）
     * @return 更新后的行动状态
     */
    @Transactional
    public CareerActionStatusResponse updateActionStatus(Long userId, Long planId, Long actionId,
                                                         CareerActionStatusRequest request) {
        getOwnedPlan(userId, planId);
        CareerAction action = requireActionInPlan(actionId, planId);

        Integer status = request.getStatus();
        boolean completed = status != null && status == 2;
        action.setStatus(status);
        if (completed) {
            action.setCompletionRate(100);
            action.setCompletedAt(LocalDateTime.now());
        } else {
            // 行动存在里程碑时，完成率按里程碑达成占比回填（对齐表文档 V5.6），
            // 避免手动回退状态丢失已由里程碑积累的部分进度
            List<CareerMilestone> milestones = careerMilestoneRepository.findByActionIdOrderBySortAsc(actionId);
            long achieved = milestones.stream()
                    .filter(m -> m.getIsAchieved() != null && m.getIsAchieved() == 1)
                    .count();
            action.setCompletionRate(milestones.isEmpty() ? 0
                    : (int) Math.round(100.0 * achieved / milestones.size()));
            action.setCompletedAt(null);
        }
        careerActionRepository.save(action);

        updateGoalStatus(action.getGoalId());
        recomputePlanProgress(planId);

        return CareerActionStatusResponse.builder()
                .actionId(action.getId())
                .status(action.getStatus())
                .statusLabel(CareerProgressStatusEnum.of(action.getStatus()).getLabel())
                .completionRate(action.getCompletionRate())
                .build();
    }

    // ==================== 上传行动成果（4.13） ====================

    /**
     * 绑定行动成果文件（POST /profile/career-plans/{planId}/actions/{actionId}/files）
     */
    @Transactional
    public CareerActionFileResponse bindActionFile(Long userId, Long planId, Long actionId,
                                                   CareerActionFileRequest request) {
        getOwnedPlan(userId, planId);
        requireActionInPlan(actionId, planId);
        AttachmentRelation relation = bindFile(request.getFileId(), userId,
                AttachmentBizTypeEnum.CAREER_ACTION.getValue(), actionId);
        return CareerActionFileResponse.builder()
                .fileId(relation.getId())
                .fileName(relation.getOriginalName())
                .fileUrl(ossFileService.generatePresignedUrl(relation.getFilePath(), ossProperties.getUrlExpireMinutes()))
                .build();
    }

    // ==================== 添加阶段反思（4.14） ====================

    /**
     * 添加阶段反思（POST /profile/career-plans/{planId}/reflections）
     */
    @Transactional
    public CareerPlanIdResponse addReflection(Long userId, Long planId, CareerReflectionAddRequest request) {
        CareerPlan plan = getOwnedPlan(userId, planId);
        CareerReflection reflection = new CareerReflection();
        reflection.setCareerPlanId(planId);
        reflection.setUserId(userId);
        reflection.setSemesterId(plan.getSemesterId());
        reflection.setReflectionContent(request.getReflectionContent());
        reflection = careerReflectionRepository.save(reflection);
        return CareerPlanIdResponse.builder().reflectionId(reflection.getId()).build();
    }

    // ==================== AI建议一键添加（4.15） ====================

    /**
     * AI建议一键添加为计划（POST /profile/career-plans/ai-add）
     * <p>
     * 根据 improvement_suggestions 内容生成 goals 与 actions，
     * career_plans.source=2、ai_suggestion_id 记录来源。
     *
     * @param userId  当前登录用户 ID
     * @param request AI建议创建请求
     * @return 创建结果
     */
    @Transactional
    public AiPlanCreateResponse aiAddPlan(Long userId, AiPlanCreateRequest request) {
        ImprovementSuggestion suggestion = improvementSuggestionRepository.findById(request.getAiSuggestionId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "AI建议不存在"));
        // 归属校验：经 weakness_id → weakness_analyses.user_id；weaknessId 为空时跳过严格校验
        if (suggestion.getWeaknessId() != null) {
            WeaknessAnalysis weakness = weaknessAnalysisRepository.findById(suggestion.getWeaknessId()).orElse(null);
            if (weakness != null && !userId.equals(weakness.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无访问权限");
            }
        }
        semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "目标学期不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));

        CareerPlan plan = new CareerPlan();
        plan.setSchoolId(user.getSchoolId() != null ? user.getSchoolId() : 1L);
        plan.setUserId(userId);
        plan.setSemesterId(request.getSemesterId());
        plan.setTitle(request.getTitle() != null ? request.getTitle() : defaultAiTitle(suggestion));
        plan.setContent(suggestion.getSuggestionContent());
        plan.setSource(2);
        plan.setAiSuggestionId(suggestion.getId());
        plan.setRequireConfirm(request.getRequireConfirm() != null ? request.getRequireConfirm() : 1);
        plan.setProgressRate(0);
        plan.setStatus(0);
        ArchiveAuditInfo audit = new ArchiveAuditInfo();
        audit.setCurrentVersion(1);
        audit.setSubmitCount(0);
        audit.setDraftSavedAt(LocalDateTime.now());
        plan.setAuditInfo(audit);
        plan = careerPlanRepository.save(plan);

        // 按建议内容逐行生成目标 + 行动
        String content = suggestion.getSuggestionContent();
        List<String> lines = (content == null || content.isBlank())
                ? List.of(plan.getTitle())
                : Arrays.stream(content.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(10)
                        .collect(Collectors.toList());
        int gi = 0;
        for (String line : lines) {
            CareerGoal goal = new CareerGoal();
            goal.setCareerPlanId(plan.getId());
            goal.setGoalTitle(truncate(line, 100));
            goal.setGoalDesc(content);
            goal.setSource(2);
            goal.setAiSuggestionId(suggestion.getId());
            goal.setStatus(0);
            goal.setSort(gi++);
            goal = careerGoalRepository.save(goal);

            CareerAction action = new CareerAction();
            action.setGoalId(goal.getId());
            action.setActionTitle(truncate(line, 100));
            action.setActionDesc(content);
            action.setStatus(0);
            action.setSource(2);
            action.setAiSuggestionId(suggestion.getId());
            action.setCompletionRate(0);
            action.setSort(0);
            careerActionRepository.save(action);
        }

        return AiPlanCreateResponse.builder()
                .planId(plan.getId())
                .status(0)
                .statusLabel(ApplyStatusEnum.of(0).getLabel())
                .source(2)
                .sourceLabel(PLAN_SOURCE_LABELS.getOrDefault(2, "AI建议添加"))
                .requireConfirm(plan.getRequireConfirm())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取归属当前用户的规划，否则抛无访问权限
     */
    private CareerPlan getOwnedPlan(Long userId, Long planId) {
        return careerPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN, "无访问权限"));
    }

    /**
     * 规划是否可编辑（仅草稿/已退回）
     */
    private void ensureEditable(CareerPlan plan) {
        int status = plan.getStatus() != null ? plan.getStatus() : 0;
        if (status != 0 && status != 3) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "当前状态不可编辑");
        }
    }

    /**
     * 校验行动属于当前规划（行动→目标→规划 链路归属）
     */
    private CareerAction requireActionInPlan(Long actionId, Long planId) {
        CareerAction action = careerActionRepository.findById(actionId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "行动不存在"));
        careerGoalRepository.findByIdAndCareerPlanId(action.getGoalId(), planId)
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN, "无访问权限"));
        return action;
    }

    /**
     * 级联软删行动：先删其里程碑与行动成果文件，再删行动本身。
     *
     * @param actionId 行动 ID
     * @param userId   当前登录用户 ID（写入附件软删的 deleted_by）
     */
    private void deleteActionCascade(Long actionId, Long userId) {
        for (CareerMilestone milestone : careerMilestoneRepository.findByActionIdOrderBySortAsc(actionId)) {
            int affected = careerMilestoneRepository.softDeleteById(milestone.getId(), LocalDateTime.now());
            if (affected == 0) {
                throw new BusinessException(ResultCode.DATA_NOT_EXIST, "里程碑不存在");
            }
        }
        deleteActionFiles(actionId, userId);
        int affected = careerActionRepository.softDeleteById(actionId, LocalDateTime.now());
        if (affected == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "行动不存在");
        }
    }

    /**
     * 删除行动已绑定的成果文件：软删 file_uploads 记录并删除 OSS 物理文件
     * （与通用接口 2.1.3 删除附件口径一致）。
     *
     * @param actionId 行动 ID
     * @param userId   当前登录用户 ID（写入附件软删的 deleted_by）
     */
    private void deleteActionFiles(Long actionId, Long userId) {
        List<AttachmentRelation> files = attachmentRelationRepository
                .findByBizTypeAndBizIdOrderBySortOrderAsc(AttachmentBizTypeEnum.CAREER_ACTION.getValue(), actionId);
        for (AttachmentRelation relation : files) {
            try {
                ossFileService.deleteFile(relation.getFilePath());
            } catch (Exception e) {
                log.warn("OSS 文件删除失败（可能已被清理）: objectKey={}", relation.getFilePath(), e);
            }
            attachmentRelationRepository.softDeleteById(relation.getId(), LocalDateTime.now(), userId);
        }
    }

    /**
     * 绑定文件到业务记录（校验 file_status=1 暂存、归属当前用户且未软删）
     */
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

    /**
     * 解绑并清理里程碑成果证明：软删 attachment_relations 记录并删除 OSS 物理文件
     * （与行动成果删除口径一致）。proofFileId 为空或已不存在时静默跳过。
     *
     * @param proofFileId 里程碑已绑定的证明文件 ID
     * @param userId      当前登录用户 ID（写入附件软删的 deleted_by）
     */
    private void unbindMilestoneProof(Long proofFileId, Long userId) {
        if (proofFileId == null) return;
        try {
            AttachmentRelation relation = attachmentRelationRepository.findById(proofFileId).orElse(null);
            if (relation == null || !userId.equals(relation.getUserId())) return;
            ossFileService.deleteFile(relation.getFilePath());
            attachmentRelationRepository.softDeleteById(proofFileId, LocalDateTime.now(), userId);
        } catch (Exception e) {
            log.warn("里程碑证明解绑失败（可能已被清理）: proofFileId={}", proofFileId, e);
        }
    }

    /**
     * 批量加载里程碑成果证明文件 ID → 附件关系 映射（消除详情逐条查询的 N+1 问题）。
     */
    private Map<Long, AttachmentRelation> collectMilestoneProofFiles(
            Map<Long, List<CareerMilestone>> milestonesByAction) {
        Set<Long> proofIds = milestonesByAction.values().stream()
                .flatMap(List::stream)
                .map(CareerMilestone::getProofFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (proofIds.isEmpty()) return Collections.emptyMap();
        return attachmentRelationRepository.findAllById(proofIds).stream()
                .collect(Collectors.toMap(AttachmentRelation::getId, f -> f, (a, b) -> a));
    }

    /**
     * 级联创建目标/行动/里程碑
     */
    private void createNestedStructures(Long planId, List<CareerPlanCreateRequest.GoalItem> goals) {
        if (goals == null) return;
        int goalSort = 0;
        for (CareerPlanCreateRequest.GoalItem g : goals) {
            CareerGoal goal = new CareerGoal();
            goal.setCareerPlanId(planId);
            goal.setGoalTitle(g.getGoalTitle());
            goal.setGoalDesc(g.getGoalDesc());
            goal.setTargetDate(parseDate(g.getTargetDate()));
            goal.setSource(1);
            goal.setStatus(0);
            goal.setSort(goalSort++);
            goal = careerGoalRepository.save(goal);

            if (g.getActions() == null) continue;
            int actionSort = 0;
            for (CareerPlanCreateRequest.ActionItem a : g.getActions()) {
                CareerAction action = new CareerAction();
                action.setGoalId(goal.getId());
                action.setActionTitle(a.getActionTitle());
                action.setActionDesc(a.getActionDesc());
                action.setStatus(0);
                action.setSource(1);
                action.setStartDate(parseDate(a.getStartDate()));
                action.setEndDate(parseDate(a.getEndDate()));
                action.setCompletionRate(0);
                action.setSort(actionSort++);
                action = careerActionRepository.save(action);

                if (a.getMilestones() == null) continue;
                int milestoneSort = 0;
                for (CareerPlanCreateRequest.MilestoneItem m : a.getMilestones()) {
                    CareerMilestone milestone = new CareerMilestone();
                    milestone.setActionId(action.getId());
                    milestone.setMilestoneTitle(m.getMilestoneTitle());
                    milestone.setMilestoneDate(parseDate(m.getMilestoneDate()));
                    milestone.setIsAchieved(0);
                    milestone.setSort(milestoneSort++);
                    careerMilestoneRepository.save(milestone);
                }
            }
        }
    }

    /**
     * 写入 model_versions 版本记录
     */
    private void writeModelVersion(CareerPlan plan, Long userId) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", plan.getId());
        snapshot.put("semesterId", plan.getSemesterId());
        snapshot.put("title", plan.getTitle());
        snapshot.put("content", plan.getContent());
        snapshot.put("requirement", plan.getRequirement());
        snapshot.put("status", plan.getStatus());
        snapshot.put("progressRate", plan.getProgressRate());
        snapshot.put("source", plan.getSource());

        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setModelType(ModelVersionModelTypeEnum.CAREER_PLAN.getValue());
        modelVersion.setModelId(plan.getId());
        modelVersion.setVersion(1);
        modelVersion.setTitle(plan.getTitle());
        modelVersion.setDataSnapshot(writeJson(snapshot));
        modelVersion.setStatus(plan.getStatus());
        modelVersion.setCreatedBy(userId);
        modelVersionRepository.save(modelVersion);
    }

    /**
     * 重算规划整体进度：按行动等权，statusWeight 0→0、1→0.5、2→1
     */
    private void recomputePlanProgress(Long planId) {
        CareerPlan plan = careerPlanRepository.findById(planId).orElse(null);
        if (plan == null) return;
        List<CareerGoal> goals = careerGoalRepository.findByCareerPlanIdOrderBySortAsc(planId);
        if (goals.isEmpty()) {
            plan.setProgressRate(0);
            careerPlanRepository.save(plan);
            return;
        }
        List<CareerAction> actions = new ArrayList<>();
        for (CareerGoal goal : goals) {
            actions.addAll(careerActionRepository.findByGoalIdOrderBySortAsc(goal.getId()));
        }
        if (actions.isEmpty()) {
            plan.setProgressRate(0);
        } else {
            double sum = 0;
            for (CareerAction action : actions) {
                sum += actionStatusWeight(action.getStatus());
            }
            plan.setProgressRate((int) Math.round(100.0 * sum / actions.size()));
        }
        careerPlanRepository.save(plan);
    }

    /**
     * 更新目标状态：全部行动完成→2；存在进行中或已完成行动→1（部分完成也视为进行中，
     * 避免"1个完成+1个未开始"误判为未开始）；否则→0
     */
    private void updateGoalStatus(Long goalId) {
        CareerGoal goal = careerGoalRepository.findById(goalId).orElse(null);
        if (goal == null) return;
        List<CareerAction> actions = careerActionRepository.findByGoalIdOrderBySortAsc(goalId);
        if (actions.isEmpty()) {
            goal.setStatus(0);
        } else if (actions.stream().allMatch(a -> a.getStatus() != null && a.getStatus() == 2)) {
            goal.setStatus(2);
        } else if (actions.stream().anyMatch(a -> a.getStatus() != null && (a.getStatus() == 1 || a.getStatus() == 2))) {
            goal.setStatus(1);
        } else {
            goal.setStatus(0);
        }
        careerGoalRepository.save(goal);
    }

    /**
     * 按里程碑达成情况推导行动状态与完成率（对齐表文档 V5.6：completion_rate 由
     * 里程碑 is_achieved 占比自动回填）。行动存在里程碑时，里程碑为进展唯一来源：
     * 全部达成→2已完成、部分达成→1进行中、均未达成→0未开始；完成率=达成数/总数。
     * 行动无里程碑时保持手动维护（4.12），本方法不做处理。
     * 推导完成后联动重算目标状态与规划进度。
     */
    private void syncActionProgress(Long actionId) {
        CareerAction action = careerActionRepository.findById(actionId).orElse(null);
        if (action == null) return;
        List<CareerMilestone> milestones = careerMilestoneRepository.findByActionIdOrderBySortAsc(actionId);
        if (milestones.isEmpty()) return;

        long achieved = milestones.stream()
                .filter(m -> m.getIsAchieved() != null && m.getIsAchieved() == 1)
                .count();
        boolean allAchieved = achieved == milestones.size();
        boolean anyAchieved = achieved > 0;
        action.setCompletionRate((int) Math.round(100.0 * achieved / milestones.size()));
        action.setStatus(allAchieved ? 2 : (anyAchieved ? 1 : 0));
        action.setCompletedAt(allAchieved ? LocalDateTime.now() : null);
        careerActionRepository.save(action);

        updateGoalStatus(action.getGoalId());
        CareerGoal goal = careerGoalRepository.findById(action.getGoalId()).orElse(null);
        if (goal != null) {
            recomputePlanProgress(goal.getCareerPlanId());
        }
    }

    /**
     * 行动状态权重
     */
    private double actionStatusWeight(Integer status) {
        if (status == null) return STATUS_WEIGHT_NOT_STARTED;
        return switch (status) {
            case 2 -> STATUS_WEIGHT_COMPLETED;
            case 1 -> STATUS_WEIGHT_IN_PROGRESS;
            default -> STATUS_WEIGHT_NOT_STARTED;
        };
    }

    /**
     * 规划 → 列表项
     */
    private CareerPlanListItem toListItem(CareerPlan plan, Map<Long, String> auditorNameMap,
                                         Map<Long, String> semesterNameMap) {
        ArchiveAuditInfo audit = plan.getAuditInfo();
        return CareerPlanListItem.builder()
                .id(plan.getId())
                .semesterId(plan.getSemesterId())
                .semesterName(semesterNameMap.get(plan.getSemesterId()))
                .title(plan.getTitle())
                .progressRate(plan.getProgressRate())
                .status(plan.getStatus())
                .statusLabel(ApplyStatusEnum.of(plan.getStatus()).getLabel())
                .submittedAt(audit != null ? toIso(audit.getSubmittedAt()) : null)
                .auditedAt(audit != null ? toIso(audit.getAuditedAt()) : null)
                .currentVersion(audit != null ? audit.getCurrentVersion() : null)
                .submitCount(audit != null ? audit.getSubmitCount() : null)
                .rejectedReason(audit != null ? audit.getRejectedReason() : null)
                .auditorName(audit != null && audit.getAuditorId() != null
                        ? auditorNameMap.get(audit.getAuditorId()) : null)
                .build();
    }

    /**
     * 批量构建学期 ID → 学期名称映射（消除列表逐行查询的 N+1 问题）
     */
    private Map<Long, String> buildSemesterNameMap(List<CareerPlan> plans) {
        Set<Long> ids = plans.stream()
                .map(CareerPlan::getSemesterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return semesterRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getName, (a, b) -> a));
    }

    /**
     * 构建审核教师 ID → 姓名映射
     */
    private Map<Long, String> buildAuditorNameMap(List<CareerPlan> plans) {
        Set<Long> ids = plans.stream()
                .map(CareerPlan::getAuditInfo)
                .filter(Objects::nonNull)
                .map(ArchiveAuditInfo::getAuditorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return userRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }

    /**
     * AI建议默认标题
     */
    private String defaultAiTitle(ImprovementSuggestion suggestion) {
        if (suggestion.getSuggestionContent() != null && !suggestion.getSuggestionContent().isBlank()) {
            return truncate(suggestion.getSuggestionContent().split("\n")[0].trim(), 100);
        }
        return "AI成长建议";
    }

    /**
     * 截断字符串
     */
    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * 解析 YYYY-MM-DD 日期
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "日期格式必须为YYYY-MM-DD");
        }
    }

    /**
     * 学期 ID → 学期名称
     */
    private String semesterName(Long semesterId) {
        if (semesterId == null) return null;
        return semesterRepository.findById(semesterId).map(Semester::getName).orElse(null);
    }

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
     * LocalDateTime → ISO 8601 带时区字符串
     */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    /**
     * LocalDate → "2005-03-15" 字符串
     */
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : null;
    }
}
