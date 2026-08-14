package com.example.studentarchives.dto.Fmy.export.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建导出模板请求 DTO（POST /admin/export-templates，管理端文档 5.5）
 * <p>
 * 创建成功后 version 初始为 1，is_default 为 0；templateMode 默认 1=字段列表模式。
 * JSON 型配置字段（fieldsConfig 等）由 Jackson 解析为对象后序列化入库。
 */
@Data
public class ExportTemplateCreateRequest {

    @NotNull(message = "schoolId 不能为空")
    private Long schoolId;

    @NotBlank(message = "templateName 不能为空")
    @Size(max = 100, message = "模板名称最大 100 字符")
    private String templateName;

    @NotBlank(message = "templateCode 不能为空")
    @Size(max = 50, message = "模板编码最大 50 字符")
    private String templateCode;

    /** 导出类型：student_archive / career_plan / resume */
    @NotBlank(message = "exportType 不能为空")
    private String exportType;

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级，默认 1 */
    private Integer scopeType = 1;

    /** 字段配置 JSON（字段列表模式时必填） */
    @NotNull(message = "fieldsConfig 不能为空")
    private Object fieldsConfig;

    /** 默认筛选条件 JSON */
    private Object filterConditions;

    /** HTML 模板内容（自由模板模式时必填） */
    private String templateContent;

    /** 模板渲染模式：1=字段列表模式 2=自由模板模式，默认 1 */
    private Integer templateMode = 1;

    /** 渲染引擎，默认 puppeteer */
    private String engineType = "puppeteer";

    /** 页面配置 JSON */
    private Object pageConfig;

    /** 纸张尺寸，默认 A4 */
    private String paperSize = "A4";

    /** 1=纵向 2=横向，默认 1 */
    private Integer orientation = 1;

    /** 页边距配置 JSON */
    private Object marginConfig;

    /** 页眉 HTML */
    private String headerHtml;

    /** 页脚 HTML */
    private String footerHtml;

    /** 水印配置 JSON */
    private Object watermarkConfig;

    /** 字体配置 JSON */
    private Object fontConfig;

    /** 预览图 URL */
    private String previewImage;

    /** 0=禁用 1=启用，默认 1 */
    private Integer status = 1;
}
