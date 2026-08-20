package com.example.studentarchives.dto.Fmy.formtemplate.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除表单模板响应 DTO（DELETE /admin/form-templates/{templateId}，《管理端接口文档》17.5）
 * <p>
 * 采用逻辑删除：记录 deleted_at 置位，学生端/教师端填写中的历史申报按当时字段快照存储，不受影响。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateDeleteResponse {

    /** 模板 ID（form_templates.id） */
    private Long id;

    /** 逻辑删除时间（ISO 8601 带时区） */
    private String deletedAt;
}
