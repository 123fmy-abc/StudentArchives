package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.delegation.request.DelegationCreateRequest;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationCancelResponse;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationCreateResponse;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationItem;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationItem.RoleBrief;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationItem.UserBrief;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationListResponse;
import com.example.studentarchives.entity.approval.ApprovalDelegation;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.enums.ScopeTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ApprovalDelegationRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 教师端审批委托服务（《教师端接口文档》十一、审批委托模块）
 * <p>
 * 教师因出差、请假等原因无法处理审批任务时，可将自己的审批权限临时委托给其他教师。
 * 数据来源：approval_delegations 表。委托仅对 approval_flow_steps.allow_delegate=1 的审批节点生效。
 * 权限码：delegate:manage — 管理自己的审批委托（创建/取消）。
 * <p>
 * 委托状态机：0=待生效（定时委托）→ 1=生效中（到达 start_time 自动切换）→ 2=已过期
 * （超过 end_time，由定时任务扫描置位）；3=已取消 为手动取消。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherDelegationService {

    /** 委托状态 */
    private static final int STATUS_PENDING = 0;   // 待生效
    private static final int STATUS_ACTIVE = 1;    // 生效中
    private static final int STATUS_EXPIRED = 2;   // 已过期
    private static final int STATUS_CANCELLED = 3; // 已取消

    /** 委托范围类型仅支持：2=学院 3=专业 4=班级 */
    private static final int SCOPE_COLLEGE = 2;
    private static final int SCOPE_MAJOR = 3;
    private static final int SCOPE_CLASS = 4;

    /** 最长委托期：180 天 */
    private static final long MAX_DELEGATION_DAYS = 180;

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final ApprovalDelegationRepository approvalDelegationRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CollegeRepository collegeRepository;
    private final MajorRepository majorRepository;
    private final ClazzRepository clazzRepository;

    // ==================== 列表 ====================

    /**
     * 获取我的审批委托列表（GET /teacher/delegations，教师端文档 15.1）
     * <p>
     * 返回当前教师创建的委托记录（委托人）与接收的委托记录（受托人），
     * 按 direction 筛选，按创建时间倒序分页返回。
     *
     * @param userId    当前登录教师用户 ID
     * @param direction delegator（我委托的）/ delegatee（委托给我的），默认全部
     * @param status    状态筛选：0=待生效 1=生效中 2=已过期 3=已取消
     * @param pageParam 分页参数
     * @return 委托列表
     */
    @Transactional(readOnly = true)
    public DelegationListResponse listDelegations(Long userId, String direction, Integer status, PageParam pageParam) {
        List<ApprovalDelegation> records = collectRecords(userId, direction);
        List<DelegationItem> items = records.stream()
                .filter(d -> status == null || Objects.equals(effectiveStatus(d), status))
                .sorted(Comparator.comparing(ApprovalDelegation::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toItem)
                .collect(Collectors.toList());

        int total = items.size();
        int from = Math.min(pageParam.getOffset(), total);
        int to = Math.min(from + pageParam.getPerPage(), total);
        List<DelegationItem> pageItems = items.subList(from, to);

        PageResult.Pagination pagination = PageResult.Pagination.builder()
                .page(pageParam.getPage())
                .perPage(pageParam.getPerPage())
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageParam.getPerPage()))
                .build();
        return DelegationListResponse.builder()
                .list(pageItems)
                .pagination(pagination)
                .build();
    }

    /** 按方向收集委托记录（默认委托人 + 受托人两条路径合并去重） */
    private List<ApprovalDelegation> collectRecords(Long userId, String direction) {
        if ("delegator".equals(direction)) {
            return approvalDelegationRepository.findByDelegatorIdOrderByCreatedAtDesc(userId);
        }
        if ("delegatee".equals(direction)) {
            return approvalDelegationRepository.findByDelegateeIdOrderByCreatedAtDesc(userId);
        }
        List<ApprovalDelegation> all = new ArrayList<>();
        all.addAll(approvalDelegationRepository.findByDelegatorIdOrderByCreatedAtDesc(userId));
        all.addAll(approvalDelegationRepository.findByDelegateeIdOrderByCreatedAtDesc(userId));
        return all.stream()
                .collect(Collectors.toMap(ApprovalDelegation::getId, d -> d, (a, b) -> a))
                .values()
                .stream()
                .toList();
    }

    // ==================== 创建 ====================

    /**
     * 创建审批委托（POST /teacher/delegations，教师端文档 15.2）
     * <p>
     * 校验规则：
     * <ul>
     *   <li>受托人必须有教师角色（roles.is_auditor=1），且不能是本人；</li>
     *   <li>startTime 必须晚于当前时间，endTime 必须晚于 startTime，最长委托期 180 天；</li>
     *   <li>委托时段不得与已有生效中的委托记录重叠（status IN (0,1)）；</li>
     *   <li>委托人只能委托自己拥有审批权限的角色和范围。</li>
     * </ul>
     * 委托的审批节点必须 allow_delegate=1 的校验在审批任务分配时执行，不在本接口强制。
     *
     * @param userId  当前登录教师用户 ID（委托人）
     * @param request 委托创建请求
     * @return 委托创建结果
     */
    @Transactional
    public DelegationCreateResponse createDelegation(Long userId, DelegationCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        // ==================== 受托人校验 ====================
        if (Objects.equals(userId, request.getDelegateeId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能将审批权限委托给本人");
        }
        User delegatee = userRepository.findById(request.getDelegateeId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "受托人不存在"));
        if (!isAuditor(delegatee.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "受托人必须有教师审批角色（is_auditor=1）");
        }

        // ==================== 时间校验 ====================
        if (!startTime.isAfter(now)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托开始时间必须晚于当前时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托结束时间必须晚于开始时间");
        }
        if (Duration.between(startTime, endTime).toDays() > MAX_DELEGATION_DAYS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托期最长不超过 180 天");
        }

        // ==================== 时段重叠校验 ====================
        List<ApprovalDelegation> overlapping = approvalDelegationRepository
                .findOverlapping(userId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "委托时段与已有生效中的委托记录重叠");
        }

        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        // ==================== 委托人角色/范围归属校验 ====================
        validateDelegatorOwnership(userId, schoolId, request);

        // ==================== 落库 ====================
        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setSchoolId(schoolId);
        delegation.setDelegatorId(userId);
        delegation.setDelegateeId(request.getDelegateeId());
        delegation.setRoleId(request.getRoleId());
        delegation.setScopeType(request.getScopeType());
        delegation.setScopeId(request.getScopeId());
        delegation.setStartTime(startTime);
        delegation.setEndTime(endTime);
        delegation.setReason(request.getReason());
        // startTime 晚于当前时间（已校验），状态恒为 0=待生效
        delegation.setStatus(STATUS_PENDING);
        delegation = approvalDelegationRepository.save(delegation);

        return DelegationCreateResponse.builder()
                .delegationId(delegation.getId())
                .delegateeName(delegatee.getName())
                .status(delegation.getStatus())
                .statusLabel(statusLabel(delegation.getStatus()))
                .startTime(toIso(delegation.getStartTime()))
                .endTime(toIso(delegation.getEndTime()))
                .build();
    }

    /** 委托人只能委托自己拥有审批权限的角色和范围 */
    private void validateDelegatorOwnership(Long delegatorId, Long schoolId, DelegationCreateRequest request) {
        // 角色归属：roleId 非空时校验委托人持有该角色
        if (request.getRoleId() != null && !hasRole(delegatorId, request.getRoleId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "只能委托自己拥有的角色");
        }
        // 范围归属：scopeType 非空时校验委托人授权范围覆盖
        if (request.getScopeType() != null) {
            Integer scopeType = request.getScopeType();
            if (scopeType != SCOPE_COLLEGE && scopeType != SCOPE_MAJOR && scopeType != SCOPE_CLASS) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "委托范围类型仅支持 2=学院 3=专业 4=班级");
            }
            if (request.getScopeId() != null) {
                // 指定具体范围：严格匹配授权
                scopeValidator.ensureOrgInScope(delegatorId, scopeType, request.getScopeId(), schoolId);
            } else {
                // 不传 scopeId = 委托该类型下所有范围：至少需持有该类型任一授权
                boolean ownsAny = scopeValidator.effectiveScopes(delegatorId).stream()
                        .anyMatch(s -> Objects.equals(s.getScopeType(), scopeType));
                if (!ownsAny) {
                    throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
                }
            }
        }
    }

    // ==================== 取消 ====================

    /**
     * 取消审批委托（PUT /teacher/delegations/{delegationId}/cancel，教师端文档 15.3）
     * <p>
     * 仅委托人本人可取消待生效/生效中的委托；已过期（2）或已取消（3）的委托不可重复取消。
     *
     * @param userId         当前登录教师用户 ID（委托人）
     * @param delegationId   委托记录 ID
     * @param cancelReason   取消原因
     * @return 取消结果
     */
    @Transactional
    public DelegationCancelResponse cancelDelegation(Long userId, Long delegationId, String cancelReason) {
        ApprovalDelegation delegation = approvalDelegationRepository.findByIdAndDelegatorId(delegationId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "委托记录不存在"));

        int effective = effectiveStatus(delegation);
        if (effective == STATUS_EXPIRED || effective == STATUS_CANCELLED) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "当前状态不可取消");
        }
        delegation.setStatus(STATUS_CANCELLED);
        delegation.setCancelledAt(LocalDateTime.now());
        delegation.setCancelReason(cancelReason);
        approvalDelegationRepository.save(delegation);

        return DelegationCancelResponse.builder()
                .delegationId(delegation.getId())
                .status(STATUS_CANCELLED)
                .statusLabel(statusLabel(STATUS_CANCELLED))
                .cancelledAt(toIso(delegation.getCancelledAt()))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** 实体 → 列表项（附委托/受托人、角色、范围名称；生效中且已过 endTime 时展示为已过期） */
    private DelegationItem toItem(ApprovalDelegation d) {
        User delegator = d.getDelegatorId() != null
                ? userRepository.findById(d.getDelegatorId()).orElse(null) : null;
        User delegatee = d.getDelegateeId() != null
                ? userRepository.findById(d.getDelegateeId()).orElse(null) : null;

        RoleBrief role = null;
        if (d.getRoleId() != null) {
            Role r = roleRepository.findById(d.getRoleId()).orElse(null);
            if (r != null) {
                role = RoleBrief.builder().roleId(r.getId()).roleName(r.getName()).build();
            }
        }

        int displayStatus = effectiveStatus(d);
        return DelegationItem.builder()
                .delegationId(d.getId())
                .delegator(toUserBrief(delegator))
                .delegatee(toUserBrief(delegatee))
                .role(role)
                .scopeType(d.getScopeType())
                .scopeTypeLabel(ScopeTypeEnum.of(d.getScopeType()) != null
                        ? ScopeTypeEnum.of(d.getScopeType()).getLabel() : null)
                .scopeId(d.getScopeId())
                .scopeName(resolveScopeName(d))
                .startTime(toIso(d.getStartTime()))
                .endTime(toIso(d.getEndTime()))
                .reason(d.getReason())
                .status(displayStatus)
                .statusLabel(statusLabel(displayStatus))
                .createdAt(toIso(d.getCreatedAt()))
                .build();
    }

    /** 用户实体 → 简信息（缺失返回 null） */
    private UserBrief toUserBrief(User user) {
        if (user == null) {
            return null;
        }
        return UserBrief.builder()
                .userId(user.getId())
                .name(user.getName())
                .userNo(user.getUserNo())
                .build();
    }

    /** 解析委托范围名称（班级/专业/学院；其他类型或缺失返回 null） */
    private String resolveScopeName(ApprovalDelegation d) {
        Long scopeId = d.getScopeId();
        if (scopeId == null) {
            return null;
        }
        return switch (d.getScopeType() == null ? -1 : d.getScopeType()) {
            case SCOPE_COLLEGE -> collegeRepository.findById(scopeId).map(College::getName).orElse(null);
            case SCOPE_MAJOR -> majorRepository.findById(scopeId).map(Major::getName).orElse(null);
            case SCOPE_CLASS -> clazzRepository.findById(scopeId).map(Clazz::getName).orElse(null);
            default -> null;
        };
    }

    /**
     * 有效状态：不依赖后台刷库，按时间实时推导。
     * <ul>
     *   <li>待生效(0)且已到 start_time → 生效中(1)；</li>
     *   <li>生效中(1)且已过 end_time → 已过期(2)；</li>
     *   <li>已取消(3)为人工置位，不可回退。</li>
     * </ul>
     */
    private int effectiveStatus(ApprovalDelegation d) {
        Integer stored = d.getStatus() != null ? d.getStatus() : STATUS_PENDING;
        if (Objects.equals(stored, STATUS_CANCELLED)) {
            return STATUS_CANCELLED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (Objects.equals(stored, STATUS_PENDING) && d.getStartTime() != null
                && !d.getStartTime().isAfter(now)) {
            stored = STATUS_ACTIVE;
        }
        if (Objects.equals(stored, STATUS_ACTIVE) && d.getEndTime() != null
                && d.getEndTime().isBefore(now)) {
            return STATUS_EXPIRED;
        }
        return stored;
    }

    /** 委托状态中文标签 */
    private String statusLabel(int status) {
        return switch (status) {
            case STATUS_PENDING -> "待生效";
            case STATUS_ACTIVE -> "生效中";
            case STATUS_EXPIRED -> "已过期";
            case STATUS_CANCELLED -> "已取消";
            default -> null;
        };
    }

    /** 用户是否为教师审批角色（roles.is_auditor=1） */
    private boolean isAuditor(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        return roleRepository.findByIdIn(roleIds).stream()
                .anyMatch(r -> Objects.equals(r.getIsAuditor(), 1));
    }

    /** 委托人是否持有指定角色 */
    private boolean hasRole(Long userId, Long roleId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .anyMatch(rid -> Objects.equals(rid, roleId));
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
