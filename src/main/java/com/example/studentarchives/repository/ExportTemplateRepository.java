package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 导出模板 Repository（对应表 export_templates）
 */
@Repository
public interface ExportTemplateRepository extends JpaRepository<ExportTemplate, Long> {

    /**
     * 查询指定学校、业务类型下的默认启用模板（取版本号最新一条）
     */
    Optional<ExportTemplate> findFirstBySchoolIdAndExportTypeAndIsDefaultAndStatusOrderByVersionDesc(
            Long schoolId, String exportType, Integer isDefault, Integer status);

    /**
     * 查询指定学校、业务类型下全部启用模板（未配置默认模板时按创建顺序兜底取一条）
     */
    List<ExportTemplate> findBySchoolIdAndExportTypeAndStatusOrderByIdAsc(
            Long schoolId, String exportType, Integer status);

    /**
     * 查询指定学校下某模板编码的启用记录（种子器幂等 / 升级补全用）
     */
    Optional<ExportTemplate> findBySchoolIdAndTemplateCodeAndStatus(Long schoolId, String templateCode, Integer status);
}
