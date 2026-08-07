package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 档案证书 Repository
 */
@Repository
public interface ArchiveCertificateRepository extends JpaRepository<ArchiveCertificate, Long> {

    /**
     * 根据档案 ID 列表查询证书信息
     */
    List<ArchiveCertificate> findByArchiveIdIn(List<Long> archiveIds);
}
