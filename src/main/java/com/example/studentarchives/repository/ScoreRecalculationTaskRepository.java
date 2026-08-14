package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.ScoreRecalculationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 评分重算任务 Repository（对应表 score_recalculation_tasks）
 * <p>
 * 支撑管理端 /admin/scores/*（《管理端接口文档》二、评分重算模块）：
 * 任务创建后异步执行，本接口负责任务生命周期查询与「同一范围已有生效中任务」的 41005 冲突校验。
 */
@Repository
public interface ScoreRecalculationTaskRepository extends JpaRepository<ScoreRecalculationTask, Long> {

    /**
     * 查询同一范围（学校 + 任务类型 + 目标 ID）下指定状态的任务（用于 41005 冲突校验）。
     * 状态过滤：0=待执行 1=执行中（进行中任务）。
     */
    List<ScoreRecalculationTask> findBySchoolIdAndTaskTypeAndTargetIdAndStatusIn(
            Long schoolId, Integer taskType, Long targetId, Collection<Integer> statuses);

    /**
     * 查询同一范围（学校 + 任务类型 + 学期）下指定状态的任务。
     * 用于 targetType=3(指定学期) 的冲突校验——此时以 semesterId 为范围标识而非 targetId。
     */
    List<ScoreRecalculationTask> findBySchoolIdAndTaskTypeAndSemesterIdAndStatusIn(
            Long schoolId, Integer taskType, Long semesterId, Collection<Integer> statuses);

    /**
     * 查询学校下指定任务类型、指定状态的任务（用于 targetType=4(全量重算) 的冲突校验）。
     */
    List<ScoreRecalculationTask> findBySchoolIdAndTaskTypeAndStatusIn(
            Long schoolId, Integer taskType, Collection<Integer> statuses);

    /**
     * 按状态列表查询任务（用于启动时恢复被中断的任务，status=0 待执行 / 1 执行中）。
     */
    List<ScoreRecalculationTask> findByStatusIn(Collection<Integer> statuses);

    /**
     * 批量把被中断的任务标记为失败（启动恢复）。
     * <p>
     * 事务由调用方 Service 层控制；WHERE status IN (:activeStatuses) 在执行时重估，
     * 幂等，不会误伤本会话中新创建或已完成的任务。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ScoreRecalculationTask t SET t.status = :failed, t.errorMessage = :errorMessage, "
            + "t.completedAt = :now WHERE t.status IN :activeStatuses")
    int markInterruptedAsFailed(@Param("activeStatuses") Collection<Integer> activeStatuses,
                                @Param("failed") Integer failed,
                                @Param("errorMessage") String errorMessage,
                                @Param("now") LocalDateTime now);
}
