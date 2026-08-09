package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传行动成果文件响应 DTO（POST /profile/career-plans/{planId}/actions/{actionId}/files）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerActionFileResponse {

    private Long fileId;

    private String fileName;

    private String fileUrl;
}
