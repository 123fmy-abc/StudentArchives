package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.entity.approval.ApprovalDelegation;
import com.example.studentarchives.entity.approval.ApprovalFlow;
import com.example.studentarchives.entity.approval.ApprovalFlowMapping;
import com.example.studentarchives.entity.approval.ApprovalFlowStep;
import com.example.studentarchives.entity.approval.ApprovalInstance;
import com.example.studentarchives.entity.approval.ApprovalNode;
import com.example.studentarchives.entity.approval.PendingApproval;
import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.repository.ApprovalDelegationRepository;
import com.example.studentarchives.repository.ApprovalFlowMappingRepository;
import com.example.studentarchives.repository.ApprovalFlowRepository;
import com.example.studentarchives.repository.ApprovalFlowStepRepository;
import com.example.studentarchives.repository.ApprovalInstanceRepository;
import com.example.studentarchives.repository.ApprovalNodeRepository;
import com.example.studentarchives.repository.PendingApprovalRepository;
import com.example.studentarchives.repository.RoleScopeRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.example.studentarchives.service.Fmy.TeacherScopeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 审批流「启动实例」服务（Lzw）
 * <p>
 * 学生提交申报（Archive / AwardApplication / CareerPlan）后，由各提交 Service 调用
 * {@link #createOnSubmit} 生成审批实例、当前节点与统一待办（pending_approvals），
 * 使教师端「待审核任务模块」（《教师端接口文档》四）4.1 能读到数据。
 * <p>
 * 流程解析遵循《学生档案系统表》审批流口径：
 * 先按 {@code school_id + business_type + business_sub_type} 命中 approval_flow_mappings
 * （校验映射生效时间窗），未命中则 fallback 到 approval_flows.is_default=1 的默认流程；
 * 审批人按 approval_flow_steps 的 role_id + scope_type 解析出持有该角色且授权范围覆盖节点范围、
 * 且授权处于生效期内的教师；同角色多人时按「当前待办最少者优先」分配，保证负载均衡与确定性。
 * <p>
 * 解析不到审批人时<strong>回退到校级管理员</strong>（持有学校级授权），避免申报「无人审核」；
 * 仍解析不到（连校级管理员都不存在）才记 ERROR 并放弃，此时<strong>不阻塞提交</strong>。
 * 该服务同时提供 {@link #createStepNode} 供多级审批流转（{@code AuditTaskService} 在审批通过后
 * 推进到下一步）复用同一套解析口径。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalSubmitService {

    private final ApprovalFlowMappingRepository flowMappingRepository;
    private final ApprovalFlowRepository flowRepository;
    private final ApprovalFlowStepRepository flowStepRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalNodeRepository nodeRepository;
    private final PendingApprovalRepository pendingApprovalRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleScopeRepository roleScopeRepository;
    private final UserRepository userRepository;
    private final ApprovalDelegationRepository approvalDelegationRepository;
    private final UserMessageRepository userMessageRepository;
    private final TeacherScopeValidator scopeValidator;

    /**
     * 提交后生成待审核任务（失败不抛出，不影响提交）。
     *
     * @param schoolId        申报人所属学校
     * @param approvableType  模型类型：Archive / AwardApplication / CareerPlan
     * @param businessSubType 业务子类型（archive 类型编码 / award 类型编码，可为 null）
     * @param approvableId    模型主键（archives.id / award_applications.id / career_plans.id）
     * @param applicantId     申请人用户 ID
     * @param applicantName   申请人姓名快照
     * @param applicantNo     申请人学号/工号快照
     * @param title           申报标题
     * @param categoryLabel   分类标签快照
     * @param submittedAt     提交时间
     */
    public void createOnSubmit(Long schoolId, String approvableType, String businessSubType,
                               Long approvableId, Long applicantId, String applicantName,
                               String applicantNo, String title, String categoryLabel,
                               LocalDateTime submittedAt) {
        try {
            doCreate(schoolId, approvableType, businessSubType, approvableId, applicantId,
                    applicantName, applicantNo, title, categoryLabel, submittedAt);
        } catch (Exception e) {
            log.error("生成待审核任务失败（不阻塞提交）: approvableType={}, approvableId={}, err={}",
                    approvableType, approvableId, e.getMessage(), e);
        }
    }

    private void doCreate(Long schoolId, String approvableType, String businessSubType,
                          Long approvableId, Long applicantId, String applicantName,
                          String applicantNo, String title, String categoryLabel,
                          LocalDateTime submittedAt) {
        ApprovalFlow flow = resolveFlow(schoolId, approvableType, businessSubType);
        if (flow == null) {
            log.warn("未配置审批流程，跳过待办生成（请管理员为该学校/类型配置流程或默认流程）: schoolId={}, approvableType={}, subType={}",
                    schoolId, approvableType, businessSubType);
            return;
        }
        List<ApprovalFlowStep> steps = flowStepRepository.findByFlowIdOrderByStepNoAsc(flow.getId());
        if (steps.isEmpty()) {
            log.warn("审批流程无步骤，跳过待办生成（请管理员为流程配置步骤）: flowId={}", flow.getId());
            return;
        }
        ApprovalFlowStep first = steps.get(0);

        LocalDateTime now = LocalDateTime.now();

        ApprovalInstance inst = new ApprovalInstance();
        inst.setApprovableType(approvableType);
        inst.setApprovableId(approvableId);
        inst.setFlowId(flow.getId());
        inst.setFlowVersion(flow.getVersion());
        inst.setApplicantId(applicantId);
        inst.setCurrentStep(first.getStepNo());
        inst.setTotalSteps(steps.size());
        inst.setStatus(1);
        inst = instanceRepository.save(inst);

        ApprovalNode node = createStepNode(inst, first, schoolId, applicantId, applicantName,
                applicantNo, title, categoryLabel, submittedAt, null);

        log.info("生成待审核任务: approvableType={}, approvableId={}, nodeId={}",
                approvableType, approvableId, node != null ? node.getId() : null);
    }

    /**
     * 为指定步骤创建审批节点与统一待办（提交首步与多级流转下一步共用）。
     * <p>
     * 审批人解析优先级：手动指定（校验角色+范围合法）→ 自动解析 → 回退校级管理员。
     * 解析不到审批人且无校级管理员可兜底时返回 null（调用方据此处理，不阻塞主流程）。
     *
     * @param instance            审批实例
     * @param step                当前要落地的流程步骤
     * @param schoolId            学校 ID
     * @param designatedAuditorId 手动指定的下一审批人（可为 null，表示自动解析）
     * @return 创建的审批节点；解析失败返回 null
     */
    public ApprovalNode createStepNode(ApprovalInstance instance, ApprovalFlowStep step,
                                       Long schoolId, Long applicantId, String applicantName,
                                       String applicantNo, String title, String categoryLabel,
                                       LocalDateTime submittedAt, Long designatedAuditorId) {
        // 1. 解析节点范围
        TeacherScopeValidator.StudentContext ctx = resolveStudentContext(applicantId);
        Long scopeId = resolveScopeId(schoolId, step.getScopeType(), ctx);

        // 2. 解析审批人
        Long auditorId = null;
        Integer assignType = 1;
        if (scopeId != null) {
            if (designatedAuditorId != null
                    && isAuditorEligible(designatedAuditorId, step.getRoleId(), step.getScopeType(), scopeId, schoolId)) {
                auditorId = designatedAuditorId;
                assignType = 2;
            } else {
                auditorId = resolveAuditor(step.getRoleId(), step.getScopeType(), scopeId, schoolId);
            }
        }
        boolean fallback = false;
        if (auditorId == null) {
            if (scopeId == null) {
                log.warn("节点范围无法解析，回退校级管理员: scopeType={}, approvableType={}, approvableId={}",
                        step.getScopeType(), instance.getApprovableType(), instance.getApprovableId());
            } else {
                log.warn("步骤无匹配审批人，回退校级管理员: roleId={}, scopeType={}, scopeId={}, approvableType={}, approvableId={}",
                        step.getRoleId(), step.getScopeType(), scopeId, instance.getApprovableType(), instance.getApprovableId());
            }
            auditorId = resolveFallbackAuditor(schoolId);
            assignType = 3;
            fallback = auditorId != null;
        }
        if (auditorId == null) {
            log.error("无法解析审批人且无校级管理员可兜底，待办生成失败（该申报将无人审核）: approvableType={}, approvableId={}, roleId={}, scopeType={}",
                    instance.getApprovableType(), instance.getApprovableId(), step.getRoleId(), step.getScopeType());
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // 回退到校级管理员时，节点范围统一记为学校级；否则沿用步骤配置范围
        Integer nodeScopeType = fallback ? 1 : step.getScopeType();
        Long nodeScopeId = fallback ? schoolId : scopeId;

        // 3. 审批委托改派（仅非兜底分配且节点允许委托时生效）
        Long effectiveAuditorId = auditorId;
        Long delegationId = null;
        if (!fallback && Integer.valueOf(1).equals(step.getAllowDelegate())) {
            ApprovalDelegation delegation = resolveActiveDelegation(
                    auditorId, step.getRoleId(), step.getScopeType(), scopeId, now);
            if (delegation != null && delegation.getDelegateeId() != null
                    && !Objects.equals(delegation.getDelegateeId(), auditorId)) {
                effectiveAuditorId = delegation.getDelegateeId();
                delegationId = delegation.getId();
                log.info("命中审批委托改派: delegatorId={}, delegateeId={}, delegationId={}",
                        auditorId, effectiveAuditorId, delegationId);
            }
        }

        ApprovalNode node = new ApprovalNode();
        node.setInstanceId(instance.getId());
        node.setStepNo(step.getStepNo());
        node.setStepName(step.getStepName());
        node.setRoleId(step.getRoleId());
        node.setScopeType(nodeScopeType);
        node.setScopeId(nodeScopeId);
        node.setAssignedAuditorId(effectiveAuditorId);
        node.setDelegationId(delegationId);
        node.setAllowDelegate(step.getAllowDelegate() != null ? step.getAllowDelegate() : 0);
        node.setAssignType(assignType);
        node.setStartedAt(now);
        node.setTimeoutAt(now.plusHours(step.getTimeoutHours() != null ? step.getTimeoutHours() : 48));
        node = nodeRepository.save(node);

        PendingApproval pending = new PendingApproval();
        pending.setSchoolId(schoolId);
        pending.setNodeId(node.getId());
        pending.setInstanceId(instance.getId());
        pending.setApprovableType(instance.getApprovableType());
        pending.setApprovableId(instance.getApprovableId());
        pending.setApplicantId(applicantId);
        pending.setApplicantName(applicantName);
        pending.setApplicantNo(applicantNo);
        pending.setTitle(title);
        pending.setCategoryLabel(categoryLabel);
        pending.setSubmittedAt(submittedAt != null ? submittedAt : now);
        pending.setAuditorId(effectiveAuditorId);
        pending.setRoleId(step.getRoleId());
        pending.setStepNo(step.getStepNo());
        pending.setStepName(step.getStepName());
        pending.setStatus(1);
        pendingApprovalRepository.save(pending);

        // 待办生成后通知实际审批人（消息生产失败不影响主流程）
        notifyAuditorOfNewTask(effectiveAuditorId, title, instance.getApprovableType(), instance.getApprovableId());

        log.info("生成待审核任务: taskId={}, approvableType={}, approvableId={}, auditorId={}, delegationId={}",
                pending.getId(), instance.getApprovableType(), instance.getApprovableId(), effectiveAuditorId, delegationId);
        return node;
    }

    // ==================== 流程 / 范围 / 审批人解析 ====================

    /**
     * 解析审批流程：先按映射表命中（校验生效时间窗），再 fallback 到默认流程。
     * 只返回 status=1（启用）的流程。
     */
    private ApprovalFlow resolveFlow(Long schoolId, String approvableType, String subType) {
        LocalDateTime now = LocalDateTime.now();
        List<ApprovalFlowMapping> mappings =
                flowMappingRepository.findBySchoolIdAndBusinessType(schoolId, approvableType);

        Comparator<ApprovalFlowMapping> byPriority = Comparator
                .comparing(ApprovalFlowMapping::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ApprovalFlowMapping::getId);

        ApprovalFlowMapping chosen = null;
        if (subType != null) {
            chosen = mappings.stream()
                    .filter(m -> subType.equals(m.getBusinessSubType()))
                    .filter(m -> isMappingEffective(m, now))
                    .sorted(byPriority)
                    .findFirst().orElse(null);
        }
        if (chosen == null) {
            // 通用映射（business_sub_type=null）：优先 is_default=1，其次按优先级
            chosen = mappings.stream()
                    .filter(m -> m.getBusinessSubType() == null)
                    .filter(m -> isMappingEffective(m, now))
                    .sorted(Comparator
                            .comparing(ApprovalFlowMapping::getIsDefault,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(ApprovalFlowMapping::getPriority,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(ApprovalFlowMapping::getId))
                    .findFirst().orElse(null);
        }
        if (chosen != null) {
            return flowRepository.findById(chosen.getFlowId())
                    .filter(f -> Integer.valueOf(1).equals(f.getStatus()))
                    .orElse(null);
        }

        return flowRepository.findBySchoolIdAndApplicableType(schoolId, approvableType).stream()
                .filter(f -> Integer.valueOf(1).equals(f.getStatus())
                        && Integer.valueOf(1).equals(f.getIsDefault()))
                .findFirst()
                .orElse(null);
    }

    /** 映射是否处于生效时间窗内（未设置视为不限） */
    private boolean isMappingEffective(ApprovalFlowMapping m, LocalDateTime now) {
        if (m.getEffectiveStart() != null && now.isBefore(m.getEffectiveStart())) {
            return false;
        }
        if (m.getEffectiveEnd() != null && now.isAfter(m.getEffectiveEnd())) {
            return false;
        }
        return true;
    }

    private TeacherScopeValidator.StudentContext resolveStudentContext(Long applicantId) {
        try {
            return scopeValidator.resolveStudentContext(applicantId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 根据 scopeType 把节点范围解析成数字 org id：1=学校 2=学院 3=专业 4=班级；5/6 无数字 id 则返回 null。 */
    private Long resolveScopeId(Long schoolId, Integer scopeType, TeacherScopeValidator.StudentContext ctx) {
        if (scopeType == null) {
            return null;
        }
        return switch (scopeType) {
            case 1 -> schoolId;
            case 2 -> ctx != null ? ctx.collegeId() : null;
            case 3 -> ctx != null ? ctx.majorId() : null;
            case 4 -> ctx != null ? ctx.classId() : null;
            default -> null;
        };
    }

    /**
     * 解析审批人：持有该角色（user_roles.role_id）、用户启用、且授权范围（role_scopes）
     * 覆盖节点范围、授权处于生效期内的教师；同角色多人时返回「当前待办最少者」。
     * <p>
     * 学校级节点（scopeType=1）：要求持有学校级授权（scope_type=1 且 scope_id=学校）；
     * 学院/专业/班级节点（scopeType=2/3/4）：数据范围需精确命中节点 scopeId。
     */
    private Long resolveAuditor(Long roleId, Integer scopeType, Long scopeId, Long schoolId) {
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        LocalDate today = LocalDate.now();
        List<Long> candidates = new ArrayList<>();
        for (UserRole ur : userRoles) {
            Long candidateId = ur.getUserId();
            if (candidateId == null || !isEnabledUser(candidateId)) {
                continue;
            }
            List<RoleScope> scopes = roleScopeRepository.findByUserIdAndStatus(candidateId, 1);
            boolean matched = scopes.stream()
                    .anyMatch(s -> matchesScope(s, scopeType, scopeId, schoolId, today));
            if (matched) {
                candidates.add(candidateId);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator
                        .comparingLong((Long uid) -> pendingApprovalRepository.countByAuditorIdAndStatus(uid, 1))
                        .thenComparing(uid -> uid))
                .orElse(null);
    }

    /** 单条授权是否覆盖节点范围且处于生效期内 */
    private boolean matchesScope(RoleScope s, Integer scopeType, Long scopeId, Long schoolId, LocalDate today) {
        if (!isInEffect(s, today)) {
            return false;
        }
        if (s.getScopeType() == null || s.getScopeId() == null) {
            return false;
        }
        if (scopeType == null) {
            return true;
        }
        if (scopeType == 1) {
            return Objects.equals(s.getScopeType(), 1) && Objects.equals(s.getScopeId(), schoolId);
        }
        return Objects.equals(s.getScopeType(), scopeType) && Objects.equals(s.getScopeId(), scopeId);
    }

    /** 用户是否启用 */
    private boolean isEnabledUser(Long userId) {
        return userRepository.findById(userId)
                .map(u -> Integer.valueOf(1).equals(u.getStatus()))
                .orElse(false);
    }

    /**
     * 回退审批人：本校持有「学校级授权（scope_type=1 且 scope_id=学校）」且授权生效中、
     * 用户启用的用户，按当前待办最少者优先（用于兜底，避免申报无人审核）。
     */
    private Long resolveFallbackAuditor(Long schoolId) {
        LocalDate today = LocalDate.now();
        List<Long> candidates = roleScopeRepository
                .findBySchoolIdAndScopeTypeAndScopeIdAndStatus(schoolId, 1, schoolId, 1).stream()
                .filter(s -> isInEffect(s, today))
                .map(RoleScope::getUserId)
                .filter(Objects::nonNull)
                .filter(this::isEnabledUser)
                .distinct()
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator
                        .comparingLong((Long uid) -> pendingApprovalRepository.countByAuditorIdAndStatus(uid, 1))
                        .thenComparing(uid -> uid))
                .orElse(null);
    }

    /** 手动指定的审批人是否具备该节点要求的角色与授权范围 */
    private boolean isAuditorEligible(Long userId, Long roleId, Integer scopeType, Long scopeId, Long schoolId) {
        if (userId == null || roleId == null) {
            return false;
        }
        boolean hasRole = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(ur -> Objects.equals(ur.getRoleId(), roleId));
        if (!hasRole) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return roleScopeRepository.findByUserIdAndStatus(userId, 1).stream()
                .anyMatch(s -> matchesScope(s, scopeType, scopeId, schoolId, today));
    }

    /** 授权生效期校验：valid_from / valid_until 未设置视为永久有效 */
    private boolean isInEffect(RoleScope scope, LocalDate today) {
        if (scope.getValidFrom() != null && today.isBefore(scope.getValidFrom())) {
            return false;
        }
        if (scope.getValidUntil() != null && today.isAfter(scope.getValidUntil())) {
            return false;
        }
        return true;
    }

    /**
     * 解析委托人在当前时点、与节点角色/范围匹配的「生效中」委托。
     * 委托的 roleId/scopeType/scopeId 为空视为不限（通配）。
     */
    private ApprovalDelegation resolveActiveDelegation(Long delegatorId, Long roleId,
                                                       Integer scopeType, Long scopeId,
                                                       LocalDateTime now) {
        if (delegatorId == null) {
            return null;
        }
        return approvalDelegationRepository.findActiveByDelegatorId(delegatorId, now).stream()
                .filter(d -> d.getRoleId() == null || Objects.equals(d.getRoleId(), roleId))
                .filter(d -> d.getScopeType() == null || Objects.equals(d.getScopeType(), scopeType))
                .filter(d -> d.getScopeId() == null || Objects.equals(d.getScopeId(), scopeId))
                .findFirst()
                .orElse(null);
    }

    /** 待办生成后向实际审批人发审核提醒（audit_remind），失败仅记日志不阻塞提交。 */
    private void notifyAuditorOfNewTask(Long auditorId, String title, String approvableType, Long approvableId) {
        if (auditorId == null) {
            return;
        }
        try {
            UserMessage msg = new UserMessage();
            msg.setUserId(auditorId);
            msg.setSenderType(1);
            msg.setCategory("audit_remind");
            msg.setTitle("新的待审核任务");
            msg.setContent("您有 1 条新的待审核任务「" + title + "」，请及时处理。");
            msg.setRelatedType(approvableType);
            msg.setRelatedId(approvableId);
            msg.setSendChannel("push");
            msg.setIsRead(0);
            msg.setIsArchived(0);
            msg.setIsImportant(1);
            userMessageRepository.save(msg);
        } catch (Exception e) {
            log.warn("待审核任务通知失败（不影响主流程）: approvableType={}, approvableId={}, auditorId={}, err={}",
                    approvableType, approvableId, auditorId, e.getMessage());
        }
    }
}
