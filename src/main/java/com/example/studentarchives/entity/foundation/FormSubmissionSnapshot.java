package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * 表单提交快照（对应表 form_submission_snapshots）
 * <p>
 * 叠加式方案A：原有强类型扩展表仍是唯一主数据源，本表仅在业务写入成功后于同一事务内
 * 顺带落一份提交请求 DTO 的 JSON 快照，用于归档/历史留痕；不参与业务查询/导出/审批。
 */
@Getter
@Setter
@Entity
@Table(name = "form_submission_snapshots")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class FormSubmissionSnapshot extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    /** 提交请求 DTO 序列化 JSON 快照 */
    @Column(name = "data", columnDefinition = "JSON")
    private String data;

    @Column(name = "created_by")
    private Long createdBy;
}
