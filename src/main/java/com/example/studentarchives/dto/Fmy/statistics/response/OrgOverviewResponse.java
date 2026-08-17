package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 组织下钻多维汇总 DTO（GET /admin/statistics/overview，文档 16.2）
 * <p>
 * scopeType 为行级维度（2=学院 3=专业 4=班级 6=年级），parentOrg 为上级组织，
 * rows 为各下级组织汇总行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgOverviewResponse {

    /** 行级维度：2=学院 3=专业 4=班级 6=年级 */
    private Integer scopeType;

    /** 上级组织 */
    private StatisticsParentOrg parentOrg;

    /** 下级组织汇总行 */
    private List<OrgOverviewRow> rows;

    /** 缓存命中标记：true=命中缓存/快照，false=全校/全学院范围聚合缓存未命中返回空集 */
    private Boolean cacheHit;
}
