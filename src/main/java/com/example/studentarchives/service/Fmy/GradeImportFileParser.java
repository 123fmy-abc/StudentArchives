package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 成绩导入文件解析器（管理端成绩导入模块 13.1 使用）
 * <p>
 * 项目未引入 Apache POI，为避免改动他人依赖与配置，本解析器使用 JDK 原生能力实现：
 * <ul>
 *   <li><code>.csv</code>：按 UTF-8（含 BOM）逐行解析，支持引号包裹的字段与转义双引号；</li>
 *   <li><code>.xlsx</code>：按 ZIP + StAX 读取 <code>xl/sharedStrings.xml</code> 与
 *       <code>xl/worksheets/sheet1.xml</code>，支持共享字符串、内联字符串与数值单元格；</li>
 *   <li><code>.xls</code>（旧版二进制）：不解析，返回 {@link ResultCode#FILE_FORMAT_ERROR}。</li>
 * </ul>
 * 解析结果统一为按模板列（{@link TemplateColumn}）映射的字段值记录，供服务层逐行校验落库。
 */
@Component
public class GradeImportFileParser {

    /** 允许解析的文件扩展名 */
    public static final String EXT_XLSX = "xlsx";
    public static final String EXT_CSV = "csv";
    public static final String EXT_XLS = "xls";

    private static final int MAX_ROWS = 200_000;

    /**
     * 模板列定义（field 对应 gpa_records 等落库字段，label 为模板表头）
     */
    @Getter
    @AllArgsConstructor
    public static class TemplateColumn {
        private final String field;
        private final String label;
        private final boolean required;
    }

    /**
     * 单行解析结果：数据行号（不含表头，从 1 开始）与字段值映射
     */
    @Getter
    @AllArgsConstructor
    public static class ParsedRow {
        private final int rowNumber;
        private final Map<String, String> values;
    }

    /**
     * 解析成绩文件字节为字段值记录列表。
     *
     * @param bytes       文件字节内容
     * @param extension   文件扩展名（小写，不含点）
     * @param columns     模板列定义（决定字段名映射与顺序）
     * @param hasHeaderRow 首行是否为表头（1=是 0=否）
     * @return 数据行记录列表
     */
    public List<ParsedRow> parse(byte[] bytes, String extension, List<TemplateColumn> columns, boolean hasHeaderRow) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "成绩文件内容为空");
        }
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "模板列定义为空");
        }
        List<List<String>> rawRows;
        if (EXT_CSV.equalsIgnoreCase(extension)) {
            rawRows = parseCsv(bytes);
        } else if (EXT_XLSX.equalsIgnoreCase(extension)) {
            rawRows = parseXlsx(bytes);
        } else {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                    "暂不支持 ." + extension + " 格式，请使用 .xlsx 或 .csv");
        }
        if (rawRows.size() > MAX_ROWS) {
            throw new BusinessException(ResultCode.PARAM_OUT_OF_RANGE, "文件行数超过上限 " + MAX_ROWS);
        }
        return mapToRecords(rawRows, columns, hasHeaderRow);
    }

    /** 生成模板文件表头行（CSV） */
    public String buildHeaderLine(List<TemplateColumn> columns) {
        StringBuilder sb = new StringBuilder();
        for (TemplateColumn col : columns) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append('"').append(col.getLabel()).append('"');
        }
        return sb.toString();
    }

    /**
     * 生成标准 .xlsx 模板字节（Apache POI 无关，使用 JDK ZIP + XML）。
     *
     * @param columns 模板列定义
     * @param samples 每列对应的示例值（与 columns 顺序一致，可为 null）
     * @return xlsx 文件字节
     */
    public byte[] buildXlsx(List<TemplateColumn> columns, List<String> samples) {
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "模板列定义为空");
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {

            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zos, "_rels/.rels", packageRelsXml());
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeZipEntry(zos, "xl/workbook.xml", workbookXml());
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", worksheetXml(columns, samples));

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

    private String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n"
                + "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n"
                + "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n"
                + "  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n"
                + "  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n"
                + "</Types>";
    }

    private String packageRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
                + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n"
                + "</Relationships>";
    }

    private String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
                + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n"
                + "</Relationships>";
    }

    private String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n"
                + "  <sheets>\n"
                + "    <sheet name=\"成绩导入模板\" sheetId=\"1\" r:id=\"rId1\"/>\n"
                + "  </sheets>\n"
                + "</workbook>";
    }

    private String worksheetXml(List<TemplateColumn> columns, List<String> samples) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        sb.append("  <sheetData>\n");
        sb.append(buildRowXml(1, columns.stream().map(TemplateColumn::getLabel).toList()));
        if (samples != null && !samples.isEmpty()) {
            List<String> sampleValues = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) {
                sampleValues.add(i < samples.size() ? samples.get(i) : "");
            }
            sb.append(buildRowXml(2, sampleValues));
        }
        sb.append("  </sheetData>\n");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private String buildRowXml(int rowNum, List<String> values) {
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

    // ==================== 字段映射 ====================

    /**
     * 将原始单元格矩阵映射为字段值记录。
     * <p>
     * 优先按表头单元格匹配模板列（field 或 label，大小写不敏感）；若表头无一匹配
     * （如文件无表头），回退为按模板列顺序位置映射。
     */
    private List<ParsedRow> mapToRecords(List<List<String>> rawRows, List<TemplateColumn> columns, boolean hasHeaderRow) {
        List<ParsedRow> result = new ArrayList<>();
        if (rawRows.isEmpty()) {
            return result;
        }
        boolean headerMode = hasHeaderRow;
        int[] indexToField = null;
        int dataStart = 0;
        if (headerMode) {
            List<String> header = rawRows.get(0);
            indexToField = matchHeader(header, columns);
            int matched = 0;
            for (int idx : indexToField) {
                if (idx >= 0) {
                    matched++;
                }
            }
            if (matched == 0) {
                // 表头无一匹配 → 视作无表头，按位置映射
                headerMode = false;
                dataStart = 0;
            } else {
                dataStart = 1;
            }
        }
        for (int i = dataStart; i < rawRows.size(); i++) {
            List<String> row = rawRows.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            if (headerMode && indexToField != null) {
                for (int c = 0; c < indexToField.length && c < row.size(); c++) {
                    int fieldIdx = indexToField[c];
                    if (fieldIdx >= 0 && fieldIdx < columns.size()) {
                        values.put(columns.get(fieldIdx).getField(), trimToNull(row.get(c)));
                    }
                }
            } else {
                for (int c = 0; c < columns.size() && c < row.size(); c++) {
                    values.put(columns.get(c).getField(), trimToNull(row.get(c)));
                }
            }
            result.add(new ParsedRow(i - dataStart + 1, values));
        }
        return result;
    }

    /** 表头单元格 → 模板列下标映射（未匹配为 -1） */
    private int[] matchHeader(List<String> header, List<TemplateColumn> columns) {
        int[] mapping = new int[header.size()];
        for (int c = 0; c < header.size(); c++) {
            String cell = trimToNull(header.get(c));
            mapping[c] = -1;
            if (cell == null) {
                continue;
            }
            for (int j = 0; j < columns.size(); j++) {
                TemplateColumn col = columns.get(j);
                if (cell.equalsIgnoreCase(col.getField()) || cell.equalsIgnoreCase(col.getLabel())) {
                    mapping[c] = j;
                    break;
                }
            }
        }
        return mapping;
    }

    private boolean isEmptyRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (trimToNull(cell) != null) {
                return false;
            }
        }
        return true;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ==================== CSV 解析 ====================

    private List<List<String>> parseCsv(byte[] bytes) {
        List<List<String>> rows = new ArrayList<>();
        String content;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        } else {
            content = new String(bytes, StandardCharsets.UTF_8);
        }
        // 兼容 \r\n 与 \r
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            rows.add(parseCsvLine(line));
        }
        return rows;
    }

    /** 单行 CSV 解析：支持双引号包裹、逗号、转义双引号（""） */
    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    cells.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(ch);
                }
            }
        }
        cells.add(cur.toString());
        return cells;
    }

    // ==================== XLSX 解析（ZIP + StAX） ====================

    private List<List<String>> parseXlsx(byte[] bytes) {
        List<String> sharedStrings = null;
        List<Map<Integer, CellVal>> sheetRows = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // 先把当前 entry 内容读完，再用独立流解析，避免 StAX reader 关闭 ZipInputStream
                byte[] entryBytes = readEntryBytes(zis);
                if ("xl/sharedStrings.xml".equals(name)) {
                    sharedStrings = readSharedStrings(new ByteArrayInputStream(entryBytes));
                } else if ("xl/worksheets/sheet1.xml".equals(name)) {
                    sheetRows = readSheetRaw(new ByteArrayInputStream(entryBytes));
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "xlsx 文件读取失败: " + e.getMessage());
        }
        if (sheetRows == null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "未找到工作表 sheet1，请检查文件");
        }
        if (sharedStrings == null) {
            sharedStrings = new ArrayList<>();
        }
        return expandRows(sheetRows, sharedStrings);
    }

    private byte[] readEntryBytes(ZipInputStream zis) throws IOException {
        return zis.readAllBytes();
    }

    /** 读取 xl/sharedStrings.xml，返回共享字符串表 */
    private List<String> readSharedStrings(InputStream in) {
        List<String> strings = new ArrayList<>();
        try {
            XMLStreamReader reader = newXMLReader(in);
            StringBuilder cur = null;
            while (reader.hasNext()) {
                int evt = reader.next();
                if (evt == XMLStreamConstants.START_ELEMENT) {
                    if ("si".equals(reader.getLocalName())) {
                        cur = new StringBuilder();
                    } else if ("t".equals(reader.getLocalName()) && cur != null) {
                        cur.append(reader.getElementText());
                    }
                } else if (evt == XMLStreamConstants.END_ELEMENT && "si".equals(reader.getLocalName())) {
                    strings.add(cur != null ? cur.toString() : "");
                    cur = null;
                }
            }
            reader.close();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "sharedStrings.xml 解析失败: " + e.getMessage());
        }
        return strings;
    }

    /** 读取 xl/worksheets/sheet1.xml，返回每行单元格（列号→单元格值，列号 1 起始） */
    private List<Map<Integer, CellVal>> readSheetRaw(InputStream in) {
        List<Map<Integer, CellVal>> rowCells = new ArrayList<>();
        Map<Integer, CellVal> currentRow = null;
        String currentCol = null;
        boolean currentIsShared = false;
        boolean inInlineString = false;
        StringBuilder inlineText = null;
        try {
            XMLStreamReader reader = newXMLReader(in);
            while (reader.hasNext()) {
                int evt = reader.next();
                if (evt == XMLStreamConstants.START_ELEMENT) {
                    String tag = reader.getLocalName();
                    if ("row".equals(tag)) {
                        currentRow = new LinkedHashMap<>();
                    } else if ("c".equals(tag) && currentRow != null) {
                        currentCol = reader.getAttributeValue(null, "r");
                        currentIsShared = "s".equals(reader.getAttributeValue(null, "t"));
                    } else if ("v".equals(tag) && currentRow != null && currentCol != null) {
                        String val = reader.getElementText();
                        int colIdx = columnIndex(currentCol);
                        currentRow.put(colIdx, new CellVal(val, currentIsShared));
                    } else if ("is".equals(tag) && currentRow != null && currentCol != null) {
                        inInlineString = true;
                        inlineText = new StringBuilder();
                    } else if ("t".equals(tag) && inInlineString && currentRow != null && currentCol != null) {
                        inlineText.append(reader.getElementText());
                    }
                } else if (evt == XMLStreamConstants.END_ELEMENT) {
                    String tag = reader.getLocalName();
                    if ("row".equals(tag)) {
                        if (currentRow != null && !currentRow.isEmpty()) {
                            rowCells.add(currentRow);
                        }
                        currentRow = null;
                        currentCol = null;
                        currentIsShared = false;
                    } else if ("c".equals(tag)) {
                        if (inInlineString && currentRow != null && currentCol != null) {
                            currentRow.put(columnIndex(currentCol),
                                    new CellVal(inlineText != null ? inlineText.toString() : null, false));
                        }
                        currentCol = null;
                        currentIsShared = false;
                        inInlineString = false;
                        inlineText = null;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "sheet1.xml 解析失败: " + e.getMessage());
        }
        return rowCells;
    }

    /** 将稀疏单元格映射展开为定长行（补齐空列），并把共享字符串索引替换为实际字符串 */
    private List<List<String>> expandRows(List<Map<Integer, CellVal>> rowCells, List<String> sharedStrings) {
        int maxCol = 0;
        for (Map<Integer, CellVal> row : rowCells) {
            for (Integer c : row.keySet()) {
                maxCol = Math.max(maxCol, c);
            }
        }
        List<List<String>> result = new ArrayList<>();
        for (Map<Integer, CellVal> row : rowCells) {
            List<String> cells = new ArrayList<>(maxCol);
            for (int i = 1; i <= maxCol; i++) {
                CellVal cell = row.get(i);
                if (cell == null || cell.value() == null) {
                    cells.add(null);
                } else if (cell.sharedString()) {
                    int idx = parseIndex(cell.value());
                    cells.add(idx >= 0 && idx < sharedStrings.size() ? sharedStrings.get(idx) : null);
                } else {
                    cells.add(cell.value());
                }
            }
            result.add(cells);
        }
        return result;
    }

    private int parseIndex(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 列字母引用（如 "A"、"AB"）转 1 起始列号 */
    private int columnIndex(String ref) {
        if (ref == null || ref.isEmpty()) {
            return 0;
        }
        int idx = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                idx = idx * 26 + (ch - 'A' + 1);
            } else if (ch >= 'a' && ch <= 'z') {
                idx = idx * 26 + (ch - 'a' + 1);
            } else {
                break;
            }
        }
        return idx;
    }

    private XMLStreamReader newXMLReader(InputStream in) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory.createXMLStreamReader(in, "UTF-8");
    }

    /** 单元格值：原始文本 + 是否为共享字符串引用 */
    private record CellVal(String value, boolean sharedString) {
    }
}
