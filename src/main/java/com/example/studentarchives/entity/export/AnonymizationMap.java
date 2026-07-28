package com.example.studentarchives.entity.export;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "anonymization_maps")
public class AnonymizationMap extends BaseEntityNoUpdate {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "anonymous_code", length = 50, nullable = false)
    private String anonymousCode;
}
