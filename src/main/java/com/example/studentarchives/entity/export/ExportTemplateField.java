package com.example.studentarchives.entity.export;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "export_template_fields")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ExportTemplateField extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "placeholder_key", length = 100, nullable = false)
    private String placeholderKey;

    @Column(name = "field_source", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'table'")
    private String fieldSource = "table";

    @Column(name = "field_path", length = 255, nullable = false)
    private String fieldPath;

    @Column(name = "data_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'string'")
    private String dataType = "string";

    @Column(name = "format_rule", length = 255)
    private String formatRule;

    @Column(name = "default_value", length = 255)
    private String defaultValue;

    @Column(name = "is_list", nullable = false, columnDefinition = "TINYINT")
    private Integer isList;

    @Lob
    @Column(name = "list_template", columnDefinition = "TEXT")
    private String listTemplate;

    @Column(name = "sort", nullable = false)
    private Integer sort;
}
