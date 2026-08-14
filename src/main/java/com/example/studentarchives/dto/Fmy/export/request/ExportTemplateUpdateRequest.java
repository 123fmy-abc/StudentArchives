package com.example.studentarchives.dto.Fmy.export.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新导出模板请求 DTO（PUT /admin/export-templates/{templateId}，管理端文档 5.6）
 * <p>
 * 全部字段可选，未传（null）表示不修改；更新成功时 version 自动 +1。
 * schoolId / exportType 不支持修改（文档 5.6 请求参数不含二者）。
 */
@Data
public class ExportTemplateUpdateRequest {

    @Size(max = 100, message = "模板名称最大 100 字符")
    private String templateName;

    @Size(max = 50, message = "模板编码最大 50 字符")
    private String templateCode;

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级 */
    private Integer scopeType;

    /** 字段配置 JSON（字段列表模式时配置） */
    private Object fieldsConfig;

    /** 默认筛选条件 JSON */
    private Object filterConditions;

    /** HTML 模板内容（自由模板模式时配置） */
    private String templateContent;

    /** 模板渲染模式：1=字段列表模式 2=自由模板模式 */
    private Integer templateMode;

    /** 渲染引擎 */
    private String engineType;

    /** 页面配置 JSON */
    private Object pageConfig;

    /** 纸张尺寸 */
    private String paperSize;

    /** 1=纵向 2=横向 */
    private Integer orientation;

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

    /** 0=禁用 1=启用 */
    private Integer status;
}
