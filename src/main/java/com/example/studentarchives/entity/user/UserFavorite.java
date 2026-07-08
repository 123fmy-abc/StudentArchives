package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_favorites")
public class UserFavorite extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "favorite_type", length = 50)
    private String favoriteType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "target_name", length = 255)
    private String targetName;

    @Column(name = "icon", length = 255)
    private String icon;

    @Column(name = "sort")
    private Integer sort;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "use_count")
    private Integer useCount;
}
