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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "favorite_type", nullable = false, length = 50)
    private String favoriteType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "target_name", nullable = false, length = 100)
    private String targetName;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "sort", nullable = false)
    private int sort;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "use_count")
    private Integer useCount;
}
