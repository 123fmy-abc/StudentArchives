package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ExportTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 导出模板渲染服务
 * <p>
 * 基于 <b>export_templates</b> 配置表（自由模板模式 template_mode=2）渲染 PDF：
 * <ol>
 *     <li>从配置列组装最终 HTML——template_content 提供正文骨架，页眉/页脚/纸张/边距/字体/水印
 *         由配置列（header_html/footer_html/paper_size/orientation/margin_config/font_config/
 *         watermark_config）驱动，通过 <style> 注入合并（后注入者级联覆盖正文内同名规则）；</li>
 *     <li>做 Mustache 占位符替换：{{key}} 插值（默认 HTML 转义）、{{#list}}...{{/list}} 列表循环、
 *         {{^list}}...{{/list}} 空值分支；</li>
 *     <li>通过 openhtmltopdf 将渲染后的 HTML 转为 PDF（纯 JVM，无外部服务）；</li>
 *     <li>自动探测并注册中文字体（统一别名 "CJK"），保证中文正常显示。</li>
 * </ol>
 * 页眉/页脚内容仅支持纯文本 + 页码占位符 <b>{{page}}</b>（当前页）/<b>{{pages}}</b>（总页数），
 * 渲染前会转换为 CSS counter(page)/counter(pages) 供 @page margin box 使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTemplateRenderService {

    /** 中文字体统一注册别名，模板 CSS font-family 首选该别名 */
    private static final String CJK_FAMILY = "CJK";

    /** 探测到可用的中文字体数量达到该值时停止继续探测 */
    private static final int FONT_LIMIT = 2;

    /** 常见中文字体候选（按优先级），供各运行环境自动探测 */
    private static final List<CjkFontCandidate> FONT_CANDIDATES = List.of(
            new CjkFontCandidate("C:\\Windows\\Fonts\\msyh.ttc", "Microsoft YaHei"),
            new CjkFontCandidate("C:\\Windows\\Fonts\\msyh.ttf", "Microsoft YaHei"),
            new CjkFontCandidate("C:\\Windows\\Fonts\\simhei.ttf", "SimHei"),
            new CjkFontCandidate("C:\\Windows\\Fonts\\simsun.ttc", "SimSun"),
            new CjkFontCandidate("/System/Library/Fonts/PingFang.ttc", "PingFang SC"),
            new CjkFontCandidate("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", "Noto Sans CJK SC"),
            new CjkFontCandidate("/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc", "Noto Sans CJK SC"),
            new CjkFontCandidate("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc", "WenQuanYi Micro Hei")
    );

    private static final MustacheFactory MUSTACHE = new DefaultMustacheFactory();

    /** 页眉/页脚中的页码占位符（渲染前转换为 CSS counter，避免被 Mustache 当作变量吞掉） */
    private static final Pattern PAGE_TOKEN = Pattern.compile("\\{\\{page\\}\\}|\\{\\{pages\\}\\}");

    private final ExportTemplateRepository exportTemplateRepository;
    private final ObjectMapper objectMapper;

    /** 可选：通过配置指定中文字体文件路径（优先级最高，覆盖自动探测） */
    @Value("${student.export.font-path:}")
    private String fontPathOverride;

    /** 首次渲染后缓存已探测到的可用字体，避免每次渲染重新探测 */
    private volatile List<CjkFontCandidate> resolvedFonts;

    /**
     * 解析指定学校、业务类型的默认导出模板：
     * 优先取 is_default=1 的启用模板，未配置默认模板时兜底取首个启用模板。
     */
    public ExportTemplate resolveDefaultTemplate(Long schoolId, String exportType) {
        return exportTemplateRepository
                .findFirstBySchoolIdAndExportTypeAndIsDefaultAndStatusOrderByVersionDesc(
                        schoolId, exportType, 1, 1)
                .orElseGet(() -> exportTemplateRepository
                        .findBySchoolIdAndExportTypeAndStatusOrderByIdAsc(schoolId, exportType, 1)
                        .stream().findFirst().orElse(null));
    }

    /**
     * 按模板渲染 PDF：配置列合并 → Mustache 占位符替换 → openhtmltopdf 转 PDF。
     *
     * @param template         导出模板（template_mode=2，template_content 为正文 HTML 骨架）
     * @param context          Mustache 渲染上下文（插值 / 列表 / 空值分支数据）
     * @param watermarkEnabled 是否添加屏幕可见、打印隐藏的水印
     * @return PDF 字节数组
     */
    public byte[] renderTemplate(ExportTemplate template, Map<String, Object> context, boolean watermarkEnabled) {
        String body = template.getTemplateContent();
        if (body == null || body.isBlank()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "导出模板内容为空，请检查模板配置");
        }
        String html = assembleDocument(template, body);
        String rendered = renderMustache(html, context);
        byte[] pdfBytes = htmlToPdf(rendered);
        if (watermarkEnabled) {
            WatermarkConfig watermark = resolveWatermarkConfig(template, context);
            if (watermark != null) {
                pdfBytes = applyPrintHiddenWatermark(pdfBytes, watermark);
            }
        }
        return pdfBytes;
    }

    // ==================== 配置列合并 ====================

    /**
     * 把模板配置列合并进正文，得到最终 HTML 文档：
     * <ul>
     *     <li>生成的 {@code @page} 规则（纸张/方向/边距/页眉/页脚）与 {@code body} 字体规则注入到
     *         {@code </head>} 之前——因注入在正文自带样式之后，CSS 级联会覆盖正文内同名规则；</li>
     * </ul>
     */
    private String assembleDocument(ExportTemplate template, String body) {
        String style = buildPageRule(template) + buildFontRule(template);
        String html = body;
        if (!style.isBlank()) {
            html = injectBefore(html, "</head>", "<style>\n" + style + "</style>\n");
        }
        return html;
    }

    /**
     * 生成 @page 规则：纸张（paper_size + orientation）、边距（margin_config）、
     * 页眉（@top-right，header_html）、页脚（@bottom-center，footer_html）。
     */
    private String buildPageRule(ExportTemplate template) {
        String size = (template.getPaperSize() != null && !template.getPaperSize().isBlank())
                ? template.getPaperSize() : "A4";
        Integer orientation = template.getOrientation();
        StringBuilder sb = new StringBuilder();
        sb.append("@page {\n");
        sb.append("  size: ").append(size);
        if (orientation != null && orientation == 2) {
            sb.append(" landscape");
        }
        sb.append(";\n");
        sb.append("  margin: ").append(buildMargin(template.getMarginConfig())).append(";\n");

        String header = template.getHeaderHtml();
        if (header != null && !header.isBlank()) {
            sb.append("  @top-right {\n")
                    .append("    content: ").append(buildMarginContent(header)).append(";\n")
                    .append("    font-family: \"").append(CJK_FAMILY).append("\", sans-serif;\n")
                    .append("    font-size: 8pt;\n")
                    .append("    color: #9aa5b5;\n")
                    .append("  }\n");
        }
        String footer = template.getFooterHtml();
        if (footer != null && !footer.isBlank()) {
            sb.append("  @bottom-center {\n")
                    .append("    content: ").append(buildMarginContent(footer)).append(";\n")
                    .append("    font-family: \"").append(CJK_FAMILY).append("\", sans-serif;\n")
                    .append("    font-size: 8pt;\n")
                    .append("    color: #9aa5b5;\n")
                    .append("  }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 解析 margin_config（{"top":20,"right":16,"bottom":18,"left":16}，单位 mm），未配置时用默认值。
     */
    private String buildMargin(String marginConfigJson) {
        double top = 20, right = 16, bottom = 18, left = 16;
        if (marginConfigJson != null && !marginConfigJson.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(marginConfigJson);
                top = numberValue(node, "top", top);
                right = numberValue(node, "right", right);
                bottom = numberValue(node, "bottom", bottom);
                left = numberValue(node, "left", left);
            } catch (Exception e) {
                log.warn("解析 margin_config 失败，使用默认边距: {}", e.getMessage());
            }
        }
        return mm(top) + "mm " + mm(right) + "mm " + mm(bottom) + "mm " + mm(left) + "mm";
    }

    /**
     * 生成 body 字体规则（font_config：family/size/color/lineHeight，均为 CSS 可直接使用的字面量）。
     */
    private String buildFontRule(ExportTemplate template) {
        String family = "\"" + CJK_FAMILY + "\", \"Noto Sans CJK SC\", \"Microsoft YaHei\", \"SimHei\", sans-serif";
        String size = "10.5pt";
        String color = "#2c3e50";
        String lineHeight = "1.65";
        String fontConfig = template.getFontConfig();
        if (fontConfig != null && !fontConfig.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(fontConfig);
                if (node.hasNonNull("family")) {
                    family = node.get("family").asText();
                }
                if (node.hasNonNull("size")) {
                    size = node.get("size").asText();
                }
                if (node.hasNonNull("color")) {
                    color = node.get("color").asText();
                }
                if (node.hasNonNull("lineHeight")) {
                    lineHeight = node.get("lineHeight").asText();
                }
            } catch (Exception e) {
                log.warn("解析 font_config 失败，使用默认字体: {}", e.getMessage());
            }
        }
        return "body {\n"
                + "  font-family: " + family + ";\n"
                + "  font-size: " + size + ";\n"
                + "  color: " + color + ";\n"
                + "  line-height: " + lineHeight + ";\n"
                + "  margin: 0;\n"
                + "}\n";
    }

    /**
     * 把页眉/页脚纯文本转成 @page margin box 的 content 值：{{page}}→counter(page)、{{pages}}→counter(pages)，
     * 其余文本片段作为带引号字面量拼接（margin box content 仅支持 CSS 字符串与计数器，不支持 HTML）。
     */
    private String buildMarginContent(String text) {
        String clean = text.replaceAll("<[^>]+>", "").trim();
        Matcher matcher = PAGE_TOKEN.matcher(clean);
        StringBuilder css = new StringBuilder();
        int last = 0;
        boolean first = true;
        while (matcher.find()) {
            String literal = clean.substring(last, matcher.start()).trim();
            if (!literal.isEmpty()) {
                if (!first) {
                    css.append(' ');
                }
                css.append('"').append(escapeCssString(literal)).append('"');
                first = false;
            }
            if (!first) {
                css.append(' ');
            }
            css.append("{{page}}".equals(matcher.group()) ? "counter(page)" : "counter(pages)");
            first = false;
            last = matcher.end();
        }
        String tail = clean.substring(last).trim();
        if (!tail.isEmpty()) {
            if (!first) {
                css.append(' ');
            }
            css.append('"').append(escapeCssString(tail)).append('"');
        }
        return css.toString();
    }

    /** 水印配置 */
    private record WatermarkConfig(String text, double opacity, double fontSize, String color) {
    }

    /**
     * 解析模板水印配置。支持 {{studentName}} 占位符，从上下文中取学生姓名；
     * 解析失败或文字为空时返回 null，跳过水印。
     */
    private WatermarkConfig resolveWatermarkConfig(ExportTemplate template, Map<String, Object> context) {
        String config = template.getWatermarkConfig();
        if (config == null || config.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(config);
            if (!node.hasNonNull("text")) {
                return null;
            }
            String rawText = node.get("text").asText();
            String text = resolveWatermarkText(rawText, context);
            if (text == null || text.isBlank()) {
                return null;
            }
            double opacity = numberValue(node, "opacity", 0.06);
            double fontSize = numberValue(node, "fontSize", 12);
            String color = node.hasNonNull("color") ? node.get("color").asText() : "#c0c4cc";
            return new WatermarkConfig(text, opacity, fontSize, color);
        } catch (Exception e) {
            log.warn("解析 watermark_config 失败，跳过水印: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析水印文字：支持 Mustache 占位符 {{studentName}}。
     * 非学生档案场景保留原配置文字。
     */
    private String resolveWatermarkText(String rawText, Map<String, Object> context) {
        if (rawText == null) {
            return null;
        }
        String text = rawText.trim();
        if ("{{studentName}}".equals(text) && context != null && context.get("studentName") != null) {
            return String.valueOf(context.get("studentName"));
        }
        return text;
    }

    /**
     * 用 PDFBox 给 PDF 每一页添加平铺水印，并将水印放入 Optional Content Group。
     * 设置 OCG 的 Usage.PrintState = OFF，实现屏幕显示、打印隐藏。
     * <p>
     * 水印属于附加保护：任何水印绘制失败（如找不到可渲染中文的字体）都只跳过水印、
     * 返回原 PDF，绝不因水印失败阻断整个导出。
     */
    private byte[] applyPrintHiddenWatermark(byte[] pdfBytes, WatermarkConfig watermark) {
        List<TrueTypeCollection> openTtcCollections = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(new java.io.ByteArrayInputStream(pdfBytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDOptionalContentGroup watermarkGroup = new PDOptionalContentGroup("Watermark");
            PDOptionalContentProperties ocProps = doc.getDocumentCatalog().getOCProperties();
            if (ocProps == null) {
                ocProps = new PDOptionalContentProperties();
                doc.getDocumentCatalog().setOCProperties(ocProps);
            }
            ocProps.addGroup(watermarkGroup);
            ocProps.setBaseState(PDOptionalContentProperties.BaseState.ON);

            COSDictionary usage = new COSDictionary();
            COSDictionary print = new COSDictionary();
            print.setItem(COSName.getPDFName("PrintState"), COSName.getPDFName("OFF"));
            usage.setItem(COSName.getPDFName("Print"), print);
            watermarkGroup.getCOSObject().setItem(COSName.getPDFName("Usage"), usage);

            PDFont font = loadWatermarkFont(doc, openTtcCollections);
            // 兜底：最终字体（Helvetica）无法渲染中文且水印文本含非 ASCII 字符时跳过水印
            if (font == PDType1Font.HELVETICA && !isAsciiOnly(watermark.text())) {
                log.warn("未找到支持中文的水印字体，本次导出跳过水印");
                return pdfBytes;
            }
            Color color = parseWatermarkColor(watermark.color(), watermark.opacity());
            float fontSize = (float) watermark.fontSize();

            for (PDPage page : doc.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.beginMarkedContent(COSName.OC, watermarkGroup);

                    int rows = 6;
                    int cols = 3;
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            float x = pageWidth * (c + 0.5f) / cols;
                            float y = pageHeight * (rows - r - 0.5f) / rows;

                            cs.saveGraphicsState();
                            cs.beginText();
                            cs.setFont(font, fontSize);
                            cs.setNonStrokingColor(color);
                            cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(-30), x, y));
                            cs.showText(watermark.text());
                            cs.endText();
                            cs.restoreGraphicsState();
                        }
                    }

                    cs.endMarkedContent();
                }
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("添加打印隐藏水印失败，本次导出跳过水印", e);
            return pdfBytes;
        } finally {
            // PDFBox 约束：TrueTypeCollection 必须在 doc.save() 之后关闭，统一在这里释放
            for (TrueTypeCollection ttc : openTtcCollections) {
                try {
                    ttc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 加载可用于水印绘制的中文字体，失败时回退到 Helvetica。
     * <p>
     * 直接 {@code PDType0Font.load(doc, File)} 不支持 TrueType Collection（.ttc）——
     * 而 Docker（font-noto-cjk）与 Windows（微软雅黑/宋体）部署环境的中文字体都是 .ttc。
     * 这里对 .ttc 用 {@link TrueTypeCollection} 取出第一个字体再加载。
     *
     * @param doc                目标 PDF 文档
     * @param openTtcCollections 已打开的 .ttc 集合（由调用方在 doc.save() 之后统一关闭）
     */
    private PDFont loadWatermarkFont(PDDocument doc, List<TrueTypeCollection> openTtcCollections) {
        for (CjkFontCandidate candidate : resolveFonts()) {
            File file = new File(candidate.file());
            try {
                if (isTtcFile(file)) {
                    TrueTypeCollection ttc = new TrueTypeCollection(file);
                    openTtcCollections.add(ttc);
                    TrueTypeFont ttf = ttc.getFontByName(candidate.family());
                    if (ttf == null) {
                        ttf = firstFontOf(ttc);
                    }
                    return PDType0Font.load(doc, ttf, true);
                }
                return PDType0Font.load(doc, file);
            } catch (Exception e) {
                log.debug("水印字体加载失败，尝试下一个: {}", candidate.file());
            }
        }
        try {
            File fallback = new File("C:\\Windows\\Fonts\\msyh.ttc");
            if (fallback.isFile()) {
                TrueTypeCollection ttc = new TrueTypeCollection(fallback);
                openTtcCollections.add(ttc);
                TrueTypeFont ttf = firstFontOf(ttc);
                return PDType0Font.load(doc, ttf, true);
            }
        } catch (Exception ignored) {
        }
        return PDType1Font.HELVETICA;
    }

    /**
     * 取 TrueType Collection 中第一个字体（fontbox 的 getFontAtIndex 为包级私有，无法直接调用）。
     * 对中文水印而言集合内任意一个 CJK 字面都能渲染中文，取第一个即可。
     */
    private TrueTypeFont firstFontOf(TrueTypeCollection ttc) throws IOException {
        TrueTypeFont[] first = new TrueTypeFont[1];
        ttc.processAllFonts(font -> {
            if (first[0] == null) {
                first[0] = font;
            }
        });
        return first[0];
    }

    /** 是否为 TrueType Collection（.ttc）字体文件 */
    private boolean isTtcFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".ttc");
    }

    /** 是否全部为 ASCII 可打印字符（Helvetica/WinAnsi 能渲染的字符集） */
    private boolean isAsciiOnly(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /** 解析水印颜色与透明度，无法解析时回退浅灰。 */
    private Color parseWatermarkColor(String color, double opacity) {
        int r = 192, g = 196, b = 204;
        String c = color == null ? "" : color.trim();
        if (c.startsWith("#")) {
            String hex = c.substring(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
            }
            if (hex.length() == 6) {
                try {
                    r = Integer.parseInt(hex.substring(0, 2), 16);
                    g = Integer.parseInt(hex.substring(2, 4), 16);
                    b = Integer.parseInt(hex.substring(4, 6), 16);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        float alpha = (float) Math.max(0, Math.min(1, opacity));
        // java.awt.Color 没有 Color(int,int,int,float) 构造：这里必须把 0-255 的 RGB 归一化到 0.0-1.0，
        // 否则 new Color(r, g, b, alpha) 会被 javac 解析成 Color(float,float,float,float)，0-255 数值越界抛异常。
        return new Color(r / 255f, g / 255f, b / 255f, alpha);
    }

    /** 读取 JSON 数字节点，缺失/非法时返回默认值 */
    private double numberValue(JsonNode node, String field, double defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isMissingNode() || !value.isNumber()) {
            return defaultValue;
        }
        return value.asDouble();
    }

    /** 数字转 CSS 长度：整数省略小数位（20.0 → "20"） */
    private String mm(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String escapeCssString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** 把 content 插入到 marker（小写匹配）之前；找不到 marker 时插到文档开头，保证容错 */
    private String injectBefore(String html, String marker, String content) {
        int idx = html.toLowerCase().indexOf(marker);
        if (idx >= 0) {
            return html.substring(0, idx) + content + html.substring(idx);
        }
        return content + html;
    }

    // ==================== Mustache 渲染 ====================

    /**
     * Mustache 占位符替换：{{key}} 默认 HTML 转义；{{#list}} / {{^list}} / {{/list}} 支持循环与空值分支。
     */
    private String renderMustache(String templateHtml, Map<String, Object> context) {
        try {
            Mustache mustache = MUSTACHE.compile(new StringReader(templateHtml), "export-template");
            StringWriter writer = new StringWriter(templateHtml.length() * 2);
            mustache.execute(writer, context).flush();
            return writer.toString();
        } catch (Exception e) {
            log.error("导出模板 Mustache 渲染失败", e);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "导出模板渲染失败");
        }
    }

    // ==================== HTML → PDF ====================

    /**
     * HTML → PDF（openhtmltopdf），渲染前注册探测到的中文字体。
     */
    private byte[] htmlToPdf(String html) {
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.withHtmlContent(html, "");
            boolean cjkRegistered = false;
            for (CjkFontCandidate font : resolveFonts()) {
                if (!cjkRegistered) {
                    builder.useFont(new File(font.file()), CJK_FAMILY);
                    cjkRegistered = true;
                }
                builder.useFont(new File(font.file()), font.family());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出模板 HTML 转 PDF 失败", e);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "导出文件生成失败");
        }
    }

    /**
     * 探测可用的中文字体文件（含配置覆盖项）。每个候选先用最小渲染自测，只有能正常出 PDF 的字体才采用，
     * 避免个别损坏/不支持的字体文件导致正式渲染整体失败。结果缓存复用。
     */
    private List<CjkFontCandidate> resolveFonts() {
        List<CjkFontCandidate> cached = resolvedFonts;
        if (cached != null) {
            return cached;
        }
        List<CjkFontCandidate> candidates = new ArrayList<>();
        if (fontPathOverride != null && !fontPathOverride.isBlank()) {
            candidates.add(new CjkFontCandidate(fontPathOverride, "OverrideFont"));
        }
        candidates.addAll(FONT_CANDIDATES);

        List<CjkFontCandidate> usable = new ArrayList<>();
        for (CjkFontCandidate candidate : candidates) {
            File file = new File(candidate.file());
            if (!file.isFile()) {
                continue;
            }
            if (isFontUsable(file, candidate.family())) {
                usable.add(candidate);
                if (usable.size() >= FONT_LIMIT) {
                    break;
                }
            }
        }
        if (usable.isEmpty()) {
            log.warn("未找到可用的中文字体，导出 PDF 中文可能无法正常显示。"
                    + "可在 application.yml 配置 student.export.font-path，或部署环境安装中文字体（如 Noto Sans CJK）。");
        }
        resolvedFonts = usable;
        return usable;
    }

    /**
     * 用最小 HTML 自测字体是否可被 openhtmltopdf 正常加载渲染。
     */
    private boolean isFontUsable(File file, String family) {
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(file, family);
            builder.withHtmlContent("<html><body>测试</body></html>", "");
            builder.toStream(new ByteArrayOutputStream());
            builder.run();
            return true;
        } catch (Exception e) {
            log.debug("导出字体不可用，跳过 {}: {}", file, e.getMessage());
            return false;
        }
    }

    /**
     * 中文字体候选：字体文件路径 + CSS 字体族名
     */
    private record CjkFontCandidate(String file, String family) {
    }
}
