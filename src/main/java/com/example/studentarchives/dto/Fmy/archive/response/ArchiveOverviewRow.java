package com.example.studentarchives.dto.Fmy.archive.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 组织档案汇总行 DTO（GET /admin/archives/overview，文档 15.3）
 * <p>
 * 某组织维度（年级/学院/专业/班级）下的档案总数与各状态数量、档案类型分布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveOverviewRow {

    /** 组织 ID（年级维度时为 0） */
    private Long orgId;

    /** 组织名称（年级维度时为年级名称，如 2023级） */
    private String orgName;

    /** 学生数量 */
    private Integer studentCount;

    /** 档案总数 */
    private Integer totalArchives;

    /** 已提交数量（status!=0 草稿，即提交过） */
    private Integer submittedCount;

    /** 通过数量（status=2） */
    private Integer approvedCount;

    /** 待审批数量（status=1） */
    private Integer pendingCount;

    /** 已退回数量（status=3） */
    private Integer rejectedCount;

    /** 草稿数量（status=0） */
    private Integer draftCount;

    /** 已撤销数量（status=4） */
    private Integer revokedCount;

    /** 档案类型分布 */
    private List<ArchiveTypeCountItem> archiveTypeDistribution;
}
