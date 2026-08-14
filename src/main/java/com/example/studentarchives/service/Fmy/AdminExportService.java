package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.export.request.ResearchExportRequest;
import com.example.studentarchives.dto.Fmy.export.response.ExportJobResponse;
import com.example.studentarchives.dto.Fmy.export.response.ResearchExportResponse;
import com.example.studentarchives.entity.ai.AiConversation;
import com.example.studentarchives.entity.ai.AiMessage;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.export.AnonymizationMap;
import com.example.studentarchives.entity.export.ExportJob;
import com.example.studentarchives.entity.export.ExportOperationLog;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.ExportTaskStatusEnum;
import com.example.studentarchives.enums.FileStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AiConversationRepository;
import com.example.studentarchives.repository.AiMessageRepository;
import com.example.studentarchives.repository.AnonymizationMapRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.ExportJobRepository;
import com.example.studentarchives.repository.ExportOperationLogRepository;
import com.example.studentarchives.repository.IndicatorRuleVersionRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端数据导出服务
 * <p>
 * 对应《管理端接口文档》五、数据导出模块：
 * <ul>
 *   <li>5.1 POST /admin/exports/research：研究数据导出。任务进入 export_jobs 异步执行，
 *       接口立即返回任务 ID；导出文件为 JSON，自动用匿名编号替代姓名与学号，
 *       创建/下载/删除写入 export_operation_logs 审计（action=1 创建）。</li>
 *   <li>5.2 GET /admin/exports/{jobId}：查询导出任务进度及下载链接。</li>
 * </ul>
 * 5.9（一键导出学生档案）在文档中标注「规划中（本版本未实现，接口契约保留）」，本模块不实现。
 * <p>
 * 异步执行经 self 自引用代理调用（@Lazy 避免循环依赖），与 {@link AdminScoreService} 保持一致；
 * 线程池使用 Spring 默认异步执行器（不新增 Bean，避免改动既有 {@code AsyncConfig}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExportService {

    /** 研究数据导出权限码（《管理端接口文档》关键权限码） */
    private static final String PERMISSION_RESEARCH = "export:research";

    /** 导出任务查询/下载权限码（《管理端接口文档》关键权限码） */
    private static final String PERMISSION_EXPORT = "export:manage";

    /** 导出类型：研究数据导出 */
    private static final String EXPORT_TYPE_RESEARCH = "archive_research";

    /** 研究数据导出支持的数据类型 */
    private static final Set<String> ALLOWED_DATA_TYPES = Set.of("archives", "scores", "audits", "ai", "career");

    /** 研究数据导出支持的范围类型（1=学校 2=学院 3=专业 4=班级 6=年级） */
    private static final Set<Integer> ALLOWED_SCOPE_TYPES = Set.of(1, 2, 3, 4, 6);

    private static final int SCOPE_SCHOOL = 1;
    private static final int SCOPE_COLLEGE = 2;
    private static final int SCOPE_MAJOR = 3;
    private static final int SCOPE_CLASS = 4;
    private static final int SCOPE_GRADE = 6;

    /** 导出文件业务类型（file_uploads.biz_type）与文件分类 */
    private static final String FILE_BIZ_TYPE = "research_export";
    private static final String FILE_CATEGORY = "json";

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AdminAuthService adminAuthService;
    private final ExportJobRepository exportJobRepository;
    private final ExportOperationLogRepository exportOperationLogRepository;
    private final AnonymizationMapRepository anonymizationMapRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterRepository semesterRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final ArchiveRepository archiveRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final AiConversationRepository aiConversationRepository;
    private final AiMessageRepository aiMessageRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final IndicatorRuleVersionRepository indicatorRuleVersionRepository;
    private final OssFileService ossFileService;
    private final ObjectMapper objectMapper;

    /** 自引用代理（@Lazy 避免循环依赖），用于提交后触发 @Async 异步执行 */
    @Lazy
    @Autowired
    private AdminExportService self;

    // ==================== 5.1 研究数据导出 ====================

    /**
     * 提交研究数据导出（POST /admin/exports/research，文档 5.1）
     * <p>
     * 校验操作人具备 {@code export:research} 权限 → 校验导出参数（学期归属本校、范围类型、
     * 数据类型、年级/范围联动）→ 创建 export_jobs 任务 → 提交异步执行 → 立即返回任务 ID。
     *
     * @param userId  当前登录用户 ID
     * @param request 导出请求
     * @return 任务 ID 与初始状态（status=0 待执行，预计耗时 30s）
     */
    public ResearchExportResponse submitResearchExport(Long userId, ResearchExportRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION_RESEARCH);

        User operator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        Long schoolId = operator.getSchoolId();

        // 学期校验：存在且属于当前学校
        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));
        if (!Objects.equals(semester.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学期不属于当前学校");
        }

        // 参数校验
        validateResearchRequest(request, schoolId);

        // 创建导出任务
        LocalDateTime now = LocalDateTime.now();
        ExportJob job = new ExportJob();
        job.setSchoolId(schoolId);
        job.setOperatorId(userId);
        job.setExportType(EXPORT_TYPE_RESEARCH);
        job.setScopeType(request.getScopeType());
        job.setScopeId(request.getScopeType() == SCOPE_SCHOOL || request.getScopeType() == SCOPE_GRADE
                ? null : request.getScopeId());
        job.setGrade(request.getGrade());
        job.setStatus(ExportTaskStatusEnum.PENDING.getValue());
        job.setTotalCount(0);
        job.setSuccessCount(0);
        job.setExpireAt(now.plusDays(7));
        job.setFilterConditions(writeJson(request));
        job = exportJobRepository.save(job);

        // 提交异步执行（@Async 走代理，接口立即返回；submitResearchExport 不加 @Transactional，
        // 依赖 JpaRepository 自动提交，异步线程即可读到刚保存的任务）
        self.executeResearchExport(job.getId());

        return ResearchExportResponse.builder()
                .jobId(job.getId())
                .status(ExportTaskStatusEnum.PENDING.getValue())
                .estimatedSeconds(30)
                .build();
    }

    // ==================== 5.2 查询导出任务 ====================

    /**
     * 查询导出任务进度及下载链接（GET /admin/exports/{jobId}，文档 5.2）
     *
     * @param userId 当前登录用户 ID
     * @param jobId  导出任务 ID
     * @return 任务状态、进度与下载链接
     */
    public ExportJobResponse getExportJob(Long userId, Long jobId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION_EXPORT);
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "导出任务不存在"));
        return toJobResponse(job);
    }

    // ==================== 异步执行 ====================

    /**
     * 异步执行研究数据导出任务（由 submitResearchExport 经 @Async 代理提交）。
     * <p>
     * 流程：任务置为执行中 → 解析目标学生 ID 列表 → 确保匿名化映射（稳定复用编号）→
     * 逐学生装配所选数据类型数据 → 生成研究数据 JSON → 上传 OSS 并绑定 file_uploads →
     * 更新任务完成 → 写 export_operation_logs 审计（action=1 创建）。任一环节失败置任务失败。
     *
     * @param jobId 导出任务 ID
     */
    @Async
    public void executeResearchExport(Long jobId) {
        // 兼容异步线程先于任务落库提交的时序：小幅度重试读取，仍取不到则直接放弃
        ExportJob job = findJobWithRetry(jobId);
        if (job == null) {
            log.warn("研究数据导出任务不存在，跳过执行: jobId={}", jobId);
            return;
        }
        Long schoolId = job.getSchoolId();
        try {
            job.setStatus(ExportTaskStatusEnum.RUNNING.getValue());
            job.setStartedAt(LocalDateTime.now());
            exportJobRepository.save(job);

            ResearchExportRequest request = parseFilterConditions(job.getFilterConditions());
            if (request == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "导出筛选条件解析失败");
            }

            // 1. 解析目标学生
            List<Long> studentIds = resolveStudentIds(schoolId, request);
            if (studentIds.isEmpty()) {
                throw new BusinessException(ResultCode.DATA_NOT_EXIST, "目标范围内没有可导出的学生");
            }
            job.setTotalCount(studentIds.size());
            exportJobRepository.save(job);

            // 2. 确保匿名化映射（同一学生多次导出复用同一编号，按学校顺序递增）
            boolean isAnonymized = request.getIsAnonymized() == null || request.getIsAnonymized();
            Map<Long, String> anonymousCodeByUser = ensureAnonymizationMaps(schoolId, studentIds);

            // 3. 装配各学生数据
            Map<Long, User> userById = userRepository.findByIdIn(studentIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
            List<Map<String, Object>> students = new ArrayList<>();
            for (Long studentId : studentIds) {
                Map<String, Object> record = buildStudentRecord(studentId, request, isAnonymized);
                User u = userById.get(studentId);
                if (isAnonymized) {
                    record.put("anonymousCode", anonymousCodeByUser.get(studentId));
                } else {
                    record.put("name", u != null ? u.getName() : null);
                    record.put("userNo", u != null ? u.getUserNo() : null);
                }
                students.add(record);
            }

            // 4. 生成研究数据 JSON
            Integer dataVersion = resolveDataVersion(schoolId);
            byte[] jsonBytes = buildResearchJson(request, schoolId, dataVersion,
                    studentIds.size(), isAnonymized, students);
            String originalName = "研究数据导出-" + java.time.LocalDate.now().format(FILE_DATE_FORMAT) + ".json";

            // 5. 上传 OSS 并绑定 file_uploads
            String objectKey = ossFileService.uploadBytes(jsonBytes, "application/json",
                    FILE_BIZ_TYPE, FILE_CATEGORY, originalName);
            AttachmentRelation relation = bindFile(job.getId(), job.getOperatorId(),
                    objectKey, originalName, jsonBytes.length, job.getExpireAt());

            // 6. 更新任务完成
            job.setStatus(ExportTaskStatusEnum.COMPLETED.getValue());
            job.setSuccessCount(studentIds.size());
            job.setFileId(relation.getId());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);

            // 7. 导出操作日志（action=1 创建）
            writeOperationLog(job, request, studentIds.size(), isAnonymized, dataVersion,
                    relation.getId());
            log.info("研究数据导出任务完成 jobId={}, students={}, version={}", jobId, studentIds.size(), dataVersion);
        } catch (Exception e) {
            log.error("研究数据导出任务执行失败 jobId={}", jobId, e);
            failJob(job, e);
        }
    }

    // ==================== 参数校验 ====================

    /** 校验研究数据导出请求参数（范围、数据类型、年级联动） */
    private void validateResearchRequest(ResearchExportRequest request, Long schoolId) {
        Integer scopeType = request.getScopeType();
        if (scopeType == null || !ALLOWED_SCOPE_TYPES.contains(scopeType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "范围类型仅支持：1=学校 2=学院 3=专业 4=班级 6=年级");
        }
        if ((scopeType == SCOPE_COLLEGE || scopeType == SCOPE_MAJOR || scopeType == SCOPE_CLASS)
                && request.getScopeId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "scopeId 不能为空");
        }
        if (scopeType == SCOPE_GRADE && (request.getGrade() == null || request.getGrade().isBlank())) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "scopeType=6(年级) 导出时 grade 必填");
        }
        List<String> dataTypes = request.getDataTypes();
        if (dataTypes == null || dataTypes.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "dataTypes 不能为空");
        }
        List<String> invalid = dataTypes.stream()
                .filter(t -> !ALLOWED_DATA_TYPES.contains(t))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "数据类型不支持：" + String.join("、", invalid) + "，支持：archives/scores/audits/ai/career");
        }
        // 范围归属校验：范围对象必须属于当前学校
        validateScopeBelongsToSchool(scopeType, request.getScopeId(), schoolId);
    }

    /** 校验范围对象归属当前学校（年级范围无独立对象，跳过） */
    private void validateScopeBelongsToSchool(Integer scopeType, Long scopeId, Long schoolId) {
        switch (scopeType) {
            case SCOPE_COLLEGE -> {
                College college = collegeRepository.findById(scopeId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学院不存在"));
                if (college.getSchoolId() != null && !Objects.equals(college.getSchoolId(), schoolId)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "学院不属于当前学校");
                }
            }
            case SCOPE_MAJOR -> {
                Major major = majorRepository.findById(scopeId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "专业不存在"));
                College college = collegeRepository.findById(major.getCollegeId()).orElse(null);
                if (college != null && college.getSchoolId() != null
                        && !Objects.equals(college.getSchoolId(), schoolId)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "专业不属于当前学校");
                }
            }
            case SCOPE_CLASS -> {
                Clazz clazz = clazzRepository.findById(scopeId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "班级不存在"));
                Major major = clazz.getMajorId() != null ? majorRepository.findById(clazz.getMajorId()).orElse(null) : null;
                College college = major != null ? collegeRepository.findById(major.getCollegeId()).orElse(null) : null;
                if (college != null && college.getSchoolId() != null
                        && !Objects.equals(college.getSchoolId(), schoolId)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "班级不属于当前学校");
                }
            }
            default -> {
                // 学校 / 年级范围无独立范围对象，无需归属校验
            }
        }
    }

    // ==================== 目标学生解析 ====================

    /**
     * 解析导出范围内的学生用户 ID 列表。
     * <p>
     * 1=学校：全校学生；2=学院：该学院下全部专业→班级→学生；
     * 3=专业：该专业下全部班级→学生；4=班级：该班级学生；
     * 6=年级：按 classes.grade 匹配全校班级→学生。
     */
    private List<Long> resolveStudentIds(Long schoolId, ResearchExportRequest request) {
        Integer scopeType = request.getScopeType();
        switch (scopeType) {
            case SCOPE_SCHOOL:
                return studentProfileRepository.findBySchoolId(schoolId).stream()
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
            case SCOPE_COLLEGE: {
                List<Long> majorIds = majorRepository.findAll().stream()
                        .filter(m -> Objects.equals(m.getCollegeId(), request.getScopeId()))
                        .map(Major::getId)
                        .collect(Collectors.toList());
                List<Long> classIds = clazzRepository.findAll().stream()
                        .filter(c -> c.getMajorId() != null && majorIds.contains(c.getMajorId()))
                        .map(Clazz::getId)
                        .collect(Collectors.toList());
                return studentProfileRepository.findByClassIdIn(classIds).stream()
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
            }
            case SCOPE_MAJOR: {
                List<Long> classIds = clazzRepository.findByMajorId(request.getScopeId()).stream()
                        .map(Clazz::getId)
                        .collect(Collectors.toList());
                return studentProfileRepository.findByClassIdIn(classIds).stream()
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
            }
            case SCOPE_CLASS:
                return studentProfileRepository.findByClassId(request.getScopeId()).stream()
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
            case SCOPE_GRADE:
            default: {
                // 年级：全校学生按班级的 grade 匹配
                List<StudentProfile> profiles = studentProfileRepository.findBySchoolId(schoolId);
                Map<Long, Clazz> clazzById = clazzRepository.findAllById(
                                profiles.stream().map(StudentProfile::getClassId).collect(Collectors.toList()))
                        .stream().collect(Collectors.toMap(Clazz::getId, Function.identity(), (a, b) -> a));
                return profiles.stream()
                        .filter(p -> {
                            Clazz clazz = clazzById.get(p.getClassId());
                            return clazz != null && Objects.equals(clazz.getGrade(), request.getGrade());
                        })
                        .map(StudentProfile::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
            }
        }
    }

    // ==================== 匿名化映射 ====================

    /**
     * 确保目标学生的匿名化映射存在，返回 userId → anonymousCode 索引。
     * <p>
     * 编号规则：{@code AN} + 6 位数字，按学校递增；已存在的学生复用原编号（同一学生多次导出一致）。
     */
    private Map<Long, String> ensureAnonymizationMaps(Long schoolId, List<Long> studentIds) {
        Map<Long, String> codeByUser = new HashMap<>();
        int maxSeq = 0;
        for (AnonymizationMap m : anonymizationMapRepository.findBySchoolId(schoolId)) {
            codeByUser.put(m.getUserId(), m.getAnonymousCode());
            maxSeq = Math.max(maxSeq, parseAnonymSeq(m.getAnonymousCode()));
        }
        List<AnonymizationMap> newMaps = new ArrayList<>();
        int seq = maxSeq;
        for (Long studentId : studentIds) {
            if (codeByUser.containsKey(studentId)) {
                continue;
            }
            seq++;
            String code = "AN" + String.format("%06d", seq);
            AnonymizationMap m = new AnonymizationMap();
            m.setSchoolId(schoolId);
            m.setUserId(studentId);
            m.setAnonymousCode(code);
            newMaps.add(m);
            codeByUser.put(studentId, code);
        }
        if (!newMaps.isEmpty()) {
            anonymizationMapRepository.saveAll(newMaps);
        }
        return codeByUser;
    }

    /** 解析匿名编号数字后缀（"AN000012" → 12），非法返回 0 */
    private int parseAnonymSeq(String code) {
        if (code == null || !code.startsWith("AN")) {
            return 0;
        }
        try {
            return Integer.parseInt(code.substring(2));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== 学生数据装配 ====================

    /** 装配单个学生所选数据类型的数据记录 */
    private Map<String, Object> buildStudentRecord(Long userId, ResearchExportRequest request, boolean isAnonymized) {
        Set<String> typeSet = new HashSet<>(request.getDataTypes());
        Map<String, Object> record = new LinkedHashMap<>();
        if (typeSet.contains("archives")) {
            List<Map<String, Object>> items = archiveRepository.findByUserId(userId).stream()
                    .map(this::toArchiveItem)
                    .collect(Collectors.toList());
            record.put("archives", filterItems(request, "archives", items));
        }
        if (typeSet.contains("audits")) {
            List<Map<String, Object>> items = archiveRepository.findByUserId(userId).stream()
                    .map(this::toAuditItem)
                    .collect(Collectors.toList());
            record.put("audits", filterItems(request, "audits", items));
        }
        if (typeSet.contains("scores")) {
            List<Map<String, Object>> items = portraitEvaluationScoreRepository
                    .findByUserIdAndSemesterId(userId, request.getSemesterId()).stream()
                    .map(this::toScoreItem)
                    .collect(Collectors.toList());
            record.put("scores", filterItems(request, "scores", items));
        }
        if (typeSet.contains("ai")) {
            List<Map<String, Object>> items = buildAiItems(userId);
            record.put("ai", filterItems(request, "ai", items));
        }
        if (typeSet.contains("career")) {
            List<Map<String, Object>> items = careerPlanRepository.findByUserId(userId).stream()
                    .map(this::toCareerItem)
                    .collect(Collectors.toList());
            record.put("career", filterItems(request, "career", items));
        }
        return record;
    }

    private Map<String, Object> toArchiveItem(Archive a) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", a.getId());
        item.put("archiveType", a.getArchiveType());
        item.put("title", a.getTitle());
        item.put("semesterId", a.getSemesterId());
        item.put("obtainedAt", a.getObtainedAt() != null ? String.valueOf(a.getObtainedAt()) : null);
        item.put("status", a.getStatus());
        return item;
    }

    private Map<String, Object> toAuditItem(Archive a) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", a.getId());
        item.put("archiveType", a.getArchiveType());
        item.put("title", a.getTitle());
        item.put("status", a.getStatus());
        item.put("submittedAt", toIso(a.getAuditInfo().getSubmittedAt()));
        item.put("auditedAt", toIso(a.getAuditInfo().getAuditedAt()));
        item.put("passedAt", toIso(a.getAuditInfo().getPassedAt()));
        item.put("returnedAt", toIso(a.getAuditInfo().getReturnedAt()));
        item.put("revokedAt", toIso(a.getAuditInfo().getRevokedAt()));
        item.put("rejectedReason", a.getAuditInfo().getRejectedReason());
        return item;
    }

    private Map<String, Object> toScoreItem(PortraitEvaluationScore s) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dimensionCode", s.getDimensionCode());
        item.put("score", s.getScore());
        item.put("targetScore", s.getTargetScore());
        item.put("change", s.getChangeVal());
        item.put("gap", s.getGap());
        item.put("ruleVersion", s.getRuleVersion());
        item.put("evaluatedAt", toIso(s.getEvaluatedAt()));
        return item;
    }

    /** 装配 AI 对话数据：用户正常状态（status=1）下最近 200 个会话及其消息时间线 */
    private List<Map<String, Object>> buildAiItems(Long userId) {
        List<AiConversation> conversations = aiConversationRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(userId, 1, PageRequest.of(0, 200))
                .getContent();
        return conversations.stream().map(c -> {
            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("conversationId", c.getId());
            conv.put("title", c.getTitle());
            List<Map<String, Object>> messages = aiMessageRepository
                    .findByConversationIdOrderByCreatedAtAsc(c.getId()).stream()
                    .map(this::toAiMessageItem)
                    .collect(Collectors.toList());
            conv.put("messages", messages);
            return conv;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toAiMessageItem(AiMessage m) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", m.getRole());
        item.put("content", m.getContent());
        item.put("modelName", m.getModelName());
        item.put("createdAt", toIso(m.getCreatedAt()));
        return item;
    }

    private Map<String, Object> toCareerItem(CareerPlan p) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", p.getId());
        item.put("title", p.getTitle());
        item.put("content", p.getContent());
        item.put("semesterId", p.getSemesterId());
        item.put("progressRate", p.getProgressRate());
        item.put("status", p.getStatus());
        item.put("updatedAt", toIso(p.getUpdatedAt()));
        return item;
    }

    /** 按请求 fields 映射过滤单条记录：fields[dataType] 为字段名列表时仅保留列表内字段 */
    private List<Map<String, Object>> filterItems(ResearchExportRequest request, String dataType,
                                                  List<Map<String, Object>> items) {
        Object raw = request.getFields() != null ? request.getFields().get(dataType) : null;
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return items;
        }
        Set<String> allowed = list.stream().map(String::valueOf).collect(Collectors.toSet());
        return items.stream().map(item -> {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : item.entrySet()) {
                if (allowed.contains(e.getKey())) {
                    filtered.put(e.getKey(), e.getValue());
                }
            }
            return filtered;
        }).collect(Collectors.toList());
    }

    // ==================== 研究数据 JSON 生成 ====================

    /**
     * 生成研究数据导出 JSON 文档。
     * <p>
     * includeMetadata=true 时携带 meta（导出类型、数据版本、字段说明、记录数、匿名化标识、生成时间）；
     * 无论是否匿名化，学生的姓名/学号均不进入数据主体（匿名化时以 anonymousCode 标识）。
     */
    private byte[] buildResearchJson(ResearchExportRequest request, Long schoolId, Integer dataVersion,
                                     int recordCount, boolean isAnonymized, List<Map<String, Object>> students) {
        Map<String, Object> doc = new LinkedHashMap<>();
        if (request.getIncludeMetadata() == null || request.getIncludeMetadata()) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("exportType", EXPORT_TYPE_RESEARCH);
            meta.put("schoolId", schoolId);
            meta.put("semesterId", request.getSemesterId());
            meta.put("scopeType", request.getScopeType());
            meta.put("scopeId", request.getScopeType() == SCOPE_SCHOOL || request.getScopeType() == SCOPE_GRADE
                    ? null : request.getScopeId());
            meta.put("grade", request.getGrade());
            meta.put("dataVersion", dataVersion);
            meta.put("recordCount", recordCount);
            meta.put("isAnonymized", isAnonymized);
            meta.put("generatedAt", toIso(LocalDateTime.now()));
            meta.put("fieldDescription", buildFieldDescription(request.getDataTypes()));
            doc.put("meta", meta);
        }
        doc.put("students", students);
        try {
            return objectMapper.writeValueAsBytes(doc);
        } catch (JsonProcessingException e) {
            log.error("研究数据导出 JSON 序列化失败", e);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "研究数据生成失败");
        }
    }

    /** 字段说明（includeMetadata=true 时写入 meta，供研究使用者理解字段口径） */
    private Map<String, Object> buildFieldDescription(List<String> dataTypes) {
        Set<String> typeSet = new HashSet<>(dataTypes);
        Map<String, Object> desc = new LinkedHashMap<>();
        desc.put("anonymousCode", "匿名编号（研究数据导出自动替代姓名与学号，同一学生多次导出一致）");
        if (typeSet.contains("archives")) {
            desc.put("archives", "档案记录：archiveType 类型 / title 标题 / semesterId 学期 / obtainedAt 获得时间 / status 状态(0草稿 1待审批 2通过 3退回 4撤销)");
        }
        if (typeSet.contains("audits")) {
            desc.put("audits", "档案审核：submittedAt 提交 / auditedAt 审核 / passedAt 通过 / returnedAt 退回 / revokedAt 撤销 / rejectedReason 驳回原因");
        }
        if (typeSet.contains("scores")) {
            desc.put("scores", "画像得分：dimensionCode 维度 / score 得分 / targetScore 目标 / change 变化 / gap 差距 / ruleVersion 规则版本");
        }
        if (typeSet.contains("ai")) {
            desc.put("ai", "AI 对话：conversationId 会话 / title 标题 / messages 消息(role 角色, content 内容, modelName 模型, createdAt 时间)");
        }
        if (typeSet.contains("career")) {
            desc.put("career", "成长规划：title 标题 / content 内容 / progressRate 进度 / status 状态");
        }
        return desc;
    }

    // ==================== 数据版本解析 ====================

    /** 当前生效的指标规则版本号（研究数据 meta 与审计 data_version 使用），无生效版本返回 null */
    private Integer resolveDataVersion(Long schoolId) {
        return indicatorRuleVersionRepository.findCurrentEffective(schoolId)
                .map(v -> (Integer) v.getVersion())
                .orElse(null);
    }

    // ==================== 文件绑定与审计 ====================

    /** 绑定研究导出文件到 file_uploads（下载有效期与任务过期时间对齐） */
    private AttachmentRelation bindFile(Long jobId, Long operatorId, String objectKey,
                                        String originalName, long size, LocalDateTime expireAt) {
        AttachmentRelation relation = new AttachmentRelation();
        relation.setUserId(operatorId);
        relation.setBizType(FILE_BIZ_TYPE);
        relation.setBizId(jobId);
        relation.setFileCategory(FILE_CATEGORY);
        relation.setOriginalName(originalName);
        relation.setFilePath(objectKey);
        relation.setFileSize(size);
        relation.setMimeType("application/json");
        relation.setDisk("oss");
        relation.setConvertStatus(0);
        relation.setSortOrder(0);
        relation.setFileStatus(FileStatusEnum.BOUND.getValue());
        relation.setDownloadExpireAt(expireAt);
        return attachmentRelationRepository.save(relation);
    }

    /**
     * 写导出操作日志（action=1 创建）。
     * <p>
     * export_operation_logs.scope_type 仅支持 1-4，年级（6）导出时审计记录以学校范围
     * （scope_type=1 + scope_id=学校）落库，年级值随 filter_conditions 快照保留（文档 5.1 说明）。
     */
    private void writeOperationLog(ExportJob job, ResearchExportRequest request, int recordCount,
                                   boolean isAnonymized, Integer dataVersion, Long fileId) {
        Integer logScopeType = request.getScopeType() == SCOPE_GRADE ? SCOPE_SCHOOL : request.getScopeType();
        Long logScopeId = request.getScopeType() == SCOPE_GRADE ? job.getSchoolId() : job.getScopeId();
        ExportOperationLog opLog = new ExportOperationLog();
        opLog.setSchoolId(job.getSchoolId());
        opLog.setOperatorId(job.getOperatorId());
        opLog.setExportType(EXPORT_TYPE_RESEARCH);
        opLog.setAction(1);
        opLog.setScopeType(logScopeType);
        opLog.setScopeId(logScopeId != null ? logScopeId : job.getSchoolId());
        opLog.setFilterConditions(job.getFilterConditions());
        opLog.setRecordCount(recordCount);
        opLog.setIsAnonymized(isAnonymized ? 1 : 0);
        opLog.setDataVersion(dataVersion);
        opLog.setFileId(fileId);
        opLog.setStatus(1);
        exportOperationLogRepository.save(opLog);
    }

    // ==================== 任务查询辅助 ====================

    private ExportJobResponse toJobResponse(ExportJob job) {
        Integer status = job.getStatus() != null ? job.getStatus() : ExportTaskStatusEnum.PENDING.getValue();
        String downloadUrl = null;
        if (Objects.equals(status, ExportTaskStatusEnum.COMPLETED.getValue()) && job.getFileId() != null) {
            downloadUrl = attachmentRelationRepository.findById(job.getFileId())
                    .map(r -> ossFileService.getFileUrl(r.getFilePath(), r.getOriginalName()))
                    .orElse(null);
        }
        return ExportJobResponse.builder()
                .id(job.getId())
                .exportType(job.getExportType())
                .status(status)
                .statusLabel(ExportTaskStatusEnum.of(status).getLabel())
                // export_jobs 无 progress 列：按任务状态推导进度（待执行 0 / 执行中 0 / 完成 100 / 失败 0）
                .progress(Objects.equals(status, ExportTaskStatusEnum.COMPLETED.getValue()) ? 100 : 0)
                .downloadUrl(downloadUrl)
                .expireAt(toIso(job.getExpireAt()))
                .createdAt(toIso(job.getCreatedAt()))
                .completedAt(toIso(job.getCompletedAt()))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** 读取任务（兼容异步线程先于提交的时序，最多重试约 2.5s） */
    private ExportJob findJobWithRetry(Long jobId) {
        for (int i = 0; i < 50; i++) {
            ExportJob job = exportJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                return job;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    /** 从任务 filter_conditions 解析导出请求 */
    private ResearchExportRequest parseFilterConditions(String filterConditions) {
        if (filterConditions == null || filterConditions.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(filterConditions, ResearchExportRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("导出任务 filter_conditions 解析失败: {}", filterConditions, e);
            return null;
        }
    }

    /** 置任务失败并记录错误信息 */
    private void failJob(ExportJob job, Exception e) {
        try {
            job.setStatus(ExportTaskStatusEnum.FAILED.getValue());
            job.setErrorMsg(errorText(e));
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
        } catch (Exception ex) {
            log.error("更新导出任务失败状态出错 jobId={}", job.getId(), ex);
        }
    }

    /** 提取异常原因文本（空消息回退为异常类型名，便于排查） */
    private String errorText(Exception e) {
        String msg = e.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }

    /** 序列化 JSON */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
