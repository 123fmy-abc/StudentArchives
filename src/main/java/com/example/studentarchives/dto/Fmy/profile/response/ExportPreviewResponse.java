package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 档案导出预览响应 DTO（GET /profile/export/preview）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportPreviewResponse {

    /** 可选导出栏目 */
    private List<SectionItem> sections;

    /** 数据版本标识 */
    private String dataVersion;

    /** 生成时间（ISO 8601 带时区） */
    private String generatedAt;

    /**
     * 导出栏目项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionItem {

        /** 栏目编码 */
        private String code;

        /** 栏目名称 */
        private String name;

        /** 是否默认选中 */
        private Boolean selected;

        /** 是否锁定（不可取消） */
        private Boolean disabled;
    }
}
