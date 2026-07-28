package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "score_calculations")
public class ScoreCalculation extends BaseEntityNoUpdate {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "rule_version", nullable = false)
    private int ruleVersion;

    @Column(name = "data_source", length = 255)
    private String dataSource;

    @Column(name = "trigger_type")
    private byte triggerType = 1;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "status")
    private byte status = 1;

    @Column(name = "lock_version", nullable = false)
    private int lockVersion;
}
