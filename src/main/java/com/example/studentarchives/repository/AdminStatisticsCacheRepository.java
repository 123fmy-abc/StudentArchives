package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.StatisticsCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 统计缓存补充 Repository（对应表 statistics_cache）
 * <p>
 * 管理端统计模块二级缓存读取：按 cache_key 精确命中，命中且未过期则直接返回
 * 预聚合结果，避免实时全量聚合打库。写回由定时任务负责，接口只读。
 */
@Repository
public interface AdminStatisticsCacheRepository extends JpaRepository<StatisticsCache, Long> {

    /** 按学校与缓存键查询统计缓存 */
    Optional<StatisticsCache> findBySchoolIdAndCacheKey(Long schoolId, String cacheKey);
}
