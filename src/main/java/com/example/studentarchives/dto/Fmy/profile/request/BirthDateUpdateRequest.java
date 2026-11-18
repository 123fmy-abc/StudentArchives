package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新出生日期请求 DTO（PUT /profile/birth-date）
 * <p>
 * 出生日期由学生本人维护（管理员也可在用户管理中纠错），
 * 格式固定为 YYYY-MM-DD，且不得晚于当天；传空字符串表示清空。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BirthDateUpdateRequest {

    /** 出生日期，格式 YYYY-MM-DD；空字符串表示清空 */
    @NotNull(message = "出生日期不能为空，如需清空请传空字符串")
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "出生日期格式不正确，应为 YYYY-MM-DD")
    private String birthDate;
}
