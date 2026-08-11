package com.example.studentarchives.dto.Fmy.form.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表单模板响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateResponse {

    private Long id;
    private Long schoolId;
    private String templateName;
    private String code;
    private String category;
    private String description;
    private JsonNode fields;
    private JsonNode layoutConfig;
    private JsonNode applicableRoles;
    private Integer isDefault;
    private Integer version;
    private Integer status;
    private String createdAt;
    private String updatedAt;
}
