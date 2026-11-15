package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.GrowthEventScoreRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 成长事件计分规则 Repository
 */
@Repository
public interface GrowthEventScoreRuleRepository extends JpaRepository<GrowthEventScoreRule, Long> {

    /**
     * 按事件类型查询规则（未删除）
     */
    Optional<GrowthEventScoreRule> findByEventType(Integer eventType);

    /**
     * 查询全部规则，按事件类型正序
     */
    List<GrowthEventScoreRule> findAllByOrderByEventTypeAsc();

    /**
     * 软删除计分规则（置 deleted_at）
     */
    @Modifying
    @Query(value = "UPDATE growth_event_score_rules SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
