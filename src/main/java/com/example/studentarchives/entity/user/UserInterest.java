package com.example.studentarchives.entity.user;

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
@Table(name = "user_interests")
@SQLRestriction("deleted_at IS NULL")
public class UserInterest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "proficiency_level", nullable = false)
    private Integer proficiencyLevel;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "is_detail")
    private Integer isDetail;

    @Column(name = "detail_content", length = 255)
    private String detailContent;
}
