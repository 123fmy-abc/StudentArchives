package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.SemesterImportRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 学期批量导入记录 Repository
 */
@Repository
public interface SemesterImportRecordRepository extends JpaRepository<SemesterImportRecord, Long> {

    /** 按学校查询导入记录，id 倒序（最新的在前） */
    Page<SemesterImportRecord> findBySchoolIdOrderByIdDesc(Long schoolId, Pageable pageable);
}
