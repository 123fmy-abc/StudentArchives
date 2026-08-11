package com.example.studentarchives.dto.Fmy.form.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表单模板创建请求
 * <p>
 * 对应图片“管理员 → 表单自定义 → 添加菜单/输入增加项目”。
 * fields 为 JSON 数组，描述表单字段定义；layout_config 描述布局；
 * applicable_roles 描述适用角色。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateCreateRequest {

    @NotNull(message = "schoolId 不能为空")
    private Long schoolId;

    @NotBlank(message = "templateName 不能为空")
    @Size(max = 100, message = "templateName 长度不能超过100")
    private String templateName;

    @NotBlank(message = "code 不能为空")
    @Size(max = 50, message = "code 长度不能超过50")
    private String code;

    /** 分类：archive/award/career_plan 等 */
    @Size(max = 50, message = "category 长度不能超过50")
    private String category = "archive";

    @Size(max = 255, message = "description 长度不能超过255")
    private String description;

    /** 字段定义 JSON 数组 */
    private JsonNode fields;

    /** 布局配置 JSON */
    private JsonNode layoutConfig;

    /** 适用角色编码 JSON 数组 */
    private JsonNode applicableRoles;
}
