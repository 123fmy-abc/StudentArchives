package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 学科竞赛扩展表 Repository
 */
@Repository
public interface ArchiveCompetitionRepository extends JpaRepository<ArchiveCompetition, Long> {

    Optional<ArchiveCompetition> findByArchiveId(Long archiveId);

    /** 按档案 ID 列表批量查询（评分重算读取竞赛等级数据源） */
    List<ArchiveCompetition> findByArchiveIdIn(Collection<Long> archiveIds);
}
