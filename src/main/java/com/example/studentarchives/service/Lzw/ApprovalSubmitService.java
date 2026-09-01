package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.entity.approval.ApprovalFlow;
import com.example.studentarchives.entity.approval.ApprovalFlowMapping;
import com.example.studentarchives.entity.approval.ApprovalFlowStep;
import com.example.studentarchives.entity.approval.ApprovalInstance;
import com.example.studentarchives.entity.approval.ApprovalNode;
import com.example.studentarchives.entity.approval.PendingApproval;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.repository.ApprovalFlowMappingRepository;
import com.example.studentarchives.repository.ApprovalFlowRepository;
import com.example.studentarchives.repository.ApprovalFlowStepRepository;
import com.example.studentarchives.repository.ApprovalInstanceRepository;
import com.example.studentarchives.repository.ApprovalNodeRepository;
import com.example.studentarchives.repository.PendingApprovalRepository;
import com.example.studentarchives.repository.RoleScopeRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.example.studentarchives.service.Fmy.TeacherScopeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 审批流「启动实例」服务（Lzw）
 * <p>
 * 学生提交申报（Archive / AwardApplication / CareerPlan）后，由各提交 Service 调用
 * {@link #createOnSubmit} 生成审批实例、当前节点与统一待办（pending_approvals），
 * 使教师端「待审核任务模块」（《教师端接口文档》四）4.1 能读到数据。
 * <p>
 * 流程解析遵循《学生档案系统表》审批流口径：
 * 先按 {@code school_id + business_type + business_sub_type} 命中 approval_flow_mappings，
 * 未命中则 fallback 到 approval_flows.is_default=1 的默认流程；审批人按
 * approval_flow_steps 的 role_id + scope_type 解析出持有该角色且授权范围覆盖节点范围的教师。
 * <p>
 * 纯内部联动，不改变任何对外接口契约；解析不到流程/步骤/范围/审批人时<strong>不阻塞提交</strong>，
 * 仅记日志并返回。
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
            log.warn("生成待审核任务失败（不阻塞提交）: approvableType={}, approvableId={}, err={}",
                    approvableType, approvableId, e.getMessage());
        }
    }

    private void doCreate(Long schoolId, String approvableType, String businessSubType,
                          Long approvableId, Long applicantId, String applicantName,
                          String applicantNo, String title, String categoryLabel,
                          LocalDateTime submittedAt) {
        ApprovalFlow flow = resolveFlow(schoolId, approvableType, businessSubType);
        if (flow == null) {
            log.info("未配置审批流程，跳过待办生成: schoolId={}, approvableType={}, subType={}",
                    schoolId, approvableType, businessSubType);
            return;
        }
        List<ApprovalFlowStep> steps = flowStepRepository.findByFlowIdOrderByStepNoAsc(flow.getId());
        if (steps.isEmpty()) {
            log.info("审批流程无步骤，跳过待办生成: flowId={}", flow.getId());
            return;
        }
        ApprovalFlowStep first = steps.get(0);

        TeacherScopeValidator.StudentContext ctx = resolveStudentContext(applicantId);
        Long scopeId = resolveScopeId(schoolId, first.getScopeType(), ctx);
        if (scopeId == null) {
            log.info("无法解析节点范围，跳过待办生成: scopeType={}", first.getScopeType());
            return;
        }
        Long auditorId = resolveAuditor(first.getRoleId(), first.getScopeType(), scopeId, schoolId);
        if (auditorId == null) {
            log.info("无法解析审批人，跳过待办生成: roleId={}, scopeType={}, scopeId={}",
                    first.getRoleId(), first.getScopeType(), scopeId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        ApprovalInstance inst = new ApprovalInstance();
        inst.setApprovableType(approvableType);
        inst.setApprovableId(approvableId);
        inst.setFlowId(flow.getId());
        inst.setFlowVersion(flow.getVersion());
        inst.setApplicantId(applicantId);
        inst.setCurrentStep(1);
        inst.setTotalSteps(steps.size());
        inst.setStatus(1);
        inst = instanceRepository.save(inst);

        ApprovalNode node = new ApprovalNode();
        node.setInstanceId(inst.getId());
        node.setStepNo(first.getStepNo());
        node.setStepName(first.getStepName());
        node.setRoleId(first.getRoleId());
        node.setScopeType(first.getScopeType());
        node.setScopeId(scopeId);
        node.setAssignedAuditorId(auditorId);
        node.setAssignType(1);
        node.setStartedAt(now);
        node.setTimeoutAt(now.plusHours(first.getTimeoutHours() != null ? first.getTimeoutHours() : 48));
        node = nodeRepository.save(node);

        PendingApproval pending = new PendingApproval();
        pending.setSchoolId(schoolId);
        pending.setNodeId(node.getId());
        pending.setInstanceId(inst.getId());
        pending.setApprovableType(approvableType);
        pending.setApprovableId(approvableId);
        pending.setApplicantId(applicantId);
        pending.setApplicantName(applicantName);
        pending.setApplicantNo(applicantNo);
        pending.setTitle(title);
        pending.setCategoryLabel(categoryLabel);
        pending.setSubmittedAt(submittedAt != null ? submittedAt : now);
        pending.setAuditorId(auditorId);
        pending.setRoleId(first.getRoleId());
        pending.setStepNo(first.getStepNo());
        pending.setStepName(first.getStepName());
        pending.setStatus(1);
        pendingApprovalRepository.save(pending);

        log.info("生成待审核任务: taskId={}, approvableType={}, approvableId={}, auditorId={}",
                pending.getId(), approvableType, approvableId, auditorId);
    }

    // ==================== 流程 / 范围 / 审批人解析 ====================

    /**
     * 解析审批流程：先按映射表命中，再 fallback 到默认流程。
     * 只返回 status=1（启用）的流程。
     */
    private ApprovalFlow resolveFlow(Long schoolId, String approvableType, String subType) {
        List<ApprovalFlowMapping> mappings =
                flowMappingRepository.findBySchoolIdAndBusinessType(schoolId, approvableType);

        Comparator<ApprovalFlowMapping> byPriority = Comparator
                .comparing(ApprovalFlowMapping::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ApprovalFlowMapping::getId);

        ApprovalFlowMapping chosen = null;
        if (subType != null) {
            chosen = mappings.stream()
                    .filter(m -> subType.equals(m.getBusinessSubType()))
                    .sorted(byPriority)
                    .findFirst().orElse(null);
        }
        if (chosen == null) {
            chosen = mappings.stream()
                    .filter(m -> m.getBusinessSubType() == null)
                    .sorted(byPriority)
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
     * 解析审批人：持有该角色（user_roles.role_id）且授权范围（role_scopes）覆盖节点范围的教师；
     * 返回第一个命中者。
     * <p>
     * 学校级节点（scopeType=1）：持有该角色且在本校有任意有效数据范围即可审批；
     * 学院/专业/班级节点（scopeType=2/3/4）：数据范围需精确命中节点 scopeId。
     * 注：role_scopes 的数据范围仅支持 2/3/4（管理端口径），故学校级节点按「本校有范围」放宽匹配。
     */
    private Long resolveAuditor(Long roleId, Integer scopeType, Long scopeId, Long schoolId) {
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        for (UserRole ur : userRoles) {
            Long candidateId = ur.getUserId();
            List<RoleScope> scopes = roleScopeRepository.findByUserIdAndStatus(candidateId, 1);
            if (scopes.isEmpty()) {
                continue;
            }
            if (scopeType != null && scopeType == 1) {
                boolean inSchool = scopes.stream().anyMatch(s -> Objects.equals(s.getSchoolId(), schoolId));
                if (inSchool) {
                    return candidateId;
                }
                continue;
            }
            for (RoleScope s : scopes) {
                if (s.getScopeType() == null || s.getScopeId() == null) {
                    continue;
                }
                if (Objects.equals(s.getScopeType(), scopeType) && Objects.equals(s.getScopeId(), scopeId)) {
                    return candidateId;
                }
            }
        }
        return null;
    }
}