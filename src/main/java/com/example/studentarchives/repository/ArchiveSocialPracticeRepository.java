package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveSocialPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 社会实践扩展表 Repository
 */
@Repository
public interface ArchiveSocialPracticeRepository extends JpaRepository<ArchiveSocialPractice, Long> {

    Optional<ArchiveSocialPractice> findByArchiveId(Long archiveId);

    /** 按档案 ID 列表批量查询（评分重算读取公益活动/志愿服务数据源） */
    List<ArchiveSocialPractice> findByArchiveIdIn(Collection<Long> archiveIds);
}
