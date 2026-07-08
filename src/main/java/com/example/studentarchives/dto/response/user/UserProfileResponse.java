package com.example.studentarchives.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String userNo;
    private String name;
    private String email;
    private String phone;
    private Integer gender;
    private String genderLabel;
    private Long schoolId;
    private String schoolName;
    private List<String> roles;
    private List<String> roleNames;
    private List<String> permissions;
    private String avatar;
}
