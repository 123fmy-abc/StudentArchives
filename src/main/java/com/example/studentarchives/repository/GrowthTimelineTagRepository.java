package com.example.studentarchives.repository;

import com.example.studentarchives.entity.growth.GrowthTimelineTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 成长时间轴标签 Repository（对应表 growth_timeline_tags）
 */
@Repository
public interface GrowthTimelineTagRepository extends JpaRepository<GrowthTimelineTag, Long> {

    /** 按时间轴节点 ID 集合批量查询标签 */
    List<GrowthTimelineTag> findByTimelineIdIn(Collection<Long> timelineIds);
}
