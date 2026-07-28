package com.example.studentarchives.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    /** 访问令牌 */
    private String accessToken;

    /** 令牌类型 */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private long expiresIn;

    /** 刷新令牌 */
    private String refreshToken;

    /** 用户基本信息 */
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String userNo;
        private String name;
        private String email;
        private Integer gender;
        private String genderLabel;
        private Long schoolId;
        private String schoolName;
        private List<String> roles;
        private List<String> roleNames;
        private String avatar;
    }
}
