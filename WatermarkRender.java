import com.example.studentarchives.config.DefaultTemplateHtml;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.service.Fmy.ExportTemplateRenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一次性验证：把带水印/不带水印的 PDF 渲染成 PNG，肉眼对比水印可见性（不属于正式源码） */
public class WatermarkRender {
    public static void main(String[] args) throws Exception {
        ExportTemplateRenderService service = new ExportTemplateRenderService(null, new ObjectMapper());
        ExportTemplate template = new ExportTemplate();
        template.setTemplateContent(DefaultTemplateHtml.CAREER_PLAN);
        template.setPaperSize("A4");
        template.setOrientation(1);
        template.setHeaderHtml("职业规划");
        template.setFooterHtml(DefaultTemplateHtml.DEFAULT_FOOTER);
        template.setMarginConfig(toJson(DefaultTemplateHtml.DEFAULT_MARGIN));
        template.setFontConfig(toJson(DefaultTemplateHtml.DEFAULT_FONT));
        template.setWatermarkConfig(toJson(DefaultTemplateHtml.DEFAULT_WATERMARK));
        template.setPageConfig(toJson(DefaultTemplateHtml.DEFAULT_PAGE));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studentName", "测试学生");
        context.put("userNo", "2026001");
        context.put("semesterName", "2025-2026学年");
        context.put("title", "职业规划");
        context.put("content", "测试内容");
        context.put("progressRate", 50);
        context.put("statusLabel", "已提交");
        context.put("goals", List.of());

        byte[] withWm = service.renderTemplate(template, context, true);
        byte[] noWm = service.renderTemplate(template, context, false);

        renderPng("watermark_with.png", withWm);
        renderPng("watermark_without.png", noWm);
        System.out.println("PNG written.");
    }

    private static void renderPng(String path, byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage img = renderer.renderImageWithDPI(0, 80);
            ImageIO.write(img, "png", new File(path));
        }
    }

    private static String toJson(Object value) throws Exception {
        return new ObjectMapper().writeValueAsString(value);
    }
}
