package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.OrgArchiveSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 组织档案汇总快照补充 Repository（对应表 org_archive_summaries）
 * <p>
 * 管理端统计看板与可视化模块按组织维度（org_type）读取最新 stat_date 的
 * 每日快照，避免全量聚合打库。学期参数可空（快照可能不区分学期）。
 */
@Repository
public interface AdminOrgArchiveSummaryRepository extends JpaRepository<OrgArchiveSummary, Long> {

    /**
     * 查询某组织最新快照（按 stat_date 最大取当日快照）
     */
    @Query("SELECT o FROM OrgArchiveSummary o " +
            "WHERE o.orgType = :orgType AND o.orgId = :orgId " +
            "AND (:semesterId IS NULL OR o.semesterId = :semesterId) " +
            "AND o.statDate = (SELECT MAX(o2.statDate) FROM OrgArchiveSummary o2 " +
            "  WHERE o2.orgType = :orgType AND o2.orgId = :orgId " +
            "  AND (:semesterId IS NULL OR o2.semesterId = :semesterId))")
    Optional<OrgArchiveSummary> findLatestByOrg(@Param("orgType") Integer orgType,
                                                @Param("orgId") Long orgId,
                                                @Param("semesterId") Long semesterId);

    /**
     * 查询某学校指定组织维度下全部最新快照（学校/学院/专业/班级各层级行）
     */
    @Query("SELECT o FROM OrgArchiveSummary o " +
            "WHERE o.orgType = :orgType AND o.schoolId = :schoolId " +
            "AND (:semesterId IS NULL OR o.semesterId = :semesterId) " +
            "AND o.statDate = (SELECT MAX(o2.statDate) FROM OrgArchiveSummary o2 " +
            "  WHERE o2.orgType = :orgType AND o2.schoolId = :schoolId " +
            "  AND (:semesterId IS NULL OR o2.semesterId = :semesterId))")
    List<OrgArchiveSummary> findLatestByLevel(@Param("orgType") Integer orgType,
                                              @Param("schoolId") Long schoolId,
                                              @Param("semesterId") Long semesterId);

    /**
     * 按业务键删除历史快照（刷新前幂等清理）
     */
    @Modifying
    @Query("DELETE FROM OrgArchiveSummary o " +
            "WHERE o.schoolId = :schoolId " +
            "AND (:semesterId IS NULL OR o.semesterId = :semesterId) " +
            "AND o.orgType = :orgType " +
            "AND o.orgId = :orgId " +
            "AND o.statDate = :statDate")
    int deleteByBusinessKey(@Param("schoolId") Long schoolId,
                            @Param("semesterId") Long semesterId,
                            @Param("orgType") Integer orgType,
                            @Param("orgId") Long orgId,
                            @Param("statDate") LocalDate statDate);

    /**
     * 删除某学校某学期某统计日的全部快照（刷新前幂等清理，含学校级与行级维度）
     */
    @Modifying
    @Query("DELETE FROM OrgArchiveSummary o " +
            "WHERE o.schoolId = :schoolId " +
            "AND (:semesterId IS NULL OR o.semesterId = :semesterId) " +
            "AND o.statDate = :statDate")
    int deleteBySchoolIdAndSemesterIdAndStatDate(@Param("schoolId") Long schoolId,
                                                  @Param("semesterId") Long semesterId,
                                                  @Param("statDate") LocalDate statDate);
}
