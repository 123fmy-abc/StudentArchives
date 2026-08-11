package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "indicator_rule_versions")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class IndicatorRuleVersion extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    /** 发布版本归属学期（semesters.id，null=不限定学期），支持按学期过滤指标树 */
    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "version_name", nullable = false, length = 100)
    private String versionName;

    @Column(name = "effective_at")
    private LocalDateTime effectiveAt;

    /** 发布时点的完整指标树 JSON 快照（含指标结构字段），供按历史版本查询指标树 */
    @Column(name = "tree_snapshot", columnDefinition = "JSON")
    private String treeSnapshot;

    @Column(name = "created_by")
    private Long createdBy;

}
