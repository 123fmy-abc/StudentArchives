package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.AuditCommentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审核意见模板 Repository（对应表 audit_comment_templates）
 * <p>
 * 供教师端「待审核任务模块」4.7 获取常用退回原因模板。
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface AuditCommentTemplateRepository extends JpaRepository<AuditCommentTemplate, Long> {

    /** 查询某学校下指定状态的模板（退回原因 category=2），Service 层按使用频率/排序排序 */
    List<AuditCommentTemplate> findBySchoolIdAndStatus(Long schoolId, Integer status);
}