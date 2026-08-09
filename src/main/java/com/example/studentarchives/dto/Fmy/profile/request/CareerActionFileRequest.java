package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传行动成果文件请求 DTO（POST /profile/career-plans/{planId}/actions/{actionId}/files）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerActionFileRequest {

    /** 已上传文件 ID（file_uploads.id） */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
}
