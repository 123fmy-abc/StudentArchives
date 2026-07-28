package com.example.studentarchives.entity.export;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "export_templates")
public class ExportTemplate extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "template_name", length = 100, nullable = false)
    private String templateName;

    @Column(name = "template_code", length = 50, nullable = false)
    private String templateCode;

    @Column(name = "export_type", length = 50, nullable = false)
    private String exportType;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "fields_config", columnDefinition = "JSON", nullable = false)
    private String fieldsConfig;

    @Column(name = "filter_conditions", columnDefinition = "JSON")
    private String filterConditions;

    @Column(name = "template_mode", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer templateMode = 1;

    @Lob
    @Column(name = "template_content", columnDefinition = "LONGTEXT")
    private String templateContent;

    @Column(name = "engine_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'puppeteer'")
    private String engineType = "puppeteer";

    @Column(name = "page_config", columnDefinition = "JSON")
    private String pageConfig;

    @Column(name = "paper_size", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'A4'")
    private String paperSize = "A4";

    @Column(name = "orientation", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer orientation = 1;

    @Column(name = "margin_config", columnDefinition = "JSON")
    private String marginConfig;

    @Lob
    @Column(name = "header_html", columnDefinition = "TEXT")
    private String headerHtml;

    @Lob
    @Column(name = "footer_html", columnDefinition = "TEXT")
    private String footerHtml;

    @Column(name = "watermark_config", columnDefinition = "JSON")
    private String watermarkConfig;

    @Column(name = "font_config", columnDefinition = "JSON")
    private String fontConfig;

    @Column(name = "preview_image", length = 500)
    private String previewImage;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_default", nullable = false, columnDefinition = "TINYINT")
    private Integer isDefault;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
