package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * 成长事件计分规则（growth_event_score_rules）
 * <p>
 * 每条规则对应一种成长时间轴事件类型（event_type，见 {@link com.example.studentarchives.enums.EventTypeEnum}），
 * {@code abilities} 为该类事件的能力维度得分项 JSON：
 * {@code [{"dimensionCode":"xxx","score":10.00}]}，dimensionCode 对应
 * {@code ability_dimensions.dimension_code}（展示型引用，非外键）。
 * <p>
 * 学生新增/修改成长时间轴事件时，能力得分由本表按事件类型自动生成，
 * 学生不再自填分数；无规则的事件类型不产生能力得分行。
 * 规则全局生效（与 ability_dimensions 一致，无 school_id）。
 */
@Getter
@Setter
@Entity
@Table(name = "growth_event_score_rules")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class GrowthEventScoreRule extends BaseEntity {

    /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
    @Column(name = "event_type", nullable = false)
    private Integer eventType;

    /**
     * 该类事件的能力维度得分项 JSON（String + columnDefinition = "JSON"，
     * 同 {@link EvaluationIndicator#getScoringRule()} 的存储方式）
     */
    @Column(name = "abilities", nullable = false, columnDefinition = "JSON")
    private String abilities;

    /** 备注 */
    @Column(name = "remark", length = 255)
    private String remark;
}
