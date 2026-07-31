package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResponse {

    /** 文件 ID（对应 file_uploads.id） */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** OSS 临时签名 URL */
    private String fileUrl;

    /** OSS 对象路径 */
    private String objectKey;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型（MIME Type） */
    private String fileType;
}
