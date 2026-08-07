package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新学生状态响应 DTO（PUT /profile/student-status）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentStatusUpdateResponse {

    /** 学生状态编码 */
    private String studentStatus;

    /** 学生状态展示名称 */
    private String studentStatusLabel;
}
