package com.example.studentarchives.service.schedule;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.PortraitEvaluationScoreRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.Fmy.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 每周学生画像报表导出处理器（task_code = weekly_report_export）。
 * <p>
 * 由动态调度器按任务 cron（示例：每周一 08:00）触发，导出当前学期全校学生的画像评分报表
 * （Excel/CSV），并作为附件邮件发送给任务配置的收件人。
 * <p>
 * 任务参数（scheduled_tasks.task_params）：
 * <pre>
 * {
 *   "exportFormat": "excel",              // excel(默认) / csv
 *   "includeDimensions": ["academic", ...], // 可选，只导出指定维度，缺省导出全部维度
 *   "emailRecipients": ["a@b.com", ...],    // 可选，收件人列表
 *   "batchSize": 500                        // 预留，当前一次性查询
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportExportHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "weekly_report_export";

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SemesterRepository semesterRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final PortraitEvaluationScoreRepository portraitEvaluationScoreRepository;
    private final AbilityDimensionRepository abilityDimensionRepository;
    private final EmailService emailService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "每周导出学生画像评分报表并邮件发送";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        Long schoolId = context.getSchoolId();
        ReportConfig config = ReportConfig.from(context.getTaskParams());

        Semester semester = semesterRepository.findCurrentBySchoolId(schoolId).orElse(null);
        if (semester == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "学校无当前学期，无法生成画像周报");
        }

        List<Long> userIds = studentProfileRepository.findBySchoolId(schoolId).stream()
                .map(StudentProfile::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "学校无学生，无法生成画像周报");
        }

        Map<Long, User> userById = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        // 维度编码 -> 中文名（含禁用，保证历史评分维度也能正确显示名称）
        Map<String, String> dimensionNameByCode = abilityDimensionRepository.findAllByOrderBySortAsc().stream()
                .collect(Collectors.toMap(AbilityDimension::getDimensionCode,
                        AbilityDimension::getDimensionName, (a, b) -> a));

        List<String> dimensions = config.includeDimensions != null && !config.includeDimensions.isEmpty()
                ? config.includeDimensions
                : new ArrayList<>(dimensionNameByCode.keySet());

        // 当前学期画像评分，按 userId -> dimensionCode 分组
        Map<Long, Map<String, BigDecimal>> scoreByUserDim = portraitEvaluationScoreRepository
                .findByUserIdInAndSemesterId(userIds, semester.getId()).stream()
                .filter(s -> dimensions.contains(s.getDimensionCode()))
                .collect(Collectors.groupingBy(
                        PortraitEvaluationScore::getUserId,
                        LinkedHashMap::new,
                        Collectors.toMap(PortraitEvaluationScore::getDimensionCode,
                                PortraitEvaluationScore::getScore, (a, b) -> a)));

        String date = LocalDate.now().format(FILE_DATE);
        byte[] fileBytes;
        String fileName;
        String contentType;
        if ("csv".equalsIgnoreCase(config.exportFormat)) {
            fileBytes = buildCsv(userIds, userById, dimensions, dimensionNameByCode, scoreByUserDim);
            fileName = "学生画像周报-" + date + ".csv";
            contentType = "text/csv";
        } else {
            fileBytes = buildXlsx(userIds, userById, dimensions, dimensionNameByCode, scoreByUserDim);
            fileName = "学生画像周报-" + date + ".xlsx";
            contentType = XLSX_CONTENT_TYPE;
        }

        List<String> recipients = config.emailRecipients;
        if (recipients == null || recipients.isEmpty()) {
            log.info("周报已生成但未配置收件人，跳过邮件发送: schoolId={}, fileName={}", schoolId, fileName);
            return;
        }

        String subject = "学生画像周报（" + semester.getName() + "）-" + date;
        String body = "附件为本周学生画像评分报表，共 " + userIds.size() + " 名学生，维度："
                + dimensions.stream()
                .map(c -> dimensionNameByCode.getOrDefault(c, c))
                .collect(Collectors.joining("、")) + "。";
        for (String to : recipients) {
            emailService.sendMailWithAttachment(to, subject, body, fileName, fileBytes, contentType);
        }
        log.info("学生画像周报导出并发送完成: schoolId={}, students={}, recipients={}",
                schoolId, userIds.size(), recipients.size());
    }

    // ==================== 报表生成 ====================

    private byte[] buildCsv(List<Long> userIds, Map<Long, User> userById, List<String> dimensions,
                            Map<String, String> dimensionNameByCode,
                            Map<Long, Map<String, BigDecimal>> scoreByUserDim) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM，避免 Excel 打开中文乱码
        sb.append("学号,姓名");
        for (String dim : dimensions) {
            sb.append(',').append(csvEscape(dimensionNameByCode.getOrDefault(dim, dim)));
        }
        sb.append('\n');
        for (Long userId : userIds) {
            User user = userById.get(userId);
            Map<String, BigDecimal> dimScores = scoreByUserDim.getOrDefault(userId, Map.of());
            sb.append(csvEscape(user != null ? user.getUserNo() : ""));
            sb.append(',').append(csvEscape(user != null ? user.getName() : ""));
            for (String dim : dimensions) {
                BigDecimal score = dimScores.get(dim);
                sb.append(',').append(score != null ? score.toPlainString() : "");
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** 精简单 sheet Excel（手写 OOXML，inlineStr 文本单元格，与 AdminExportService 口径一致） */
    private byte[] buildXlsx(List<Long> userIds, Map<Long, User> userById, List<String> dimensions,
                             Map<String, String> dimensionNameByCode,
                             Map<Long, Map<String, BigDecimal>> scoreByUserDim) {
        List<String[]> rows = new ArrayList<>();
        String[] header = new String[2 + dimensions.size()];
        header[0] = "学号";
        header[1] = "姓名";
        for (int i = 0; i < dimensions.size(); i++) {
            header[2 + i] = dimensionNameByCode.getOrDefault(dimensions.get(i), dimensions.get(i));
        }
        rows.add(header);
        for (Long userId : userIds) {
            User user = userById.get(userId);
            Map<String, BigDecimal> dimScores = scoreByUserDim.getOrDefault(userId, Map.of());
            String[] row = new String[header.length];
            row[0] = user != null ? user.getUserNo() : "";
            row[1] = user != null ? user.getName() : "";
            for (int i = 0; i < dimensions.size(); i++) {
                BigDecimal score = dimScores.get(dimensions.get(i));
                row[2 + i] = score != null ? score.toPlainString() : "";
            }
            rows.add(row);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zip, "_rels/.rels", rootRelsXml());
            writeZipEntry(zip, "xl/workbook.xml", workbookXml());
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(rows));
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成学生画像周报 xlsx 失败", e);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "画像周报 Excel 生成失败");
        }
    }

    private void writeZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"学生画像周报\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    private String worksheetXml(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        int rowIdx = 1;
        for (String[] row : rows) {
            sb.append("<row r=\"").append(rowIdx).append("\">");
            for (int c = 0; c < row.length; c++) {
                sb.append("<c r=\"").append(columnLetter(c)).append(rowIdx)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(xmlEscape(row[c] == null ? "" : row[c]))
                        .append("</t></is></c>");
            }
            sb.append("</row>");
            rowIdx++;
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private String columnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        int i = index + 1;
        while (i > 0) {
            int rem = (i - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            i = (i - 1) / 26;
        }
        return sb.toString();
    }

    private String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    // ==================== 任务参数解析 ====================

    private static class ReportConfig {
        String exportFormat = "excel";
        List<String> includeDimensions;
        List<String> emailRecipients;

        static ReportConfig from(JsonNode taskParams) {
            ReportConfig config = new ReportConfig();
            if (taskParams == null) {
                return config;
            }
            if (taskParams.hasNonNull("exportFormat")) {
                config.exportFormat = taskParams.get("exportFormat").asText();
            }
            if (taskParams.hasNonNull("includeDimensions") && taskParams.get("includeDimensions").isArray()) {
                config.includeDimensions = new ArrayList<>();
                taskParams.get("includeDimensions").forEach(n -> config.includeDimensions.add(n.asText()));
            }
            if (taskParams.hasNonNull("emailRecipients") && taskParams.get("emailRecipients").isArray()) {
                config.emailRecipients = new ArrayList<>();
                taskParams.get("emailRecipients").forEach(n -> config.emailRecipients.add(n.asText()));
            }
            return config;
        }
    }
}
