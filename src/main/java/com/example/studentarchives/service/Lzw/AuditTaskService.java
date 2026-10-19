package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Lzw.activity.response.ActivityDetailResponse;
import com.example.studentarchives.entity.approval.ApprovalDelegation;
import com.example.studentarchives.entity.approval.ApprovalFlowStep;
import com.example.studentarchives.entity.approval.ApprovalInstance;
import com.example.studentarchives.entity.approval.ApprovalNode;
import com.example.studentarchives.entity.approval.PendingApproval;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.archive.AuditCommentTemplate;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import com.example.studentarchives.entity.log.AuditLog;
import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.enums.ActivityTypeEnum;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.ApprovalInstanceStatusEnum;
import com.example.studentarchives.enums.ApprovalNodeActionEnum;
import com.example.studentarchives.enums.ArchiveTypeEnum;
import com.example.studentarchives.enums.AuditActionEnum;
import com.example.studentarchives.enums.AwardTypeEnum;
import com.example.studentarchives.enums.DuplicateCheckStatusEnum;
import com.example.studentarchives.enums.StatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ApprovalDelegationRepository;
import com.example.studentarchives.repository.ApprovalFlowStepRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ApprovalInstanceRepository;
import com.example.studentarchives.repository.ApprovalNodeRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.AuditCommentTemplateRepository;
import com.example.studentarchives.repository.AuditLogRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PendingApprovalRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.example.studentarchives.service.Fmy.AdminScoreService;
import com.example.studentarchives.service.Fmy.TeacherScopeValidator;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师端「待审核任务模块」Service（《教师端接口文档》四，4.1 ~ 4.7）。
 * <p>
 * 数据来源：pending_approvals（统一待办任务） + approval_nodes / approval_instances
 * （流程节点/实例） + archives / award_applications / career_plans（申报主体）。
 * 详情中的类型专用字段与版本历史复用 {@link ActivityService#getDetail} 的既有映射逻辑。
 * <p>
 * 范围控制：普通教师按 {@code role_scopes} 校验（复用 {@link TeacherScopeValidator}），
 * 管理员（admin 角色）绕过范围校验；撤销接口要求 admin 角色（见 4.6）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditTaskService {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final PendingApprovalRepository pendingApprovalRepository;
    private final ApprovalNodeRepository approvalNodeRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditCommentTemplateRepository auditCommentTemplateRepository;
    private final ApprovalDelegationRepository approvalDelegationRepository;
    private final ApprovalFlowStepRepository approvalFlowStepRepository;
    private final ApprovalSubmitService approvalSubmitService;

    private final ArchiveRepository archiveRepository;
    private final AwardApplicationRepository awardApplicationRepository;
    private final CareerPlanRepository careerPlanRepository;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final SemesterRepository semesterRepository;
    private final DictionaryRepository dictionaryRepository;
    private final UserMessageRepository userMessageRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;

    private final ActivityService activityService;
    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final AdminScoreService adminScoreService;
    private final ObjectMapper objectMapper;

    // ==================== 4.1 获取待审核列表 ====================

    /**
     * 获取待审核列表（GET /teacher/audits/pending）
     * <p>
     * 基础查询 pending_approvals.status=1（待审批）且属于当前学校，再按教师范围、
     * 业务类型/档案类型/学期/年级/关键词/范围类型/范围ID 过滤，最后排序分页。
     * 默认按提交时间升序（先提交先审）。
     */
    public PageResult<PendingListItem> listPending(Long teacherId, String type, String archiveType,
                                                   Integer scopeType, Long scopeId, Long semesterId,
                                                   String grade, String keyword, String sortBy,
                                                   String sortOrder, PageParam pageParam) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        List<PendingApproval> ordered = collectPending(teacherId, schoolId, type, archiveType,
                scopeType, scopeId, semesterId, grade, keyword, sortOrder);

        int total = ordered.size();
        int from = pageParam.getOffset();
        int to = Math.min(from + pageParam.getPerPage(), total);

        Map<Long, ApplicantInfo> applicantCache = new HashMap<>();
        List<PendingListItem> items = (from < total)
                ? ordered.subList(from, to).stream()
                    .map(p -> toListItem(p, applicantCache))
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return PageResult.of(items, total, pageParam);
    }

    // ==================== 4.2 获取待审核详情 ====================

    /**
     * 获取待审核详情（GET /teacher/audits/pending/{taskId}）
     * <p>
     * 详情中的类型专用字段（detail）与版本历史复用 {@link ActivityService#getDetail}，
     * 佐证材料、申请人信息、审批历史、重复检测与 cursor 由本服务组装。
     */
    public PendingDetailResponse getDetail(Long teacherId, Long taskId, String type, String archiveType,
                                           Integer scopeType, Long scopeId, Long semesterId,
                                           String grade, String keyword, String sortOrder) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        PendingApproval p = loadTask(taskId);
        ensureTaskAccess(p, teacherId, schoolId);

        // 复用动态记录详情：拿到类型专用 detail 映射与版本历史
        ActivityTypeEnum activityType = toActivityType(p.getApprovableType());
        ActivityDetailResponse ad = activityType != null
                ? activityService.getDetail(activityType, p.getApprovableId(), p.getApplicantId())
                : null;

        // 申报主体原始字段（baseInfo / duplicateInfo 来源）
        ApprovableInfo info = resolveApprovableInfo(p);
        Map<String, Object> detailMap = ad != null ? ad.getDetail() : Collections.emptyMap();

        ApplicantInfo applicantInfo = buildApplicantInfo(p.getApplicantId());
        User applicantUser = userRepository.findById(p.getApplicantId()).orElse(null);

        return PendingDetailResponse.builder()
                .taskId(p.getId())
                .type(toTypeCode(p.getApprovableType()))
                .archiveType(info != null ? info.archiveType : null)
                .approvableId(p.getApprovableId())
                .title(p.getTitle())
                .applicant(buildApplicant(p, applicantUser, applicantInfo))
                .baseInfo(buildBaseInfo(info, detailMap))
                .detail(detailMap)
                .evidenceFiles(buildEvidenceFiles(p.getApprovableType(), p.getApprovableId()))
                .duplicateInfo(buildDuplicateInfo(info))
                .approvalHistory(buildApprovalHistory(p.getInstanceId()))
                .versionHistory(buildVersionHistory(ad))
                .cursor(buildCursor(teacherId, schoolId, taskId, type, archiveType, scopeType, scopeId,
                        semesterId, grade, keyword, sortOrder))
                .build();
    }

    // ==================== 4.3 单个审核通过 ====================

    /** 单个审核通过（POST /teacher/audits/{taskId}/approve） */
    @Transactional
    public ApproveResult approve(Long teacherId, Long taskId, String comment, Long nextAuditorId) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        return approveInternal(taskId, comment, nextAuditorId, teacherId, schoolId);
    }

    private ApproveResult approveInternal(Long taskId, String comment, Long nextAuditorId,
                                          Long teacherId, Long schoolId) {
        PendingApproval p = loadTask(taskId);
        ensureTaskAccess(p, teacherId, schoolId);
        if (!Integer.valueOf(1).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "任务已处理，请勿重复审核");
        }
        LocalDateTime now = LocalDateTime.now();

        // 1. 完成当前节点（动作=通过）
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node != null) {
            node.setAction(ApprovalNodeActionEnum.APPROVE.getValue());
            node.setActualAuditorId(teacherId);
            node.setComment(comment);
            node.setCompletedAt(now);
            approvalNodeRepository.save(node);
        }

        // 2. 当前待办置为已通过
        p.setStatus(ApplyStatusEnum.APPROVED.getValue());
        pendingApprovalRepository.save(p);

        // 3. 推进流程：存在下一步则生成下一步节点与待办（非终审），否则整体通过（终审）
        ApprovalInstance inst = approvalInstanceRepository.findById(p.getInstanceId()).orElse(null);
        ApprovalFlowStep nextStep = resolveNextStep(inst, p.getStepNo());
        if (nextStep != null) {
            // 非终审：仅当当前步骤允许指定下一审批人时才采纳 nextAuditorId
            ApprovalFlowStep currentStep = resolveStep(inst, p.getStepNo());
            boolean designateAllowed = currentStep != null
                    && Integer.valueOf(1).equals(currentStep.getAllowDesignateNext());
            if (nextAuditorId != null && !designateAllowed) {
                log.warn("当前步骤不允许指定下一审批人，忽略 nextAuditorId: taskId={}, stepNo={}",
                        taskId, p.getStepNo());
            }
            Long designated = designateAllowed ? nextAuditorId : null;

            ApprovalNode nextNode = approvalSubmitService.createStepNode(inst, nextStep, schoolId,
                    p.getApplicantId(), p.getApplicantName(), p.getApplicantNo(), p.getTitle(),
                    p.getCategoryLabel(), p.getSubmittedAt(), designated);

            writeAuditLog(p, AuditActionEnum.APPROVE.getValue(), comment, null, teacherId, 1, null);

            if (nextNode != null) {
                inst.setCurrentStep(nextStep.getStepNo());
                approvalInstanceRepository.save(inst);
                notifyStudent(p, "audit_remind", "申报进入下一级审批",
                        "您的申报「" + p.getTitle() + "」已通过当前审核，等待下一级审批。");
            } else {
                // 下一步审批人解析失败（无校级管理员兜底），保持实例审批中，等待管理员配置
                log.error("多级审批推进失败，下一步无审批人: taskId={}, instanceId={}, nextStepNo={}",
                        taskId, p.getInstanceId(), nextStep.getStepNo());
            }
            return ApproveResult.builder()
                    .taskId(p.getId())
                    .approvableId(p.getApprovableId())
                    .status(ApplyStatusEnum.APPROVED.getValue())
                    .statusLabel("已通过（进入下一级）")
                    .auditedAt(format(now))
                    .build();
        }

        // 终审通过
        if (inst != null) {
            inst.setStatus(ApprovalInstanceStatusEnum.APPROVED.getValue());
            inst.setCompletedAt(now);
            approvalInstanceRepository.save(inst);
        }

        ApprovableRef ref = approveApprovable(p, now, teacherId);
        writeAuditLog(p, AuditActionEnum.APPROVE.getValue(), comment, null, teacherId, ref.version(), null);
        notifyStudent(p, "audit_remind", "申报审核通过", "您的申报「" + p.getTitle() + "」已审核通过。");
        triggerScoreRecalc(schoolId, p, ref.semesterId(), teacherId);

        return ApproveResult.builder()
                .taskId(p.getId())
                .approvableId(p.getApprovableId())
                .status(ApplyStatusEnum.APPROVED.getValue())
                .statusLabel(ApplyStatusEnum.APPROVED.getLabel())
                .auditedAt(format(now))
                .build();
    }

    /** 解析流程实例中指定步骤号的步骤配置（无实例/步骤号返回 null） */
    private ApprovalFlowStep resolveStep(ApprovalInstance inst, Integer stepNo) {
        if (inst == null || stepNo == null) {
            return null;
        }
        return approvalFlowStepRepository.findByFlowIdOrderByStepNoAsc(inst.getFlowId()).stream()
                .filter(s -> Objects.equals(s.getStepNo(), stepNo))
                .findFirst().orElse(null);
    }

    /** 解析当前步骤之后的下一步骤（stepNo 严格大于当前步骤号的最小者） */
    private ApprovalFlowStep resolveNextStep(ApprovalInstance inst, Integer currentStepNo) {
        if (inst == null || currentStepNo == null) {
            return null;
        }
        return approvalFlowStepRepository.findByFlowIdOrderByStepNoAsc(inst.getFlowId()).stream()
                .filter(s -> s.getStepNo() != null && s.getStepNo() > currentStepNo)
                .min(Comparator.comparing(ApprovalFlowStep::getStepNo))
                .orElse(null);
    }

    // ==================== 4.4 单个审核退回 ====================

    /** 单个审核退回（POST /teacher/audits/{taskId}/reject） */
    @Transactional
    public RejectResult reject(Long teacherId, Long taskId, String comment, String templateCode,
                               Integer rejectToStep) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "退回原因不能为空");
        }
        // 退回原因模板使用频率统计
        if (templateCode != null && !templateCode.isBlank()) {
            incrementTemplateUsage(schoolId, templateCode);
        }
        // rejectToStep 仅在多级流程「退回到指定步骤」时生效，当前单节点流程默认退回申请人，暂不落地该字段。

        PendingApproval p = loadTask(taskId);
        ensureTaskAccess(p, teacherId, schoolId);
        if (!Integer.valueOf(1).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "任务已处理，请勿重复审核");
        }
        LocalDateTime now = LocalDateTime.now();

        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node != null) {
            node.setAction(ApprovalNodeActionEnum.REJECT.getValue());
            node.setActualAuditorId(teacherId);
            node.setComment(comment);
            node.setCompletedAt(now);
            approvalNodeRepository.save(node);
        }

        p.setStatus(ApplyStatusEnum.REJECTED.getValue());
        pendingApprovalRepository.save(p);

        approvalInstanceRepository.findById(p.getInstanceId()).ifPresent(inst -> {
            inst.setStatus(ApprovalInstanceStatusEnum.REJECTED.getValue());
            inst.setCompletedAt(now);
            approvalInstanceRepository.save(inst);
        });

        ApprovableRef ref = rejectApprovable(p, comment, now, teacherId);
        writeAuditLog(p, AuditActionEnum.REJECT.getValue(), comment, null, teacherId, ref.version(), null);
        notifyStudent(p, "audit_remind", "申报被退回", "您的申报「" + p.getTitle() + "」被退回：" + comment);
        triggerScoreRecalc(p.getSchoolId(), p, ref.semesterId(), teacherId);

        return RejectResult.builder()
                .taskId(p.getId())
                .approvableId(p.getApprovableId())
                .status(ApplyStatusEnum.REJECTED.getValue())
                .statusLabel(ApplyStatusEnum.REJECTED.getLabel())
                .returnedAt(format(now))
                .rejectedReason(comment)
                .build();
    }

    // ==================== 4.5 批量审核通过 ====================

    /** 批量审核通过（POST /teacher/audits/batch/approve）：逐条处理，单条失败不影响其它（部分成功）。 */
    @Transactional
    public BatchResult batchApprove(Long teacherId, List<Long> taskIds, String comment) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "taskIds 不能为空");
        }
        List<BatchResultItem> results = new ArrayList<>();
        int success = 0;
        int failed = 0;
        for (Long taskId : taskIds) {
            try {
                ApproveResult r = approveInternal(taskId, comment, null, teacherId, schoolId);
                results.add(new BatchResultItem(taskId, true, r.getStatus()));
                success++;
            } catch (BusinessException e) {
                results.add(new BatchResultItem(taskId, false, null));
                failed++;
            }
        }
        return new BatchResult(taskIds.size(), success, failed, results);
    }

    // ==================== 4.6 撤销已审核记录 ====================

    /** 撤销已审核记录（POST /teacher/audits/{taskId}/revoke，admin 角色或 audit:revoke 权限码）。 */
    @Transactional
    public RevokeResult revoke(Long teacherId, Long taskId, String revokeReason) {
        adminAuthService.requireAdminOrPermission(teacherId, "audit:revoke");
        if (revokeReason == null || revokeReason.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "撤销原因不能为空");
        }
        PendingApproval p = loadTask(taskId);
        if (!Integer.valueOf(2).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅已通过的记录可撤销");
        }
        LocalDateTime now = LocalDateTime.now();

        Long revokedLogId = auditLogRepository
                .findTopByAuditableTypeAndAuditableIdAndActionOrderByIdDesc(
                        p.getApprovableType(), p.getApprovableId(), AuditActionEnum.APPROVE.getValue())
                .map(AuditLog::getId)
                .orElse(null);

        p.setStatus(ApplyStatusEnum.REVOKED.getValue());
        pendingApprovalRepository.save(p);

        approvalInstanceRepository.findById(p.getInstanceId()).ifPresent(inst -> {
            inst.setStatus(ApprovalInstanceStatusEnum.REVOKED.getValue());
            inst.setCompletedAt(now);
            approvalInstanceRepository.save(inst);
        });

        ApprovableRef ref = revokeApprovable(p, now, teacherId);
        writeAuditLog(p, AuditActionEnum.WITHDRAW.getValue(), null, revokeReason, teacherId, ref.version(), revokedLogId);
        notifyStudent(p, "audit_remind", "申报记录被撤销", "您的申报「" + p.getTitle() + "」审核记录被撤销：" + revokeReason);
        triggerScoreRecalc(p.getSchoolId(), p, ref.semesterId(), teacherId);

        return RevokeResult.builder()
                .taskId(p.getId())
                .approvableId(p.getApprovableId())
                .status(ApplyStatusEnum.REVOKED.getValue())
                .statusLabel(ApplyStatusEnum.REVOKED.getLabel())
                .revokeReason(revokeReason)
                .revokedAt(format(now))
                .build();
    }

    // ==================== 4.8 转交待审核任务 ====================

    /**
     * 转交待审核任务（POST /teacher/audits/{taskId}/transfer）
     * <p>
     * 当前审批人将待审核任务转交给本校另一位审批教师：任务保持待审核状态，
     * 待办改派给目标审批人，并在审核记录（audit_logs.action=4「转交」）中留痕，
     * 同时向目标审批人发送审核提醒。转交后原审批人不再持有该待办。
     */
    @Transactional
    public TransferResult transfer(Long teacherId, Long taskId, Long targetAuditorId, String comment) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        if (targetAuditorId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "转交对象不能为空");
        }
        if (Objects.equals(targetAuditorId, teacherId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能转交给本人");
        }
        User target = userRepository.findById(targetAuditorId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "转交对象不存在"));
        if (!Objects.equals(target.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能转交给本校教师");
        }
        if (!isAuditor(targetAuditorId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转交对象不具备审批角色");
        }

        PendingApproval p = loadTask(taskId);
        ensureTaskAccess(p, teacherId, schoolId);
        if (!Integer.valueOf(1).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "任务已处理，请勿重复操作");
        }
        LocalDateTime now = LocalDateTime.now();

        // 节点改派：待办归属目标审批人（节点仍待审核，action 不置位）
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node != null) {
            node.setAssignedAuditorId(targetAuditorId);
            approvalNodeRepository.save(node);
        }

        // 待办改派给目标审批人
        p.setAuditorId(targetAuditorId);
        pendingApprovalRepository.save(p);

        // 审核记录写入「转交」（audit_logs.action=4），管理员「审核记录」按转交筛选即可命中
        writeAuditLog(p, AuditActionEnum.TRANSFER.getValue(), buildTransferComment(target.getName(), comment),
                null, teacherId, 1, null);

        // 通知目标审批人
        notifyAuditor(targetAuditorId, "audit_remind", "收到转交的待审核任务",
                "「" + p.getTitle() + "」已转交给您审核，请及时处理。", p);

        return TransferResult.builder()
                .taskId(p.getId())
                .approvableId(p.getApprovableId())
                .targetAuditorId(targetAuditorId)
                .transferredAt(format(now))
                .build();
    }

    // ==================== 4.7 获取常用退回原因模板 ====================

    /** 获取常用退回原因模板（GET /teacher/audits/reject-templates），按使用频率降序、排序升序。 */
    public List<RejectTemplateItem> getRejectTemplates(Long teacherId) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        return auditCommentTemplateRepository.findBySchoolIdAndStatus(schoolId, 1).stream()
                .filter(t -> Integer.valueOf(2).equals(t.getCategory()))
                .sorted(Comparator
                        .comparing(AuditCommentTemplate::getUsageCount,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AuditCommentTemplate::getSort,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> new RejectTemplateItem(t.getTemplateCode(), t.getTemplateContent(), t.getUsageCount()))
                .collect(Collectors.toList());
    }

    // ==================== 私有：列表聚合 ====================

    /** 收集教师待审任务并过滤排序（不设分页），供列表与 cursor 共用。 */
    private List<PendingApproval> collectPending(Long teacherId, Long schoolId, String type,
                                                 String archiveType, Integer scopeType, Long scopeId,
                                                 Long semesterId, String grade, String keyword,
                                                 String sortOrder) {
        List<PendingApproval> all = pendingApprovalRepository.findByStatusAndSchoolId(1, schoolId);
        Set<Long> delegatedDelegatorIds = resolveDelegatedDelegatorIds(teacherId);
        List<PendingApproval> result = new ArrayList<>();
        for (PendingApproval p : all) {
            if (!isInScope(p, teacherId, schoolId, delegatedDelegatorIds)) {
                continue;
            }
            if (type != null && !type.isEmpty() && !type.equals(toTypeCode(p.getApprovableType()))) {
                continue;
            }
            ApprovableInfo info = resolveApprovableInfo(p);
            if (info == null) {
                continue;
            }
            if (archiveType != null && !archiveType.isEmpty() && !archiveType.equals(info.archiveType)) {
                continue;
            }
            if (semesterId != null && !semesterId.equals(info.semesterId)) {
                continue;
            }
            if (grade != null && !grade.isEmpty() && !grade.equals(buildApplicantInfo(p.getApplicantId()).grade)) {
                continue;
            }
            if (keyword != null && !keyword.isEmpty() && !matchesKeyword(p, keyword)) {
                continue;
            }
            if (scopeType != null && !matchesNodeScope(p, scopeType, scopeId)) {
                continue;
            }
            result.add(p);
        }
        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        Comparator<PendingApproval> bySubmit = Comparator.comparing(
                PendingApproval::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        result.sort(bySubmit.thenComparing(PendingApproval::getId));
        if (desc) {
            Collections.reverse(result);
        }
        return result;
    }

    private boolean matchesKeyword(PendingApproval p, String keyword) {
        String k = keyword.toLowerCase();
        return contains(p.getTitle(), k)
                || contains(p.getApplicantName(), k)
                || contains(p.getApplicantNo(), k);
    }

    private boolean contains(String s, String k) {
        return s != null && s.toLowerCase().contains(k);
    }

    private boolean matchesNodeScope(PendingApproval p, Integer scopeType, Long scopeId) {
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node == null) {
            return false;
        }
        if (!scopeType.equals(node.getScopeType())) {
            return false;
        }
        if (scopeId != null && node.getScopeId() != null && !scopeId.equals(node.getScopeId())) {
            return false;
        }
        return true;
    }

    // ==================== 私有：范围校验 ====================

    private void ensureTaskAccess(PendingApproval p, Long teacherId, Long schoolId) {
        if (!isInScope(p, teacherId, schoolId, resolveDelegatedDelegatorIds(teacherId))) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    private boolean isInScope(PendingApproval p, Long teacherId, Long schoolId, Set<Long> delegatedDelegatorIds) {
        if (isAdmin(teacherId)) {
            return true;
        }
        if (p.getAuditorId() != null && p.getAuditorId().equals(teacherId)) {
            return true;
        }
        // 审批委托：受托人可代委托人处理其待办（仅限节点创建时快照 allow_delegate=1 的任务）
        if (p.getAuditorId() != null && delegatedDelegatorIds != null
                && delegatedDelegatorIds.contains(p.getAuditorId())
                && delegationAllowedFor(p)) {
            return true;
        }
        return studentInScope(p.getApplicantId(), teacherId, schoolId);
    }

    /** 委托代审前置校验：任务所在节点创建时对应的步骤须 allow_delegate=1（节点缺失或旧数据视为不允许） */
    private boolean delegationAllowedFor(PendingApproval p) {
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        return node != null && Integer.valueOf(1).equals(node.getAllowDelegate());
    }

    /** 返回当前教师作为受托人时可代审的委托人 ID 集合（生效中的委托） */
    private Set<Long> resolveDelegatedDelegatorIds(Long teacherId) {
        if (teacherId == null) {
            return Set.of();
        }
        return approvalDelegationRepository.findActiveByDelegateeId(teacherId, LocalDateTime.now()).stream()
                .map(ApprovalDelegation::getDelegatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean studentInScope(Long studentId, Long teacherId, Long schoolId) {
        try {
            scopeValidator.ensureStudentInScope(teacherId, studentId, schoolId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private boolean isAdmin(Long teacherId) {
        AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(teacherId);
        return role != null && role.isAdmin();
    }

    // ==================== 私有：申报主体状态流转 ====================

    private ApprovableRef approveApprovable(PendingApproval p, LocalDateTime now, Long teacherId) {
        String at = p.getApprovableType();
        if ("Archive".equals(at)) {
            Archive a = archiveRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
            a.setStatus(ApplyStatusEnum.APPROVED.getValue());
            ArchiveAuditInfo ai = nullSafe(a.getAuditInfo());
            ai.setPassedAt(now);
            ai.setAuditedAt(now);
            ai.setAuditorId(teacherId);
            a.setAuditInfo(ai);
            archiveRepository.save(a);
            return new ApprovableRef(a.getSemesterId(), ai.getCurrentVersion());
        }
        if ("AwardApplication".equals(at)) {
            AwardApplication a = awardApplicationRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
            a.setStatus(ApplyStatusEnum.APPROVED.getValue());
            ArchiveAuditInfo ai = nullSafe(a.getAuditInfo());
            ai.setPassedAt(now);
            ai.setAuditedAt(now);
            ai.setAuditorId(teacherId);
            a.setAuditInfo(ai);
            awardApplicationRepository.save(a);
            return new ApprovableRef(a.getSemesterId(), ai.getCurrentVersion());
        }
        if ("CareerPlan".equals(at)) {
            CareerPlan c = careerPlanRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
            c.setStatus(ApplyStatusEnum.APPROVED.getValue());
            ArchiveAuditInfo ai = nullSafe(c.getAuditInfo());
            ai.setPassedAt(now);
            ai.setAuditedAt(now);
            ai.setAuditorId(teacherId);
            c.setAuditInfo(ai);
            careerPlanRepository.save(c);
            return new ApprovableRef(c.getSemesterId(), ai.getCurrentVersion());
        }
        return new ApprovableRef(null, 1);
    }

    private ApprovableRef rejectApprovable(PendingApproval p, String reason, LocalDateTime now, Long teacherId) {
        String at = p.getApprovableType();
        if ("Archive".equals(at)) {
            Archive a = archiveRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
            a.setStatus(ApplyStatusEnum.REJECTED.getValue());
            ArchiveAuditInfo ai = nullSafe(a.getAuditInfo());
            ai.setReturnedAt(now);
            ai.setRejectedReason(reason);
            ai.setAuditorId(teacherId);
            a.setAuditInfo(ai);
            archiveRepository.save(a);
            return new ApprovableRef(a.getSemesterId(), ai.getCurrentVersion());
        }
        if ("AwardApplication".equals(at)) {
            AwardApplication a = awardApplicationRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
            a.setStatus(ApplyStatusEnum.REJECTED.getValue());
            ArchiveAuditInfo ai = nullSafe(a.getAuditInfo());
            ai.setReturnedAt(now);
            ai.setRejectedReason(reason);
            ai.setAuditorId(teacherId);
            a.setAuditInfo(ai);
            awardApplicationRepository.save(a);
            return new ApprovableRef(a.getSemesterId(), ai.getCurrentVersion());
        }
        if ("CareerPlan".equals(at)) {
            CareerPlan c = careerPlanRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
            c.setStatus(ApplyStatusEnum.REJECTED.getValue());
            ArchiveAuditInfo ai = nullSafe(c.getAuditInfo());
            ai.setReturnedAt(now);
            ai.setRejectedReason(reason);
            ai.setAuditorId(teacherId);
            c.setAuditInfo(ai);
            careerPlanRepository.save(c);
            return new ApprovableRef(c.getSemesterId(), ai.getCurrentVersion());
        }
        return new ApprovableRef(null, 1);
    }

    private ApprovableRef revokeApprovable(PendingApproval p, LocalDateTime now, Long teacherId) {
        String at = p.getApprovableType();
        if ("Archive".equals(at)) {
            Archive a = archiveRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案记录不存在"));
            a.setStatus(ApplyStatusEnum.REVOKED.getValue());
            ArchiveAuditInfo ai = nullSafe(a.getAuditInfo());
            ai.setRevokedAt(now);
            ai.setAuditorId(teacherId);
            a.setAuditInfo(ai);
            archiveRepository.save(a);
            return new ApprovableRef(a.getSemesterId(), ai.getCurrentVersion());
        }
        if ("AwardApplication".equals(at)) {
            AwardApplication a = awardApplicationRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "奖项记录不存在"));
            a.setStatus(ApplyStatusEnum.REVOKED.getValue());
            ArchiveAuditInfo ai = nullSafe(a.getAuditInfo());
            ai.setRevokedAt(now);
            ai.setAuditorId(teacherId);
            a.setAuditInfo(ai);
            awardApplicationRepository.save(a);
            return new ApprovableRef(a.getSemesterId(), ai.getCurrentVersion());
        }
        if ("CareerPlan".equals(at)) {
            CareerPlan c = careerPlanRepository.findById(p.getApprovableId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "规划记录不存在"));
            c.setStatus(ApplyStatusEnum.REVOKED.getValue());
            ArchiveAuditInfo ai = nullSafe(c.getAuditInfo());
            ai.setRevokedAt(now);
            ai.setAuditorId(teacherId);
            c.setAuditInfo(ai);
            careerPlanRepository.save(c);
            return new ApprovableRef(c.getSemesterId(), ai.getCurrentVersion());
        }
        return new ApprovableRef(null, 1);
    }

    private static ArchiveAuditInfo nullSafe(ArchiveAuditInfo ai) {
        return ai != null ? ai : new ArchiveAuditInfo();
    }

    // ==================== 私有：审核日志 / 通知 / 评分 ====================

    private void writeAuditLog(PendingApproval p, Integer action, String comment, String revokeReason,
                               Long teacherId, Integer version, Long revokedLogId) {
        AuditLog logEntry = new AuditLog();
        logEntry.setAuditableType(p.getApprovableType());
        logEntry.setAuditableId(p.getApprovableId());
        logEntry.setAuditorId(teacherId);
        logEntry.setAction(action);
        logEntry.setComment(comment);
        logEntry.setRevokeReason(revokeReason);
        logEntry.setRevokedLogId(revokedLogId);
        logEntry.setVersion(version != null ? version : 1);
        logEntry.setIsDeletable(0);
        auditLogRepository.save(logEntry);
    }

    private void notifyStudent(PendingApproval p, String category, String title, String content) {
        UserMessage msg = new UserMessage();
        msg.setUserId(p.getApplicantId());
        msg.setSenderType(1);
        msg.setCategory(category);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedType(p.getApprovableType());
        msg.setRelatedId(p.getApprovableId());
        msg.setSendChannel("push");
        msg.setIsRead(0);
        msg.setIsArchived(0);
        msg.setIsImportant(0);
        userMessageRepository.save(msg);
    }

    /** 向审批人发审核提醒（转交等场景） */
    private void notifyAuditor(Long auditorId, String category, String title, String content, PendingApproval p) {
        if (auditorId == null) {
            return;
        }
        UserMessage msg = new UserMessage();
        msg.setUserId(auditorId);
        msg.setSenderType(1);
        msg.setCategory(category);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedType(p.getApprovableType());
        msg.setRelatedId(p.getApprovableId());
        msg.setSendChannel("push");
        msg.setIsRead(0);
        msg.setIsArchived(0);
        msg.setIsImportant(1);
        userMessageRepository.save(msg);
    }

    /** 组装转交审核记录的备注（含目标审批人姓名） */
    private String buildTransferComment(String targetName, String reason) {
        String base = "转交给 " + (targetName != null ? targetName : "其他教师");
        if (reason != null && !reason.isBlank()) {
            return base + "：" + reason;
        }
        return base;
    }

    /** 用户是否持有启用中的审批角色（roles.is_auditor=1） */
    private boolean isAuditor(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        return roleRepository.findByIdInAndStatus(roleIds, StatusEnum.ENABLED.getValue()).stream()
                .anyMatch(r -> Objects.equals(r.getIsAuditor(), 1));
    }

    private void triggerScoreRecalc(Long schoolId, PendingApproval p, Long semesterId, Long teacherId) {
        if (semesterId == null) {
            return;
        }
        try {
            adminScoreService.recalculateStudent(schoolId, p.getApplicantId(), semesterId, teacherId);
        } catch (Exception e) {
            log.warn("触发评分重算失败: taskId={}, applicantId={}, err={}",
                    p.getId(), p.getApplicantId(), e.getMessage());
        }
    }

    private void incrementTemplateUsage(Long schoolId, String templateCode) {
        auditCommentTemplateRepository.findBySchoolIdAndStatus(schoolId, 1).stream()
                .filter(t -> templateCode.equals(t.getTemplateCode()))
                .findFirst()
                .ifPresent(t -> {
                    t.setUsageCount((t.getUsageCount() == null ? 0 : t.getUsageCount()) + 1);
                    auditCommentTemplateRepository.save(t);
                });
    }

    // ==================== 私有：DTO 组装 ====================

    private PendingApproval loadTask(Long taskId) {
        return pendingApprovalRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "待审核任务不存在"));
    }

    private PendingListItem toListItem(PendingApproval p, Map<Long, ApplicantInfo> applicantCache) {
        ApprovableInfo info = resolveApprovableInfo(p);
        ApplicantInfo applicant = applicantCache.computeIfAbsent(p.getApplicantId(), this::buildApplicantInfo);
        ApplyStatusEnum status = ApplyStatusEnum.of(p.getStatus());
        DuplicateCheckStatusEnum dup = DuplicateCheckStatusEnum.of(info != null ? info.duplicateCheckStatus : null);
        return PendingListItem.builder()
                .taskId(p.getId())
                .type(toTypeCode(p.getApprovableType()))
                .archiveType(info != null ? info.archiveType : null)
                .archiveTypeLabel(info != null ? info.archiveTypeLabel : null)
                .approvableId(p.getApprovableId())
                .title(p.getTitle())
                .applicantId(p.getApplicantId())
                .applicantName(p.getApplicantName())
                .applicantNo(p.getApplicantNo())
                .className(applicant != null ? applicant.className : null)
                .majorName(applicant != null ? applicant.majorName : null)
                .semesterId(info != null ? info.semesterId : null)
                .semesterName(lookupSemesterName(info != null ? info.semesterId : null))
                .submitTime(format(p.getSubmittedAt()))
                .stepNo(p.getStepNo())
                .stepName(p.getStepName())
                .status(p.getStatus())
                .statusLabel(status.getLabel())
                .duplicateCheckStatus(info != null && info.duplicateCheckStatus != null
                        ? info.duplicateCheckStatus : dup.getValue())
                .duplicateCheckStatusLabel(dup.getLabel())
                .build();
    }

    private Applicant buildApplicant(PendingApproval p, User user, ApplicantInfo applicantInfo) {
        return Applicant.builder()
                .userId(p.getApplicantId())
                .name(user != null ? user.getName() : p.getApplicantName())
                .studentNo(user != null ? user.getUserNo() : p.getApplicantNo())
                .className(applicantInfo != null ? applicantInfo.className : null)
                .majorName(applicantInfo != null ? applicantInfo.majorName : null)
                .grade(applicantInfo != null ? applicantInfo.grade : null)
                .build();
    }

    private BaseInfo buildBaseInfo(ApprovableInfo info, Map<String, Object> detail) {
        String participantRole = str(detail.get("participantRole"));
        return BaseInfo.builder()
                .semesterId(info != null ? info.semesterId : null)
                .semesterName(lookupSemesterName(info != null ? info.semesterId : null))
                .obtainTime(str(detail.get("obtainTime")))
                .certificateNo(str(detail.get("certificateNo")))
                .issuingUnit(str(detail.get("issuingUnit")))
                .validUntil(str(detail.get("validUntil")))
                .participantRole(participantRole)
                .participantRoleLabel(resolveLabel("participant_role", participantRole))
                .build();
    }

    private List<FileItem> buildEvidenceFiles(String approvableType, Long approvableId) {
        String bizType = toTypeCode(approvableType);
        if (bizType == null) {
            return Collections.emptyList();
        }
        return attachmentRelationRepository.findByBizTypeAndBizId(bizType, approvableId).stream()
                .map(f -> new FileItem(f.getId(), f.getOriginalName(), f.getFilePath(), f.getFilePath(), f.getFileSize()))
                .collect(Collectors.toList());
    }

    private DuplicateInfo buildDuplicateInfo(ApprovableInfo info) {
        if (info == null || info.duplicateCheckStatus == null) {
            return new DuplicateInfo(false, Collections.emptyList());
        }
        boolean isDuplicate = Integer.valueOf(2).equals(info.duplicateCheckStatus);
        return new DuplicateInfo(isDuplicate, parseDuplicateRecords(info.duplicateInfo));
    }

    private List<ApprovalHistoryItem> buildApprovalHistory(Long instanceId) {
        if (instanceId == null) {
            return Collections.emptyList();
        }
        Map<Long, String> auditorNames = new HashMap<>();
        return approvalNodeRepository.findByInstanceIdOrderByStepNoAsc(instanceId).stream()
                .filter(n -> n.getAction() != null)
                .map(n -> ApprovalHistoryItem.builder()
                        .nodeId(n.getId())
                        .stepNo(n.getStepNo())
                        .stepName(n.getStepName())
                        .auditorName(resolveUserName(n.getActualAuditorId() != null
                                ? n.getActualAuditorId() : n.getAssignedAuditorId(), auditorNames))
                        .action(n.getAction())
                        .actionLabel(ApprovalNodeActionEnum.of(n.getAction()).getLabel())
                        .comment(n.getComment())
                        .createdAt(format(n.getCompletedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<VersionHistoryItem> buildVersionHistory(ActivityDetailResponse ad) {
        if (ad == null || ad.getVersionHistory() == null) {
            return Collections.emptyList();
        }
        return ad.getVersionHistory().stream()
                .map(v -> new VersionHistoryItem(v.getVersion(), v.getStatus(), v.getStatusLabel(),
                        v.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private Cursor buildCursor(Long teacherId, Long schoolId, Long taskId, String type, String archiveType,
                               Integer scopeType, Long scopeId, Long semesterId, String grade, String keyword,
                               String sortOrder) {
        List<PendingApproval> ordered = collectPending(teacherId, schoolId, type, archiveType, scopeType,
                scopeId, semesterId, grade, keyword, sortOrder);
        Long prev = null;
        Long next = null;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(taskId)) {
                if (i > 0) {
                    prev = ordered.get(i - 1).getId();
                }
                if (i + 1 < ordered.size()) {
                    next = ordered.get(i + 1).getId();
                }
                break;
            }
        }
        return new Cursor(prev, next);
    }

    private List<Object> parseDuplicateRecords(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ==================== 私有：字段解析 ====================

    /** 解析申报主体快照信息（列表/详情共用）。 */
    private ApprovableInfo resolveApprovableInfo(PendingApproval p) {
        String at = p.getApprovableType();
        if ("Archive".equals(at)) {
            Archive a = archiveRepository.findById(p.getApprovableId()).orElse(null);
            if (a == null) {
                return null;
            }
            ArchiveTypeEnum e = ArchiveTypeEnum.of(a.getArchiveType());
            return new ApprovableInfo(a.getArchiveType(), e != null ? e.getLabel() : null, a.getSemesterId(),
                    a.getDuplicateCheckStatus(), a.getDuplicateInfo());
        }
        if ("AwardApplication".equals(at)) {
            AwardApplication a = awardApplicationRepository.findById(p.getApprovableId()).orElse(null);
            if (a == null) {
                return null;
            }
            AwardTypeEnum e = AwardTypeEnum.of(a.getAwardType());
            return new ApprovableInfo(a.getAwardType(), e != null ? e.getLabel() : null, a.getSemesterId(),
                    null, null);
        }
        if ("CareerPlan".equals(at)) {
            CareerPlan c = careerPlanRepository.findById(p.getApprovableId()).orElse(null);
            if (c == null) {
                return null;
            }
            return new ApprovableInfo(null, null, c.getSemesterId(), null, null);
        }
        return null;
    }

    private ApplicantInfo buildApplicantInfo(Long applicantId) {
        StudentProfile sp = studentProfileRepository.findByUserId(applicantId).orElse(null);
        Long classId = sp != null ? sp.getClassId() : null;
        Clazz clazz = classId != null ? clazzRepository.findById(classId).orElse(null) : null;
        Long majorId = clazz != null ? clazz.getMajorId() : null;
        Major major = majorId != null ? majorRepository.findById(majorId).orElse(null) : null;
        return new ApplicantInfo(clazz != null ? clazz.getName() : null,
                major != null ? major.getName() : null,
                clazz != null ? clazz.getGrade() : null);
    }

    private String lookupSemesterName(Long semesterId) {
        if (semesterId == null) {
            return null;
        }
        return semesterRepository.findById(semesterId).map(s -> s.getName()).orElse(null);
    }

    private String resolveLabel(String dictType, String dictCode) {
        if (dictType == null || dictCode == null) {
            return null;
        }
        return dictionaryRepository.findActiveByDictType(dictType).stream()
                .filter(d -> dictCode.equals(d.getDictCode()))
                .findFirst()
                .map(d -> d.getDictName())
                .orElse(null);
    }

    private String resolveUserName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(userId, id -> userRepository.findById(id).map(User::getName).orElse(null));
    }

    private String toTypeCode(String approvableType) {
        if (approvableType == null) {
            return null;
        }
        return switch (approvableType) {
            case "Archive" -> "archive";
            case "AwardApplication" -> "award";
            case "CareerPlan" -> "career_plan";
            default -> approvableType;
        };
    }

    private ActivityTypeEnum toActivityType(String approvableType) {
        if (approvableType == null) {
            return null;
        }
        return switch (approvableType) {
            case "Archive" -> ActivityTypeEnum.ARCHIVE;
            case "AwardApplication" -> ActivityTypeEnum.AWARD;
            case "CareerPlan" -> ActivityTypeEnum.CAREER_PLAN;
            default -> null;
        };
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o) : null;
    }

    private static String format(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(ZONE).format(ISO_WITH_ZONE);
    }

    // ==================== 内嵌记录 / DTO ====================

    /** 申报主体快照 */
    private record ApprovableInfo(String archiveType, String archiveTypeLabel, Long semesterId,
                                  Integer duplicateCheckStatus, String duplicateInfo) {
    }

    /** 学生组织归属快照 */
    private record ApplicantInfo(String className, String majorName, String grade) {
    }

    /** 审批流转后返回的学期/版本 */
    private record ApprovableRef(Long semesterId, Integer version) {
    }

    @Data
    @Builder
    public static class PendingListItem {
        private Long taskId;
        private String type;
        private String archiveType;
        private String archiveTypeLabel;
        private Long approvableId;
        private String title;
        private Long applicantId;
        private String applicantName;
        private String applicantNo;
        private String className;
        private String majorName;
        private Long semesterId;
        private String semesterName;
        private String submitTime;
        private Integer stepNo;
        private String stepName;
        private Integer status;
        private String statusLabel;
        private Integer duplicateCheckStatus;
        private String duplicateCheckStatusLabel;
    }

    @Data
    @Builder
    public static class PendingDetailResponse {
        private Long taskId;
        private String type;
        private String archiveType;
        private Long approvableId;
        private String title;
        private Applicant applicant;
        private BaseInfo baseInfo;
        private Map<String, Object> detail;
        private List<FileItem> evidenceFiles;
        private DuplicateInfo duplicateInfo;
        private List<ApprovalHistoryItem> approvalHistory;
        private List<VersionHistoryItem> versionHistory;
        private Cursor cursor;
    }

    @Data
    @Builder
    public static class Applicant {
        private Long userId;
        private String name;
        private String studentNo;
        private String className;
        private String majorName;
        private String grade;
    }

    @Data
    @Builder
    public static class BaseInfo {
        private Long semesterId;
        private String semesterName;
        private String obtainTime;
        private String certificateNo;
        private String issuingUnit;
        private String validUntil;
        private String participantRole;
        private String participantRoleLabel;
    }

    @Data
    public static class FileItem {
        private final Long fileId;
        private final String fileName;
        private final String fileUrl;
        private final String previewUrl;
        private final Long fileSize;
    }

    @Data
    public static class DuplicateInfo {
        private final Boolean isDuplicate;
        private final List<Object> duplicateRecords;
    }

    @Data
    @Builder
    public static class ApprovalHistoryItem {
        private Long nodeId;
        private Integer stepNo;
        private String stepName;
        private String auditorName;
        private Integer action;
        private String actionLabel;
        private String comment;
        private String createdAt;
    }

    @Data
    public static class VersionHistoryItem {
        private final Integer version;
        private final Integer status;
        private final String statusLabel;
        private final String createdAt;
    }

    @Data
    public static class Cursor {
        private final Long prevTaskId;
        private final Long nextTaskId;
    }

    @Data
    @Builder
    public static class ApproveResult {
        private Long taskId;
        private Long approvableId;
        private Integer status;
        private String statusLabel;
        private String auditedAt;
    }

    @Data
    @Builder
    public static class RejectResult {
        private Long taskId;
        private Long approvableId;
        private Integer status;
        private String statusLabel;
        private String returnedAt;
        private String rejectedReason;
    }

    @Data
    public static class BatchResult {
        private final int total;
        private final int success;
        private final int failed;
        private final List<BatchResultItem> results;
    }

    @Data
    public static class BatchResultItem {
        private final Long taskId;
        private final boolean success;
        private final Integer status;
    }

    @Data
    @Builder
    public static class RevokeResult {
        private Long taskId;
        private Long approvableId;
        private Integer status;
        private String statusLabel;
        private String revokeReason;
        private String revokedAt;
    }

    @Data
    @Builder
    public static class TransferResult {
        private Long taskId;
        private Long approvableId;
        private Long targetAuditorId;
        private String transferredAt;
    }

    @Data
    public static class RejectTemplateItem {
        private final String templateCode;
        private final String templateContent;
        private final Integer usageCount;
    }

    // ==================== 请求体（内嵌 POJO） ====================

    @Data
    public static class ApproveRequest {
        private String comment;
        private Long nextAuditorId;
    }

    @Data
    public static class RejectRequest {
        private String comment;
        private String templateCode;
        private Integer rejectToStep;
    }

    @Data
    public static class BatchApproveRequest {
        private List<Long> taskIds;
        private String comment;
    }

    @Data
    public static class RevokeRequest {
        private String revokeReason;
    }

    @Data
    public static class TransferRequest {
        private Long targetAuditorId;
        private String comment;
    }
}