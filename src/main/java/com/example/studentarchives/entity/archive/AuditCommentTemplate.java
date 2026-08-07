package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "audit_comment_templates")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AuditCommentTemplate extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "template_code", length = 50)
    private String templateCode;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    @Column(name = "category", nullable = false)
    private Integer category = 2;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;
}
