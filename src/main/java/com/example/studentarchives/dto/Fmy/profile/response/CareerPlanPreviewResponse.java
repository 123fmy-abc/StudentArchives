package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 职业规划文件预览响应 DTO（GET /profile/career-plans/{planId}/preview）
 * <p>
 * 与下载共用同一份生成/缓存文件，返回 OSS 签名 inline 预览 URL（response-content-disposition=inline），
 * 前端可在新标签页内嵌渲染 PDF。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerPlanPreviewResponse {

    /** OSS 签名在线预览 URL（浏览器内嵌渲染 PDF） */
    private String previewUrl;

    /** 文件名 */
    private String fileName;

    /** 导出用途：internal（带屏幕水印预览）/ external（无水印预览） */
    private String purpose;

    /** 生成时间（ISO 8601 带时区） */
    private String generatedAt;
}
