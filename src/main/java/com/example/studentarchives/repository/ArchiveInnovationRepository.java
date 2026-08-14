package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveInnovation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 创新创业扩展表 Repository
 */
@Repository
public interface ArchiveInnovationRepository extends JpaRepository<ArchiveInnovation, Long> {

    Optional<ArchiveInnovation> findByArchiveId(Long archiveId);

    /** 按档案 ID 列表批量查询（评分重算读取创新创业项目落地数据源） */
    List<ArchiveInnovation> findByArchiveIdIn(Collection<Long> archiveIds);
}
