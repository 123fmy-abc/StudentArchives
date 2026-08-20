package com.example.studentarchives.dto.Fmy.formtemplate.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建表单模板请求 DTO（POST /admin/form-templates，《管理端接口文档》17.3）
 * <p>
 * 创建成功后 version 初始为 1，is_default 默认 0，status 默认 1。
 * schoolId 不接收前端传入，统一由当前登录用户所属学校推导（管理端全局约定）。
 * 字段配置 fields 数组至少 1 个字段；layoutConfig 布局配置、applicableRoles 适用角色均为可选的 JSON 结构。
 */
@Data
public class FormTemplateCreateRequest {

    @NotBlank(message = "templateName 不能为空")
    @Size(max = 100, message = "模板名称最大 100 字符")
    private String templateName;

    /** 模板编码，对应档案/奖项类型编码，同一 schoolId + category 下唯一 */
    @NotBlank(message = "code 不能为空")
    @Size(max = 50, message = "模板编码最大 50 字符")
    private String code;

    /** 适用类别：archive档案 / award奖项 / career_plan职业规划，默认 archive */
    @NotBlank(message = "category 不能为空")
    private String category = "archive";

    @Size(max = 255, message = "模板说明最大 255 字符")
    private String description;

    /** 字段配置数组（结构见 FormTemplateFieldItem），至少 1 个字段 */
    @NotNull(message = "fields 不能为空")
    @Size(min = 1, message = "字段配置至少 1 个字段")
    @Valid
    private List<FormTemplateFieldItem> fields;

    /** 布局配置 JSON（如 sections 分区字段引用） */
    private Object layoutConfig;

    /** 适用角色编码数组，如 ["student","teacher"]；不传表示全部角色 */
    private List<String> applicableRoles;

    /** 0=非默认 1=默认，默认 0 */
    private Integer isDefault = 0;

    /** 0=禁用 1=启用，默认 1 */
    private Integer status = 1;
}
