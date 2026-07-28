package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    @Column(name = "target_name", length = 100)
    private String targetName;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;
}
