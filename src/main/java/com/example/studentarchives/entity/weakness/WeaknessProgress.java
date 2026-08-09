package com.example.studentarchives.entity.weakness;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "weakness_progress")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class WeaknessProgress extends BaseEntity {

    @Column(name = "weakness_id", nullable = false)
    private Long weaknessId;

    @Column(name = "progress_value", nullable = false)
    private Integer progressValue;

    @Column(name = "progress_desc", length = 255)
    private String progressDesc;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
