package com.example.studentarchives.service.Lzw;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.Fmy.OssProperties;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.service.Fmy.GradeImportFileParser;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 管理端学期管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》九、学期管理模块（9.1 ~ 9.7）。
 * 数据来源：semesters（批量导入读取 file_uploads + OSS）。
 * <p>
 * 权限：9.1~9.5 文档附录标注「管理端可增删改，公共端只读」，要求 admin 角色；
 * 9.6/9.7 文档关键权限码标注 {@code semester:import}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterManageService {

    /** 日期输出格式（YYYY-MM-DD） */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 学期批量导入权限码（文档九、9.6） */
    private static final String IMPORT_PERMISSION = "semester:import";

    /** 学期导入模板列（学期名称/开始日期/结束日期） */
    private static final List<GradeImportFileParser.TemplateColumn> SEMESTER_COLUMNS = List.of(
            new GradeImportFileParser.TemplateColumn("name", "学期名称", true),
            new GradeImportFileParser.TemplateColumn("startDate", "开始日期", true),
            new GradeImportFileParser.TemplateColumn("endDate", "结束日期", true)
    );

    private final SemesterRepository semesterRepository;
    private final SchoolRepository schoolRepository;
    private final AdminAuthService adminAuthService;
    private final GradeImportFileParser fileParser;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final OSS ossClient;
    private final OssProperties ossProperties;

    // ==================== 9.1 获取学期列表 ====================

    @Transactional(readOnly = true)
    public PageResult<SemesterListItem> listSemesters(Long operatorId, SemesterListQuery query, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);

        Specification<Semester> spec = buildSemesterSpec(query.getSchoolId(), query.getStatus());
        Sort sort = Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<Semester> page = semesterRepository.findAll(spec, pageable);

        // 批量加载学校名称
        Set<Long> schoolIds = page.getContent().stream()
                .map(Semester::getSchoolId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> schoolNameMap = schoolIds.isEmpty() ? Map.of()
                : schoolRepository.findByIdIn(new ArrayList<>(schoolIds)).stream()
                        .collect(Collectors.toMap(School::getId, School::getName, (a, b) -> a));

        List<SemesterListItem> items = page.getContent().stream().map(s -> SemesterListItem.builder()
                .semesterId(s.getId())
                .name(s.getName())
                .schoolId(s.getSchoolId())
                .schoolName(s.getSchoolId() != null ? schoolNameMap.get(s.getSchoolId()) : null)
                .startDate(formatDate(s.getStartDate()))
                .endDate(formatDate(s.getEndDate()))
                .isCurrent(s.getIsCurrent())
                .status(s.getStatus())
                .statusLabel(statusLabel(s.getStatus()))
                .createdAt(toIso(s.getCreatedAt()))
                .build()).collect(Collectors.toList());

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 9.2 创建学期 ====================

    @Transactional
    public SemesterIdResponse createSemester(Long operatorId, SemesterSaveRequest body) {
        adminAuthService.requireAdmin(operatorId);

        Long schoolId = body.getSchoolId();
        if (schoolId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "schoolId 不能为空");
        }
        String name = requireNotBlank(body.getName(), "学期名称不能为空");
        LocalDate startDate = parseDate(body.getStartDate(), "开始日期");
        LocalDate endDate = parseDate(body.getEndDate(), "结束日期");

        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));

        validateSemester(schoolId, name, startDate, endDate, null);

        Semester semester = new Semester();
        semester.setSchoolId(schoolId);
        semester.setName(name);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setIsCurrent(0);
        semester.setStatus(1);
        semesterRepository.save(semester);

        return SemesterIdResponse.builder().semesterId(semester.getId()).build();
    }

    // ==================== 9.3 更新学期 ====================

    @Transactional
    public void updateSemester(Long operatorId, Long semesterId, SemesterSaveRequest body) {
        adminAuthService.requireAdmin(operatorId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));

        Long schoolId = body.getSchoolId() != null ? body.getSchoolId() : semester.getSchoolId();
        String name = (body.getName() != null && !body.getName().isBlank())
                ? body.getName().trim() : semester.getName();
        LocalDate startDate = body.getStartDate() != null
                ? parseDate(body.getStartDate(), "开始日期") : semester.getStartDate();
        LocalDate endDate = body.getEndDate() != null
                ? parseDate(body.getEndDate(), "结束日期") : semester.getEndDate();

        if (body.getSchoolId() != null) {
            schoolRepository.findById(body.getSchoolId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));
        }

        validateSemester(schoolId, name, startDate, endDate, semesterId);

        semester.setSchoolId(schoolId);
        semester.setName(name);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semesterRepository.save(semester);
    }

    // ==================== 9.4 设置当前学期 ====================

    @Transactional
    public void setCurrentSemester(Long operatorId, Long semesterId) {
        adminAuthService.requireAdmin(operatorId);

        Semester target = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));

        Long schoolId = target.getSchoolId();
        if (schoolId == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "学期未关联学校");
        }

        // 文档 9.4：将该学校所有学期 is_current 置 0，再将本学期的 is_current 置 1
        List<Semester> semesters = semesterRepository.findBySchoolId(schoolId);
        for (Semester s : semesters) {
            s.setIsCurrent(s.getId().equals(semesterId) ? 1 : 0);
        }
        semesterRepository.saveAll(semesters);
    }

    // ==================== 9.5 启用/禁用学期 ====================

    @Transactional
    public void updateSemesterStatus(Long operatorId, Long semesterId, Integer status) {
        adminAuthService.requireAdmin(operatorId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));

        validateStatus(status);
        semester.setStatus(status);
        semesterRepository.save(semester);
    }

    // ==================== 9.6 批量导入学期 ====================

    /**
     * 批量导入学期（POST /admin/semesters/import，文档 9.6）
     * <p>
     * 同步逐行校验执行，不进入任务表。文件支持 .xlsx / .csv（模板见 9.7）。
     * 逐行校验：必填/日期格式、结束日期晚于开始日期、同校名称唯一（overwrite=true 时覆盖同名
     * 学期的开始/结束日期）、学期日期不与已有学期重叠；失败行记入 failures 不中断其余行。
     */
    @Transactional
    public SemesterImportResponse importSemesters(Long operatorId, SemesterImportRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, IMPORT_PERMISSION);

        Long schoolId = body.getSchoolId();
        if (schoolId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "schoolId 不能为空");
        }
        Long fileId = body.getFileId();
        if (fileId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "fileId 不能为空");
        }
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));

        AttachmentRelation file = attachmentRelationRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "导入文件不存在"));
        if (file.getFilePath() == null || file.getFilePath().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "导入文件未上传到存储");
        }

        String ext = extractExtension(file.getOriginalName());
        if (!"xlsx".equals(ext) && !"csv".equals(ext)) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "不支持的文件格式，仅支持 .xlsx / .csv");
        }

        byte[] bytes = readOssBytes(file.getFilePath());
        List<GradeImportFileParser.ParsedRow> rows = fileParser.parse(bytes, ext, SEMESTER_COLUMNS, true);

        boolean overwrite = Boolean.TRUE.equals(body.getOverwrite());
        List<SemesterImportFailItem> failures = new ArrayList<>();
        int total = rows.size();
        int success = 0;

        // 同学校已有学期（含本次已导入的新学期），用于名称唯一与日期重叠校验
        List<Semester> existingAll = new ArrayList<>(semesterRepository.findBySchoolId(schoolId));
        Map<String, Semester> existingByName = existingAll.stream()
                .collect(Collectors.toMap(Semester::getName, s -> s, (a, b) -> a));
        Set<String> processedNames = new HashSet<>();

        for (GradeImportFileParser.ParsedRow row : rows) {
            String name = row.getValues().get("name");
            try {
                if (name == null || name.isBlank()) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "学期名称为空");
                }
                name = name.trim();
                LocalDate startDate = parseDate(row.getValues().get("startDate"), "开始日期");
                LocalDate endDate = parseDate(row.getValues().get("endDate"), "结束日期");
                if (!endDate.isAfter(startDate)) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "结束日期必须晚于开始日期");
                }
                if (!processedNames.add(name)) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "文件中存在重复的学期名称");
                }

                Semester existingDb = existingByName.get(name);
                if (existingDb != null) {
                    // 文档 9.6 ④：名称与已有学期相同 → 判定为同一条学期数据
                    if (!overwrite) {
                        throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "该学校下已存在同名学期");
                    }
                    validateNoOverlap(existingAll, existingDb.getId(), startDate, endDate);
                    existingDb.setStartDate(startDate);
                    existingDb.setEndDate(endDate);
                    semesterRepository.save(existingDb);
                    success++;
                    continue;
                }

                validateNoOverlap(existingAll, null, startDate, endDate);

                Semester semester = new Semester();
                semester.setSchoolId(schoolId);
                semester.setName(name);
                semester.setStartDate(startDate);
                semester.setEndDate(endDate);
                semester.setIsCurrent(0);
                semester.setStatus(1);
                semesterRepository.save(semester);
                existingAll.add(semester);
                success++;
            } catch (BusinessException be) {
                failures.add(new SemesterImportFailItem(row.getRowNumber(), name, be.getMessage()));
            }
        }

        return SemesterImportResponse.builder()
                .totalCount(total)
                .successCount(success)
                .failCount(failures.size())
                .failures(failures)
                .build();
    }

    // ==================== 9.7 下载导入模板 ====================

    /**
     * 下载学期批量导入模板（GET /admin/semesters/import-template，文档 9.7）
     * <p>
     * 返回标准 .xlsx 文件，包含表头（学期名称/开始日期/结束日期）与一行示例。
     */
    public byte[] importTemplate(Long operatorId) {
        adminAuthService.requireAdminOrPermission(operatorId, IMPORT_PERMISSION);
        return buildTemplateXlsx();
    }

    // ==================== 查询与校验辅助 ====================

    private Specification<Semester> buildSemesterSpec(Long schoolId, Integer status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (schoolId != null) {
                predicates.add(cb.equal(root.get("schoolId"), schoolId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 文档 9.2 校验：endDate > startDate；同一学校下 name 唯一；学期日期不与已有学期重叠。
     * excludeSemesterId 非空时跳过该学期（更新场景排除自身）。
     */
    private void validateSemester(Long schoolId, String name, LocalDate startDate, LocalDate endDate, Long excludeSemesterId) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "开始日期和结束日期不能为空");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期必须晚于开始日期");
        }
        semesterRepository.findBySchoolIdAndName(schoolId, name)
                .filter(existing -> excludeSemesterId == null || !existing.getId().equals(excludeSemesterId))
                .ifPresent(existing -> { throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该学校下已存在同名学期"); });

        semesterRepository.findBySchoolId(schoolId).stream()
                .filter(existing -> excludeSemesterId == null || !existing.getId().equals(excludeSemesterId))
                .filter(existing -> existing.getStartDate() != null && existing.getEndDate() != null)
                .filter(existing -> startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate()))
                .findAny()
                .ifPresent(existing -> { throw new BusinessException(ResultCode.PARAM_ERROR, "学期日期与已有学期重叠"); });
    }

    /** 日期重叠校验：startDate~endDate 与 existingAll 中任一学期（排除 excludeId）重叠则报错 */
    private void validateNoOverlap(List<Semester> existingAll, Long excludeId, LocalDate startDate, LocalDate endDate) {
        for (Semester s : existingAll) {
            if (excludeId != null && excludeId.equals(s.getId())) {
                continue;
            }
            if (s.getStartDate() == null || s.getEndDate() == null) {
                continue;
            }
            if (startDate.isBefore(s.getEndDate()) && endDate.isAfter(s.getStartDate())) {
                throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "学期日期与已有学期重叠");
            }
        }
    }

    // ==================== 通用辅助 ====================

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, message);
        }
        return value.trim();
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, fieldName + "不能为空");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, fieldName + "格式错误，应为 YYYY-MM-DD");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(禁用) 或 1(启用)");
        }
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(ISO_DATE) : null;
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "启用" : "禁用";
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    /** 读取 OSS 对象字节内容 */
    private byte[] readOssBytes(String objectKey) {
        OSSObject object = ossClient.getObject(ossProperties.getBucketName(), objectKey);
        try (InputStream in = object.getObjectContent()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.THIRD_OSS_FAILED, "导入文件读取失败");
        }
    }

    /** 提取小写扩展名（不含点） */
    private String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    // ==================== 模板生成（9.7，JDK ZIP + XML，无第三方 Excel 依赖） ====================

    private byte[] buildTemplateXlsx() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            writeZipEntry(zos, "[Content_Types].xml", TEMPLATE_CONTENT_TYPES);
            writeZipEntry(zos, "_rels/.rels", TEMPLATE_PACKAGE_RELS);
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", TEMPLATE_WORKBOOK_RELS);
            writeZipEntry(zos, "xl/workbook.xml", TEMPLATE_WORKBOOK);
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", templateWorksheetXml());
            zos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SYS_ERROR, "模板 Excel 生成失败: " + e.getMessage());
        }
    }

    private void writeZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String templateWorksheetXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        sb.append("  <sheetData>\n");
        sb.append(templateRowXml(1, List.of("学期名称", "开始日期", "结束日期")));
        sb.append(templateRowXml(2, List.of("2026-2027第一学期", "2026-09-01", "2027-01-15")));
        sb.append("  </sheetData>\n");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private String templateRowXml(int rowNum, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <row r=\"").append(rowNum).append("\">\n");
        for (int i = 0; i < values.size(); i++) {
            String ref = columnRef(i + 1) + rowNum;
            sb.append("      <c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                    .append(escapeXml(values.get(i)))
                    .append("</t></is></c>\n");
        }
        sb.append("    </row>\n");
        return sb.toString();
    }

    private String columnRef(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int n = colIndex;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.append((char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.reverse().toString();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // ==================== 模板 XLSX 常量 ====================

    private static final String TEMPLATE_CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n"
            + "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n"
            + "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n"
            + "  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n"
            + "  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n"
            + "</Types>";

    private static final String TEMPLATE_PACKAGE_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n"
            + "</Relationships>";

    private static final String TEMPLATE_WORKBOOK_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n"
            + "</Relationships>";

    private static final String TEMPLATE_WORKBOOK = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n"
            + "  <sheets>\n"
            + "    <sheet name=\"学期导入模板\" sheetId=\"1\" r:id=\"rId1\"/>\n"
            + "  </sheets>\n"
            + "</workbook>";

    // ==================== 内嵌 POJO ====================

    /** 9.1 查询条件 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterListQuery {
        private Long schoolId;
        private Integer status;
    }

    /** 9.1 列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SemesterListItem {
        private Long semesterId;
        private String name;
        private Long schoolId;
        private String schoolName;
        private String startDate;
        private String endDate;
        private Integer isCurrent;
        private Integer status;
        private String statusLabel;
        private String createdAt;
    }

    /** 9.2 / 9.3 学期保存请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterSaveRequest {
        private Long schoolId;
        private String name;
        private String startDate;
        private String endDate;
    }

    /** 9.2 创建学期响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterIdResponse {
        private Long semesterId;
    }

    /** 9.5 启用/禁用学期请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterStatusRequest {
        private Integer status;
    }

    /** 9.6 批量导入请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterImportRequest {
        private Long schoolId;
        private Long fileId;
        private Boolean overwrite;
    }

    /** 9.6 批量导入响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterImportResponse {
        private Integer totalCount;
        private Integer successCount;
        private Integer failCount;
        private List<SemesterImportFailItem> failures;
    }

    /** 9.6 导入失败明细项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterImportFailItem {
        private Integer row;
        private String name;
        private String reason;
    }
}
