package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交档案导出响应 DTO（POST /profile/export）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportSubmitResponse {

    private Long exportJobId;

    /** 0=待处理 1=处理中 2=已完成 3=失败 */
    private Integer status;

    private String statusLabel;

    /** 生成的文件 ID（file_uploads.id），7 天内可通过 /common/files/{fileId}/download 重新下载 */
    private Long fileId;

    /** 下载链接（签名 URL，有效期由 oss.url-expire-minutes 配置） */
    private String downloadUrl;

    /** 原始文件名，供前端设置 download 属性 */
    private String originalName;

    /** 下载链接过期时间（ISO 8601 带时区） */
    private String expireAt;
}
