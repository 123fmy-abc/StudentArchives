package com.example.studentarchives.dto.Fmy.growthscorerule.response;

import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成长事件计分规则响应（GET / POST / PUT /admin/growth-source-score-rules）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthScoreRuleResponse {

    /** 规则 ID */
    private Long id;

    /** 来源记录类型：archives / award_applications / archive_internships 等 */
    private String sourceType;

    /** 来源记录类型标签（学生档案/奖项申报/实习经历 等） */
    private String sourceTypeLabel;

    /** 该类来源记录的维度得分项 */
    private List<GrowthScoreRuleItem> abilities;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
