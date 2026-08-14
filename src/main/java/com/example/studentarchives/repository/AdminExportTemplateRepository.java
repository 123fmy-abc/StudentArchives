package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端导出模板 Repository（对应表 export_templates）
 * <p>
 * 与 {@link ExportTemplateRepository} 并存，仅承载《管理端接口文档》五、数据导出模块
 * （5.3~5.8 导出模板管理）所需的派生查询与软删除，避免改动既有 Repository。
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 对两个 Repository 均生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface AdminExportTemplateRepository extends JpaRepository<ExportTemplate, Long> {

    /**
     * 查询某学校某导出类型下的全部模板（含禁用，用于设置默认模板时批量取消原默认）
     */
    List<ExportTemplate> findBySchoolIdAndExportType(Long schoolId, String exportType);

    /**
     * 查询某学校下指定模板编码的模板（含禁用、未删除，用于创建/更新时校验编码唯一）
     */
    List<ExportTemplate> findBySchoolIdAndTemplateCode(Long schoolId, String templateCode);

    /**
     * 软删除模板（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数（0 表示模板不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE export_templates SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
