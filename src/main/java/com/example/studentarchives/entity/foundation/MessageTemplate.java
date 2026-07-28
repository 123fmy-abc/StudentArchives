package com.example.studentarchives.entity.foundation;

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
@Table(name = "message_templates")
@SQLRestriction("deleted_at IS NULL")
public class MessageTemplate extends BaseEntity {

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "title_template", length = 255)
    private String titleTemplate;

    @Lob
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;

    @Column(name = "variables", columnDefinition = "JSON")
    private String variables;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
