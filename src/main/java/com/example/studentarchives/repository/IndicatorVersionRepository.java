package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.IndicatorVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 指标版本历史 Repository
 * <p>
 * 对应表：indicator_versions（单指标在指定规则版本下的权重/计分规则快照）。
 * 该表无 deleted_at，发布指标规则版本时由应用层写入快照，供历史评分追溯。
 */
@Repository
public interface IndicatorVersionRepository extends JpaRepository<IndicatorVersion, Long> {

    /**
     * 查询指定规则版本号下的所有指标快照（用于基于历史版本发布新版本时深拷贝）。
     */
    @Query("SELECT v FROM IndicatorVersion v WHERE v.version = :version")
    List<IndicatorVersion> findByVersion(@Param("version") Integer version);
}
