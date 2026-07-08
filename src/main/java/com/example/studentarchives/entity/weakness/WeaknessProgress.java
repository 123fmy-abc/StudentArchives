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

    private Long weaknessId;

    private Integer progressValue;

    private String progressDesc;

    private LocalDateTime recordedAt;
}
