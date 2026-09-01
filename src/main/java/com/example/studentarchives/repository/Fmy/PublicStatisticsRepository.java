package com.example.studentarchives.repository.Fmy;

import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 登录页公开统计专用 Repository
 * <p>
 * 仅承载登录页统计所需的计数查询。软删除记录由实体上的
 * {@code @SQLRestriction} 自动排除（与 ArchiveRepository 等保持一致），
 * 多 Repository 绑定同一实体的模式见 AdminArchiveRepository。
 */
@Repository
public interface PublicStatisticsRepository extends JpaRepository<Archive, Long> {

    /**
     * 统计指定状态的档案数（archives.status，待审核=1）
     */
    @Query("SELECT COUNT(a) FROM Archive a WHERE a.status = :status")
    long countArchivesByStatus(@Param("status") Integer status);

    /**
     * 统计指定状态的奖项申报数（award_applications.status，待审核=1）
     */
    @Query("SELECT COUNT(a) FROM AwardApplication a WHERE a.status = :status")
    long countAwardApplicationsByStatus(@Param("status") Integer status);
}
