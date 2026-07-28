package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "indicator_rule_versions")
public class IndicatorRuleVersion extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "version_name", nullable = false, length = 100)
    private String versionName;

    @Column(name = "effective_at")
    private LocalDateTime effectiveAt;

    @Column(name = "created_by")
    private Long createdBy;

}
