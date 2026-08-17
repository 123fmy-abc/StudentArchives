package com.example.studentarchives.dto.Fmy.archive.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 组织档案汇总响应 DTO（GET /admin/archives/overview，文档 15.3）
 * <p>
 * orgType 为汇总维度（2=学院 3=专业 4=班级 6=年级，不传默认全校单条），
 * rows 为各组织维度下钻的汇总行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveOverviewResponse {

    /** 汇总维度：1=学校 2=学院 3=专业 4=班级 6=年级 */
    private Integer orgType;

    /** 汇总行 */
    private List<ArchiveOverviewRow> rows;
}
