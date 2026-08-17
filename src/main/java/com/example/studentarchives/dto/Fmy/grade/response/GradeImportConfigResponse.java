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
