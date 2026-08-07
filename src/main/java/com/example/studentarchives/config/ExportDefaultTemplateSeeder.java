package com.example.studentarchives.config;

import com.example.studentarchives.entity.export.ExportTemplate;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.repository.ExportTemplateRepository;
import com.example.studentarchives.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 默认导出模板种子器
 * <p>
 * 应用启动时幂等写入 <b>export_templates</b> 的默认模板行（template_mode=2 自由模板、is_default=1），
 * 使学生端档案导出、职业规划下载优先走模板渲染。模板内容来自 {@link DefaultTemplateHtml} 常量
 * （不再依赖 classpath 资源文件），页眉/页脚/纸张/边距/字体/水印等配置列一并填充。
 * <p>
 * 对已存在的旧种子行做升级补全：仅填充为空的配置列，不覆盖用户自定义值。
 * <p>
 * 当前播种三类默认模板：
 * <ul>
 *     <li>{@code student_archive_default} / {@code student_archive} — 学生成长档案导出（4.17）；</li>
 *     <li>{@code career_plan_default} / {@code career_plan} — 职业规划文件（4.5 下载）；</li>
 *     <li>{@code resume_default} / {@code resume} — 个人简历导出。</li>
 * </ul>
 * <p>
 * 为什么不用 Flyway 迁移：模板表 created_by 外键关联 users，而 users 数据由手动种子脚本
 * （seed_students.sql）灌入，全新数据库上 Flyway 迁移阶段用户表为空，INSERT 会触发外键失败。
 * 因此改为启动时探测：无用户时跳过并告警，导入用户数据后重启应用即可自动补建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportDefaultTemplateSeeder implements ApplicationRunner {

    /** 初始化覆盖的学校（单校场景，默认学校 id=1，与 seed_students.sql 一致） */
    private static final long DEFAULT_SCHOOL_ID = 1L;

    /** 默认页眉（按模板不同单独指定） */
    private static final String STUDENT_ARCHIVE_HEADER = "学生成长档案";
    private static final String CAREER_PLAN_HEADER = "职业规划";
    private static final String RESUME_HEADER = "个人简历";

    /** 模板种子描述 */
    private record TemplateSeed(String code, String exportType, String templateName,
                                List<Map<String, Object>> fieldsConfig, String headerHtml, String templateContent) {
    }

    /** 默认模板列表（可扩展新增业务类型） */
    private static final List<TemplateSeed> DEFAULT_TEMPLATES = List.of(
            new TemplateSeed(
                    "student_archive_default", "student_archive", "学生成长档案默认模板",
                    List.of(
                            Map.of("key", "studentName", "name", "姓名", "source", "variable", "path", "users.name"),
                            Map.of("key", "userNo", "name", "学号", "source", "variable", "path", "users.user_no"),
                            Map.of("key", "scores", "name", "画像分数", "source", "table", "path", "portrait_evaluation_scores", "isList", true),
                            Map.of("key", "awards", "name", "获奖记录", "source", "table", "path", "award_summaries", "isList", true),
                            Map.of("key", "practices", "name", "实践经历", "source", "table", "path", "archives", "isList", true),
                            Map.of("key", "careerPlans", "name", "成长规划", "source", "table", "path", "career_plans", "isList", true)
                    ),
                    STUDENT_ARCHIVE_HEADER,
                    DefaultTemplateHtml.STUDENT_ARCHIVE),
            new TemplateSeed(
                    "career_plan_default", "career_plan", "职业规划默认模板",
                    List.of(
                            Map.of("key", "studentName", "name", "姓名", "source", "variable", "path", "users.name"),
                            Map.of("key", "userNo", "name", "学号", "source", "variable", "path", "users.user_no"),
                            Map.of("key", "title", "name", "规划标题", "source", "variable", "path", "career_plans.title"),
                            Map.of("key", "content", "name", "规划内容", "source", "variable", "path", "career_plans.content"),
                            Map.of("key", "requirement", "name", "要求/目标", "source", "variable", "path", "career_plans.requirement"),
                            Map.of("key", "progressRate", "name", "整体进度", "source", "variable", "path", "career_plans.progress_rate"),
                            Map.of("key", "goals", "name", "目标", "source", "table", "path", "career_goals", "isList", true),
                            Map.of("key", "reflections", "name", "阶段反思", "source", "table", "path", "career_reflections", "isList", true),
                            Map.of("key", "feedbacks", "name", "教师反馈", "source", "table", "path", "career_plan_feedbacks", "isList", true)
                    ),
                    CAREER_PLAN_HEADER,
                    DefaultTemplateHtml.CAREER_PLAN),
            new TemplateSeed(
                    "resume_default", "resume", "个人简历默认模板",
                    List.of(
                            Map.of("key", "studentName", "name", "姓名", "source", "variable", "path", "users.name"),
                            Map.of("key", "userNo", "name", "学号", "source", "variable", "path", "users.user_no"),
                            Map.of("key", "title", "name", "简历标题", "source", "variable", "path", "student_profiles.title"),
                            Map.of("key", "phone", "name", "手机", "source", "variable", "path", "user_contact_infos.phone"),
                            Map.of("key", "email", "name", "邮箱", "source", "variable", "path", "user_contact_infos.email"),
                            Map.of("key", "schoolName", "name", "学校", "source", "variable", "path", "schools.name"),
                            Map.of("key", "college", "name", "学院", "source", "variable", "path", "colleges.name"),
                            Map.of("key", "major", "name", "专业", "source", "variable", "path", "majors.name"),
                            Map.of("key", "clazz", "name", "班级", "source", "variable", "path", "classes.name"),
                            Map.of("key", "grade", "name", "年级", "source", "variable", "path", "classes.grade"),
                            Map.of("key", "gpas", "name", "学期 GPA", "source", "table", "path", "semester_gpa_summaries", "isList", true),
                            Map.of("key", "awards", "name", "获奖情况", "source", "table", "path", "award_competition_stars", "isList", true),
                            Map.of("key", "skillCategories", "name", "技能与兴趣", "source", "table", "path", "user_interests", "isList", true),
                            Map.of("key", "practices", "name", "实践经历", "source", "table", "path", "archives", "isList", true),
                            Map.of("key", "certificates", "name", "证书", "source", "table", "path", "archive_certificates", "isList", true),
                            Map.of("key", "selfEvaluation", "name", "自我评价", "source", "variable", "path", "student_profiles.self_evaluation")
                    ),
                    RESUME_HEADER,
                    DefaultTemplateHtml.RESUME)
    );

    private final ExportTemplateRepository exportTemplateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (TemplateSeed seed : DEFAULT_TEMPLATES) {
            seedDefaultTemplate(DEFAULT_SCHOOL_ID, seed);
        }
    }

    private void seedDefaultTemplate(Long schoolId, TemplateSeed seed) {
        Optional<ExportTemplate> existing = exportTemplateRepository
                .findBySchoolIdAndTemplateCodeAndStatus(schoolId, seed.code(), 1);
        if (existing.isPresent()) {
            reconcileTemplate(existing.get(), seed);
            return;
        }
        User creator = userRepository.findFirstBySchoolIdOrderByIdAsc(schoolId).orElse(null);
        if (creator == null) {
            log.warn("跳过导出模板初始化（{}）：学校 {} 暂无用户，无法设置 created_by（导入用户数据后重启应用即可）",
                    seed.exportType(), schoolId);
            return;
        }

        ExportTemplate template = new ExportTemplate();
        template.setSchoolId(schoolId);
        template.setTemplateName(seed.templateName());
        template.setTemplateCode(seed.code());
        template.setExportType(seed.exportType());
        template.setScopeType(1);
        template.setFieldsConfig(writeJson(seed.fieldsConfig()));
        template.setFilterConditions("{}");
        template.setTemplateMode(2);
        template.setTemplateContent(seed.templateContent());
        template.setEngineType("html");
        template.setPaperSize("A4");
        template.setOrientation(1);
        template.setHeaderHtml(seed.headerHtml());
        template.setFooterHtml(DefaultTemplateHtml.DEFAULT_FOOTER);
        template.setMarginConfig(writeJson(DefaultTemplateHtml.DEFAULT_MARGIN));
        template.setFontConfig(writeJson(DefaultTemplateHtml.DEFAULT_FONT));
        template.setWatermarkConfig(writeJson(DefaultTemplateHtml.DEFAULT_WATERMARK));
        template.setPageConfig(writeJson(DefaultTemplateHtml.DEFAULT_PAGE));
        template.setVersion(1);
        template.setIsDefault(1);
        template.setStatus(1);
        template.setCreatedBy(creator.getId());
        exportTemplateRepository.save(template);
        log.info("已初始化默认导出模板: school_id={} code={}", schoolId, seed.code());
    }

    /**
     * 升级补全旧种子行：
     * 1. 默认模板（code 以 _default 结尾）的正文与页眉始终跟随代码升级，确保默认样式保持最新；
     * 2. 其他列仅在为空时填充；
     * 3. 水印列额外升级旧版默认配置（见 {@link #isLegacyDefaultWatermark}）。
     */
    private void reconcileTemplate(ExportTemplate template, TemplateSeed seed) {
        boolean changed = false;
        boolean isDefaultTemplate = seed.code() != null && seed.code().endsWith("_default");
        if (isDefaultTemplate) {
            if (!Objects.equals(template.getTemplateContent(), seed.templateContent())) {
                template.setTemplateContent(seed.templateContent());
                template.setVersion(template.getVersion() != null ? template.getVersion() + 1 : 1);
                changed = true;
            }
            if (!Objects.equals(template.getHeaderHtml(), seed.headerHtml())) {
                template.setHeaderHtml(seed.headerHtml());
                changed = true;
            }
        } else if (isBlank(template.getTemplateContent())) {
            template.setTemplateContent(seed.templateContent());
            changed = true;
        }
        if (isBlank(template.getHeaderHtml())) {
            template.setHeaderHtml(seed.headerHtml());
            changed = true;
        }
        if (isBlank(template.getFooterHtml())) {
            template.setFooterHtml(DefaultTemplateHtml.DEFAULT_FOOTER);
            changed = true;
        }
        if (isBlank(template.getMarginConfig())) {
            template.setMarginConfig(writeJson(DefaultTemplateHtml.DEFAULT_MARGIN));
            changed = true;
        }
        if (isBlank(template.getFontConfig())) {
            template.setFontConfig(writeJson(DefaultTemplateHtml.DEFAULT_FONT));
            changed = true;
        }
        if (isBlank(template.getWatermarkConfig()) || isLegacyDefaultWatermark(template.getWatermarkConfig())) {
            template.setWatermarkConfig(writeJson(DefaultTemplateHtml.DEFAULT_WATERMARK));
            changed = true;
        }
        if (isBlank(template.getPageConfig())) {
            template.setPageConfig(writeJson(DefaultTemplateHtml.DEFAULT_PAGE));
            changed = true;
        }
        if (changed) {
            exportTemplateRepository.save(template);
            log.info("已补全默认导出模板配置列: id={} code={}", template.getId(), seed.code());
        }
    }

    /**
     * 是否为旧版默认水印配置（text=学生成长档案 / opacity=0.06 / fontSize=24 / 未写 color）。
     * 旧默认未携带 color，渲染器会落入深蓝兜底色，重启后升级为浅色小字号新默认；
     * 仅精确匹配旧默认，用户自定义的水印配置不会被覆盖。
     */
    private boolean isLegacyDefaultWatermark(String watermarkConfig) {
        try {
            JsonNode node = objectMapper.readTree(watermarkConfig);
            if (!"学生成长档案".equals(node.path("text").asText())) {
                return false;
            }
            if (Math.abs(node.path("opacity").asDouble(0) - 0.06) > 0.0001) {
                return false;
            }
            // 历史种子数据实际写入过 fontSize=60 的旧默认（无 color 字段），一并识别并升级
            int fontSize = node.path("fontSize").asInt(0);
            if (fontSize != 24 && fontSize != 60) {
                return false;
            }
            return !node.has("color");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 JSON 失败", e);
            return "{}";
        }
    }
}
