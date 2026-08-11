package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.IndicatorVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 指标版本历史 Repository
 * <p>
 * 对应表：indicator_versions（单指标在指定规则版本下的权重/计分规则快照）。
 * 该表无 deleted_at，发布指标规则版本时由应用层写入快照，供历史评分追溯。
 */
@Repository
public interface IndicatorVersionRepository extends JpaRepository<IndicatorVersion, Long> {
}
