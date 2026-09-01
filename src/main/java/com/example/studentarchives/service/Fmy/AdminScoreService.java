package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.indicator.response.AdminIndicatorTreeResponse;
import com.example.studentarchives.dto.Fmy.score.request.ScoreRecalculateRequest;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculateResponse;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculationTaskResponse;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.archive.ArchiveCertificate;
import com.example.studentarchives.entity.archive.ArchiveCompetition;
import com.example.studentarchives.entity.archive.ArchiveInnovation;
import com.example.studentarchives.entity.archive.ArchiveOrganization;
import com.example.studentarchives.entity.archive.ArchiveResearch;
import com.example.studentarchives.entity.archive.ArchiveScholarship;
import com.example.studentarchives.entity.archive.ArchiveSocialPractice;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.evaluation.ScoreCalculation;
import com.example.studentarchives.entity.evaluation.ScoreCalculationDetail;
import com.example.studentarchives.entity.evaluation.ScoreRecalculationTask;
import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import com.example.studentarchives.entity.foundation.IndicatorRuleVersion;
import com.example.studentarchives.entity.grade.GpaRecord;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ArchiveCertificateRepository;
import com.example.studentarchives.repository.ArchiveCompetitionRepository;
import com.example.studentarchives.repository.ArchiveInnovationRepository;
import com.example.studentarchives.repository.ArchiveOrganizationRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ArchiveResearchRepository;
import com.example.studentarchives.repository.ArchiveScholarshipRepository;
import com.example.studentarchives.repository.ArchiveSocialPracticeRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.EvaluationIndicatorRepository;
import com.example.studentarchives.repository.GpaRecordRepository;
import com.example.studentarchives.repository.IndicatorRuleVersionRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.ScoreCalculationDetailRepository;
import com.example.studentarchives.repository.ScoreCalculationRepository;
import com.example.studentarchives.repository.ScoreRecalculationTaskRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端评分重算服务
 * <p>
 * 对应《管理端接口文档》三、评分重算模块（3.1 触发评分重算 / 3.2 查询任务进度）。
 * <ul>
 *   <li>3.1 POST /admin/scores/recalculate：校验 admin 角色或 score:recalculate 权限码，
 *       校验重算范围与「同一范围已有生效中任务」（41005），创建 score_recalculation_tasks
 *       记录后提交异步执行，接口立即返回任务 ID。</li>
 *   <li>3.2 GET /admin/scores/recalculation-tasks/{taskId}：查询任务状态、进度与结果摘要。</li>
 * </ul>
 * 异步执行时按目标学期自己最近发布的指标规则版本（indicator_rule_versions 按 semester_id 匹配，
 * 未单独发布则回退全校当前生效版本）重算各学生各维度画像得分，替换该学生该学期旧的评分计算批次
 * 与画像得分。指标配置一律读取发布时冻结的 tree_snapshot，保证发布后的草稿编辑不影响历史评分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminScoreService {

    /** 触发评分重算权限码（《管理端接口文档》关键权限码） */
    private static final String RECALC_PERMISSION = "score:recalculate";

    /** 任务状态：0=排队中 1=执行中 2=完成 3=失败 */
    private static final int STATUS_QUEUED = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_FAILED = 3;

    /** 重算范围枚举（对齐 score_recalculation_tasks.task_type）：1=指定学生 2=指定班级 3=指定学期 4=全量 5=指定专业 */
    private static final int TARGET_STUDENT = 1;
    private static final int TARGET_CLASS = 2;
    private static final int TARGET_SEMESTER = 3;
    private static final int TARGET_FULL = 4;
    private static final int TARGET_MAJOR = 5;

    /** 计算触发类型（对齐 score_calculations.trigger_type 的 DDL 注释）：1=手动触发 2=系统自动/定时任务 */
    private static final int TRIGGER_MANUAL = 1;
    private static final int TRIGGER_AUTO = 2;

    /** 班级范围类型（role_scopes.scope_type，教师端范围校验用） */
    private static final int SCOPE_CLASS = 4;

    private static final BigDecimal BD_100 = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final ScoreRecalculationTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final GpaRecordRepository gpaRecordRepository;
    private final ArchiveRepository archiveRepository;
    private final ArchiveCompetitionRepository archiveCompetitionRepository;
    private final ArchiveCertificateRepository archiveCertificateRepository;
    private final ArchiveOrganizationRepository archiveOrganizationRepository;
    private final ArchiveScholarshipRepository archiveScholarshipRepository;
    private final ArchiveResearchRepository archiveResearchRepository;
    private final ArchiveInnovationRepository archiveInnovationRepository;
    private final ArchiveSocialPracticeRepository archiveSocialPracticeRepository;
    private final IndicatorRuleVersionRepository indicatorRuleVersionRepository;
    private final EvaluationIndicatorRepository evaluationIndicatorRepository;
    private final ScoreCalculationRepository scoreCalculationRepository;
    private final ScoreCalculationDetailRepository scoreCalculationDetailRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final DataCompletenessService dataCompletenessService;
    private final ObjectMapper objectMapper;

    /** 自引用代理（@Lazy 避免循环依赖），用于触发后提交 @Async 异步执行 */
    @Lazy
    @Autowired
    private AdminScoreService self;

    // ==================== 2.1 触发评分重算 ====================

    /**
     * 触发评分重算（POST /admin/scores/recalculate，文档 2.1）
     * <p>
     * 管理员触发指定学生/班级/学期/专业/全量的评分重算。任务进入 score_recalculation_tasks
     * 异步执行，接口立即返回任务 ID。
     *
     * @param userId  当前登录用户 ID
     * @param request 触发请求
     * @return 任务 ID 与初始状态
     */
    public ScoreRecalculateResponse triggerRecalculate(Long userId, ScoreRecalculateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, RECALC_PERMISSION);
        return doTriggerRecalculate(userId, request);
    }

    /**
     * 触发评分重算核心逻辑（鉴权/范围校验完成后共用）：校验学期与范围 → 创建任务 → 提交异步执行。
     * <p>
     * 管理端 2.1 与教师端 11.4 共用同一执行路径（任务进入 score_recalculation_tasks
     * 独立事务异步执行）。
     *
     * @param userId  当前登录用户 ID
     * @param request 触发请求
     * @return 任务 ID 与初始状态
     */
    private ScoreRecalculateResponse doTriggerRecalculate(Long userId, ScoreRecalculateRequest request) {
        User operator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));

        // 学期校验：存在且属于当前学校
        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));
        if (!Objects.equals(semester.getSchoolId(), operator.getSchoolId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学期不属于当前学校");
        }

        // 重算范围参数校验
        validateTarget(request);

        // 同一范围已有生效中任务 → 41005
        checkNoRunningTask(operator.getSchoolId(), request);

        LocalDateTime now = LocalDateTime.now();
        ScoreRecalculationTask task = new ScoreRecalculationTask();
        task.setSchoolId(operator.getSchoolId());
        task.setTaskType(request.getTargetType());
        task.setTargetId(request.getTargetType() == TARGET_FULL ? null : request.getTargetId());
        task.setSemesterId(request.getSemesterId());
        task.setStatus(STATUS_QUEUED);
        task.setTriggeredBy(userId);
        task.setTriggeredAt(now);
        task.setProgress(0);
        taskRepository.save(task);

        // 提交异步执行（@Async 走代理，接口立即返回）
        self.executeAsync(task.getId());

        return ScoreRecalculateResponse.builder()
                .taskId(task.getId())
                .targetType(task.getTaskType())
                .targetId(task.getTargetId())
                .semesterId(task.getSemesterId())
                .status(task.getStatus())
                .statusLabel(statusLabel(task.getStatus()))
                .createdAt(toIso(now))
                .message("评分重算任务已创建")
                .build();
    }

    /**
     * 单个学生指定学期的评分重算（事件/内部调用）。
     * <p>
     * 按该学期生效的规则版本（读 tree_snapshot）异步计算，并持久化一条
     * score_recalculation_tasks 任务记录（targetType=1 指定学生）走完整状态生命周期，
     * 便于审计与查询。operatorId 为空时回退为学校首个用户（同导出模板种子器的 created_by 约定）；
     * 学校无用户则仅跳过落库、仍继续执行重算（避免 triggered_by 外键失败）。
     *
     * @param schoolId   学校 ID
     * @param userId     学生用户 ID
     * @param semesterId 学期 ID
     * @param operatorId 操作人 ID（可为 null）
     */
    @Async("scoreRecalculationExecutor")
    public void recalculateStudent(Long schoolId, Long userId, Long semesterId, Long operatorId) {
        ResolvedRule resolved = resolveRuleForSemester(schoolId, semesterId);
        if (resolved == null) {
            log.warn("无可用指标规则版本，跳过自动评分: schoolId={}, userId={}, semesterId={}",
                    schoolId, userId, semesterId);
            // 无评分规则版本时仍同步计算数据完整度（档案审核通过/成绩导入等触发场景）
            try {
                dataCompletenessService.recalculateForStudent(userId, semesterId);
            } catch (Exception e) {
                log.warn("数据完整度计算失败（无规则版本分支），不影响主流程: userId={}, semesterId={}",
                        userId, semesterId, e);
            }
            return;
        }
        Map<Long, EvaluationIndicator> indicatorById = toIndicatorMap(resolved.indicators());
        Long comparedSemesterId = resolvePreviousSemesterId(schoolId, semesterId);

        ScoreRecalculationTask task = new ScoreRecalculationTask();
        task.setSchoolId(schoolId);
        task.setTaskType(TARGET_STUDENT);
        task.setTargetId(userId);
        task.setSemesterId(semesterId);
        task.setStatus(STATUS_QUEUED);
        task.setTriggeredBy(resolveTriggeredBy(schoolId, operatorId));
        task.setTriggeredAt(LocalDateTime.now());
        task.setTotalCount(1);
        task.setProgress(0);

        // 学校无任何用户时无法满足 triggered_by 外键，跳过落库，仅执行重算
        if (task.getTriggeredBy() == null) {
            log.warn("学校无用户，跳过评分重算任务落库: schoolId={}, userId={}, semesterId={}",
                    schoolId, userId, semesterId);
        } else {
            taskRepository.save(task);
        }

        try {
            task.setStatus(STATUS_RUNNING);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);

            // 经 self 代理调用事务包装方法，清理/落库在同一事务内完成
            self.reprocessStudent(task, userId, indicatorById, resolved.version(), comparedSemesterId, TRIGGER_AUTO);

            task.setStatus(STATUS_DONE);
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(100);
            task.setSuccessCount(1);
            task.setFailCount(0);
            taskRepository.save(task);
            log.info("档案审核通过自动评分完成: schoolId={}, userId={}, semesterId={}, ruleVersion={}",
                    schoolId, userId, semesterId, resolved.version());
        } catch (Exception e) {
            log.error("档案审核通过自动评分失败: schoolId={}, userId={}, semesterId={}",
                    schoolId, userId, semesterId, e);
            failTask(task, errorText(e));
        }
    }

    /** 解析任务触发人：优先操作人 ID，为空时回退为学校首个用户（可能仍为 null） */
    private Long resolveTriggeredBy(Long schoolId, Long operatorId) {
        if (operatorId != null) {
            return operatorId;
        }
        return userRepository.findFirstBySchoolIdOrderByIdAsc(schoolId)
                .map(User::getId)
                .orElse(null);
    }

    // ==================== 2.2 查询评分重算任务进度 ====================

    /**
     * 查询评分重算任务进度（GET /admin/scores/recalculation-tasks/{taskId}，文档 2.2）
     *
     * @param userId 当前登录用户 ID
     * @param taskId 评分重算任务 ID
     * @return 任务状态、进度与结果摘要
     */
    public ScoreRecalculationTaskResponse getRecalculationTask(Long userId, Long taskId) {
        adminAuthService.requireAdminOrPermission(userId, RECALC_PERMISSION);
        ScoreRecalculationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "评分重算任务不存在"));
        return toTaskResponse(task);
    }

    /**
     * 教师端触发评分重算（POST /teacher/scores/recalculate，教师端文档 11.4）
     * <p>
     * 复用 2.1 异步引擎（{@link #doTriggerRecalculate}），仅替换鉴权与范围策略：
     * 管理员放行或持有 {@code score:recalculate} 权限码，且 targetType 仅支持
     * 1=指定学生 2=指定班级 3=指定学期（4=全量 / 5=指定专业仅限管理端）。
     * 目标范围必须落在教师 {@code role_scopes} 授权范围内：学生按归属组织链匹配、
     * 班级按同类型 scopeId 匹配、学期重算覆盖全校需学校级授权，越权返回 20005。
     *
     * @param userId  当前登录用户 ID
     * @param request 触发请求（targetType 仅 1/2/3）
     * @return 任务 ID 与初始状态
     */
    public ScoreRecalculateResponse triggerRecalculateByTeacher(Long userId, ScoreRecalculateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, RECALC_PERMISSION);
        Integer targetType = request.getTargetType();
        if (targetType == null || targetType < TARGET_STUDENT || targetType > TARGET_SEMESTER) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        switch (targetType) {
            case TARGET_STUDENT:
                if (request.getTargetId() == null) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "targetType=1 时 targetId 必填");
                }
                scopeValidator.ensureStudentInScope(userId, request.getTargetId(), schoolId);
                break;
            case TARGET_CLASS:
                if (request.getTargetId() == null) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "targetType=2 时 targetId 必填");
                }
                scopeValidator.ensureOrgInScope(userId, SCOPE_CLASS, request.getTargetId(), schoolId);
                break;
            case TARGET_SEMESTER:
                // 学期重算覆盖全校学生，需学校级授权（或 admin）
                scopeValidator.ensureSchoolScope(userId, schoolId);
                break;
            default:
                break;
        }
        return doTriggerRecalculate(userId, request);
    }

    /**
     * 教师端查询评分重算任务进度（GET /teacher/scores/recalculation-tasks/{taskId}，教师端文档 11.5）
     * <p>
     * 复用 2.2 查询逻辑（{@link #toTaskResponse}），教师侧补充任务归属校验：
     * 仅本人触发的任务（triggered_by）可查，管理员可查任意任务，非本人且非管理员返回 20005。
     *
     * @param userId 当前登录用户 ID
     * @param taskId 评分重算任务 ID
     * @return 任务状态、进度与结果摘要
     */
    public ScoreRecalculationTaskResponse getRecalculationTaskByTeacher(Long userId, Long taskId) {
        adminAuthService.requireAdminOrPermission(userId, RECALC_PERMISSION);
        ScoreRecalculationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "评分重算任务不存在"));
        if (!Objects.equals(task.getTriggeredBy(), userId)) {
            AdminAuthService.OperatorRole role = adminAuthService.resolveOperatorRole(userId);
            if (role == null || !role.isAdmin()) {
                throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
            }
        }
        return toTaskResponse(task);
    }

    // ==================== 异步执行 ====================

    /**
     * 异步执行评分重算任务（由 triggerRecalculate 经 @Async 代理提交）。
     * <p>
     * 流程：任务置为执行中 → 读取当前生效指标规则版本 → 解析目标学生 → 逐学生重算评分
     * （替换旧批次与画像得分）→ 刷新进度 → 置为完成/失败。每个学生独立事务
     * （经 {@link #reprocessStudent} 事务包装），单个学生失败仅回滚其自身写入、不中断整体任务。
     *
     * @param taskId 评分重算任务 ID
     */
    @Async("scoreRecalculationExecutor")
    public void executeAsync(Long taskId) {
        ScoreRecalculationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("评分重算任务不存在，跳过执行: taskId={}", taskId);
            return;
        }
        try {
            task.setStatus(STATUS_RUNNING);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);

            ResolvedRule resolved = resolveRuleForSemester(task.getSchoolId(), task.getSemesterId());
            if (resolved == null) {
                failTask(task, "当前学期无生效的指标规则版本，无法执行评分重算");
                return;
            }
            Map<Long, EvaluationIndicator> indicatorById = toIndicatorMap(resolved.indicators());
            Long comparedSemesterId = resolvePreviousSemesterId(task.getSchoolId(), task.getSemesterId());

            List<Long> studentIds = resolveStudentIds(task);
            if (studentIds.isEmpty()) {
                failTask(task, "目标范围内没有可重算的学生");
                return;
            }

            task.setTotalCount(studentIds.size());
            taskRepository.save(task);

            int success = 0;
            int fail = 0;
            int total = studentIds.size();
            List<StudentFailure> failures = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                Long studentId = studentIds.get(i);
                try {
                    // 经 self 代理调用事务包装方法：@Modifying 清理与落库需活动事务，
                    // 事务边界按单个学生划分，失败仅回滚该学生写入
                    self.reprocessStudent(task, studentId, indicatorById, resolved.version(), comparedSemesterId, TRIGGER_MANUAL);
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("评分重算学生失败 taskId={}, userId={}", taskId, studentId, e);
                    failures.add(new StudentFailure(studentId, errorText(e)));
                }
                int done = i + 1;
                if (done % 10 == 0 || done == total) {
                    task.setProgress((int) Math.round(done * 100.0 / total));
                    task.setSuccessCount(success);
                    task.setFailCount(fail);
                    taskRepository.save(task);
                }
            }

            task.setStatus(STATUS_DONE);
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(100);
            task.setSuccessCount(success);
            task.setFailCount(fail);
            task.setErrorMessage(serializeFailures(failures));
            taskRepository.save(task);
            log.info("评分重算任务完成 taskId={}, total={}, success={}, fail={}", taskId, total, success, fail);
        } catch (Exception e) {
            log.error("评分重算任务执行失败 taskId={}", taskId, e);
            failTask(task, e.getMessage());
        }
    }

    // ==================== 指标规则版本解析（评分数据源） ====================

    /** 指定学期评分应使用的指标规则版本号及其指标集合 */
    private record ResolvedRule(int version, List<EvaluationIndicator> indicators) {
    }

    /** 单个学生重算失败记录（userId + 失败原因，任务完成时序列化为 JSON 持久化到 error_message） */
    private record StudentFailure(Long userId, String message) {
    }

    /**
     * 解析指定学期评分应使用的指标规则版本及该版本的指标集合。
     * <p>
     * 版本选取：优先取该学期自己最近发布的规则版本（semester_id 匹配、版本号最大）；
     * 该学期未发布过则回退全校当前生效版本（与原行为一致，兼容「未单独发布学期的学期按全校规则评分」）。
     * 两者都没有返回 null（跳过评分）。
     * <p>
     * 指标一律从发布时冻结的 {@code tree_snapshot} 重建（而非活表 evaluation_indicators），
     * 保证发布后的草稿编辑不影响历史评分；快照落地前发布的旧版本（tree_snapshot 为空）
     * 回退按版本号查活表（活表仅对最新版本有数据，更早版本返回空 → 跳过）。
     *
     * @param schoolId   学校 ID
     * @param semesterId 学期 ID（可为 null）
     * @return 规则版本号与指标集合；无可用规则版本返回 null
     */
    private ResolvedRule resolveRuleForSemester(Long schoolId, Long semesterId) {
        IndicatorRuleVersion ruleVersion = null;
        if (semesterId != null) {
            ruleVersion = indicatorRuleVersionRepository
                    .findTopBySchoolIdAndSemesterIdOrderByVersionDesc(schoolId, semesterId)
                    .orElse(null);
        }
        if (ruleVersion == null) {
            ruleVersion = indicatorRuleVersionRepository.findCurrentEffective(schoolId).orElse(null);
        }
        if (ruleVersion == null) {
            return null;
        }
        List<EvaluationIndicator> indicators = indicatorsFromSnapshot(ruleVersion.getTreeSnapshot());
        if (indicators == null) {
            indicators = evaluationIndicatorRepository.findActiveByVersion(ruleVersion.getVersion());
        }
        if (indicators == null || indicators.isEmpty()) {
            return null;
        }
        return new ResolvedRule(ruleVersion.getVersion(), indicators);
    }

    /**
     * 从指标规则版本快照 JSON 重建指标集合：递归扁平化 + 由嵌套结构还原 parentId，仅保留启用节点
     * （与 {@code findActiveByVersion} 的 status=1 口径一致）。快照为空或解析失败返回 null（调用方回退活表）。
     */
    private List<EvaluationIndicator> indicatorsFromSnapshot(String treeSnapshot) {
        if (treeSnapshot == null || treeSnapshot.isBlank()) {
            return null;
        }
        List<AdminIndicatorTreeResponse.IndicatorNode> nodes;
        try {
            nodes = objectMapper.readValue(treeSnapshot,
                    new TypeReference<List<AdminIndicatorTreeResponse.IndicatorNode>>() {});
        } catch (JsonProcessingException e) {
            log.error("指标树快照解析失败，评分回退活表: {}", e.getMessage());
            return null;
        }
        List<EvaluationIndicator> indicators = new ArrayList<>();
        flattenSnapshot(nodes, null, indicators);
        return indicators;
    }

    /** 递归扁平化快照节点为指标实体，parentId 由嵌套还原 */
    private void flattenSnapshot(List<AdminIndicatorTreeResponse.IndicatorNode> nodes,
                                 Long parentId, List<EvaluationIndicator> out) {
        if (nodes == null) {
            return;
        }
        for (AdminIndicatorTreeResponse.IndicatorNode n : nodes) {
            if (!Integer.valueOf(1).equals(n.getStatus())) {
                continue;
            }
            EvaluationIndicator e = new EvaluationIndicator();
            e.setId(n.getId());
            e.setLevel(n.getLevel());
            e.setParentId(parentId);
            e.setWeight(n.getWeight());
            e.setDimensionCode(n.getDimensionCode());
            e.setScoringRule(n.getScoringRule() != null ? n.getScoringRule().toString() : null);
            out.add(e);
            flattenSnapshot(n.getChildren(), n.getId(), out);
        }
    }

    /** 指标列表 → ID 索引 Map */
    private Map<Long, EvaluationIndicator> toIndicatorMap(List<EvaluationIndicator> indicators) {
        return indicators.stream()
                .collect(Collectors.toMap(EvaluationIndicator::getId, i -> i, (a, b) -> a));
    }

    // ==================== 单学生评分计算 ====================

    /**
     * 计算单个学生在指定学期下的评分，并替换旧批次与画像得分。
     * <p>
     * 按当前生效指标规则版本的三级指标逐条应用计分规则（AVG/SUM/MAX/WEIGHTED/THRESHOLD/COUNT），
     * 生成 score_calculations + score_calculation_details；再按一级指标（能力维度）汇总
     * 生成 portrait_evaluation_scores（score、target、change、gap）。
     *
     * @param task                重算任务（携带 school_id / semester_id / triggered_by）
     * @param userId              学生用户 ID
     * @param indicatorById       生效版本的指标树（ID → 指标）
     * @param ruleVersion         生效指标规则版本号
     * @param comparedSemesterId  对比的上一学期 ID（可为 null）
     * @param triggerType         计算触发类型（1=手动触发 2=系统自动/定时任务，写入 score_calculations.trigger_type）
     */
    /**
     * 单学生重算的事务入口（必须经 self 代理调用，@Transactional 才会生效）。
     * <p>
     * 评分重算的清理（@Modifying 原生软删）与落库（批次/明细/画像得分）都要求活动事务，
     * 否则 @Modifying 的 executeUpdate 会抛 TransactionRequiredException（"Executing an update/delete query"）。
     * 事务边界按「单个学生」划分：单个学生失败只回滚其自身写入，不影响整体任务与其他学生。
     */
    @Transactional
    public void reprocessStudent(ScoreRecalculationTask task, Long userId,
                                 Map<Long, EvaluationIndicator> indicatorById,
                                 int ruleVersion, Long comparedSemesterId, int triggerType) {
        processStudent(task, userId, indicatorById, ruleVersion, comparedSemesterId, triggerType);
    }

    private void processStudent(ScoreRecalculationTask task, Long userId,
                                Map<Long, EvaluationIndicator> indicatorById,
                                int ruleVersion, Long comparedSemesterId, int triggerType) {
        Long semesterId = task.getSemesterId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 清理该学生该学期旧数据（先软删旧批次/明细/画像得分，避免唯一键冲突）
        List<ScoreCalculation> oldCalcs = scoreCalculationRepository.findByUserIdAndSemesterId(userId, semesterId);
        List<Long> oldCalcIds = oldCalcs.stream().map(ScoreCalculation::getId).collect(Collectors.toList());
        scoreCalculationRepository.softDeleteByUserIdAndSemesterId(userId, semesterId, now);
        if (!oldCalcIds.isEmpty()) {
            scoreCalculationDetailRepository.softDeleteByCalculationIds(oldCalcIds, now);
        }
        portraitEvaluationScoreRepository.softDeleteByUserIdAndSemesterId(userId, semesterId, now);

        // 2. 收集评分数据源
        StudentScoringContext ctx = buildContext(userId, semesterId);

        // 3. 三级指标逐条计算（Pass 1：WEIGHTED 规则先按维度汇总为空计算）
        List<EvaluationIndicator> leaves = indicatorById.values().stream()
                .filter(i -> i.getLevel() != null && i.getLevel() == 3)
                .collect(Collectors.toList());
        if (leaves.isEmpty()) {
            log.debug("生效版本无三级指标，跳过 userId={}", userId);
            return;
        }
        Map<Long, LeafResult> leafResults = new LinkedHashMap<>();
        for (EvaluationIndicator leaf : leaves) {
            leafResults.put(leaf.getId(), computeIndicatorScore(leaf, ctx));
        }
        Map<String, BigDecimal> dimensionTotals = aggregateDimensionTotals(indicatorById, leafResults);
        ctx.setDimensionTotals(dimensionTotals);

        // Pass 2：依赖维度汇总的 WEIGHTED 规则重算
        boolean hasWeighted = leaves.stream().anyMatch(this::isWeightedRule);
        if (hasWeighted) {
            for (EvaluationIndicator leaf : leaves) {
                if (isWeightedRule(leaf)) {
                    leafResults.put(leaf.getId(), computeIndicatorScore(leaf, ctx));
                }
            }
            dimensionTotals = aggregateDimensionTotals(indicatorById, leafResults);
        }

        // 4. 创建新评分计算批次
        ScoreCalculation calc = new ScoreCalculation();
        calc.setUserId(userId);
        calc.setSemesterId(semesterId);
        calc.setCalculatedAt(now);
        calc.setRuleVersion(ruleVersion);
        calc.setDataSource("评分重算");
        calc.setTriggerType(triggerType);
        calc.setOperatorId(task.getTriggeredBy());
        calc.setStatus(1);      // 完成
        scoreCalculationRepository.save(calc);

        // 5. 生成计算明细
        List<ScoreCalculationDetail> details = new ArrayList<>();
        for (Map.Entry<Long, LeafResult> entry : leafResults.entrySet()) {
            EvaluationIndicator leaf = indicatorById.get(entry.getKey());
            if (leaf == null) {
                continue;
            }
            LeafResult result = entry.getValue();
            BigDecimal weight = nullToZero(leaf.getWeight());
            ScoreCalculationDetail detail = new ScoreCalculationDetail();
            detail.setCalculationId(calc.getId());
            detail.setIndicatorId(leaf.getId());
            detail.setDimensionCode(leaf.getDimensionCode());
            detail.setRawScore(result.rawScore());
            detail.setWeight(weight);
            detail.setWeightedScore(round(result.rawScore().multiply(weight)));
            detail.setSourceArchiveIds(toJsonArray(result.sourceArchiveIds()));
            details.add(detail);
        }
        if (!details.isEmpty()) {
            scoreCalculationDetailRepository.saveAll(details);
        }

        // 6. 生成画像得分（各能力维度）
        List<PortraitEvaluationScore> portraitScores = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : dimensionTotals.entrySet()) {
            String dimensionCode = entry.getKey();
            EvaluationIndicator level1 = findLevel1ByDimension(indicatorById, dimensionCode);
            BigDecimal score = round(entry.getValue());
            BigDecimal target = level1 != null
                    ? round(BD_100.multiply(nullToZero(level1.getWeight())))
                    : BD_100;
            BigDecimal previous = previousDimensionScore(userId, comparedSemesterId, dimensionCode);
            BigDecimal change = previous != null ? round(score.subtract(previous)) : ZERO;
            BigDecimal gap = round(target.subtract(score));

            PortraitEvaluationScore ps = new PortraitEvaluationScore();
            ps.setUserId(userId);
            ps.setSemesterId(semesterId);
            ps.setCalculationId(calc.getId());
            ps.setDimensionCode(dimensionCode);
            ps.setScore(score);
            ps.setTargetScore(target);
            ps.setChangeVal(change);
            ps.setGap(gap);
            ps.setComparedSemesterId(comparedSemesterId);
            ps.setRuleVersion(ruleVersion);
            ps.setEvaluatedAt(now);
            portraitScores.add(ps);
        }
        if (!portraitScores.isEmpty()) {
            portraitEvaluationScoreRepository.saveAll(portraitScores);
        }

        // 7. 同步计算数据完整度并落库（派生数据，失败不影响评分，独立事务）
        try {
            dataCompletenessService.recalculateForStudent(userId, semesterId);
        } catch (Exception e) {
            log.warn("数据完整度计算失败，不影响评分: userId={}, semesterId={}",
                    userId, semesterId, e);
        }
    }

    // ==================== 计分规则引擎 ====================

    /** 三级指标计分结果：原始分 + 贡献该分的来源档案 ID 列表 */
    private record LeafResult(BigDecimal rawScore, List<Long> sourceArchiveIds) {
    }

    /** 数据源解析结果：候选分值列表 + 贡献档案 ID 列表 */
    private record SourceValue(List<BigDecimal> values, List<Long> archiveIds) {

        static SourceValue empty() {
            return new SourceValue(List.of(), List.of());
        }

        static SourceValue of(List<BigDecimal> values, List<Long> archiveIds) {
            return new SourceValue(values != null ? values : List.of(), archiveIds != null ? archiveIds : List.of());
        }

        static SourceValue ofValue(BigDecimal value) {
            return new SourceValue(value != null ? List.of(value) : List.of(), List.of());
        }
    }

    /**
     * 应用三级指标的计分规则，返回原始分（0-100）。
     * 支持的规则类型：AVG / SUM / MAX / WEIGHTED / THRESHOLD / COUNT（《管理端接口文档》1.2）。
     */
    private LeafResult computeIndicatorScore(EvaluationIndicator leaf, StudentScoringContext ctx) {
        JsonNode rule = parseScoringRule(leaf.getScoringRule());
        if (rule == null) {
            return new LeafResult(ZERO, List.of());
        }
        String type = rule.path("type").asText("");
        String source = rule.path("source").asText("");

        BigDecimal raw = ZERO;
        List<Long> sourceIds = List.of();
        switch (type) {
            case "AVG" -> {
                SourceValue sv = resolveSource(source, ctx);
                raw = avg(sv.values());
                sourceIds = sv.archiveIds();
            }
            case "SUM" -> {
                SourceValue sv = resolveSource(source, ctx);
                raw = sumCapped(sv.values(), rule.path("max").asInt(100));
                sourceIds = sv.archiveIds();
            }
            case "MAX" -> {
                SourceValue sv = resolveSource(source, ctx);
                raw = max(sv.values());
                sourceIds = sv.archiveIds();
            }
            case "WEIGHTED" -> {
                JsonNode weights = rule.path("weights");
                BigDecimal total = ZERO;
                if (weights.isObject()) {
                    var it = weights.fields();
                    while (it.hasNext()) {
                        var entry = it.next();
                        try {
                            BigDecimal w = new BigDecimal(entry.getValue().asText());
                            BigDecimal v = ctx.getDimensionTotals().getOrDefault(entry.getKey(), ZERO);
                            total = total.add(round(v.multiply(w)));
                        } catch (NumberFormatException ignore) {
                            // 权重非法忽略该项
                        }
                    }
                }
                raw = total;
            }
            case "THRESHOLD" -> {
                SourceValue sv = resolveSource(source, ctx);
                raw = threshold(sv.values(), rule);
            }
            case "COUNT" -> {
                SourceValue sv = resolveSource(source, ctx);
                raw = countScore(sv.values(), rule);
            }
            default -> raw = ZERO;
        }
        return new LeafResult(round(raw), sourceIds);
    }

    /**
     * 解析计分规则数据源，返回候选分值列表与贡献档案 ID。
     * 数据来源：gpa_records（课程成绩）、archives（档案）、student_profiles（志愿时长）等。
     */
    private SourceValue resolveSource(String source, StudentScoringContext ctx) {
        List<Archive> archives = ctx.archives();
        switch (source) {
            case "required_course_scores": {
                List<BigDecimal> scores = ctx.gpaRecords().stream()
                        .filter(g -> isRequiredCourse(g.getCourseType()))
                        .map(GpaRecord::getScore)
                        .filter(Objects::nonNull)
                        .map(this::capHundred)
                        .collect(Collectors.toList());
                return SourceValue.of(scores, List.of());
            }
            case "course_excellent_rate": {
                List<BigDecimal> scores = ctx.gpaRecords().stream()
                        .filter(g -> isRequiredCourse(g.getCourseType()))
                        .map(GpaRecord::getScore)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (scores.isEmpty()) {
                    return SourceValue.ofValue(ZERO);
                }
                long excellent = scores.stream().filter(s -> s.compareTo(new BigDecimal("85")) >= 0).count();
                BigDecimal rate = BD_100.multiply(BigDecimal.valueOf(excellent))
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                return SourceValue.ofValue(rate);
            }
            case "certificate_level": {
                List<BigDecimal> values = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (Archive a : archives) {
                    if (isType(a, "certificate")) {
                        ArchiveCertificate cert = ctx.certificateByArchive().get(a.getId());
                        String name = cert != null ? cert.getCertificateName() : null;
                        values.add(certificateLevelScore(name != null ? name : a.getTitle()));
                        ids.add(a.getId());
                    }
                }
                return SourceValue.of(values, ids);
            }
            case "language_certificate_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> isType(a, "certificate"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "competition_scores": {
                List<BigDecimal> values = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (Archive a : archives) {
                    if (isType(a, "competition")) {
                        ArchiveCompetition comp = ctx.competitionByArchive().get(a.getId());
                        String level = comp != null ? comp.getAwardLevel() : null;
                        values.add(competitionScore(level, a.getTitle()));
                        ids.add(a.getId());
                    }
                }
                return SourceValue.of(values, ids);
            }
            case "award_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> isType(a, "competition") || isType(a, "scholarship") || isType(a, "award"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "innovation_project_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> isType(a, "innovation") || isType(a, "innovation_project")
                                || containsTitle(a, "创业") || containsTitle(a, "创新"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "research_project_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> isType(a, "research") || isType(a, "project"))
                        .filter(a -> !containsTitle(a, "论文") && !containsTitle(a, "paper"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "research_completion_rate": {
                List<Archive> research = archives.stream()
                        .filter(a -> isType(a, "research"))
                        .collect(Collectors.toList());
                if (research.isEmpty()) {
                    return SourceValue.ofValue(ZERO);
                }
                long completed = research.stream().filter(a -> a.getObtainedAt() != null).count();
                BigDecimal rate = BD_100.multiply(BigDecimal.valueOf(completed))
                        .divide(BigDecimal.valueOf(research.size()), 2, RoundingMode.HALF_UP);
                return SourceValue.ofValue(rate);
            }
            case "paper_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> containsTitle(a, "论文") || containsTitle(a, "paper")
                                || containsTitle(a, "Paper") || containsTitle(a, "SCI") || containsTitle(a, "EI"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "patent_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> containsTitle(a, "专利") || containsTitle(a, "软著")
                                || containsTitle(a, "著作权") || containsTitle(a, "patent"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "org_position_level": {
                List<BigDecimal> values = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (Archive a : archives) {
                    if (isType(a, "organization")) {
                        ArchiveOrganization org = ctx.organizationByArchive().get(a.getId());
                        String title = org != null ? org.getPositionTitle() : null;
                        values.add(orgPositionScore(title != null ? title : a.getTitle()));
                        ids.add(a.getId());
                    }
                }
                return SourceValue.of(values, ids);
            }
            case "public_welfare_count": {
                List<Archive> list = archives.stream()
                        .filter(a -> isType(a, "social_practice")
                                || containsTitle(a, "公益") || containsTitle(a, "志愿")
                                || containsTitle(a, "社区") || containsTitle(a, "实践"))
                        .collect(Collectors.toList());
                return countOf(list);
            }
            case "volunteer_hours": {
                BigDecimal hours = ctx.volunteerHours();
                return SourceValue.ofValue(hours != null ? hours : ZERO);
            }
            case "volunteer_evaluation": {
                List<BigDecimal> values = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (Archive a : archives) {
                    if (isType(a, "social_practice") || containsTitle(a, "志愿") || containsTitle(a, "公益")) {
                        values.add(new BigDecimal("90"));
                        ids.add(a.getId());
                    }
                }
                return SourceValue.of(values, ids);
            }
            case "honor_level": {
                List<BigDecimal> values = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (Archive a : archives) {
                    if (isType(a, "scholarship")) {
                        ArchiveScholarship sch = ctx.scholarshipByArchive().get(a.getId());
                        String level = sch != null ? sch.getAwardLevel() : null;
                        values.add(honorScore(level, a.getTitle()));
                        ids.add(a.getId());
                    }
                }
                return SourceValue.of(values, ids);
            }
            case "violation_count": {
                long count = archives.stream()
                        .filter(a -> containsTitle(a, "违纪") || containsTitle(a, "处分")
                                || containsTitle(a, "违规") || containsTitle(a, "记过"))
                        .count();
                return SourceValue.ofValue(BigDecimal.valueOf(count));
            }
            case "physical_test_score": {
                List<Archive> list = archives.stream()
                        .filter(a -> containsTitle(a, "体测") || containsTitle(a, "体质") || containsTitle(a, "体能"))
                        .collect(Collectors.toList());
                if (list.isEmpty()) {
                    return SourceValue.ofValue(ZERO);
                }
                return SourceValue.of(List.of(new BigDecimal("85")),
                        list.stream().map(Archive::getId).collect(Collectors.toList()));
            }
            case "mental_health_level": {
                List<Archive> list = archives.stream()
                        .filter(a -> containsTitle(a, "心理"))
                        .collect(Collectors.toList());
                if (list.isEmpty()) {
                    return SourceValue.ofValue(ZERO);
                }
                return SourceValue.of(List.of(new BigDecimal("80")),
                        list.stream().map(Archive::getId).collect(Collectors.toList()));
            }
            default:
                return SourceValue.empty();
        }
    }

    // ==================== 规则计算工具 ====================

    private boolean isWeightedRule(EvaluationIndicator leaf) {
        JsonNode rule = parseScoringRule(leaf.getScoringRule());
        return rule != null && "WEIGHTED".equals(rule.path("type").asText());
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }
        BigDecimal sum = values.stream().reduce(ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumCapped(List<BigDecimal> values, int max) {
        BigDecimal sum = (values == null ? List.<BigDecimal>of() : values).stream().reduce(ZERO, BigDecimal::add);
        BigDecimal cap = BigDecimal.valueOf(max);
        return sum.compareTo(cap) > 0 ? cap : sum;
    }

    private BigDecimal max(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }
        return values.stream().max(BigDecimal::compareTo).orElse(ZERO);
    }

    private BigDecimal threshold(List<BigDecimal> values, JsonNode rule) {
        BigDecimal value = (values == null || values.isEmpty()) ? ZERO : values.get(0);
        BigDecimal threshold = new BigDecimal(rule.path("threshold").asText("0"));
        BigDecimal score = new BigDecimal(rule.path("score").asText("100"));
        return value.compareTo(threshold) >= 0 ? score : ZERO;
    }

    private BigDecimal countScore(List<BigDecimal> values, JsonNode rule) {
        BigDecimal count = (values == null || values.isEmpty()) ? ZERO : values.get(0);
        BigDecimal perUnit = new BigDecimal(rule.path("perUnit").asText("1"));
        BigDecimal scorePerUnit = new BigDecimal(rule.path("scorePerUnit").asText("10"));
        int max = rule.path("max").asInt(100);
        if (perUnit.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        BigDecimal score = count.divide(perUnit, 0, RoundingMode.HALF_UP).multiply(scorePerUnit);
        BigDecimal cap = BigDecimal.valueOf(max);
        return score.compareTo(cap) > 0 ? cap : score;
    }

    // ==================== 数据源取值工具 ====================

    private boolean isType(Archive a, String type) {
        return a.getArchiveType() != null && a.getArchiveType().equalsIgnoreCase(type);
    }

    private boolean containsTitle(Archive a, String keyword) {
        return a.getTitle() != null && a.getTitle().contains(keyword);
    }

    private boolean isRequiredCourse(String courseType) {
        if (courseType == null) {
            return false;
        }
        String t = courseType.toLowerCase();
        return t.contains("必修") || t.contains("required");
    }

    /** 课程成绩上限 100 */
    private BigDecimal capHundred(BigDecimal v) {
        return v.compareTo(BD_100) > 0 ? BD_100 : v;
    }

    /** 竞赛等级 → 分值：国家=100 省=85 市=70 校=60 院=50 其他=40 */
    private BigDecimal competitionScore(String level, String title) {
        String text = level != null ? level : (title != null ? title : "");
        if (containsLevel(text, "国家")) return new BigDecimal("100");
        if (containsLevel(text, "省")) return new BigDecimal("85");
        if (containsLevel(text, "市")) return new BigDecimal("70");
        if (containsLevel(text, "校")) return new BigDecimal("60");
        if (containsLevel(text, "院")) return new BigDecimal("50");
        return new BigDecimal("40");
    }

    /** 证书等级 → 分值：六级=100 四级=80 雅思/托福=90 其他=60 */
    private BigDecimal certificateLevelScore(String name) {
        String text = name != null ? name : "";
        if (containsLevel(text, "六级") || containsLevel(text, "CET6") || containsLevel(text, "cet6")) {
            return new BigDecimal("100");
        }
        if (containsLevel(text, "四级") || containsLevel(text, "CET4") || containsLevel(text, "cet4")) {
            return new BigDecimal("80");
        }
        if (containsLevel(text, "雅思") || containsLevel(text, "托福")) {
            return new BigDecimal("90");
        }
        return new BigDecimal("60");
    }

    /** 组织任职等级 → 分值：主席/部长=100 副部长=85 干事=75 成员=65 其他=60 */
    private BigDecimal orgPositionScore(String title) {
        String text = title != null ? title : "";
        if (containsLevel(text, "主席") || containsLevel(text, "部长") || containsLevel(text, "主任")) {
            return new BigDecimal("100");
        }
        if (containsLevel(text, "副部长") || containsLevel(text, "副主任")) {
            return new BigDecimal("85");
        }
        if (containsLevel(text, "干事")) {
            return new BigDecimal("75");
        }
        if (containsLevel(text, "成员")) {
            return new BigDecimal("65");
        }
        return new BigDecimal("60");
    }

    /** 荣誉称号等级 → 分值：国家=100 省=85 校=70 其他=60 */
    private BigDecimal honorScore(String level, String title) {
        String text = level != null ? level : (title != null ? title : "");
        if (containsLevel(text, "国家")) return new BigDecimal("100");
        if (containsLevel(text, "省")) return new BigDecimal("85");
        if (containsLevel(text, "校")) return new BigDecimal("70");
        return new BigDecimal("60");
    }

    private boolean containsLevel(String text, String keyword) {
        return text != null && text.contains(keyword);
    }

    /** 计数量数据源 → [count] */
    private SourceValue countOf(List<Archive> list) {
        return SourceValue.of(List.of(BigDecimal.valueOf(list.size())),
                list.stream().map(Archive::getId).collect(Collectors.toList()));
    }

    // ==================== 评分上下文 ====================

    /** 单学生评分数据源上下文（汇总后供计分规则读取） */
    private static class StudentScoringContext {
        final List<GpaRecord> gpaRecords;
        final List<Archive> archives;
        final Map<Long, ArchiveCompetition> competitionByArchive;
        final Map<Long, ArchiveCertificate> certificateByArchive;
        final Map<Long, ArchiveOrganization> organizationByArchive;
        final Map<Long, ArchiveScholarship> scholarshipByArchive;
        final Map<Long, ArchiveResearch> researchByArchive;
        final Map<Long, ArchiveInnovation> innovationByArchive;
        final Map<Long, ArchiveSocialPractice> socialPracticeByArchive;
        final BigDecimal volunteerHours;
        Map<String, BigDecimal> dimensionTotals = new HashMap<>();

        StudentScoringContext(List<GpaRecord> gpaRecords, List<Archive> archives,
                              Map<Long, ArchiveCompetition> competitionByArchive,
                              Map<Long, ArchiveCertificate> certificateByArchive,
                              Map<Long, ArchiveOrganization> organizationByArchive,
                              Map<Long, ArchiveScholarship> scholarshipByArchive,
                              Map<Long, ArchiveResearch> researchByArchive,
                              Map<Long, ArchiveInnovation> innovationByArchive,
                              Map<Long, ArchiveSocialPractice> socialPracticeByArchive,
                              BigDecimal volunteerHours) {
            this.gpaRecords = gpaRecords;
            this.archives = archives;
            this.competitionByArchive = competitionByArchive;
            this.certificateByArchive = certificateByArchive;
            this.organizationByArchive = organizationByArchive;
            this.scholarshipByArchive = scholarshipByArchive;
            this.researchByArchive = researchByArchive;
            this.innovationByArchive = innovationByArchive;
            this.socialPracticeByArchive = socialPracticeByArchive;
            this.volunteerHours = volunteerHours;
        }

        List<GpaRecord> gpaRecords() {
            return gpaRecords;
        }

        List<Archive> archives() {
            return archives;
        }

        Map<Long, ArchiveCompetition> competitionByArchive() {
            return competitionByArchive;
        }

        Map<Long, ArchiveCertificate> certificateByArchive() {
            return certificateByArchive;
        }

        Map<Long, ArchiveOrganization> organizationByArchive() {
            return organizationByArchive;
        }

        Map<Long, ArchiveScholarship> scholarshipByArchive() {
            return scholarshipByArchive;
        }

        Map<Long, ArchiveResearch> researchByArchive() {
            return researchByArchive;
        }

        Map<Long, ArchiveInnovation> innovationByArchive() {
            return innovationByArchive;
        }

        Map<Long, ArchiveSocialPractice> socialPracticeByArchive() {
            return socialPracticeByArchive;
        }

        BigDecimal volunteerHours() {
            return volunteerHours;
        }

        Map<String, BigDecimal> getDimensionTotals() {
            return dimensionTotals;
        }

        void setDimensionTotals(Map<String, BigDecimal> dimensionTotals) {
            this.dimensionTotals = dimensionTotals;
        }
    }

    /** 收集单学生的评分数据源 */
    private StudentScoringContext buildContext(Long userId, Long semesterId) {
        List<GpaRecord> gpaRecords = gpaRecordRepository.findByUserIdAndSemesterIdOrderByCourseCodeAsc(userId, semesterId);

        // 已通过档案（status=2），按学期过滤（无学期归属的档案视为全学期适用）
        List<Archive> archives = archiveRepository.findByUserIdAndStatus(userId, 2);
        if (semesterId != null) {
            archives = archives.stream()
                    .filter(a -> a.getSemesterId() == null || Objects.equals(a.getSemesterId(), semesterId))
                    .collect(Collectors.toList());
        }
        List<Long> archiveIds = archives.stream().map(Archive::getId).collect(Collectors.toList());

        BigDecimal volunteerHours = studentProfileRepository.findByUserId(userId)
                .map(StudentProfile::getVolunteerHours)
                .orElse(null);

        return new StudentScoringContext(
                gpaRecords,
                archives,
                indexByArchive(archiveCompetitionRepository.findByArchiveIdIn(archiveIds), ArchiveCompetition::getArchiveId),
                indexByArchive(archiveCertificateRepository.findByArchiveIdIn(archiveIds), ArchiveCertificate::getArchiveId),
                indexByArchive(archiveOrganizationRepository.findByArchiveIdIn(archiveIds), ArchiveOrganization::getArchiveId),
                indexByArchive(archiveScholarshipRepository.findByArchiveIdIn(archiveIds), ArchiveScholarship::getArchiveId),
                indexByArchive(archiveResearchRepository.findByArchiveIdIn(archiveIds), ArchiveResearch::getArchiveId),
                indexByArchive(archiveInnovationRepository.findByArchiveIdIn(archiveIds), ArchiveInnovation::getArchiveId),
                indexByArchive(archiveSocialPracticeRepository.findByArchiveIdIn(archiveIds), ArchiveSocialPractice::getArchiveId),
                volunteerHours);
    }

    private <T> Map<Long, T> indexByArchive(List<T> list, Function<T, Long> idExtractor) {
        return list.stream().collect(Collectors.toMap(idExtractor, t -> t, (a, b) -> a));
    }

    // ==================== 汇总与辅助 ====================

    /** 按三级指标结果汇总出各能力维度总分（一级指标维度） */
    private Map<String, BigDecimal> aggregateDimensionTotals(Map<Long, EvaluationIndicator> indicatorById,
                                                             Map<Long, LeafResult> leafResults) {
        Map<Long, BigDecimal> level2Totals = new HashMap<>();
        for (Map.Entry<Long, LeafResult> entry : leafResults.entrySet()) {
            EvaluationIndicator leaf = indicatorById.get(entry.getKey());
            if (leaf == null || leaf.getParentId() == null) {
                continue;
            }
            BigDecimal weighted = round(entry.getValue().rawScore().multiply(nullToZero(leaf.getWeight())));
            level2Totals.merge(leaf.getParentId(), weighted, BigDecimal::add);
        }
        Map<String, BigDecimal> dimensionTotals = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : level2Totals.entrySet()) {
            EvaluationIndicator level2 = indicatorById.get(entry.getKey());
            if (level2 == null || level2.getParentId() == null) {
                continue;
            }
            EvaluationIndicator level1 = indicatorById.get(level2.getParentId());
            if (level1 == null || level1.getDimensionCode() == null) {
                continue;
            }
            dimensionTotals.merge(level1.getDimensionCode(), entry.getValue(), BigDecimal::add);
        }
        return dimensionTotals;
    }

    private EvaluationIndicator findLevel1ByDimension(Map<Long, EvaluationIndicator> indicatorById, String dimensionCode) {
        return indicatorById.values().stream()
                .filter(i -> i.getLevel() != null && i.getLevel() == 1
                        && Objects.equals(i.getDimensionCode(), dimensionCode))
                .findFirst()
                .orElse(null);
    }

    /** 对比学期的上一学期（按 start_date 取当前学期之前最近的一条） */
    private Long resolvePreviousSemesterId(Long schoolId, Long semesterId) {
        Semester current = semesterRepository.findById(semesterId).orElse(null);
        if (current == null || current.getStartDate() == null) {
            return null;
        }
        return semesterRepository.findActiveBySchoolId(schoolId).stream()
                .filter(s -> s.getStartDate() != null && s.getStartDate().isBefore(current.getStartDate()))
                .max(Comparator.comparing(Semester::getStartDate))
                .map(Semester::getId)
                .orElse(null);
    }

    /** 上一学期该维度的画像得分（用于 change 计算），无则返回 null */
    private BigDecimal previousDimensionScore(Long userId, Long comparedSemesterId, String dimensionCode) {
        if (comparedSemesterId == null) {
            return null;
        }
        return portraitEvaluationScoreRepository.findByUserIdAndSemesterId(userId, comparedSemesterId).stream()
                .filter(p -> Objects.equals(p.getDimensionCode(), dimensionCode))
                .findFirst()
                .map(PortraitEvaluationScore::getScore)
                .orElse(null);
    }

    /** 解析目标学生 ID 列表 */
    private List<Long> resolveStudentIds(ScoreRecalculationTask task) {
        Integer type = task.getTaskType();
        switch (type) {
            case TARGET_STUDENT:
                return studentProfileRepository.findByUserId(task.getTargetId())
                        .map(sp -> List.of(sp.getUserId()))
                        .orElse(List.of());
            case TARGET_CLASS:
                return studentProfileRepository.findByClassId(task.getTargetId()).stream()
                        .map(StudentProfile::getUserId)
                        .collect(Collectors.toList());
            case TARGET_MAJOR:
                List<Long> classIds = clazzRepository.findByMajorId(task.getTargetId()).stream()
                        .map(Clazz::getId)
                        .collect(Collectors.toList());
                if (classIds.isEmpty()) {
                    return List.of();
                }
                return studentProfileRepository.findByClassIdIn(classIds).stream()
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
            case TARGET_SEMESTER:
            case TARGET_FULL:
            default:
                return studentProfileRepository.findBySchoolId(task.getSchoolId()).stream()
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
        }
    }

    /** 重算范围参数校验 */
    private void validateTarget(ScoreRecalculateRequest request) {
        Integer type = request.getTargetType();
        if (type == TARGET_STUDENT || type == TARGET_CLASS || type == TARGET_MAJOR) {
            if (request.getTargetId() == null) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "targetId 不能为空");
            }
        }
        if (type == TARGET_STUDENT) {
            studentProfileRepository.findByUserId(request.getTargetId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学生不存在"));
        } else if (type == TARGET_CLASS) {
            clazzRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "班级不存在"));
        } else if (type == TARGET_MAJOR) {
            majorRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "专业不存在"));
        }
    }

    /** 同一范围已有生效中任务校验（0=待执行 1=执行中），返回 41005 */
    private void checkNoRunningTask(Long schoolId, ScoreRecalculateRequest request) {
        List<Integer> activeStatuses = List.of(STATUS_QUEUED, STATUS_RUNNING);
        List<ScoreRecalculationTask> running;
        Integer type = request.getTargetType();
        if (type == TARGET_FULL) {
            running = taskRepository.findBySchoolIdAndTaskTypeAndStatusIn(schoolId, TARGET_FULL, activeStatuses);
        } else if (type == TARGET_SEMESTER) {
            running = taskRepository.findBySchoolIdAndTaskTypeAndSemesterIdAndStatusIn(
                    schoolId, TARGET_SEMESTER, request.getSemesterId(), activeStatuses);
        } else {
            running = taskRepository.findBySchoolIdAndTaskTypeAndTargetIdAndStatusIn(
                    schoolId, type, request.getTargetId(), activeStatuses);
        }
        if (!running.isEmpty()) {
            throw new BusinessException(ResultCode.INDICATOR_RECALC_TASK_RUNNING, "同一范围已有评分重算任务在执行");
        }
    }

    /** 置任务失败并记录错误信息 */
    private void failTask(ScoreRecalculationTask task, String message) {
        try {
            task.setStatus(STATUS_FAILED);
            task.setErrorMessage(message);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        } catch (Exception e) {
            log.error("更新评分重算任务失败状态出错 taskId={}", task.getId(), e);
        }
    }

    // ==================== 启动恢复 ====================

    /**
     * 启动恢复被中断的评分重算任务（由 {@link com.example.studentarchives.config.ScoreRecalculationTaskRecoveryRunner}
     * 在应用启动完成时调用）。
     * <p>
     * 应用重启/崩溃会让仍处于 0=排队中 / 1=执行中的任务永久卡死，并占用同范围触发权
     * （41005 冲突校验）。本方法把这些任务统一标记为 3=失败并记录中断原因与完成时间，
     * 管理员可在看到失败后手动重新触发。幂等：@Modifying 的 WHERE status IN (0,1) 在执行时
     * 重估，不会误伤本会话中新创建或已完成的任务。
     */
    @Transactional
    public void recoverInterruptedTasks() {
        List<Integer> interrupted = List.of(STATUS_QUEUED, STATUS_RUNNING);
        List<ScoreRecalculationTask> stuckTasks = taskRepository.findByStatusIn(interrupted);
        if (stuckTasks.isEmpty()) {
            log.info("启动恢复：无被中断的评分重算任务");
            return;
        }
        for (ScoreRecalculationTask task : stuckTasks) {
            log.warn("启动恢复：标记被中断的评分重算任务为失败 taskId={}, schoolId={}, taskType={}, targetId={}, semesterId={}, status={}",
                    task.getId(), task.getSchoolId(), task.getTaskType(), task.getTargetId(),
                    task.getSemesterId(), task.getStatus());
        }
        int updated = taskRepository.markInterruptedAsFailed(interrupted, STATUS_FAILED,
                "应用重启，评分重算任务被中断，已标记为失败", LocalDateTime.now());
        log.warn("启动恢复：已将 {} 个被中断的评分重算任务标记为失败", updated);
    }

    private ScoreRecalculationTaskResponse toTaskResponse(ScoreRecalculationTask task) {
        return ScoreRecalculationTaskResponse.builder()
                .id(task.getId())
                .targetType(task.getTaskType())
                .targetId(task.getTargetId())
                .semesterId(task.getSemesterId())
                .status(task.getStatus())
                .statusLabel(statusLabel(task.getStatus()))
                .progress(task.getProgress())
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failCount(task.getFailCount())
                .startedAt(toIso(task.getStartedAt()))
                .completedAt(toIso(task.getCompletedAt()))
                .errorMessage(task.getErrorMessage())
                .failures(parseFailures(task.getErrorMessage()))
                .message(taskMessage(task))
                .build();
    }

    /** 失败明细序列化为 JSON 数组写入 error_message；无失败返回 null（保持与旧行为一致） */
    private String serializeFailures(List<StudentFailure> failures) {
        if (failures == null || failures.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(failures);
        } catch (JsonProcessingException e) {
            log.error("序列化评分重算失败明细失败，回退为文本摘要", e);
            return failures.stream()
                    .map(f -> "学生 " + f.userId() + ": " + f.message())
                    .collect(Collectors.joining("; "));
        }
    }

    /** 从 error_message 解析失败明细列表；非 JSON（任务级失败原因）或为空时返回 null */
    private List<ScoreRecalculationTaskResponse.StudentFailureItem> parseFailures(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(errorMessage,
                    new TypeReference<List<ScoreRecalculationTaskResponse.StudentFailureItem>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** 提取异常原因文本（空消息回退为异常类型名，便于排查） */
    private String errorText(Exception e) {
        String msg = e.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }

    // ==================== 通用工具 ====================

    /** 任务状态提示消息（供 data.message 展示）：排队/执行中给状态文案，完成给结果摘要，失败给失败原因 */
    private String taskMessage(ScoreRecalculationTask task) {
        Integer status = task.getStatus();
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_QUEUED -> "任务排队中";
            case STATUS_RUNNING -> "任务执行中";
            case STATUS_DONE ->
                    "评分重算完成（成功 " + task.getSuccessCount() + " 条，失败 " + task.getFailCount() + " 条）";
            case STATUS_FAILED -> task.getErrorMessage() != null ? task.getErrorMessage() : "评分重算失败";
            default -> "";
        };
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_QUEUED -> "排队中";
            case STATUS_RUNNING -> "执行中";
            case STATUS_DONE -> "完成";
            case STATUS_FAILED -> "失败";
            default -> "";
        };
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private JsonNode parseScoringRule(String scoringRule) {
        if (scoringRule == null || scoringRule.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(scoringRule);
        } catch (Exception e) {
            log.warn("scoring_rule 解析失败: {}", scoringRule, e);
            return null;
        }
    }

    private String toJsonArray(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
