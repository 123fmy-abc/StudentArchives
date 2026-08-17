package com.example.studentarchives.repository;

import com.example.studentarchives.entity.grade.GradeImportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 成绩导入配置 Repository（对应表 grade_import_configs）
 * <p>
 * 每个学校至多一条启用配置（uk_gic_school_id 条件唯一），用于读取允许扩展名、
 * 模板列定义、是否含表头等导入规则。未配置时由服务侧回退内置默认模板。
 */
@Repository
public interface GradeImportConfigRepository extends JpaRepository<GradeImportConfig, Long> {

    /** 按学校查询启用状态的导入配置 */
    Optional<GradeImportConfig> findBySchoolIdAndStatus(Long schoolId, Integer status);

    /** 按学校查询导入配置（不限制状态，用于一校一条的 CRUD） */
    Optional<GradeImportConfig> findBySchoolId(Long schoolId);

    /**
     * 软删除成绩导入配置（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数（0 表示配置不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE grade_import_configs SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
