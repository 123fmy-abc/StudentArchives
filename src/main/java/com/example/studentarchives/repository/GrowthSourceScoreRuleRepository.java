package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.GrowthSourceScoreRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 成长事件计分规则 Repository（按来源记录类型）
 */
@Repository
public interface GrowthSourceScoreRuleRepository extends JpaRepository<GrowthSourceScoreRule, Long> {

    /**
     * 按来源记录类型查询规则（未删除）
     */
    Optional<GrowthSourceScoreRule> findBySourceType(String sourceType);

    /**
     * 查询全部规则，按来源类型正序
     */
    List<GrowthSourceScoreRule> findAllByOrderBySourceTypeAsc();

    /**
     * 软删除计分规则（置 deleted_at）
     */
    @Modifying
    @Query(value = "UPDATE growth_source_score_rules SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
