package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowCreateRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowMappingUpsertRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowStepItem;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowStepsRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowUpdateRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowCreateResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowDeleteResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowDetailResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowItem;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowMappingDeleteResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowMappingItem;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowMappingResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowStepResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowStepsResponse;
import com.example.studentarchives.entity.approval.ApprovalFlow;
import com.example.studentarchives.entity.approval.ApprovalFlowMapping;
import com.example.studentarchives.entity.approval.ApprovalFlowStep;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ApprovalFlowMappingRepository;
import com.example.studentarchives.repository.ApprovalFlowRepository;
import com.example.studentarchives.repository.ApprovalFlowStepRepository;
import com.example.studentarchives.repository.ApprovalInstanceRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端审批流程配置服务
 * <p>
 * 对应《管理端接口文档》六、审批流程配置模块，统一前缀 /admin/approval-flows 与
 * /admin/approval-flow-mappings：
 * <ul>
 *   <li>6.1 GET /admin/approval-flows：流程列表（分页，按学校/适用类型/状态筛选）；</li>
 *   <li>6.2 POST /admin/approval-flows：创建流程（version 初始 1，可选初始步骤）；</li>
 *   <li>6.3 GET /admin/approval-flows/{flowId}：流程详情（含步骤）；</li>
 *   <li>6.4 PUT /admin/approval-flows/{flowId}：更新流程基础信息（步骤走 6.7）；</li>
 *   <li>6.5 DELETE /admin/approval-flows/{flowId}：软删除流程（审批中实例禁止删除，级联软删步骤）；</li>
 *   <li>6.6 GET /admin/approval-flows/{flowId}/steps：流程步骤列表；</li>
 *   <li>6.7 PUT /admin/approval-flows/{flowId}/steps：保存步骤（按 step_no 全量覆盖）；</li>
 *   <li>6.8 GET /admin/approval-flow-mappings：业务映射列表（分页，冗余流程名）；</li>
 *   <li>6.9 POST /admin/approval-flow-mappings：创建/更新映射（传 id 更新，否则创建）；</li>
 *   <li>6.10 DELETE /admin/approval-flow-mappings/{mappingId}：软删除映射。</li>
 * </ul>
 * 所有接口需校验 admin 角色或 approval:flow:manage 权限码，越权返回 20005 无访问权限。
 * 删除均采用逻辑删除（deleted_at 置位）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApprovalFlowService {

    /** 审批流程配置管理权限码（《管理端接口文档》关键权限码） */
    private static final String PERMISSION = "approval:flow:manage";

    /** 审批实例状态：1=审批中（approval_instances.status） */
    private static final int INSTANCE_STATUS_IN_PROGRESS = 1;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final ApprovalFlowRepository approvalFlowRepository;
    private final ApprovalFlowStepRepository approvalFlowStepRepository;
    private final ApprovalFlowMappingRepository approvalFlowMappingRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final SchoolRepository schoolRepository;

    // ==================== 6.1 获取审批流程列表 ====================

    /**
     * 获取审批流程列表（GET /admin/approval-flows，文档 6.1）
     * <p>
     * 按当前登录用户所属学校、适用类型、启用状态筛选，按 id 倒序分页。
     *
     * @param userId          当前登录用户 ID
     * @param applicableType  适用类型（可选）：Archive/AwardApplication/CareerPlan/GrowthTimeline/Announcement
     * @param status          0=禁用 1=启用（可选，不传返回全部）
     * @param pageParam       分页参数
     * @return 分页的流程列表
     */
    @Transactional(readOnly = true)
    public PageResult<ApprovalFlowItem> listFlows(Long userId, String applicableType,
                                                  Integer status, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        List<ApprovalFlow> base = approvalFlowRepository.findBySchoolId(schoolId);
        List<ApprovalFlow> all = base.stream()
                .filter(f -> applicableType == null || applicableType.isBlank()
                        || Objects.equals(f.getApplicableType(), applicableType))
                .filter(f -> status == null || Objects.equals(f.getStatus(), status))
                .sorted(Comparator.comparing(ApprovalFlow::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        long total = all.size();
        int offset = pageParam.getOffset();
        List<ApprovalFlow> pageItems = offset >= all.size()
                ? List.of()
                : all.subList(offset, Math.min(offset + pageParam.getPerPage(), all.size()));

        List<ApprovalFlowItem> list = pageItems.stream()
                .map(this::toFlowItem)
                .collect(Collectors.toList());
        return PageResult.of(list, total, pageParam);
    }

    // ==================== 6.2 创建审批流程 ====================

    /**
     * 创建审批流程（POST /admin/approval-flows，文档 6.2）
     * <p>
     * version 初始 1；(schoolId + applicableType + applicableSubType + version) 组合唯一，
     * 重复返回 30003 数据重复；isDefault/status 缺省 0/1；isDefault=1 时清空同组其他流程的默认标记；
     * 传 steps 时一并保存初始步骤（校验见 6.7）。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @Transactional
    public ApprovalFlowCreateResponse createFlow(Long userId, ApprovalFlowCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));
        checkFlowVersionUnique(schoolId, request.getApplicableType(),
                request.getApplicableSubType(), 1, null);

        ApprovalFlow flow = new ApprovalFlow();
        flow.setSchoolId(schoolId);
        flow.setFlowName(request.getFlowName());
        flow.setApplicableType(request.getApplicableType());
        flow.setApplicableSubType(request.getApplicableSubType());
        flow.setVersion(1);
        flow.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        flow.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        flow.setCreatedBy(userId);
        flow = approvalFlowRepository.save(flow);

        if (Objects.equals(flow.getIsDefault(), 1)) {
            clearOtherFlowDefaults(flow);
        }
        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            applySteps(flow.getId(), request.getSteps());
        }

        log.info("创建审批流程: id={}, schoolId={}, applicableType={}, applicableSubType={}, operatorId={}",
                flow.getId(), flow.getSchoolId(), flow.getApplicableType(), flow.getApplicableSubType(), userId);
        return toFlowCreateResponse(flow);
    }

    // ==================== 6.3 获取审批流程详情 ====================

    /**
     * 获取审批流程详情（GET /admin/approval-flows/{flowId}，文档 6.3）
     * <p>
     * 返回流程基础信息与按 stepNo 升序的步骤列表。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @return 流程详情
     */
    @Transactional(readOnly = true)
    public ApprovalFlowDetailResponse getFlowDetail(Long userId, Long flowId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ApprovalFlow flow = requireFlow(flowId);
        List<ApprovalFlowStepResponse> steps = approvalFlowStepRepository.findByFlowIdOrderByStepNoAsc(flowId).stream()
                .map(this::toStepResponse)
                .collect(Collectors.toList());
        return toFlowDetailResponse(flow, steps);
    }

    // ==================== 6.4 更新审批流程 ====================

    /**
     * 更新审批流程（PUT /admin/approval-flows/{flowId}，文档 6.4）
     * <p>
     * 仅更新流程基础信息（flowName/applicableType/applicableSubType/isDefault/status），
     * 步骤维护使用 6.7；不自动 version+1（版本由"复制创建新版本"维护）。
     * 已存在 approval_instances 引用的流程，禁止修改适用类型/子类型（流程身份字段），
     * 建议通过复制创建新版本（文档 6.4 注意）。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @param request 更新请求（全字段可选，null=不修改）
     * @return 更新结果
     */
    @Transactional
    public ApprovalFlowCreateResponse updateFlow(Long userId, Long flowId, ApprovalFlowUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        ApprovalFlow flow = requireFlow(flowId);

        if (request.getApplicableType() != null && !request.getApplicableType().isBlank()) {
            if (!Objects.equals(request.getApplicableType(), flow.getApplicableType())) {
                requireIdentityModifiable(flowId, "适用类型");
                flow.setApplicableType(request.getApplicableType());
            }
        }
        if (request.getApplicableSubType() != null
                && !Objects.equals(request.getApplicableSubType(), flow.getApplicableSubType())) {
            requireIdentityModifiable(flowId, "适用子类型");
            flow.setApplicableSubType(request.getApplicableSubType());
        }
        if (request.getFlowName() != null) {
            flow.setFlowName(request.getFlowName());
        }
        if (request.getIsDefault() != null) {
            flow.setIsDefault(request.getIsDefault());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "status 仅支持 0=禁用 1=启用");
            }
            flow.setStatus(request.getStatus());
        }

        // 适用类型/子类型变更后校验 (school + type + subtype + version) 唯一
        checkFlowVersionUnique(flow.getSchoolId(), flow.getApplicableType(),
                flow.getApplicableSubType(), flow.getVersion(), flow.getId());
        flow = approvalFlowRepository.save(flow);

        if (Objects.equals(flow.getIsDefault(), 1)) {
            clearOtherFlowDefaults(flow);
        }
        log.info("更新审批流程: id={}, operatorId={}", flowId, userId);
        return toFlowCreateResponse(flow);
    }

    // ==================== 6.5 删除审批流程 ====================

    /**
     * 删除审批流程（DELETE /admin/approval-flows/{flowId}，文档 6.5）
     * <p>
     * 软删除（deleted_at 置位）；存在进行中的 approval_instances（status=1 审批中）时禁止删除；
     * 级联软删除该流程的全部步骤。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @return 删除结果
     */
    @Transactional
    public ApprovalFlowDeleteResponse deleteFlow(Long userId, Long flowId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        requireFlow(flowId);
        if (approvalInstanceRepository.existsByFlowIdAndStatus(flowId, INSTANCE_STATUS_IN_PROGRESS)) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "存在进行中的审批实例，禁止删除");
        }
        LocalDateTime deletedAt = LocalDateTime.now();
        int updated = approvalFlowRepository.softDeleteById(flowId, deletedAt);
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "流程不存在");
        }
        approvalFlowStepRepository.softDeleteByFlowId(flowId, deletedAt);
        log.info("删除审批流程: id={}, operatorId={}", flowId, userId);
        return ApprovalFlowDeleteResponse.builder()
                .id(flowId)
                .deletedAt(toIso(deletedAt))
                .build();
    }

    // ==================== 6.6 获取流程步骤列表 ====================

    /**
     * 获取流程步骤列表（GET /admin/approval-flows/{flowId}/steps，文档 6.6）
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @return 步骤列表（按 stepNo 升序）
     */
    @Transactional(readOnly = true)
    public List<ApprovalFlowStepResponse> listSteps(Long userId, Long flowId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        requireFlow(flowId);
        return approvalFlowStepRepository.findByFlowIdOrderByStepNoAsc(flowId).stream()
                .map(this::toStepResponse)
                .collect(Collectors.toList());
    }

    // ==================== 6.7 保存流程步骤 ====================

    /**
     * 保存流程步骤（PUT /admin/approval-flows/{flowId}/steps，文档 6.7）
     * <p>
     * 完整步骤列表全量覆盖：后端按 step_no 匹配，已存在则更新、新增的 step_no 插入、
     * 未包含的旧 step_no 软删除。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @param request 步骤保存请求（steps 必填，全量覆盖）
     * @return 保存后的步骤列表
     */
    @Transactional
    public ApprovalFlowStepsResponse saveSteps(Long userId, Long flowId, ApprovalFlowStepsRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        requireFlow(flowId);
        List<ApprovalFlowStepResponse> steps = applySteps(flowId, request.getSteps());
        return ApprovalFlowStepsResponse.builder()
                .flowId(flowId)
                .steps(steps)
                .build();
    }

    // ==================== 6.8 获取审批流程映射列表 ====================

    /**
     * 获取审批流程映射列表（GET /admin/approval-flow-mappings，文档 6.8）
     * <p>
     * 按当前登录用户所属学校、业务类型、业务子类型筛选，按 id 倒序分页；响应冗余回显流程名称。
     *
     * @param userId          当前登录用户 ID
     * @param businessType    业务类型（可选）
     * @param businessSubType 业务子类型（可选）
     * @param pageParam       分页参数
     * @return 分页的映射列表
     */
    @Transactional(readOnly = true)
    public PageResult<ApprovalFlowMappingItem> listMappings(Long userId, String businessType,
                                                            String businessSubType, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        List<ApprovalFlowMapping> base = approvalFlowMappingRepository.findBySchoolId(schoolId);
        List<ApprovalFlowMapping> all = base.stream()
                .filter(m -> businessType == null || businessType.isBlank()
                        || Objects.equals(m.getBusinessType(), businessType))
                .filter(m -> businessSubType == null || businessSubType.isBlank()
                        || Objects.equals(m.getBusinessSubType(), businessSubType))
                .sorted(Comparator.comparing(ApprovalFlowMapping::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        long total = all.size();
        int offset = pageParam.getOffset();
        List<ApprovalFlowMapping> pageItems = offset >= all.size()
                ? List.of()
                : all.subList(offset, Math.min(offset + pageParam.getPerPage(), all.size()));

        Map<Long, String> flowNames = loadFlowNames(pageItems);
        List<ApprovalFlowMappingItem> list = pageItems.stream()
                .map(m -> toMappingItem(m, flowNames.get(m.getFlowId())))
                .collect(Collectors.toList());
        return PageResult.of(list, total, pageParam);
    }

    // ==================== 6.9 创建/更新审批流程映射 ====================

    /**
     * 创建/更新审批流程映射（POST /admin/approval-flow-mappings，文档 6.9）
     * <p>
     * 传 id 则更新（schoolId 不可变更），否则创建；
     * (schoolId + businessType + businessSubType) 组合在软删除条件下唯一（空值敏感），重复返回 30003；
     * effectiveStart/effectiveEnd 均传时校验 end &gt; start（对齐 DB CHECK 约束）；
     * isDefault=1 时清空同组其他映射的默认标记。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建/更新请求
     * @return 映射响应
     */
    @Transactional
    public ApprovalFlowMappingResponse upsertMapping(Long userId, ApprovalFlowMappingUpsertRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));
        ApprovalFlow flow = approvalFlowRepository.findById(request.getFlowId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "流程不存在"));

        boolean isUpdate = request.getId() != null;
        ApprovalFlowMapping mapping = null;
        if (isUpdate) {
            mapping = requireMapping(request.getId());
            if (!Objects.equals(mapping.getSchoolId(), schoolId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "无权更新其他学校的流程映射");
            }
        }
        checkMappingUnique(schoolId, request.getBusinessType(), request.getBusinessSubType(), request.getId());

        LocalDateTime effectiveStart = parseDateTime(request.getEffectiveStart());
        LocalDateTime effectiveEnd = parseDateTime(request.getEffectiveEnd());
        if (effectiveStart != null && effectiveEnd != null && !effectiveEnd.isAfter(effectiveStart)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "effectiveEnd 必须晚于 effectiveStart");
        }

        if (isUpdate) {
            mapping.setBusinessType(request.getBusinessType());
            mapping.setBusinessSubType(request.getBusinessSubType());
            mapping.setFlowId(request.getFlowId());
            mapping.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : mapping.getIsDefault());
            mapping.setEffectiveStart(effectiveStart);
            mapping.setEffectiveEnd(effectiveEnd);
            mapping.setPriority(request.getPriority() != null ? request.getPriority() : mapping.getPriority());
        } else {
            mapping = new ApprovalFlowMapping();
            mapping.setSchoolId(schoolId);
            mapping.setBusinessType(request.getBusinessType());
            mapping.setBusinessSubType(request.getBusinessSubType());
            mapping.setFlowId(request.getFlowId());
            mapping.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
            mapping.setEffectiveStart(effectiveStart);
            mapping.setEffectiveEnd(effectiveEnd);
            mapping.setPriority(request.getPriority() != null ? request.getPriority() : 0);
            mapping.setCreatedBy(userId);
        }
        mapping = approvalFlowMappingRepository.save(mapping);

        if (Objects.equals(mapping.getIsDefault(), 1)) {
            clearOtherMappingDefaults(mapping);
        }
        log.info("{}审批流程映射: id={}, businessType={}, businessSubType={}, flowId={}, operatorId={}",
                isUpdate ? "更新" : "创建", mapping.getId(), mapping.getBusinessType(),
                mapping.getBusinessSubType(), mapping.getFlowId(), userId);
        return toMappingResponse(mapping, flow.getFlowName());
    }

    // ==================== 6.10 删除审批流程映射 ====================

    /**
     * 删除审批流程映射（DELETE /admin/approval-flow-mappings/{mappingId}，文档 6.10）
     * <p>
     * 软删除（deleted_at 置位）。
     *
     * @param userId    当前登录用户 ID
     * @param mappingId 映射 ID
     * @return 删除结果
     */
    @Transactional
    public ApprovalFlowMappingDeleteResponse deleteMapping(Long userId, Long mappingId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        requireMapping(mappingId);
        LocalDateTime deletedAt = LocalDateTime.now();
        int updated = approvalFlowMappingRepository.softDeleteById(mappingId, deletedAt);
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "映射不存在");
        }
        log.info("删除审批流程映射: id={}, operatorId={}", mappingId, userId);
        return ApprovalFlowMappingDeleteResponse.builder()
                .id(mappingId)
                .deletedAt(toIso(deletedAt))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** 按 ID 加载流程，不存在返回 30001 */
    private ApprovalFlow requireFlow(Long flowId) {
        return approvalFlowRepository.findById(flowId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "流程不存在"));
    }

    /** 按 ID 加载映射，不存在返回 30001 */
    private ApprovalFlowMapping requireMapping(Long mappingId) {
        return approvalFlowMappingRepository.findById(mappingId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "流程映射不存在"));
    }

    /** 流程身份字段（适用类型/子类型）被审批实例引用时禁止修改（文档 6.4 注意） */
    private void requireIdentityModifiable(Long flowId, String fieldLabel) {
        if (approvalInstanceRepository.existsByFlowId(flowId)) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE,
                    "该流程已被审批实例引用，禁止修改" + fieldLabel + "，建议通过复制创建新版本");
        }
    }

    /**
     * 校验 (schoolId + applicableType + applicableSubType + version) 组合唯一。
     * applicableSubType 采用空值敏感比较（NULL=通用，与同值 NULL 视为重复）。
     *
     * @param excludeId 更新时排除当前流程 ID，创建传 null
     */
    private void checkFlowVersionUnique(Long schoolId, String applicableType, String applicableSubType,
                                        Integer version, Long excludeId) {
        boolean duplicate = approvalFlowRepository.findBySchoolIdAndApplicableType(schoolId, applicableType).stream()
                .filter(f -> Objects.equals(f.getApplicableSubType(), applicableSubType))
                .filter(f -> Objects.equals(f.getVersion(), version))
                .anyMatch(f -> excludeId == null || !Objects.equals(f.getId(), excludeId));
        if (duplicate) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE,
                    "同一学校、适用类型、适用子类型下已存在版本 " + version + " 的流程");
        }
    }

    /** 清空同 (school, type, subtype) 组内其他流程的默认标记（isDefault=0），保证组内仅一条默认 */
    private void clearOtherFlowDefaults(ApprovalFlow flow) {
        List<ApprovalFlow> sameGroup = approvalFlowRepository
                .findBySchoolIdAndApplicableType(flow.getSchoolId(), flow.getApplicableType()).stream()
                .filter(f -> Objects.equals(f.getApplicableSubType(), flow.getApplicableSubType()))
                .filter(f -> !Objects.equals(f.getId(), flow.getId()))
                .collect(Collectors.toList());
        boolean changed = false;
        for (ApprovalFlow other : sameGroup) {
            if (Objects.equals(other.getIsDefault(), 1)) {
                other.setIsDefault(0);
                approvalFlowRepository.save(other);
                changed = true;
            }
        }
        if (changed) {
            log.info("清空同组其他流程默认标记: schoolId={}, applicableType={}, applicableSubType={}",
                    flow.getSchoolId(), flow.getApplicableType(), flow.getApplicableSubType());
        }
    }

    /** 校验 (schoolId + businessType + businessSubType) 组合唯一（空值敏感），excludeId 更新时排除自身 */
    private void checkMappingUnique(Long schoolId, String businessType, String businessSubType, Long excludeId) {
        boolean duplicate = approvalFlowMappingRepository.findBySchoolIdAndBusinessType(schoolId, businessType).stream()
                .filter(m -> Objects.equals(m.getBusinessSubType(), businessSubType))
                .anyMatch(m -> excludeId == null || !Objects.equals(m.getId(), excludeId));
        if (duplicate) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "同一学校、业务类型、业务子类型下已存在流程映射");
        }
    }

    /** 清空同 (school, type, subtype) 组内其他映射的默认标记，保证组内仅一条默认 */
    private void clearOtherMappingDefaults(ApprovalFlowMapping mapping) {
        List<ApprovalFlowMapping> sameGroup = approvalFlowMappingRepository
                .findBySchoolIdAndBusinessType(mapping.getSchoolId(), mapping.getBusinessType()).stream()
                .filter(m -> Objects.equals(m.getBusinessSubType(), mapping.getBusinessSubType()))
                .filter(m -> !Objects.equals(m.getId(), mapping.getId()))
                .collect(Collectors.toList());
        boolean changed = false;
        for (ApprovalFlowMapping other : sameGroup) {
            if (Objects.equals(other.getIsDefault(), 1)) {
                other.setIsDefault(0);
                approvalFlowMappingRepository.save(other);
                changed = true;
            }
        }
        if (changed) {
            log.info("清空同组其他映射默认标记: schoolId={}, businessType={}, businessSubType={}",
                    mapping.getSchoolId(), mapping.getBusinessType(), mapping.getBusinessSubType());
        }
    }

    /**
     * 按 step_no 全量覆盖流程步骤：已存在则更新、新增的 step_no 插入、未包含的旧步骤软删除。
     * 创建流程（6.2）传入初始步骤时复用本逻辑。
     *
     * @return 保存后的步骤响应列表（按 stepNo 升序）
     */
    private List<ApprovalFlowStepResponse> applySteps(Long flowId, List<ApprovalFlowStepItem> items) {
        // 1. 业务校验（stepNo 唯一、0/1 开关、退回动作等跨字段规则）
        Set<Integer> seen = new HashSet<>();
        for (ApprovalFlowStepItem item : items) {
            if (!seen.add(item.getStepNo())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "步骤 stepNo 不能重复: " + item.getStepNo());
            }
            validateStepItem(item);
        }

        // 2. 加载已有步骤，按 stepNo 建立映射
        Map<Integer, ApprovalFlowStep> existing = approvalFlowStepRepository.findByFlowIdOrderByStepNoAsc(flowId)
                .stream().collect(Collectors.toMap(ApprovalFlowStep::getStepNo, s -> s));

        // 3. 全量覆盖：已存在更新、新增插入
        List<ApprovalFlowStep> saved = new ArrayList<>();
        for (ApprovalFlowStepItem item : items) {
            ApprovalFlowStep step = existing.get(item.getStepNo());
            if (step == null) {
                step = new ApprovalFlowStep();
                step.setFlowId(flowId);
                step.setStepNo(item.getStepNo());
            }
            applyStepFields(step, item);
            approvalFlowStepRepository.save(step);
            saved.add(step);
        }

        // 4. 未包含的旧步骤软删除
        for (Map.Entry<Integer, ApprovalFlowStep> entry : existing.entrySet()) {
            if (!seen.contains(entry.getKey())) {
                approvalFlowStepRepository.softDeleteById(entry.getValue().getId(), LocalDateTime.now());
            }
        }

        return saved.stream()
                .sorted(Comparator.comparing(ApprovalFlowStep::getStepNo))
                .map(this::toStepResponse)
                .collect(Collectors.toList());
    }

    /** 步骤项跨字段业务校验（Bean Validation 已覆盖必填/范围，此处补 0/1 开关、退回动作关联校验） */
    private void validateStepItem(ApprovalFlowStepItem item) {
        if (item.getAutoAssign() != null && item.getAutoAssign() != 0 && item.getAutoAssign() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "autoAssign 仅支持 0=手动 1=自动");
        }
        if (item.getAllowDelegate() != null && item.getAllowDelegate() != 0 && item.getAllowDelegate() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "allowDelegate 仅支持 0/1");
        }
        if (item.getAllowSkip() != null && item.getAllowSkip() != 0 && item.getAllowSkip() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "allowSkip 仅支持 0/1");
        }
        if (item.getAllowDesignateNext() != null && item.getAllowDesignateNext() != 0 && item.getAllowDesignateNext() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "allowDesignateNext 仅支持 0/1");
        }
        if (item.getTimeoutHours() != null && item.getTimeoutHours() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "timeoutHours 最小为 1");
        }
        if (item.getRejectAction() != null) {
            String rejectAction = item.getRejectAction();
            if (!"end".equals(rejectAction) && !"return".equals(rejectAction)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "rejectAction 仅支持 end/return");
            }
            if ("return".equals(rejectAction) && item.getRejectToStep() == null) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "rejectAction=return 时 rejectToStep 必填");
            }
        }
    }

    /** 将请求步骤项写入实体（缺省值：autoAssign=1、allowDelegate=0、allowSkip=0、allowDesignateNext=0、timeoutHours=48、rejectAction=end、sort=0） */
    private void applyStepFields(ApprovalFlowStep step, ApprovalFlowStepItem item) {
        step.setStepName(item.getStepName());
        step.setRoleId(item.getRoleId());
        step.setScopeType(item.getScopeType());
        step.setScopeRule(item.getScopeRule());
        step.setAutoAssign(item.getAutoAssign() != null ? item.getAutoAssign() : 1);
        step.setAllowDelegate(item.getAllowDelegate() != null ? item.getAllowDelegate() : 0);
        step.setAllowSkip(item.getAllowSkip() != null ? item.getAllowSkip() : 0);
        step.setAllowDesignateNext(item.getAllowDesignateNext() != null ? item.getAllowDesignateNext() : 0);
        step.setTimeoutHours(item.getTimeoutHours() != null ? item.getTimeoutHours() : 48);
        step.setRejectAction(item.getRejectAction() != null ? item.getRejectAction() : "end");
        step.setRejectToStep(item.getRejectToStep());
        step.setSort(item.getSort() != null ? item.getSort() : 0);
    }

    /** 批量加载流程名称映射（供映射列表冗余 flowName） */
    private Map<Long, String> loadFlowNames(List<ApprovalFlowMapping> mappings) {
        if (mappings.isEmpty()) {
            return Map.of();
        }
        Set<Long> flowIds = mappings.stream()
                .map(ApprovalFlowMapping::getFlowId)
                .collect(Collectors.toSet());
        return approvalFlowRepository.findAllById(flowIds).stream()
                .collect(Collectors.toMap(ApprovalFlow::getId, ApprovalFlow::getFlowName, (a, b) -> a));
    }

    // ==================== DTO 转换 ====================

    private ApprovalFlowItem toFlowItem(ApprovalFlow flow) {
        return ApprovalFlowItem.builder()
                .id(flow.getId())
                .schoolId(flow.getSchoolId())
                .flowName(flow.getFlowName())
                .applicableType(flow.getApplicableType())
                .applicableSubType(flow.getApplicableSubType())
                .version(flow.getVersion())
                .isDefault(flow.getIsDefault())
                .status(flow.getStatus())
                .createdAt(toIso(flow.getCreatedAt()))
                .build();
    }

    private ApprovalFlowCreateResponse toFlowCreateResponse(ApprovalFlow flow) {
        return ApprovalFlowCreateResponse.builder()
                .id(flow.getId())
                .schoolId(flow.getSchoolId())
                .flowName(flow.getFlowName())
                .applicableType(flow.getApplicableType())
                .applicableSubType(flow.getApplicableSubType())
                .version(flow.getVersion())
                .isDefault(flow.getIsDefault())
                .status(flow.getStatus())
                .createdAt(toIso(flow.getCreatedAt()))
                .build();
    }

    private ApprovalFlowDetailResponse toFlowDetailResponse(ApprovalFlow flow, List<ApprovalFlowStepResponse> steps) {
        return ApprovalFlowDetailResponse.builder()
                .id(flow.getId())
                .schoolId(flow.getSchoolId())
                .flowName(flow.getFlowName())
                .applicableType(flow.getApplicableType())
                .applicableSubType(flow.getApplicableSubType())
                .version(flow.getVersion())
                .isDefault(flow.getIsDefault())
                .status(flow.getStatus())
                .steps(steps)
                .createdAt(toIso(flow.getCreatedAt()))
                .build();
    }

    private ApprovalFlowStepResponse toStepResponse(ApprovalFlowStep step) {
        return ApprovalFlowStepResponse.builder()
                .id(step.getId())
                .stepNo(step.getStepNo())
                .stepName(step.getStepName())
                .roleId(step.getRoleId())
                .scopeType(step.getScopeType())
                .scopeRule(step.getScopeRule())
                .autoAssign(step.getAutoAssign())
                .allowDelegate(step.getAllowDelegate())
                .allowSkip(step.getAllowSkip())
                .allowDesignateNext(step.getAllowDesignateNext())
                .timeoutHours(step.getTimeoutHours())
                .rejectAction(step.getRejectAction())
                .rejectToStep(step.getRejectToStep())
                .sort(step.getSort())
                .build();
    }

    private ApprovalFlowMappingItem toMappingItem(ApprovalFlowMapping mapping, String flowName) {
        return ApprovalFlowMappingItem.builder()
                .id(mapping.getId())
                .schoolId(mapping.getSchoolId())
                .businessType(mapping.getBusinessType())
                .businessSubType(mapping.getBusinessSubType())
                .flowId(mapping.getFlowId())
                .flowName(flowName)
                .isDefault(mapping.getIsDefault())
                .effectiveStart(toIso(mapping.getEffectiveStart()))
                .effectiveEnd(toIso(mapping.getEffectiveEnd()))
                .priority(mapping.getPriority())
                .createdAt(toIso(mapping.getCreatedAt()))
                .build();
    }

    private ApprovalFlowMappingResponse toMappingResponse(ApprovalFlowMapping mapping, String flowName) {
        return ApprovalFlowMappingResponse.builder()
                .id(mapping.getId())
                .schoolId(mapping.getSchoolId())
                .businessType(mapping.getBusinessType())
                .businessSubType(mapping.getBusinessSubType())
                .flowId(mapping.getFlowId())
                .flowName(flowName)
                .isDefault(mapping.getIsDefault())
                .effectiveStart(toIso(mapping.getEffectiveStart()))
                .effectiveEnd(toIso(mapping.getEffectiveEnd()))
                .priority(mapping.getPriority())
                .createdAt(toIso(mapping.getCreatedAt()))
                .build();
    }

    // ==================== 时间工具 ====================

    /** ISO 8601 带时区字符串 → LocalDateTime（空白返回 null，格式错误返回 10003） */
    private LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR,
                    "时间格式错误，需为 ISO 8601，如 2026-08-01T00:00:00+08:00");
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
