package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.AbilityDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 能力维度 Repository
 */
@Repository
public interface AbilityDimensionRepository extends JpaRepository<AbilityDimension, Long> {

    /**
     * 查询所有启用的能力维度，按 sort 正序
     */
    @Query("SELECT a FROM AbilityDimension a WHERE a.status = 1 ORDER BY a.sort ASC")
    List<AbilityDimension> findAllActive();

    /**
     * 查询所有能力维度（含禁用），按 sort 正序
     */
    List<AbilityDimension> findAllByOrderBySortAsc();

    /**
     * 按维度编码查询（未删除）
     */
    Optional<AbilityDimension> findByDimensionCode(String dimensionCode);

    /**
     * 软删除能力维度（置 deleted_at）
     */
    @Modifying
    @Query(value = "UPDATE ability_dimensions SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
