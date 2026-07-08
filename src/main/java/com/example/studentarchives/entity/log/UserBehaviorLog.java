package com.example.studentarchives.entity.log;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_behavior_logs")
public class UserBehaviorLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "behavior_type", nullable = false, length = 50)
    private String behaviorType;

    @Column(name = "target_page")
    private String targetPage;

    @Column(name = "target_module")
    private String targetModule;

    @Column(name = "meta", columnDefinition = "JSON")
    private String meta;
}
