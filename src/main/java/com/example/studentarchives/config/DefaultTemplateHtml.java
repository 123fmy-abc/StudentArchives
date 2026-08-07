package com.example.studentarchives.config;

import java.util.Map;

/**
 * 默认导出模板 HTML 内容与共享默认配置
 * <p>
 * 作为模板表种子数据写入 {@code export_templates.template_content}，不再依赖 classpath 资源文件，
 * 模板表是唯一配置来源。页面布局配置（纸张/边距/页眉/页脚/字体/水印）由配置列驱动，渲染器在运行时合并。
 * <p>
 * 注意：<b>不要在此处编写内联 {@code @page} 与 {@code body} 字体规则</b>——纸张/边距/页眉/页脚/字体/水印
 * 均由配置列（paper_size、orientation、margin_config、header_html、footer_html、font_config、
 * watermark_config）驱动，渲染器会在运行时把它们合并进最终 HTML。
 */
public final class DefaultTemplateHtml {

    // ==================== 共享默认配置（种子器与内存兜底模板复用） ====================

    /** 页脚（支持 {{page}} 当前页、{{pages}} 总页数占位符）；页眉按模板不同单独指定 */
    public static final String DEFAULT_FOOTER = "第 {{page}} 页 / 共 {{pages}} 页";

    /** 默认页边距（mm） */
    public static final Map<String, Object> DEFAULT_MARGIN = Map.of("top", 20, "right", 16, "bottom", 18, "left", 16);

    /** 默认字体 */
    public static final Map<String, Object> DEFAULT_FONT = Map.of(
            "family", "\"CJK\", \"Noto Sans CJK SC\", \"Microsoft YaHei\", \"SimHei\", sans-serif",
            "size", "10.5pt",
            "color", "#2c3e50",
            "lineHeight", "1.65");

    /** 默认水印（学生姓名占位符，渲染时解析为当前学生） */
    public static final Map<String, Object> DEFAULT_WATERMARK = Map.of(
            "text", "{{studentName}}",
            "opacity", 0.06,
            "fontSize", 12,
            "color", "#c0c4cc");

    /** 默认页面 */
    public static final Map<String, Object> DEFAULT_PAGE = Map.of("size", "A4", "orientation", "portrait");

    // ==================== 学生成长档案模板 ====================

    public static final String STUDENT_ARCHIVE = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8"/>
                <title>学生成长档案</title>
                <style>
                    .cover {
                        text-align: center;
                        padding: 56px 0 28px 0;
                        border-bottom: 3px solid #1f6feb;
                        margin-bottom: 26px;
                    }
                    .cover-title {
                        font-size: 26pt;
                        font-weight: bold;
                        color: #1f6feb;
                        letter-spacing: 6px;
                    }
                    .cover-sub {
                        margin-top: 10px;
                        font-size: 11pt;
                        color: #6b7a90;
                    }
                    .cover-meta {
                        margin-top: 22px;
                        font-size: 12pt;
                        color: #2c3e50;
                    }
                    h1 {
                        font-size: 14pt;
                        color: #ffffff;
                        background: #1f6feb;
                        padding: 6px 12px;
                        border-radius: 4px;
                        margin: 22px 0 12px 0;
                        letter-spacing: 1px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 4px 0 8px 0;
                    }
                    th, td {
                        border: 1px solid #d5dbe3;
                        padding: 6px 10px;
                        font-size: 10pt;
                    }
                    th {
                        background: #eef3fc;
                        color: #1f4e99;
                        font-weight: bold;
                        text-align: left;
                    }
                    .kv td:first-child {
                        width: 150px;
                        background: #f7f9fc;
                        font-weight: bold;
                        color: #45526b;
                    }
                    .empty {
                        color: #9aa5b5;
                        font-style: italic;
                        padding: 6px 0;
                    }
                    .foot {
                        margin-top: 30px;
                        padding-top: 10px;
                        border-top: 1px solid #e5e9f0;
                        font-size: 9pt;
                        color: #9aa5b5;
                        text-align: right;
                    }
                </style>
            </head>
            <body>

