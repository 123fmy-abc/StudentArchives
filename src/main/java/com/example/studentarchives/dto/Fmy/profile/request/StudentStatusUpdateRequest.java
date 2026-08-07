package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新学生状态请求 DTO（PUT /profile/student-status）
 * <p>
 * 仅可更新 student_profiles.student_status。全量更新语义：studentStatus 必填，
 * 缺失直接报参数校验错误。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatusUpdateRequest {

    /** 学生状态编码：current / fresh_graduate / graduated */
    @NotBlank(message = "学生状态不能为空")
    @Size(max = 30, message = "学生状态编码长度不能超过30")
    private String studentStatus;
}
