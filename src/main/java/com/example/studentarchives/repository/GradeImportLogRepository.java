package com.example.studentarchives.repository;

import com.example.studentarchives.entity.grade.GradeImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 成绩导入历史 Repository（对应表 grade_import_logs）
 * <p>
 * 管理端成绩导入模块专用。列表按学期/导入状态动态筛选时使用 Specification 组合查询。
 */
@Repository
public interface GradeImportLogRepository
        extends JpaRepository<GradeImportLog, Long>, JpaSpecificationExecutor<GradeImportLog> {

    /** 查询学校最新一条导入配置（按 id 倒序取第一条） */
    Optional<GradeImportLog> findTopBySchoolIdOrderByIdDesc(Long schoolId);
}
