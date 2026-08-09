package com.example.studentarchives.entity.message;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "announcement_reads")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AnnouncementRead extends BaseEntity {

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;
}
