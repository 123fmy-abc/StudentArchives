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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    /** 单次批量审核上限，防止单个请求长时间占用连接与行锁 */
    private static final int MAX_BATCH_APPROVE_SIZE = 100;

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

    /** 自引用代理（@Lazy 避免循环依赖）：批量审核时逐条开启独立事务（REQUIRES_NEW） */
    @Lazy
    @Autowired
    private AuditTaskService self;

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
                .version(p.getLockVersion())
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
    public ApproveResult approve(Long teacherId, Long taskId, String comment, Long nextAuditorId,
                                 Integer expectedVersion) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        return approveInternal(taskId, comment, nextAuditorId, teacherId, schoolId, expectedVersion);
    }

    private ApproveResult approveInternal(Long taskId, String comment, Long nextAuditorId,
                                          Long teacherId, Long schoolId, Integer expectedVersion) {
        PendingApproval p = loadTask(taskId);
        ensureTaskAccess(p, teacherId, schoolId);
        if (!Integer.valueOf(1).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "任务已处理，请勿重复审核");
        }
        verifyTaskVersion(p, expectedVersion);
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

        // 2. 当前待办置为已通过；立即 flush，并发时在此处版本冲突并返回 409
        p.setStatus(ApplyStatusEnum.APPROVED.getValue());
        flushTaskOrConflict(p);

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

            ApprovalNode nextNode = approvalSubmitService.createStepNode(inst, nextStep, p.getSchoolId(),
                    p.getApplicantId(), p.getApplicantName(), p.getApplicantNo(), p.getTitle(),
                    p.getCategoryLabel(), p.getSubmittedAt(), designated);

            // 下一步节点/待办必须与本节点通过同生共死：创建失败则整体回滚，
            // 否则会出现「当前任务已通过、但无人持有下一级待办」的永久停滞
            if (nextNode == null) {
                log.error("多级审批推进失败，下一步无审批人，本次审批回滚: taskId={}, instanceId={}, nextStepNo={}",
                        taskId, p.getInstanceId(), nextStep.getStepNo());
                throw new BusinessException(ResultCode.APPROVAL_TASK_CREATE_FAILED,
                        "审批推进失败：无法解析下一级审批人，请检查审批流程配置后重试");
            }

            inst.setCurrentStep(nextStep.getStepNo());
            approvalInstanceRepository.save(inst);
            writeAuditLog(p, AuditActionEnum.APPROVE.getValue(), comment, null, teacherId, 1, null);
            notifyStudent(p, "audit_remind", "申报进入下一级审批",
                    "您的申报「" + p.getTitle() + "」已通过当前审核，等待下一级审批。");

            return ApproveResult.builder()
                    .taskId(p.getId())
                    .approvableId(p.getApprovableId())
                    .status(ApplyStatusEnum.APPROVED.getValue())
                    .statusLabel("已通过（进入下一级）")
                    .auditedAt(format(now))
                    .scoreRecalcStatus("SKIPPED")
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

        ApproveResult result = ApproveResult.builder()
                .taskId(p.getId())
                .approvableId(p.getApprovableId())
                .status(ApplyStatusEnum.APPROVED.getValue())
                .statusLabel(ApplyStatusEnum.APPROVED.getLabel())
                .auditedAt(format(now))
                .scoreRecalcStatus("PENDING")
                .build();
        // 重算需读取已提交的审批结果，事务提交后再提交任务；回执写入响应，失败不再静默
        runAfterCommit(() -> {
            ScoreRecalcDispatch recalc = triggerScoreRecalc(p.getSchoolId(), p, ref.semesterId(), teacherId);
            result.setScoreRecalcStatus(recalc.status());
            result.setScoreRecalcTaskId(recalc.taskId());
        });
        return result;
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
                               Integer rejectToStep, Integer expectedVersion) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "退回原因不能为空");
        }
        // rejectToStep 仅在多级流程「退回到指定步骤」时生效，当前单节点流程默认退回申请人，暂不落地该字段。

        PendingApproval p = loadTask(taskId);
        ensureTaskAccess(p, teacherId, schoolId);
        // 退回原因模板使用频率统计（鉴权通过后按任务所属学校计，避免越权请求污染本校统计）
        if (templateCode != null && !templateCode.isBlank()) {
            incrementTemplateUsage(p.getSchoolId(), templateCode);
        }
        if (!Integer.valueOf(1).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "任务已处理，请勿重复审核");
        }
        verifyTaskVersion(p, expectedVersion);
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
        flushTaskOrConflict(p);

        approvalInstanceRepository.findById(p.getInstanceId()).ifPresent(inst -> {
            inst.setStatus(ApprovalInstanceStatusEnum.REJECTED.getValue());
            inst.setCompletedAt(now);
            approvalInstanceRepository.save(inst);
        });

        ApprovableRef ref = rejectApprovable(p, comment, now, teacherId);
        writeAuditLog(p, AuditActionEnum.REJECT.getValue(), comment, null, teacherId, ref.version(), null);
        notifyStudent(p, "audit_remind", "申报被退回", "您的申报「" + p.getTitle() + "」被退回：" + comment);

        RejectResult result = RejectResult.builder()
                .taskId(p.getId())
                .approvableId(p.getApprovableId())
                .status(ApplyStatusEnum.REJECTED.getValue())
                .statusLabel(ApplyStatusEnum.REJECTED.getLabel())
                .returnedAt(format(now))
                .rejectedReason(comment)
                .scoreRecalcStatus("PENDING")
                .build();
        // 与通过一致：提交后再重算，避免异步线程读到未提交的退回状态
        runAfterCommit(() -> {
            ScoreRecalcDispatch recalc = triggerScoreRecalc(p.getSchoolId(), p, ref.semesterId(), teacherId);
            result.setScoreRecalcStatus(recalc.status());
            result.setScoreRecalcTaskId(recalc.taskId());
        });
        return result;
    }

    // ==================== 4.5 批量审核通过 ====================

    /**
     * 批量审核通过（POST /teacher/audits/batch/approve）：逐条独立事务，单条失败仅回滚该条（部分成功）。
     * <p>
     * 外层不开启事务（NOT_SUPPORTED），每条经 {@link #self} 代理进入 REQUIRES_NEW 事务：
     * 业务异常回滚该条且不残留半成品数据；任何异常都被收敛为该条失败，不影响其它条目。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchResult batchApprove(Long teacherId, List<Long> taskIds, String comment) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "taskIds 不能为空");
        }
        adminAuthService.requireAdminOrPermission(teacherId, "audit:batch");

        List<Long> distinctIds = taskIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "taskIds 不能为空");
        }
        if (distinctIds.size() > MAX_BATCH_APPROVE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "单次批量审核不能超过 " + MAX_BATCH_APPROVE_SIZE + " 条");
        }

        List<BatchResultItem> results = new ArrayList<>(distinctIds.size());
        int success = 0;
        int failed = 0;
        for (Long taskId : distinctIds) {
            try {
                ApproveResult r = self.approveInNewTransaction(teacherId, taskId, comment);
                results.add(new BatchResultItem(taskId, true, r.getStatus(), null, null,
                        r.getScoreRecalcStatus(), r.getScoreRecalcTaskId()));
                success++;
            } catch (BusinessException e) {
                log.warn("批量审核失败: teacherId={}, taskId={}, code={}, msg={}",
                        teacherId, taskId, e.getCode(), e.getMessage());
                results.add(new BatchResultItem(taskId, false, null, e.getCode(), e.getMessage(), null, null));
                failed++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("批量审核版本冲突: teacherId={}, taskId={}", teacherId, taskId, e);
                results.add(new BatchResultItem(taskId, false, null,
                        ResultCode.DATA_VERSION_CONFLICT.getCode(),
                        "该任务已被其他审批人处理", null, null));
                failed++;
            } catch (Exception e) {
                log.error("批量审核异常: teacherId={}, taskId={}", teacherId, taskId, e);
                results.add(new BatchResultItem(taskId, false, null,
                        ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), null, null));
                failed++;
            }
        }
        return new BatchResult(distinctIds.size(), success, failed, results);
    }

    /**
     * 批量审核的单条事务入口（REQUIRES_NEW）。
     * <p>
     * 必须经 {@link #self} 代理调用，@Transactional 才会生效；单条提交/回滚，
     * 失败项不会把已写入的节点/待办带进最终提交。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApproveResult approveInNewTransaction(Long teacherId, Long taskId, String comment) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        return approveInternal(taskId, comment, null, teacherId, schoolId, null);
    }

    // ==================== 4.6 撤销已审核记录 ====================

    /** 撤销已审核记录（POST /teacher/audits/{taskId}/revoke，admin 角色或 audit:revoke 权限码）。 */
    @Transactional
    public RevokeResult revoke(Long teacherId, Long taskId, String revokeReason, Integer expectedVersion) {
        adminAuthService.requireAdminOrPermission(teacherId, "audit:revoke");
        if (revokeReason == null || revokeReason.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "撤销原因不能为空");
        }
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        PendingApproval p = loadTask(taskId);
        // 撤销权限不免除学校隔离：仅允许撤销本校任务
        if (!Objects.equals(p.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        if (!Integer.valueOf(2).equals(p.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅已通过的记录可撤销");
        }
        verifyTaskVersion(p, expectedVersion);
        LocalDateTime now = LocalDateTime.now();

        Long revokedLogId = auditLogRepository
                .findTopByAuditableTypeAndAuditableIdAndActionOrderByIdDesc(
                        p.getApprovableType(), p.getApprovableId(), AuditActionEnum.APPROVE.getValue())
                .map(AuditLog::getId)
                .orElse(null);

        p.setStatus(ApplyStatusEnum.REVOKED.getValue());
        flushTaskOrConflict(p);

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
    public TransferResult transfer(Long teacherId, Long taskId, Long targetAuditorId, String comment,
                                   Integer expectedVersion) {
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
        verifyTaskVersion(p, expectedVersion);

        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "审批节点不存在"));
        // 转交不得绕过流程步骤的角色/范围要求
        if (!approvalSubmitService.isAuditorEligible(targetAuditorId, node.getRoleId(),
                node.getScopeType(), node.getScopeId(), p.getSchoolId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转交对象不满足该节点要求的审批角色或授权范围");
        }
        LocalDateTime now = LocalDateTime.now();

        // 节点改派：待办归属目标审批人（节点仍待审核，action 不置位）
        node.setAssignedAuditorId(targetAuditorId);
        approvalNodeRepository.save(node);

        // 待办改派给目标审批人
        p.setAuditorId(targetAuditorId);
        flushTaskOrConflict(p);

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
        List<ApprovalDelegation> activeDelegations = resolveActiveDelegations(teacherId);
        List<PendingApproval> result = new ArrayList<>();
        for (PendingApproval p : all) {
            if (!isInScope(p, teacherId, schoolId, activeDelegations)) {
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
        if (!isInScope(p, teacherId, schoolId, resolveActiveDelegations(teacherId))) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
    }

    /**
     * 任务可见范围判定：管理员 → 本人待办 → 审批委托代审 → 授权范围。
     * <p>
     * 委托代审必须同时满足：同校、节点 allow_delegate=1、委托声明的角色/范围匹配节点、
     * 且任务落在<b>原审批人</b>授权范围内，避免受托人借委托处理原审批人无权处理的档案。
     * 因委托改派而来的任务（节点 delegation_id 非空）在委托撤销/过期/范围失配后立即失效，
     * 不再因 assignedAuditorId 归属受托人而放行。
     */
    private boolean isInScope(PendingApproval p, Long teacherId, Long schoolId,
                              List<ApprovalDelegation> activeDelegations) {
        if (isAdmin(teacherId)) {
            // 管理员仅免除角色范围限制，仍必须限定在本校任务内，避免凭 taskId 跨校审批
            return Objects.equals(p.getSchoolId(), schoolId);
        }
        // 本人待办：仅限本校任务；委托改派任务须委托仍生效且范围未变
        if (p.getAuditorId() != null && p.getAuditorId().equals(teacherId)
                && Objects.equals(p.getSchoolId(), schoolId)
                && isAssignmentAccessible(p, teacherId, schoolId, activeDelegations)) {
            return true;
        }
        if (isDelegatedTaskInScope(p, teacherId, schoolId, activeDelegations)) {
            return true;
        }
        // 共享待办：授权范围覆盖学生还不够，还必须持有当前节点要求的角色与节点授权范围，
        // 否则「按角色配置的审核节点」会被范围内任意教师处理。
        return studentInScope(p.getApplicantId(), teacherId, schoolId) && isNodeAuditorEligible(p, teacherId);
    }

    /** 当前教师是否满足待办所在节点的角色与范围要求（与审批人解析口径一致） */
    private boolean isNodeAuditorEligible(PendingApproval p, Long teacherId) {
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node == null) {
            return false;
        }
        return approvalSubmitService.isAuditorEligible(teacherId, node.getRoleId(),
                node.getScopeType(), node.getScopeId(), p.getSchoolId());
    }

    /**
     * 本人待办是否仍可处理：非委托改派的任务直接放行；由委托改派的任务要求对应委托
     * 仍处于生效期、受托人未变、角色/范围仍匹配且任务在原审批人范围内。
     */
    private boolean isAssignmentAccessible(PendingApproval p, Long teacherId, Long schoolId,
                                           List<ApprovalDelegation> activeDelegations) {
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node == null || node.getDelegationId() == null) {
            return true;
        }
        return activeDelegations.stream()
                .filter(d -> Objects.equals(d.getId(), node.getDelegationId()))
                .filter(d -> Objects.equals(d.getDelegateeId(), teacherId))
                .filter(d -> d.getDelegatorId() != null)
                .filter(d -> Objects.equals(d.getSchoolId(), schoolId))
                .anyMatch(d -> delegationMatchesNode(d, node)
                        && studentInScope(p.getApplicantId(), d.getDelegatorId(), schoolId));
    }

    /**
     * 受托人代审判定：待办原归属人为委托人，委托生效且同校，节点允许委托，
     * 委托声明的角色/范围与节点匹配，任务在原审批人授权范围内。
     */
    private boolean isDelegatedTaskInScope(PendingApproval p, Long teacherId, Long schoolId,
                                           List<ApprovalDelegation> activeDelegations) {
        if (p.getAuditorId() == null || activeDelegations.isEmpty()
                || !Objects.equals(p.getSchoolId(), schoolId)) {
            return false;
        }
        List<ApprovalDelegation> candidates = activeDelegations.stream()
                .filter(d -> Objects.equals(d.getDelegatorId(), p.getAuditorId()))
                .filter(d -> Objects.equals(d.getSchoolId(), schoolId))
                .toList();
        if (candidates.isEmpty()) {
            return false;
        }
        ApprovalNode node = approvalNodeRepository.findById(p.getNodeId()).orElse(null);
        if (node == null || !Integer.valueOf(1).equals(node.getAllowDelegate())) {
            return false;
        }
        return candidates.stream()
                .filter(d -> delegationMatchesNode(d, node))
                .anyMatch(d -> studentInScope(p.getApplicantId(), d.getDelegatorId(), schoolId));
    }

    /** 委托声明的角色/范围是否覆盖节点（null 字段表示不限） */
    private boolean delegationMatchesNode(ApprovalDelegation d, ApprovalNode node) {
        if (d.getRoleId() != null && !Objects.equals(d.getRoleId(), node.getRoleId())) {
            return false;
        }
        if (d.getScopeType() != null && !Objects.equals(d.getScopeType(), node.getScopeType())) {
            return false;
        }
        return d.getScopeId() == null || Objects.equals(d.getScopeId(), node.getScopeId());
    }

    /** 当前教师作为受托人的生效委托列表 */
    private List<ApprovalDelegation> resolveActiveDelegations(Long teacherId) {
        if (teacherId == null) {
            return List.of();
        }
        return approvalDelegationRepository.findActiveByDelegateeId(teacherId, LocalDateTime.now());
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

    /**
     * 提交评分重算并返回回执：任务同步落库后异步执行。
     * <ul>
     *   <li>无学期归属 → {@code SKIPPED}（该档案不参与学期评分）</li>
     *   <li>任务创建成功 → {@code PENDING} + 任务 ID，前端可凭 ID 查询完成/失败与原因</li>
     *   <li>任务创建异常 → {@code FAILED}，审批响应明确提示重算未提交，不再静默</li>
     * </ul>
     */
    private ScoreRecalcDispatch triggerScoreRecalc(Long schoolId, PendingApproval p, Long semesterId, Long teacherId) {
        if (semesterId == null) {
            return new ScoreRecalcDispatch("SKIPPED", null);
        }
        try {
            Long taskId = adminScoreService.submitStudentRecalculation(
                    schoolId, p.getApplicantId(), semesterId, teacherId);
            return taskId != null
                    ? new ScoreRecalcDispatch("PENDING", taskId)
                    : new ScoreRecalcDispatch("SKIPPED", null);
        } catch (Exception e) {
            log.error("提交评分重算任务失败: taskId={}, applicantId={}, err={}",
                    p.getId(), p.getApplicantId(), e.getMessage(), e);
            return new ScoreRecalcDispatch("FAILED", null);
        }
    }

    /** 评分重算回执：状态（PENDING/SKIPPED/FAILED）+ 任务 ID */
    private record ScoreRecalcDispatch(String status, Long taskId) {
    }

    /**
     * 事务提交后执行（评分重算需读取已提交的审批结果，必须在提交后触发）；
     * 无活动事务同步时直接执行。
     */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
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

    /**
     * 校验客户端携带的版本号（可选契约）：页面停留过久后提交时提前暴露冲突。
     * 未携带版本（旧客户端）时跳过，由 {@link #flushTaskOrConflict} 的乐观锁兜底。
     */
    private void verifyTaskVersion(PendingApproval p, Integer expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(p.getLockVersion())) {
            throw new BusinessException(ResultCode.DATA_VERSION_CONFLICT,
                    "数据已被他人修改，请刷新后重试");
        }
    }

    /**
     * 立即 flush 待办状态，让并发版本冲突在写节点/日志/通知之前暴露，统一转为 409。
     */
    private void flushTaskOrConflict(PendingApproval p) {
        try {
            pendingApprovalRepository.saveAndFlush(p);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ResultCode.DATA_VERSION_CONFLICT,
                    "该任务已被其他审批人处理，请刷新后重试");
        }
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
                .version(p.getLockVersion())
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
        /** 任务版本号（并发审批冲突检测，提交时回传） */
        private Integer version;
    }

    @Data
    @Builder
    public static class PendingDetailResponse {
        private Long taskId;
        private Integer version;
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
        /** 评分重算状态：PENDING=已提交异步任务，SKIPPED=无需重算，FAILED=任务提交失败 */
        private String scoreRecalcStatus;
        /** 评分重算任务 ID（PENDING 时返回，可查询完成/失败与原因） */
        private Long scoreRecalcTaskId;
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
        /** 评分重算状态：PENDING=已提交异步任务，SKIPPED=无需重算，FAILED=任务提交失败 */
        private String scoreRecalcStatus;
        /** 评分重算任务 ID（PENDING 时返回，可查询完成/失败与原因） */
        private Long scoreRecalcTaskId;
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
        private final Integer errorCode;
        private final String errorMessage;
        /** 评分重算状态：PENDING/SKIPPED/FAILED（成功条目返回） */
        private final String scoreRecalcStatus;
        /** 评分重算任务 ID */
        private final Long scoreRecalcTaskId;
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
        /** 任务版本号（可选，用于提前发现并发冲突） */
        private Integer version;
    }

    @Data
    public static class RejectRequest {
        private String comment;
        private String templateCode;
        private Integer rejectToStep;
        /** 任务版本号（可选，用于提前发现并发冲突） */
        private Integer version;
    }

    @Data
    public static class BatchApproveRequest {
        private List<Long> taskIds;
        private String comment;
    }

    @Data
    public static class RevokeRequest {
        private String revokeReason;
        /** 任务版本号（可选，用于提前发现并发冲突） */
        private Integer version;
    }

    @Data
    public static class TransferRequest {
        private Long targetAuditorId;
        private String comment;
        /** 任务版本号（可选，用于提前发现并发冲突） */
        private Integer version;
    }
}