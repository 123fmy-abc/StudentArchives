package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新政治面貌请求 DTO（PUT /profile/political-status）
 * <p>
 * 仅可更新 student_profiles.political_status（政治面貌字典编码）。
 * 全量更新语义：politicalStatus 必填，缺失直接报参数校验错误。
 * 学号、姓名、年级、专业、出生日期等学籍信息由教务系统同步，应用层只读。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicInfoUpdateRequest {

    /** 政治面貌字典编码（字典类型 political_status） */
    @NotBlank(message = "政治面貌不能为空")
    @Size(max = 50, message = "政治面貌编码长度不能超过50")
    private String politicalStatus;
}
