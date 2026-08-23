package com.example.studentarchives.repository;

import com.example.studentarchives.entity.schedule.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 定时任务 Repository
 */
@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long>, JpaSpecificationExecutor<ScheduledTask> {
}