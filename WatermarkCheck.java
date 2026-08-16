import com.example.studentarchives.config.DefaultTemplateHtml;
import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.service.Fmy.ExportTemplateRenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一次性验证：职业规划 PDF 水印是否真的被画进文件（编译后直接跑，不属于正式源码） */
public class WatermarkCheck {
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

        System.out.println("=== watermarkEnabled=true  -> size=" + withWm.length);
        checkPdf("watermarkEnabled=true ", withWm);
        System.out.println("=== watermarkEnabled=false -> size=" + noWm.length);
        checkPdf("watermarkEnabled=false", noWm);
    }

    private static void checkPdf(String label, byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDOptionalContentProperties oc = doc.getDocumentCatalog().getOCProperties();
            boolean hasOcg = false;
            if (oc != null) {
                for (PDOptionalContentGroup group : oc.getOptionalContentGroups()) {
                    if ("Watermark".equals(group.getName())) {
                        hasOcg = true;
                    }
                }
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            System.out.println(label + " -> 页面数=" + doc.getNumberOfPages()
                    + ", 存在Watermark图层=" + hasOcg
                    + ", 含水印文字[测试学生]=" + text.contains("测试学生"));
        }
    }

    private static String toJson(Object value) throws Exception {
        return new ObjectMapper().writeValueAsString(value);
    }
}
