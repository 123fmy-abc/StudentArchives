package com.example.studentarchives.dto.Fmy.formtemplate.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表单模板列表项 DTO（GET /admin/form-templates，《管理端接口文档》17.1）
 * <p>
 * 列表页不返回 fields、layoutConfig 大字段，需调详情接口获取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormTemplateItem {

    private Long id;

    private Long schoolId;

    private String templateName;

    /** 模板编码，对应档案/奖项类型编码 */
    private String code;

    /** 适用类别：archive/award/career_plan */
    private String category;

    /** 适用类别中文标签 */
    private String categoryLabel;

    private String description;

    /** 适用角色编码数组（JSON 列解析），null 表示全部角色 */
    private List<String> applicableRoles;

    /** 0=非默认 1=默认模板 */
    private Integer isDefault;

    private Integer version;

    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    private Long createdBy;

    /** 创建人姓名 */
    private String createdByName;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
