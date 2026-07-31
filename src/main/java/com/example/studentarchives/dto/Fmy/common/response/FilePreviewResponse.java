package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件预览响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilePreviewResponse {

    /** 文件 ID */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 文件访问 URL */
    private String fileUrl;

    /** OSS 对象路径 */
    private String objectKey;

    /** 文件类型（MIME Type） */
    private String fileType;

    /** 预览 URL（可预览文件才返回） */
    private String previewUrl;

    /** 是否可预览 */
    private Boolean canPreview;
}
