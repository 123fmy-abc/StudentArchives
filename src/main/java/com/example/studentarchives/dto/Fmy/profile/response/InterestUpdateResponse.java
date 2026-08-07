package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新个人兴趣标签响应 DTO（PUT /profile/interests）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterestUpdateResponse {

    /** 更新条数 */
    private Integer updatedCount;

    /** 新增条数 */
    private Integer addedCount;
}
