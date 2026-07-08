package com.example.studentarchives.entity.foundation;

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
@Table(name = "message_templates")
public class MessageTemplate extends BaseEntity {

    @Column(name = "template_code")
    private String templateCode;

    @Column(name = "template_name")
    private String templateName;

    private String category;

    @Column(name = "title_template")
    private String titleTemplate;

    @Lob
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;

    @Column(columnDefinition = "JSON")
    private String variables;

    private Integer status;
}
