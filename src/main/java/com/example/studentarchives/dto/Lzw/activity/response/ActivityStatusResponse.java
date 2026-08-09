package com.example.studentarchives.dto.Lzw.activity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态记录状态变更响应（编辑/删除/撤回后的统一返回体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatusResponse {

    private Long id;
    private String type;
    private Integer status;

    @JsonProperty("status_label")
    private String statusLabel;

    @JsonProperty("current_version")
    private Integer currentVersion;

    @JsonProperty("submit_count")
    private Integer submitCount;
}
