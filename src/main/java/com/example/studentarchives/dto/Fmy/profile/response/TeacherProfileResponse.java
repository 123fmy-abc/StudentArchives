package com.example.studentarchives.dto.Fmy.profile.response;

import com.example.studentarchives.annotation.Sensitive;
import com.example.studentarchives.dto.Fmy.auth.response.UserScopeInfoResponse;
import com.example.studentarchives.enums.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 教师个人信息响应 DTO
 * <p>
 * 用于教师端个人中心 GET /teacher/profile（14.1），字段与文档契约精确对齐。
 * 数据来源：users、user_contact_infos、user_roles/role_scopes（复用 /auth/me 组装），
 * collegeName / title 补充自 teacher_profiles（college_id → colleges.name）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileResponse {

    private Long userId;
    private String userNo;
    private String name;

    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Sensitive(SensitiveType.PHONE)
    private String phone;
    private Integer gender;
    private String genderLabel;

    /** 所属学院名称（teacher_profiles.college_id → colleges.name） */
    private String collegeName;

    /** 职称（teacher_profiles.title） */
    private String title;
    private List<String> roles;
    private List<UserScopeInfoResponse> scopes;
    private String avatar;
}
