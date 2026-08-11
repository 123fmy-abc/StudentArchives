package com.example.studentarchives.dto.Fmy.auth.response;

import com.example.studentarchives.annotation.Sensitive;
import com.example.studentarchives.enums.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户信息响应 DTO
 * <p>
 * 用于 /auth/me 接口，比登录返回的 user 对象多了 phone 和 permissions。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long userId;
    private String userNo;
    private String name;
    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Sensitive(SensitiveType.PHONE)
    private String phone;
    private Integer gender;
    private String genderLabel;
    private Long schoolId;
    private String schoolName;
    private List<String> roles;
    private List<String> roleNames;
    private List<String> permissions;
    private List<UserScopeInfoResponse> scopes;
    private String avatar;
}
