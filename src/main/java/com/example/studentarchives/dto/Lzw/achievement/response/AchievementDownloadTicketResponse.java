package com.example.studentarchives.dto.Lzw.achievement.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成果附件下载凭证（GET /achievements/{id}/files/{fileId}/download）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDownloadTicketResponse {

    /** 短时效签名地址 */
    private String url;

    /** 下载文件名 */
    private String fileName;

    private String mimeType;

    private Long size;

    /** 过期时间（ISO 8601） */
    private String expiresAt;
}
