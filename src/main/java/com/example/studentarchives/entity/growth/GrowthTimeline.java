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

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "event_type", nullable = false)
    private byte eventType;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "ability_data", columnDefinition = "JSON")
    private String abilityData;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Column(name = "event_time", nullable = false)
    private LocalDate eventTime;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 100)
    private String sourceType;
}
