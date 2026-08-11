package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.IndicatorRuleVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // ==================== 管理端指标配置 ====================

    /**
     * 查询学校下最新的规则版本（版本号最大的一条）
     */
    Optional<IndicatorRuleVersion> findTopBySchoolIdOrderByVersionDesc(Long schoolId);

    /**
     * 查询学校下指定学期最近发布的规则版本（版本号最大的一条）
     */
    Optional<IndicatorRuleVersion> findTopBySchoolIdAndSemesterIdOrderByVersionDesc(Long schoolId, Long semesterId);

    /**
     * 查询学校下指定版本名称的规则版本（用于重复发布校验）
     */
    Optional<IndicatorRuleVersion> findBySchoolIdAndVersionName(Long schoolId, String versionName);

    /**
     * 分页查询学校下的规则版本，按版本号倒序
     */
    List<IndicatorRuleVersion> findBySchoolIdOrderByVersionDesc(Long schoolId, Pageable pageable);

    /**
     * 分页查询学校下指定学期的规则版本，按版本号倒序
     */
    List<IndicatorRuleVersion> findBySchoolIdAndSemesterIdOrderByVersionDesc(Long schoolId, Long semesterId, Pageable pageable);

    /**
     * 统计学校下的规则版本数量
     */
    long countBySchoolId(Long schoolId);

    /**
     * 统计学校下指定学期的规则版本数量
     */
    long countBySchoolIdAndSemesterId(Long schoolId, Long semesterId);
}
