package com.example.studentarchives.entity.growth;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "growth_timelines")
public class GrowthTimeline extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private Long semesterId;

    private Integer eventType;

    private String eventName;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String coverImage;

    @Column(columnDefinition = "JSON")
    private String abilityData;

    @Column(columnDefinition = "JSON")
    private String tags;

    private LocalDate eventTime;

    private Long sourceId;

    private String sourceType;
}
