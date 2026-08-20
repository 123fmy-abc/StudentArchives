package com.example.studentarchives.dto.Fmy.formtemplate.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 表单模板字段配置项（form_templates.fields 数组元素，《管理端接口文档》17.2 字段配置说明）
 * <p>
 * type 支持：input文本 / textarea多行文本 / number数字 / select下拉 / radio单选 /
 * checkbox多选 / date日期 / upload附件上传 / switch开关（取值合法性与字段结构在
 * {@code AdminFormTemplateService} 中二次校验，见 17.3 业务规则）。
 * <p>
 * options 为静态选项（select/radio/checkbox 用）；dataSource 为动态数据源
 * （如 {"dictType":"competition_level"} 拉取字典、{"api":"/common/semesters"} 拉取接口数据）；
 * rules 为校验规则 JSON（必填、长度、正则等）；visible 为联动逻辑（依赖其他字段值显隐）。
 * 以上均以 Object 接收，由 Service 统一序列化为 JSON 入库。
 */
@Data
public class FormTemplateFieldItem {

    @NotBlank(message = "字段 key 不能为空")
    @Size(max = 50, message = "字段 key 最大 50 字符")
    private String key;

    @NotBlank(message = "字段 label 不能为空")
    @Size(max = 100, message = "字段 label 最大 100 字符")
    private String label;

    /** 组件类型：input/textarea/number/select/radio/checkbox/date/upload/switch */
    @NotBlank(message = "字段 type 不能为空")
    private String type;

    /** 是否必填 */
    private Boolean required;

    @Size(max = 200, message = "placeholder 最大 200 字符")
    private String placeholder;

    /** 校验规则 JSON（必填、长度、正则等） */
    private Object rules;

    /** 静态选项数组（select/radio/checkbox 用），如 [{"value":"national","label":"国家级"}] */
    private Object options;

    /** 动态数据源（字典/接口），如 {"dictType":"competition_level"} */
    private Object dataSource;

    /** 联动逻辑（依赖其他字段值显隐） */
    private Object visible;

    /** 排序号，越小越靠前 */
    private Integer sort;
}
