package com.example.studentarchives.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * 软删除过滤条件，须在每个具体实体类上显式声明：
     * {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)}
     * <p>
     * 原因：Hibernate 的 {@code @SQLRestriction}（及旧版 {@code @Where}）不会从
     * {@code @MappedSuperclass} 继承到子实体（已知问题 HHH-18723）。此前把注解放在本基类上
     * 是无效的——导致软删除后数据仍可被查询/更新。请勿把该注解加回本基类。
     */
    public static final String DELETED_AT_IS_NULL = "deleted_at IS NULL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
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
