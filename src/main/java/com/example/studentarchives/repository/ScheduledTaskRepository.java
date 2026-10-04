package com.example.studentarchives.repository;

import com.example.studentarchives.entity.schedule.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务 Repository
 */
@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long>, JpaSpecificationExecutor<ScheduledTask> {

    /**
     * 判断学校下是否已存在同 task_code 的任务（用于创建/改名唯一校验）。
     * <p>实体级 {@code @SQLRestriction} 自动过滤软删记录，与数据库
     * {@code uk_st_task_code(school_id, task_code, is_deleted_null)} 条件唯一索引语义一致。
     */
    boolean existsBySchoolIdAndTaskCode(Long schoolId, String taskCode);

    /**
     * 查询指定启停状态与运行类型的任务，供动态调度器注册（status=1 且 run_type=1）。
     * <p>实体级 {@code @SQLRestriction} 自动过滤软删记录。
     */
    List<ScheduledTask> findByStatusAndRunType(Integer status, Integer runType);

    /**
     * 软删除指定任务（仅自定义任务可删，系统内置任务由 Service 层拦截）。
     */
    @Modifying
    @Query(value = "UPDATE scheduled_tasks SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}