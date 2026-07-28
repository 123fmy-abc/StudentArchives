package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "statistics_cache")
public class StatisticsCache extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "cache_key", nullable = false, length = 255)
    private String cacheKey;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "stat_type", nullable = false, length = 50)
    private String statType;

    @Column(name = "stat_data", columnDefinition = "JSON")
    private String statData;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;
}
