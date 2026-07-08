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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "proficiency_level", nullable = false)
    private byte proficiencyLevel;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "sort", nullable = false)
    private int sort;

    @Column(name = "is_detail", nullable = false)
    private byte isDetail;

    @Column(name = "detail_content", length = 255)
    private String detailContent;
}
