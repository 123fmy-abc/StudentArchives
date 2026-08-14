package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveResearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 学术研究扩展表 Repository
 */
@Repository
public interface ArchiveResearchRepository extends JpaRepository<ArchiveResearch, Long> {

    Optional<ArchiveResearch> findByArchiveId(Long archiveId);

    /** 按档案 ID 列表批量查询（评分重算读取科研项目/论文数据源） */
    List<ArchiveResearch> findByArchiveIdIn(Collection<Long> archiveIds);
}
