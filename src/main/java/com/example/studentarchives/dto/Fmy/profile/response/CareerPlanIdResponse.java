package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 职业规划子项添加响应 DTO（goalId / actionId / milestoneId / reflectionId 通用）
 * <p>
 * 单个添加接口仅填充对应字段，其余为 null，经 @JsonInclude(NON_NULL) 省略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerPlanIdResponse {

    private Long goalId;

    private Long actionId;

    private Long milestoneId;

    private Long reflectionId;
}
