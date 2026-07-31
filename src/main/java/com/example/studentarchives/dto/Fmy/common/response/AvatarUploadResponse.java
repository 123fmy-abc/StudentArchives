package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传响应 DTO
 * <p>
 * 上传成功后返回头像的完整访问 URL 及 OSS 对象路径。
 * 头像 URL 默认 30 天有效，过期后需重新上传或刷新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvatarUploadResponse {

    /** 头像完整访问 URL（带签名，30 天有效） */
    private String avatarUrl;

    /** OSS 对象路径（avatar/{uuid}.{ext}） */
    private String objectKey;
}
