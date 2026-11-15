package com.example.studentarchives.dto.Fmy.growthscorerule.response;

import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成长事件计分规则响应（GET / POST / PUT /admin/growth-event-score-rules）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthScoreRuleResponse {

    /** 规则 ID */
    private Long id;

    /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
    private Integer eventType;

    /** 事件类型标签（奖项/成绩/实践/职业规划/短板改进/能力提升） */
    private String eventTypeLabel;

    /** 该类事件的能力维度得分项 */
    private List<GrowthScoreRuleItem> abilities;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
