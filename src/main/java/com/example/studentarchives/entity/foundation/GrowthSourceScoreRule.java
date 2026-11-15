package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * 成长事件计分规则（growth_source_score_rules）
 * <p>
 * 每条规则对应一种来源记录类型（source_type，见
 * {@link com.example.studentarchives.enums.GrowthSourceTypeEnum}），
 * {@code abilities} 为该类来源记录的维度得分项 JSON：
 * {@code [{"dimensionCode":"xxx","score":10.00}]}，dimensionCode 对应
 * {@code ability_dimensions.dimension_code}（展示型引用，非外键）。
 * <p>
 * 学生新增/修改成长时间轴事件时，能力得分由本表按来源记录类型自动生成，
 * 学生不自填分数；无规则的来源类型不产生能力得分行。
 * 规则全局生效（与 ability_dimensions 一致，无 school_id）。
 */
@Getter
@Setter
@Entity
@Table(name = "growth_source_score_rules")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class GrowthSourceScoreRule extends BaseEntity {

    /** 来源记录类型：archives / award_applications / archive_internships 等 */
    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    /**
     * 该类来源记录的维度得分项 JSON（String + columnDefinition = "JSON"，
     * 同 {@link EvaluationIndicator#getScoringRule()} 的存储方式）
     */
    @Column(name = "abilities", nullable = false, columnDefinition = "JSON")
    private String abilities;

    /** 备注 */
    @Column(name = "remark", length = 255)
    private String remark;
}
