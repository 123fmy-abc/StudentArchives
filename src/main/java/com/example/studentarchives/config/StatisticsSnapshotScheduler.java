package com.example.studentarchives.config;

import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.service.Fmy.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 统计快照定时刷新任务
 * <p>
 * 每日凌晨 2 点自动为所有学校刷新当前学期的统计快照，
 * 包含学校级与学院/专业/班级行级维度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsSnapshotScheduler {

    private final StatisticsSnapshotService statisticsSnapshotService;
    private final SchoolRepository schoolRepository;

    /**
     * 每日 02:00 执行快照刷新
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshDailySnapshot() {
        log.info("开始执行每日统计快照刷新任务");
        List<School> schools = schoolRepository.findAll();
        for (School school : schools) {
            try {
                statisticsSnapshotService.refresh(school.getId(), null);
            } catch (Exception e) {
                log.warn("学校快照刷新失败, schoolId={}", school.getId(), e);
            }
        }
        log.info("每日统计快照刷新任务结束，共处理 {} 所学校", schools.size());
    }
}
