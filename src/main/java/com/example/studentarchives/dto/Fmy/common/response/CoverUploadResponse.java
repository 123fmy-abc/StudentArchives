package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封面上传响应 DTO（POST /common/upload/cover）
 * <p>
 * 上传成功后返回封面完整访问 URL 及 OSS 对象路径。
 * 封面 URL 默认 30 天有效，过期后需重新上传或刷新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoverUploadResponse {

    /** 封面完整访问 URL（带签名，30 天有效） */
    private String coverUrl;

    /** OSS 对象路径（cover/photo/{yyyy-MM}/{uuid}.{ext}） */
    private String objectKey;
}
