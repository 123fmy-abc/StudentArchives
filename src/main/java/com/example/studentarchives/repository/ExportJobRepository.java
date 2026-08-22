package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 导出任务 Repository（对应表 export_jobs）
 * <p>
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 自动过滤已软删除记录，
 * 因此 {@link #findByOperatorId} 分页查询天然只返回未删除任务。
 */
@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    /**
     * 按操作人分页查询导出任务（教师端 12.3 任务列表按 {@code operator_id} 过滤）。
     */
    Page<ExportJob> findByOperatorId(Long operatorId, Pageable pageable);

    /**
     * 软删除导出任务（deleted_at 置为当前时间，仅命中未删除记录）。
     *
     * @return 受影响行数（0 表示任务不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE export_jobs SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
