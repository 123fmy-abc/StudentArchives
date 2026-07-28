package com.example.studentarchives.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@SoftDelete(columnName = "deleted_at")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 用于 MySQL 条件唯一索引兼容的生成列（WHERE deleted_at IS NULL）。
     * 该字段由数据库自动生成，应用层只读。
     */
    @Column(name = "is_deleted_null", insertable = false, updatable = false)
    private Boolean isDeletedNull;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (deletedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
