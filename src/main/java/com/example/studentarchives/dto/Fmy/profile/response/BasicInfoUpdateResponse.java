package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新政治面貌响应 DTO（PUT /profile/political-status）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicInfoUpdateResponse {

    /** 政治面貌字典编码 */
    private String politicalStatus;

    /** 政治面貌展示名称 */
    private String politicalStatusLabel;
}
