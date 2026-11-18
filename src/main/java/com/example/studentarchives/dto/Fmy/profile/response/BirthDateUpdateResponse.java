package com.example.studentarchives.dto.Fmy.profile.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新出生日期响应 DTO（PUT /profile/birth-date）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthDateUpdateResponse {

    /** 出生日期，格式 YYYY-MM-DD；清空后返回 null */
    private String birthDate;
}
