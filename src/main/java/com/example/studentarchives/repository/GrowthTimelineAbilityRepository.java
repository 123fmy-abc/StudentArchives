package com.example.studentarchives.repository;

import com.example.studentarchives.entity.growth.GrowthTimelineAbility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 成长时间轴能力得分 Repository（对应表 growth_timeline_abilities）
 */
@Repository
public interface GrowthTimelineAbilityRepository extends JpaRepository<GrowthTimelineAbility, Long> {

    /** 按时间轴节点 ID 集合批量查询能力得分 */
    List<GrowthTimelineAbility> findByTimelineIdIn(Collection<Long> timelineIds);
}