            <div class="cover">
                <div class="cover-title">学生成长档案</div>
                <div class="cover-sub">{{schoolName}} · 数据版本 {{dataVersion}}</div>
                <div class="cover-meta">{{studentName}}（{{userNo}}）</div>
                <div class="cover-meta">{{college}}{{#college}} · {{/college}}{{major}}{{#major}} · {{/major}}{{clazz}}</div>
            </div>

            {{#showAcademicInfo}}
            <h1>一、学籍信息</h1>
            <table class="kv">
                <tr><td>姓名</td><td>{{studentName}}</td></tr>
                <tr><td>学号</td><td>{{userNo}}</td></tr>
                <tr><td>学院</td><td>{{college}}</td></tr>
                <tr><td>专业</td><td>{{major}}</td></tr>
                <tr><td>班级</td><td>{{clazz}}</td></tr>
                <tr><td>年级</td><td>{{grade}}</td></tr>
                <tr><td>政治面貌</td><td>{{politicalStatus}}</td></tr>
                <tr><td>邮箱</td><td>{{email}}</td></tr>
                <tr><td>手机</td><td>{{phone}}</td></tr>
            </table>
            {{/showAcademicInfo}}

            {{#showDimensionScores}}
            <h1>二、画像分数</h1>
            {{#scores}}
            <table>
                <tr><th>能力维度</th><th>得分</th><th>目标分</th><th>差距</th></tr>
                <tr><td>{{dimensionName}}</td><td>{{score}}</td><td>{{targetScore}}</td><td>{{gap}}</td></tr>
            </table>
            {{/scores}}
            {{^scores}}
            <div class="empty">暂无画像分数数据</div>
            {{/scores}}
            {{/showDimensionScores}}

            {{#showAwards}}
            <h1>三、获奖记录</h1>
            <table>
                <tr><th>奖项类别</th><th>数量</th><th>最高等级</th><th>最近获奖时间</th></tr>
                {{#awards}}
                <tr><td>{{category}}</td><td>{{totalCount}} 项</td><td>{{maxLevel}}</td><td>{{latestAt}}</td></tr>
                {{/awards}}
            </table>
            {{^awards}}
            <div class="empty">暂无获奖记录</div>
            {{/awards}}
            {{/showAwards}}

            {{#showPractices}}
            <h1>四、实践经历（已审核通过）</h1>
            <table>
                <tr><th>实践标题</th><th>类型</th><th>获得时间</th></tr>
                {{#practices}}
                <tr><td>{{title}}</td><td>{{archiveType}}</td><td>{{obtainedAt}}</td></tr>
                {{/practices}}
            </table>
            {{^practices}}
            <div class="empty">暂无已通过的实践经历</div>
            {{/practices}}
            {{/showPractices}}

            {{#showCareerPlans}}
            <h1>五、成长规划</h1>
            <table>
                <tr><th>规划标题</th><th>完成进度</th></tr>
                {{#careerPlans}}
                <tr><td>{{title}}</td><td>{{progressRate}}%</td></tr>
                {{/careerPlans}}
            </table>
            {{^careerPlans}}
            <div class="empty">暂无成长规划</div>
            {{/careerPlans}}
            {{/showCareerPlans}}

            <div class="foot">导出时间：{{exportTime}}</div>

            </body>
            </html>
            """;

    // ==================== 职业规划模板 ====================

    /**
     * 单条职业规划的独立文档模板（4.5 下载职业规划文件）。
     * <p>
     * 数据按「目标→行动→里程碑」嵌套装配：{{#goals}} 内 {{#actions}} 内 {{#milestones}}；
     * 空数据统一走 {{^goals}}/{{^actions}}/{{^reflections}}/{{^feedbacks}} 分支显示「暂无」，
     * 保证章节编号稳定。封面取学生姓名/学号/学期，不依赖学院/专业/班级数据。
     */
    public static final String CAREER_PLAN = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8"/>
                <title>职业规划</title>
                <style>
                    .cover {
                        text-align: center;
                        padding: 56px 0 28px 0;
                        border-bottom: 3px solid #1f6feb;
                        margin-bottom: 26px;
                    }
                    .cover-title {
                        font-size: 26pt;
                        font-weight: bold;
                        color: #1f6feb;
                        letter-spacing: 6px;
                    }
                    .cover-sub {
                        margin-top: 10px;
                        font-size: 11pt;
                        color: #6b7a90;
                    }
                    .cover-meta {
                        margin-top: 22px;
                        font-size: 12pt;
                        color: #2c3e50;
                    }
                    h1 {
                        font-size: 14pt;
                        color: #ffffff;
                        background: #1f6feb;
                        padding: 6px 12px;
                        border-radius: 4px;
                        margin: 22px 0 12px 0;
                        letter-spacing: 1px;
                    }
                    h2 {
                        font-size: 12pt;
                        color: #1f4e99;
                        margin: 14px 0 8px 0;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 4px 0 8px 0;
                    }
                    th, td {
                        border: 1px solid #d5dbe3;
                        padding: 6px 10px;
                        font-size: 10pt;
                    }
                    th {
                        background: #eef3fc;
                        color: #1f4e99;
                        font-weight: bold;
                        text-align: left;
                    }
                    .kv td:first-child {
                        width: 150px;
                        background: #f7f9fc;
                        font-weight: bold;
                        color: #45526b;
                    }
                    .empty {
                        color: #9aa5b5;
                        font-style: italic;
                        padding: 6px 0;
                    }
                    .milestone {
                        margin: 2px 0;
                        font-size: 10pt;
                    }
                    .foot {
                        margin-top: 30px;
                        padding-top: 10px;
                        border-top: 1px solid #e5e9f0;
                        font-size: 9pt;
                        color: #9aa5b5;
                        text-align: right;
                    }
                </style>
            </head>
            <body>

            <div class="cover">
                <div class="cover-title">职业规划</div>
                <div class="cover-sub">{{studentName}}（{{userNo}}）{{#semesterName}} · {{semesterName}}{{/semesterName}}</div>
                <div class="cover-meta">{{title}}</div>
            </div>

            <h1>一、规划信息</h1>
            <table class="kv">
                <tr><td>规划标题</td><td>{{title}}</td></tr>
                <tr><td>所属学期</td><td>{{semesterName}}</td></tr>
                <tr><td>整体进度</td><td>{{progressRate}}%</td></tr>
                <tr><td>审批状态</td><td>{{statusLabel}}</td></tr>
                <tr><td>提交时间</td><td>{{submittedAt}}</td></tr>
                <tr><td>审核时间</td><td>{{auditedAt}}</td></tr>
                <tr><td>审核教师</td><td>{{auditorName}}</td></tr>
                <tr><td>退回原因</td><td>{{rejectedReason}}</td></tr>
            </table>

            <h1>二、要求与目标</h1>
            {{#requirement}}
            <p>{{requirement}}</p>
            {{/requirement}}
            {{^requirement}}
            <div class="empty">暂无要求与目标</div>
            {{/requirement}}

            <h1>三、规划内容</h1>
            {{#content}}
            <p>{{content}}</p>
            {{/content}}
            {{^content}}
            <div class="empty">暂无规划内容</div>
            {{/content}}

            <h1>四、目标与行动</h1>
            {{#goals}}
            <h2>{{goalTitle}}</h2>
            <table class="kv">
                <tr><td>目标描述</td><td>{{goalDesc}}</td></tr>
                <tr><td>目标日期</td><td>{{targetDate}}</td></tr>
                <tr><td>目标状态</td><td>{{statusLabel}}</td></tr>
            </table>
            {{#actions}}
            <table>
                <tr><th>行动标题</th><th>行动状态</th><th>起止时间</th><th>完成度</th></tr>
                <tr><td>{{actionTitle}}</td><td>{{statusLabel}}</td><td>{{timeRange}}</td><td>{{completionRate}}%</td></tr>
            </table>
            <div>
                {{#milestones}}
                <div class="milestone">· {{milestoneTitle}}（{{milestoneDate}}，{{achievedLabel}}）</div>
                {{/milestones}}
                {{^milestones}}
                <div class="empty">该行动暂无里程碑</div>
                {{/milestones}}
            </div>
            {{/actions}}
            {{^actions}}
            <div class="empty">该目标暂无行动</div>
            {{/actions}}
            {{/goals}}
            {{^goals}}
            <div class="empty">暂无目标与行动</div>
            {{/goals}}

            <h1>五、阶段反思</h1>
            {{#reflections}}
            <table class="kv">
                <tr><td>反思时间</td><td>{{createdAt}}</td></tr>
                <tr><td>反思内容</td><td>{{reflectionContent}}</td></tr>
            </table>
            {{/reflections}}
            {{^reflections}}
            <div class="empty">暂无阶段反思</div>
            {{/reflections}}

            <h1>六、教师反馈</h1>
            {{#feedbacks}}
            <table class="kv">
                <tr><td>反馈教师</td><td>{{teacherName}}</td></tr>
                <tr><td>反馈时间</td><td>{{createdAt}}</td></tr>
                <tr><td>反馈内容</td><td>{{feedbackContent}}</td></tr>
            </table>
            {{/feedbacks}}
            {{^feedbacks}}
            <div class="empty">暂无教师反馈</div>
            {{/feedbacks}}

            <div class="foot">导出时间：{{exportTime}}</div>

            </body>
            </html>
            """;

    // ==================== 个人简历模板 ====================

    /**
     * 个人简历导出模板（学生端个人中心简历导出）。
     * <p>
     * 数据上下文由 {@link com.example.studentarchives.service.Fmy.ResumeExportService} 装配，
     * 包含：基础信息、教育背景（学期 GPA）、获奖情况、技能与兴趣、实践经历、证书、自我评价。
     */
    public static final String RESUME = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8"/>
                <title>个人简历</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        font-family: "CJK", "Noto Sans CJK SC", "Microsoft YaHei", "SimHei", sans-serif;
                        color: #2c3e50;
                        line-height: 1.6;
                        margin: 0;
                        padding: 0;
                    }
                    .resume { width: 100%; }
                    .header {
                        border-bottom: 2px solid #1f6feb;
                        padding-bottom: 14px;
                        margin-bottom: 18px;
                    }
                    .name {
                        font-size: 28pt;
                        font-weight: bold;
                        color: #1a1a1a;
                        margin: 0 0 6px 0;
                    }
                    .subtitle {
                        font-size: 11pt;
                        color: #6b7a90;
                        margin-bottom: 10px;
                    }
                    .contact-row { font-size: 9.5pt; color: #5a6a7d; }
                    .contact-item {
                        display: inline-block;
                        margin-right: 14px;
                        white-space: nowrap;
                    }
                    .contact-icon {
                        color: #1f6feb;
                        margin-right: 4px;
                        display: inline-block;
                        vertical-align: middle;
                        width: 11px;
                        height: 11px;
                    }
                    .contact-icon svg {
                        display: inline-block;
                        vertical-align: middle;
                    }
                    .section { margin-bottom: 16px; }
                    .section-title {
                        font-size: 13pt;
                        font-weight: bold;
                        color: #1f4e99;
                        border-bottom: 1px solid #d5dbe3;
                        padding-bottom: 5px;
                        margin-bottom: 10px;
                    }
                    .edu-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 8px;
                    }
                    .edu-table td {
                        padding: 0;
                        vertical-align: baseline;
                    }
                    .edu-label {
                        color: #6b7a90;
                        font-size: 9.5pt;
                        margin-right: 6px;
                    }
                    .edu-major {
                        font-weight: bold;
                        color: #2c3e50;
                        font-size: 10.5pt;
                    }
                    .edu-class {
                        color: #6b7a90;
                        font-size: 9.5pt;
                        text-align: right;
                    }
                    .gpa-chip {
                        display: inline-block;
                        background: #f5f7fa;
                        border: 1px solid #e5e9f0;
                        border-radius: 3px;
                        padding: 3px 10px;
                        font-size: 8.5pt;
                        color: #5a6a7d;
                        margin: 0 6px 6px 0;
                    }
                    .item-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 2px;
                    }
                    .item-table td {
                        padding: 5px 0;
                        vertical-align: top;
                    }
                    .bullet {
                        color: #1f6feb;
                        font-size: 11pt;
                        padding-right: 6px;
                        line-height: 1;
                    }
                    .item-title {
                        color: #2c3e50;
                        font-size: 10pt;
                    }
                    .level-badge {
                        display: inline-block;
                        background: #eef3fc;
                        color: #1f6feb;
                        border-radius: 3px;
                        padding: 1px 7px;
                        font-size: 8pt;
                        margin-left: 6px;
                    }
                    .level-secondary {
                        color: #6b7a90;
                        font-size: 8pt;
                        margin-left: 4px;
                    }
                    .item-date {
                        color: #6b7a90;
                        font-size: 8.5pt;
                        text-align: right;
                        white-space: nowrap;
                    }
                    .award-name {
                        color: #c9a227;
                        font-size: 8.5pt;
                    }
                    .skill-chip {
                        display: inline-block;
                        background: #f7f9fc;
                        border: 1px solid #e5e9f0;
                        border-radius: 3px;
                        padding: 4px 10px;
                        font-size: 8.5pt;
                        color: #4a5568;
                        margin: 0 6px 6px 0;
                    }
                    .certificate-chip {
                        display: inline-block;
                        background: #f0f9f4;
                        border: 1px solid #d4edda;
                        border-radius: 3px;
                        padding: 4px 10px;
                        font-size: 8.5pt;
                        color: #2f855a;
                        margin: 0 6px 6px 0;
                    }
                    .self-evaluation {
                        color: #4a5568;
                        font-size: 9.5pt;
                        text-indent: 2em;
                        margin: 0;
                    }
                    .empty {
                        color: #9aa5b5;
                        font-style: italic;
                        font-size: 9pt;
                    }
                </style>
            </head>
            <body>

            <div class="resume">
                <div class="header">
                    <div class="name">{{studentName}}</div>
                    <div class="subtitle">{{title}}</div>
                    <div class="contact-row">
                        <span class="contact-item"><span class="contact-icon"><svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#1f6feb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg></span>{{phone}}</span>
                        <span class="contact-item"><span class="contact-icon"><svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#1f6feb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg></span>{{email}}</span>
                        <span class="contact-item"><span class="contact-icon"><svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#1f6feb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/><circle cx="6" cy="15" r="1"/></svg></span>{{userNo}}</span>
                        <span class="contact-item"><span class="contact-icon"><svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#1f6feb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg></span>{{grade}}</span>
                        <span class="contact-item"><span class="contact-icon"><svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#1f6feb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18"/><path d="M5 21V7l8-4 8 4v14"/><path d="M12 11h.01"/><path d="M12 15h.01"/><path d="M12 19h.01"/></svg></span>{{clazz}}</span>
                    </div>
                </div>

                {{#showEducation}}
                <div class="section">
                    <div class="section-title">教育背景</div>
                    <table class="edu-table">
                        <tr>
                            <td>
                                <span class="edu-major">{{schoolName}}{{#major}} · {{major}}{{/major}}{{#degreeTypeLabel}} · {{degreeTypeLabel}}{{/degreeTypeLabel}}</span>
                            </td>
                            <td class="edu-class">{{grade}} · {{clazz}}</td>
                        </tr>
                    </table>
                    {{#gpas}}
                    <span class="gpa-chip">{{semesterName}} · GPA {{gpa}}</span>
                    {{/gpas}}
                    {{^gpas}}
                    <div class="empty">暂无学期成绩</div>
                    {{/gpas}}
                </div>
                {{/showEducation}}

                {{#showAwards}}
                <div class="section">
                    <div class="section-title">获奖情况</div>
                    {{#awards}}
                    <table class="item-table">
                        <tr>
                            <td>
                                <span class="bullet">•</span>
                                <span class="item-title">{{title}}</span>
                            </td>
                            <td class="item-date">
                                {{#level}}<span class="level-badge">{{level}}</span>{{/level}}
                                {{date}}{{#awardLevel}} <span class="award-name">{{awardLevel}}</span>{{/awardLevel}}
                            </td>
                        </tr>
                    </table>
                    {{/awards}}
                    {{^awards}}
                    <div class="empty">暂无获奖记录</div>
                    {{/awards}}
                </div>
                {{/showAwards}}

                {{#showSkills}}
                <div class="section">
                    <div class="section-title">技能与兴趣</div>
                    {{#skillCategories}}
                    <span class="skill-chip"><strong>{{category}}</strong> {{joinedItems}} {{proficiency}}</span>
                    {{/skillCategories}}
                    {{^skillCategories}}
                    <div class="empty">暂无技能与兴趣</div>
                    {{/skillCategories}}
                </div>
                {{/showSkills}}

                {{#showPractices}}
                <div class="section">
                    <div class="section-title">实践经历</div>
                    {{#practices}}
                    <table class="item-table">
                        <tr>
                            <td><span class="bullet">•</span> <span class="item-title">{{title}}</span></td>
                            <td class="item-date">{{date}}</td>
                        </tr>
                    </table>
                    {{/practices}}
                    {{^practices}}
                    <div class="empty">暂无实践经历</div>
                    {{/practices}}
                </div>
                {{/showPractices}}

                {{#showCertificates}}
                <div class="section">
                    <div class="section-title">证书</div>
                    {{#certificates}}
                    <span class="certificate-chip">{{name}}</span>
                    {{/certificates}}
                    {{^certificates}}
                    <div class="empty">暂无证书</div>
                    {{/certificates}}
                </div>
                {{/showCertificates}}

                {{#showSelfEvaluation}}
                <div class="section">
                    <div class="section-title">自我评价</div>
                    {{#selfEvaluation}}
                    <p class="self-evaluation">{{selfEvaluation}}</p>
                    {{/selfEvaluation}}
                    {{^selfEvaluation}}
                    <div class="empty">暂无自我评价</div>
                    {{/selfEvaluation}}
                </div>
                {{/showSelfEvaluation}}
            </div>

            </body>
            </html>
            """;

    private DefaultTemplateHtml() {
    }
}
