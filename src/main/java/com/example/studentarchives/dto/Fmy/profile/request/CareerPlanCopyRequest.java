package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 复制上一学期计划请求 DTO（POST /profile/career-plans/copy）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerPlanCopyRequest {

    /** 源学期 ID */
    @NotNull(message = "源学期不能为空")
    private Long sourceSemesterId;

    /** 目标学期 ID */
    @NotNull(message = "目标学期不能为空")
    private Long targetSemesterId;

    /** 新规划标题，不传则自动生成 */
    @Size(max = 255, message = "规划标题长度不能超过255")
    private String title;
}
