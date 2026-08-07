package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportTemplateField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 导出模板字段映射 Repository（对应表 export_template_fields）
 */
@Repository
public interface ExportTemplateFieldRepository extends JpaRepository<ExportTemplateField, Long> {

    List<ExportTemplateField> findByTemplateIdOrderBySortAsc(Long templateId);
}
