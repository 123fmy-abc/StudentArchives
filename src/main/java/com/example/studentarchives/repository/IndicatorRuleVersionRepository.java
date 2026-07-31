package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.IndicatorRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 指标规则版本 Repository
 */
@Repository
public interface IndicatorRuleVersionRepository extends JpaRepository<IndicatorRuleVersion, Long> {

    /**
     * 查询当前生效的规则版本（effective_at <= NOW()，按 effective_at 倒序取最新一条）
     */
    @Query(value = "SELECT * FROM indicator_rule_versions WHERE school_id = :schoolId AND effective_at <= NOW() AND deleted_at IS NULL ORDER BY effective_at DESC LIMIT 1",
            nativeQuery = true)
    Optional<IndicatorRuleVersion> findCurrentEffective(@Param("schoolId") Long schoolId);
}
