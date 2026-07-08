package com.example.studentarchives.entity.weakness;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "weakness_progress")
public class WeaknessProgress extends BaseEntity {

    @Column(name = "weakness_id", nullable = false)
    private Long weaknessId;

    @Column(name = "progress_value", nullable = false)
    private int progressValue;

    @Column(name = "progress_desc", length = 255)
    private String progressDesc;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
