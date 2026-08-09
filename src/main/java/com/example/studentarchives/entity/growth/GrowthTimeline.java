package com.example.studentarchives.entity.growth;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "growth_timelines")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class GrowthTimeline extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "event_type", nullable = false)
    private Integer eventType;

    @Column(name = "event_name", length = 255, nullable = false)
    private String eventName;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "event_at", nullable = false)
    private LocalDate eventAt;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 100)
    private String sourceType;

    @Column(name = "event_key", length = 64)
    private String eventKey;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = 0;
}
