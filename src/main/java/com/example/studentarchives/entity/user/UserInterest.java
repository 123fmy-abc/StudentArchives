package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_interests")
public class UserInterest extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tag_name", length = 50)
    private String tagName;

    @Column(name = "proficiency_level")
    private Integer proficiencyLevel;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "sort")
    private Integer sort;

    @Column(name = "is_detail")
    private Integer isDetail;

    @Column(name = "detail_content", columnDefinition = "TEXT")
    @Lob
    private String detailContent;
}
