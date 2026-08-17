package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上级组织信息 DTO（GET /admin/statistics/overview，文档 16.2）
 * <p>
 * 当前下钻范围的上级组织（学校/学院/专业），用于看板顶部面包屑定位。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsParentOrg {

    /** 组织 ID */
    private Long orgId;

    /** 组织名称 */
    private String orgName;
}
