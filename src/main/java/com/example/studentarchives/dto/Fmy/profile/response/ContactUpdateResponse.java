package com.example.studentarchives.dto.Fmy.profile.response;

import com.example.studentarchives.annotation.Sensitive;
import com.example.studentarchives.enums.SensitiveType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新个人联系信息响应 DTO（PUT /profile/contact）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactUpdateResponse {

    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Sensitive(SensitiveType.PHONE)
    private String phone;

    private String avatar;

    private String address;

    private String emergencyName;

    private String emergencyRelation;

    @Sensitive(SensitiveType.PHONE)
    private String emergencyPhone;
}
