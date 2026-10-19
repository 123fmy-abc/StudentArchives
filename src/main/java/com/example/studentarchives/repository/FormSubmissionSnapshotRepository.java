package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.FormSubmissionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 表单提交快照 Repository（对应表 form_submission_snapshots）
 * <p>
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 对本 Repository 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface FormSubmissionSnapshotRepository extends JpaRepository<FormSubmissionSnapshot, Long> {

    /**
     * 按业务类型 + 业务主键查询快照（供后续回显，按 ID 倒序）。
     */
    List<FormSubmissionSnapshot> findByBusinessTypeAndBusinessIdOrderByIdDesc(String businessType, Long businessId);
}
