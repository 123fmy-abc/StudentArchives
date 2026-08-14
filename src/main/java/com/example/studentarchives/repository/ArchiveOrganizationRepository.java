package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 组织履历扩展表 Repository
 */
@Repository
public interface ArchiveOrganizationRepository extends JpaRepository<ArchiveOrganization, Long> {

    Optional<ArchiveOrganization> findByArchiveId(Long archiveId);

    /** 按档案 ID 列表批量查询（评分重算读取组织任职等级数据源） */
    List<ArchiveOrganization> findByArchiveIdIn(Collection<Long> archiveIds);
}
