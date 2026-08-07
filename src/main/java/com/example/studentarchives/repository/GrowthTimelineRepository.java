package com.example.studentarchives.repository;

import com.example.studentarchives.entity.growth.GrowthTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 成长时间轴 Repository（对应表 growth_timelines）
 */
@Repository
public interface GrowthTimelineRepository extends JpaRepository<GrowthTimeline, Long> {

    /** 按用户查询时间轴节点，按事件时间倒序 */
    List<GrowthTimeline> findByUserIdOrderByEventAtDesc(Long userId);

    /** 按用户 + 学期查询时间轴节点，按事件时间倒序 */
    List<GrowthTimeline> findByUserIdAndSemesterIdOrderByEventAtDesc(Long userId, Long semesterId);
}
