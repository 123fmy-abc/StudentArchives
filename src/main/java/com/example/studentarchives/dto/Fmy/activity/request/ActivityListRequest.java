package com.example.studentarchives.dto.Fmy.activity.request;

import com.example.studentarchives.common.PageParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 动态记录列表查询请求（GET /activities）
 * <p>
 * 数据来源：archives / award_applications / career_plans 表聚合查询。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivityListRequest extends PageParam {

    /** 记录类型（不传=全部）：archive / award / career_plan */
    private String type;

    /** 档案/奖项类型编码（如 academic_competition, competition_star） */
    @JsonProperty("archive_type")
    private String archiveType;

    /** 状态：0=草稿 1=待审批 2=通过 3=已退回 4=已撤销 */
    private Integer status;

    /** 学期 ID */
    @JsonProperty("semester_id")
    private Long semesterId;

    /** 搜索关键词（匹配标题） */
    private String keyword;
}
