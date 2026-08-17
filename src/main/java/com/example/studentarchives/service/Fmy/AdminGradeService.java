package com.example.studentarchives.service.Fmy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.Fmy.OssProperties;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportRequest;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportDetailResponse;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportFailItem;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportListItem;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportResponse;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.grade.GradeImportConfig;
import com.example.studentarchives.entity.grade.GradeImportLog;
import com.example.studentarchives.entity.grade.GpaRecord;
import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AdminGpaRecordRepository;
import com.example.studentarchives.repository.AdminSemesterGpaSummaryRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.GpaRecordRepository;
import com.example.studentarchives.repository.GradeImportLogRepository;
import com.example.studentarchives.repository.SemesterGpaSummaryRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端成绩导入服务
 * <p>
 * 对应《管理端接口文档》十三、成绩导入模块（13.1 导入成绩 / 13.2 导入历史列表 /
 * 13.3 导入详情 / 13.4 下载导入模板），统一权限码 {@code grade:import}。
 * <ul>
 *   <li>13.1 上传成绩文件并启动异步导入任务：创建 grade_import_logs（status=0 导入中）后
 *       经 {@code scoreRecalculationExecutor} 线程池异步解析（.xlsx/.csv，见
 *       {@link GradeImportFileParser}），逐行校验写入 gpa_records、刷新 semester_gpa_summaries
 *       与班级/专业排名，最后触发 AdminScoreService 对该学生该学期画像分数增量重算；
 *       校验失败的记录写入 grade_import_logs.fail_details。</li>
 *   <li>13.2 按学期/导入状态分页查询导入历史（grade_import_logs 按 id 倒序）。</li>
 *   <li>13.3 查询单条导入详情（含失败明细）。</li>
 *   <li>13.4 下载成绩导入 CSV 模板（项目未引入 Excel 依赖，以 UTF-8 BOM 的 CSV 提供）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGradeService {

    /** 导入成绩权限码（《管理端接口文档》关键权限码） */
    private static final String IMPORT_PERMISSION = "grade:import";

    /** 导入状态：0=导入中 1=完成 2=失败 */
    private static final int STATUS_IMPORTING = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_FAILED = 2;

    private static final BigDecimal BD_100 = new BigDecimal("100");
    private static final BigDecimal BD_5 = new BigDecimal("5");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final AdminGradeImportConfigService adminGradeImportConfigService;
    private final GradeImportLogRepository gradeImportLogRepository;
    private final GradeImportFileParser fileParser;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final GpaRecordRepository gpaRecordRepository;
    private final AdminGpaRecordRepository adminGpaRecordRepository;
    private final SemesterGpaSummaryRepository semesterGpaSummaryRepository;
    private final AdminSemesterGpaSummaryRepository adminSemesterGpaSummaryRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final AdminScoreService adminScoreService;
    private final OSS ossClient;
    private final OssProperties ossProperties;
    private final ObjectMapper objectMapper;

    /** 自引用代理（@Lazy 避免循环依赖），用于提交 @Async 异步导入 */
    @Lazy
    @Autowired
    private AdminGradeService self;

    // ==================== 13.1 导入成绩 ====================

    /**
     * 导入成绩（POST /admin/grades/import，文档 13.1）
     * <p>
     * 创建导入任务记录（status=0 导入中）后提交异步解析，接口立即返回任务 ID。
     *
     * @param userId  当前登录用户 ID
     * @param request 导入请求（semesterId / fileId / overwrite）
     * @return 任务 ID 与初始状态
     */
    public GradeImportResponse importGrades(Long userId, GradeImportRequest request) {
        adminAuthService.requireAdminOrPermission(userId, IMPORT_PERMISSION);

        User operator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        Long schoolId = operator.getSchoolId();

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));
        if (!Objects.equals(semester.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学期不属于当前学校");
        }

        AttachmentRelation file = attachmentRelationRepository.findById(request.getFileId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "成绩文件不存在"));
        if (file.getFilePath() == null || file.getFilePath().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "成绩文件未上传到存储");
        }

        String ext = extractExtension(file.getOriginalName());
        Set<String> allowed = adminGradeImportConfigService.resolveAllowedExtensions(schoolId);
        if (ext == null || !allowed.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "不支持的文件格式，允许: " + allowed);
        }

        LocalDateTime now = LocalDateTime.now();
        GradeImportLog log = new GradeImportLog();
        log.setSchoolId(schoolId);
        log.setOperatorId(userId);
        log.setSemesterId(request.getSemesterId());
        log.setFileId(request.getFileId());
        log.setImportStatus(STATUS_IMPORTING);
        log.setStartedAt(now);
        gradeImportLogRepository.save(log);

        self.executeImportAsync(log.getId(), Boolean.TRUE.equals(request.getOverwrite()));

        return GradeImportResponse.builder()
                .importId(log.getId())
                .status(STATUS_IMPORTING)
                .statusLabel("导入中")
                .estimatedSeconds(estimateSeconds(file.getFileSize()))
                .build();
    }

    // ==================== 13.2 导入历史列表 ====================

    /**
     * 获取导入历史列表（GET /admin/grades/imports，文档 13.2）
     * <p>
     * 按学校隔离，支持学期/导入状态筛选，按 id 倒序分页。
     *
     * @param userId       当前登录用户 ID
     * @param semesterId   学期筛选（可选）
     * @param importStatus 导入状态筛选（可选，0=导入中 1=完成 2=失败）
     * @param pageParam    分页参数
     * @return 分页导入历史列表
     */
    public PageResult<GradeImportListItem> listImports(Long userId, Long semesterId, Integer importStatus, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, IMPORT_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        Specification<GradeImportLog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("schoolId"), schoolId));
            if (semesterId != null) {
                preds.add(cb.equal(root.get("semesterId"), semesterId));
            }
            if (importStatus != null) {
                preds.add(cb.equal(root.get("importStatus"), importStatus));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<GradeImportLog> page = gradeImportLogRepository.findAll(spec, pageable);

        List<GradeImportLog> logs = page.getContent();
        Set<Long> semesterIds = logs.stream().map(GradeImportLog::getSemesterId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> operatorIds = logs.stream().map(GradeImportLog::getOperatorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> fileIds = logs.stream().map(GradeImportLog::getFileId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> semesterNames = semesterRepository.findAllById(semesterIds).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getName, (a, b) -> a));
        Map<Long, String> operatorNames = userRepository.findByIdIn(operatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        Map<Long, String> fileNames = attachmentRelationRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(AttachmentRelation::getId, AttachmentRelation::getOriginalName, (a, b) -> a));

        List<GradeImportListItem> items = logs.stream()
                .map(l -> toListItem(l, semesterNames, operatorNames, fileNames))
                .collect(Collectors.toList());
        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 13.3 导入详情 ====================

    /**
     * 获取导入详情（GET /admin/grades/imports/{importId}，文档 13.3）
     * <p>
     * 返回操作人/文件名与失败明细，按学校隔离校验。
     *
     * @param userId   当前登录用户 ID
     * @param importId 导入任务 ID
     * @return 导入详情（含失败明细）
     */
    public GradeImportDetailResponse importDetail(Long userId, Long importId) {
        adminAuthService.requireAdminOrPermission(userId, IMPORT_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        GradeImportLog log = gradeImportLogRepository.findById(importId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "导入记录不存在"));
        if (!Objects.equals(log.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "导入记录不存在");
        }

        String semesterName = semesterRepository.findById(log.getSemesterId())
                .map(Semester::getName).orElse(null);
        String operatorName = userRepository.findById(log.getOperatorId())
                .map(User::getName).orElse(null);

        return GradeImportDetailResponse.builder()
                .id(log.getId())
                .semesterId(log.getSemesterId())
                .semesterName(semesterName)
                .operatorId(log.getOperatorId())
                .operatorName(operatorName)
                .fileId(log.getFileId())
                .totalCount(log.getTotalCount())
                .successCount(log.getSuccessCount())
                .failCount(log.getFailCount())
                .failDetails(parseFailDetails(log.getFailDetails()))
                .importStatus(log.getImportStatus())
                .importStatusLabel(statusLabel(log.getImportStatus()))
                .startedAt(toIso(log.getStartedAt()))
                .completedAt(toIso(log.getCompletedAt()))
                .build();
    }

    // ==================== 13.4 下载导入模板 ====================

    /**
     * 下载成绩导入模板（GET /admin/grades/import-template，文档 13.4）
     * <p>
     * 返回标准 .xlsx 文件，包含字段说明与格式示例。表头由
     * {@code grade_import_configs.template_columns} 配置决定。
     *
     * @param userId 当前登录用户 ID
     * @return 模板 Excel 字节
     */
    public byte[] importTemplate(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, IMPORT_PERMISSION);
        return adminGradeImportConfigService.downloadTemplate(userId);
    }

    // ==================== 异步导入执行 ====================

    /**
     * 异步执行成绩导入（由 13.1 经 self 代理提交）。
     * <p>
     * 解析文件 → 逐行校验落库 gpa_records → 刷新 semester_gpa_summaries 与排名 →
     * 更新导入日志 → 触发涉及学生的画像分数增量重算。任一阶段异常标记导入失败。
     *
     * @param importLogId 导入任务 ID
     * @param overwrite   是否覆盖已存在记录（默认追加）
     */
    @Async("scoreRecalculationExecutor")
    public void executeImportAsync(Long importLogId, boolean overwrite) {
        GradeImportLog importLog = gradeImportLogRepository.findById(importLogId).orElse(null);
        if (importLog == null) {
            log.warn("成绩导入任务不存在，跳过: importLogId={}", importLogId);
            return;
        }
        Long schoolId = importLog.getSchoolId();
        Long semesterId = importLog.getSemesterId();
        Long operatorId = importLog.getOperatorId();

        List<GradeImportFailItem> fails = new ArrayList<>();
        Set<Long> affectedUserIds = new LinkedHashSet<>();
        int total = 0;
        int success = 0;
        try {
            GradeImportConfig config = adminGradeImportConfigService.requireEnabledConfig(schoolId);
            List<GradeImportFileParser.TemplateColumn> columns = adminGradeImportConfigService.resolveTemplateColumns(config);
            boolean hasHeaderRow = config.getHasHeaderRow() == null || config.getHasHeaderRow() == 1;

            AttachmentRelation file = attachmentRelationRepository.findById(importLog.getFileId()).orElse(null);
            if (file == null || file.getFilePath() == null || file.getFilePath().isBlank()) {
                throw new BusinessException(ResultCode.FILE_NOT_FOUND, "成绩文件不存在");
            }
            byte[] bytes = readOssBytes(file.getFilePath());
            String ext = extractExtension(file.getOriginalName());

            List<GradeImportFileParser.ParsedRow> rows = fileParser.parse(bytes, ext, columns, hasHeaderRow);
            total = rows.size();

            for (GradeImportFileParser.ParsedRow row : rows) {
                try {
                    Long userId = applyRow(schoolId, semesterId, row, overwrite);
                    success++;
                    affectedUserIds.add(userId);
                } catch (BusinessException be) {
                    fails.add(new GradeImportFailItem(row.getRowNumber(),
                            row.getValues().get("studentNo"), be.getMessage()));
                }
            }

            refreshSummaries(schoolId, semesterId, affectedUserIds);

            importLog.setTotalCount(total);
            importLog.setSuccessCount(success);
            importLog.setFailCount(fails.size());
            importLog.setFailDetails(fails.isEmpty() ? null : writeJsonSafe(fails));
            importLog.setImportStatus(STATUS_DONE);
            importLog.setCompletedAt(LocalDateTime.now());
            gradeImportLogRepository.save(importLog);

            for (Long userId : affectedUserIds) {
                try {
                    adminScoreService.recalculateStudent(schoolId, userId, semesterId, operatorId);
                } catch (Exception e) {
                    log.warn("导入后画像评分重算失败: userId={}, semesterId={}, error={}",
                            userId, semesterId, e.getMessage());
                }
            }
            log.info("成绩导入完成: importId={}, total={}, success={}, fail={}", importLogId, total, success, fails.size());
        } catch (Exception e) {
            List<GradeImportFailItem> all = new ArrayList<>(fails);
            all.add(new GradeImportFailItem(0, null, "导入失败: " + errorText(e)));
            importLog.setTotalCount(total);
            importLog.setSuccessCount(success);
            importLog.setFailCount(all.size());
            importLog.setFailDetails(writeJsonSafe(all));
            importLog.setImportStatus(STATUS_FAILED);
            importLog.setCompletedAt(LocalDateTime.now());
            gradeImportLogRepository.save(importLog);
            log.error("成绩导入失败: importLogId={}, error={}", importLogId, e.getMessage(), e);
        }
    }

    /**
     * 逐行校验并写入 gpa_records，返回学生用户 ID。
     *
     * @throws BusinessException 校验失败（学号不存在/成绩格式错误等）
     */
    private Long applyRow(Long schoolId, Long semesterId, GradeImportFileParser.ParsedRow row, boolean overwrite) {
        Map<String, String> v = row.getValues();

        String studentNo = v.get("studentNo");
        if (studentNo == null) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "学号为空");
        }
        User student = userRepository.findByUserNo(studentNo.trim()).orElse(null);
        if (student == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "学号不存在");
        }
        if (!Objects.equals(student.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号不属于当前学校");
        }

        String courseName = v.get("courseName");
        if (courseName == null) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "课程名称为空");
        }

        BigDecimal score = parseDecimal(v.get("score"), "成绩");
        if (score == null) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "成绩为空");
        }
        if (score.compareTo(ZERO) < 0 || score.compareTo(BD_100) > 0) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "成绩超出范围(0-100)");
        }

        BigDecimal gpa = parseDecimal(v.get("gpa"), "绩点");
        if (gpa != null && (gpa.compareTo(ZERO) < 0 || gpa.compareTo(BD_5) > 0)) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "绩点超出范围(0-5)");
        }

        BigDecimal credit = parseDecimal(v.get("credit"), "学分");
        if (credit != null && credit.compareTo(ZERO) < 0) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "学分格式错误");
        }

        Integer attemptNo = parseInteger(v.get("attemptNo"), "修读次数");
        if (attemptNo != null && attemptNo < 1) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "修读次数格式错误");
        }
        if (attemptNo == null) {
            attemptNo = 1;
        }

        String courseCode = v.get("courseCode");
        if (overwrite && courseCode != null) {
            List<GpaRecord> existing = adminGpaRecordRepository
                    .findByUserIdAndSemesterIdAndCourseCode(student.getId(), semesterId, courseCode.trim());
            if (!existing.isEmpty()) {
                adminGpaRecordRepository.deleteAll(existing);
            }
        }

        GpaRecord record = new GpaRecord();
        record.setSchoolId(schoolId);
        record.setUserId(student.getId());
        record.setSemesterId(semesterId);
        record.setCourseCode(courseCode != null ? courseCode.trim() : null);
        record.setCourseName(courseName.trim());
        record.setCourseType(v.get("courseType"));
        record.setAttemptNo(attemptNo);
        record.setScore(score);
        record.setGpa(gpa);
        record.setCredit(credit);
        adminGpaRecordRepository.save(record);
        return student.getId();
    }

    // ==================== 学期汇总与排名 ====================

    /** 刷新受影响学生的 semester_gpa_summaries 及班级/专业排名 */
    private void refreshSummaries(Long schoolId, Long semesterId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Map<Long, Long> userIdToClassId = new HashMap<>();
        Map<Long, Long> classIdToMajorId = new HashMap<>();
        for (StudentProfile profile : studentProfileRepository.findByUserIdIn(userIds)) {
            userIdToClassId.put(profile.getUserId(), profile.getClassId());
        }
        Set<Long> classIds = new HashSet<>(userIdToClassId.values());
        if (!classIds.isEmpty()) {
            for (Clazz clazz : clazzRepository.findByIdIn(classIds)) {
                classIdToMajorId.put(clazz.getId(), clazz.getMajorId());
            }
        }
        Map<Long, Long> userIdToMajorId = new HashMap<>();
        for (Map.Entry<Long, Long> e : userIdToClassId.entrySet()) {
            Long majorId = classIdToMajorId.get(e.getValue());
            if (majorId != null) {
                userIdToMajorId.put(e.getKey(), majorId);
            }
        }
        for (Long userId : userIds) {
            updateSummary(semesterId, userId,
                    userIdToClassId.get(userId), userIdToMajorId.get(userId));
        }
        computeRanks(semesterId, userIdToClassId, userIdToMajorId);
    }

    /** 按该学期 gpa_records 汇总并 upsert semester_gpa_summaries（成绩/绩点按学分加权） */
    private void updateSummary(Long semesterId, Long userId, Long classId, Long majorId) {
        List<GpaRecord> records = gpaRecordRepository
                .findByUserIdAndSemesterIdOrderByCourseCodeAsc(userId, semesterId);
        BigDecimal totalCredit = ZERO;
        BigDecimal weightedGpaSum = ZERO;
        BigDecimal weightedScoreSum = ZERO;
        for (GpaRecord r : records) {
            if (r.getCredit() == null) {
                continue;
            }
            totalCredit = totalCredit.add(r.getCredit());
            if (r.getGpa() != null) {
                weightedGpaSum = weightedGpaSum.add(r.getGpa().multiply(r.getCredit()));
            }
            if (r.getScore() != null) {
                weightedScoreSum = weightedScoreSum.add(r.getScore().multiply(r.getCredit()));
            }
        }
        BigDecimal weightedGpa = null;
        BigDecimal averageScore = null;
        if (totalCredit.compareTo(ZERO) > 0) {
            weightedGpa = weightedGpaSum.divide(totalCredit, 2, RoundingMode.HALF_UP);
            averageScore = weightedScoreSum.divide(totalCredit, 2, RoundingMode.HALF_UP);
        }

        SemesterGpaSummary summary = semesterGpaSummaryRepository
                .findByUserIdAndSemesterId(userId, semesterId)
                .orElseGet(SemesterGpaSummary::new);
        summary.setUserId(userId);
        summary.setSemesterId(semesterId);
        summary.setClassId(classId);
        summary.setMajorId(majorId);
        summary.setCourseCount(records.size());
        summary.setTotalCredit(totalCredit);
        summary.setWeightedGpa(weightedGpa);
        summary.setAverageScore(averageScore);
        semesterGpaSummaryRepository.save(summary);
    }

    /** 按班级/专业重算受影响学生的加权绩点排名（weighted_gpa 倒序） */
    private void computeRanks(Long semesterId, Map<Long, Long> userIdToClassId, Map<Long, Long> userIdToMajorId) {
        for (Map.Entry<Long, List<Long>> e : groupByOrg(userIdToClassId).entrySet()) {
            List<SemesterGpaSummary> summaries = adminSemesterGpaSummaryRepository
                    .findBySemesterIdAndClassId(semesterId, e.getKey());
            summaries.sort(Comparator.comparing(SemesterGpaSummary::getWeightedGpa,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SemesterGpaSummary::getId));
            for (int i = 0; i < summaries.size(); i++) {
                SemesterGpaSummary s = summaries.get(i);
                if (!Objects.equals(s.getRankInClass(), i + 1)) {
                    s.setRankInClass(i + 1);
                    semesterGpaSummaryRepository.save(s);
                }
            }
        }
        for (Map.Entry<Long, List<Long>> e : groupByOrg(userIdToMajorId).entrySet()) {
            List<SemesterGpaSummary> summaries = adminSemesterGpaSummaryRepository
                    .findBySemesterIdAndMajorId(semesterId, e.getKey());
            summaries.sort(Comparator.comparing(SemesterGpaSummary::getWeightedGpa,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SemesterGpaSummary::getId));
            for (int i = 0; i < summaries.size(); i++) {
                SemesterGpaSummary s = summaries.get(i);
                if (!Objects.equals(s.getRankInMajor(), i + 1)) {
                    s.setRankInMajor(i + 1);
                    semesterGpaSummaryRepository.save(s);
                }
            }
        }
    }

    /** 用户 → 组织 ID 分组 */
    private Map<Long, List<Long>> groupByOrg(Map<Long, Long> userIdToOrgId) {
        Map<Long, List<Long>> groups = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> e : userIdToOrgId.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            groups.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        return groups;
    }

    // ==================== 私有工具 ====================

    /** 读取 OSS 对象字节内容 */
    private byte[] readOssBytes(String objectKey) {
        OSSObject object = ossClient.getObject(ossProperties.getBucketName(), objectKey);
        try (InputStream in = object.getObjectContent()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.THIRD_OSS_FAILED, "成绩文件读取失败");
        }
    }

    private List<GradeImportFailItem> parseFailDetails(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<GradeImportFailItem>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeJsonSafe(List<GradeImportFailItem> fails) {
        try {
            return objectMapper.writeValueAsString(fails);
        } catch (Exception e) {
            return "[{\"row\":0,\"reason\":\"失败明细序列化异常\"}]";
        }
    }

    private GradeImportListItem toListItem(GradeImportLog log,
                                           Map<Long, String> semesterNames,
                                           Map<Long, String> operatorNames,
                                           Map<Long, String> fileNames) {
        return GradeImportListItem.builder()
                .id(log.getId())
                .semesterId(log.getSemesterId())
                .semesterName(semesterNames.get(log.getSemesterId()))
                .operatorName(operatorNames.get(log.getOperatorId()))
                .fileName(fileNames.get(log.getFileId()))
                .totalCount(log.getTotalCount())
                .successCount(log.getSuccessCount())
                .failCount(log.getFailCount())
                .importStatus(log.getImportStatus())
                .importStatusLabel(statusLabel(log.getImportStatus()))
                .startedAt(toIso(log.getStartedAt()))
                .completedAt(toIso(log.getCompletedAt()))
                .build();
    }

    private static String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_IMPORTING -> "导入中";
            case STATUS_DONE -> "已完成";
            case STATUS_FAILED -> "失败";
            default -> "未知";
        };
    }

    private static BigDecimal parseDecimal(String s, String field) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, field + "格式错误");
        }
    }

    private static Integer parseInteger(String s, String field) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, field + "格式错误");
        }
    }

    private static String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private static int estimateSeconds(Long fileSize) {
        long size = fileSize != null ? fileSize : 0;
        long sec = 20 + size / 200_000;
        if (sec < 10) {
            return 10;
        }
        if (sec > 300) {
            return 300;
        }
        return (int) sec;
    }

    private static String toIso(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE);
    }

    private static String errorText(Throwable e) {
        String msg = e.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }
}
