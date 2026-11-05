package com.example.studentarchives.dto.Fmy.grade.response;

import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigColumnItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成绩导入配置响应 DTO（GET /admin/grade-import-configs）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeImportConfigResponse {

    private Long id;

    /** 当前学校是否已创建自定义配置；false 表示返回系统内置默认模板 */
    private Boolean configured;

    /** 配置来源：SCHOOL=学校自定义，DEFAULT=系统默认 */
    private String source;

    private Long schoolId;

    private List<String> allowedExtensions;

    private Long maxFileSize;

    private List<GradeImportConfigColumnItem> templateColumns;

    private Integer hasHeaderRow;

    private Integer batchSize;

    private Integer allowOverwrite;

    private Integer status;

    private Long createdBy;

    private String createdAt;

    private String updatedAt;
}
