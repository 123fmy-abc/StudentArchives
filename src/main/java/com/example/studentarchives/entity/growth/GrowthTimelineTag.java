package com.example.studentarchives.entity.growth;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "growth_timeline_tags")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class GrowthTimelineTag extends BaseEntity {

    @Column(name = "timeline_id", nullable = false)
    private Long timelineId;

    @Column(name = "tag_name", length = 100, nullable = false)
    private String tagName;
}
