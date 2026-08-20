package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端表单模板 Repository（对应表 form_templates）
 * <p>
 * 承载《管理端接口文档》十七、表单自定义模板模块（17.1~17.6）所需的派生查询与软删除。
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 对本 Repository 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface AdminFormTemplateRepository extends JpaRepository<FormTemplate, Long> {

    /**
     * 查询某学校某类别下的全部模板（含禁用、未删除）。
     * 用于列表筛选与设置默认模板时批量取消同 schoolId + code + category 下的原默认。
     */
    List<FormTemplate> findBySchoolIdAndCategory(Long schoolId, String category);

    /**
     * 查询某学校某类别某编码下的模板（含禁用、未删除），用于创建/更新时校验编码唯一。
     * 唯一约束为 UNIQUE(school_id, code, category, is_deleted_null)（软删除条件下同校同编码同类别唯一）。
     */
    List<FormTemplate> findBySchoolIdAndCategoryAndCode(Long schoolId, String category, String code);

    /**
     * 软删除模板（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数（0 表示模板不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE form_templates SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
