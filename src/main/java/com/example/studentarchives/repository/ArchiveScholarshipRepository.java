package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveScholarship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 奖学金扩展表 Repository
 */
@Repository
public interface ArchiveScholarshipRepository extends JpaRepository<ArchiveScholarship, Long> {

    Optional<ArchiveScholarship> findByArchiveId(Long archiveId);

    /** 按档案 ID 列表批量查询（评分重算读取荣誉称号等级数据源） */
    List<ArchiveScholarship> findByArchiveIdIn(Collection<Long> archiveIds);
}
