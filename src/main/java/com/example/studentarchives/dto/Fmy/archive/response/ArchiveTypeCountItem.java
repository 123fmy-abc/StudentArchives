package com.example.studentarchives.dto.Fmy.archive.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 档案类型分布项 DTO（GET /admin/archives/overview，文档 15.3）
 * <p>
 * 统计某组织维度下各档案类型的档案数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveTypeCountItem {

    /** 档案类型编码，如 competition / scholarship */
    private String archiveType;

    /** 该类型档案数量 */
    private Integer count;
}
