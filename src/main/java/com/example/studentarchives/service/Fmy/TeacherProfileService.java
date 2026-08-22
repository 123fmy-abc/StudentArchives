package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.auth.response.UserInfoResponse;
import com.example.studentarchives.dto.Fmy.profile.response.TeacherProfileResponse;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.user.TeacherProfile;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师端个人中心服务
 * <p>
 * 提供教师端个人中心模块数据查询（《教师端接口文档》十四、个人中心模块）。
 * 14.1 获取教师个人信息：复用 {@link AuthService#getCurrentUser}（GET /auth/me）组装公共字段，
 * 再补充 {@code teacher_profiles} 的 {@code collegeName}（college_id → colleges.name）与
 * {@code title} 两个教师特有字段。
 * 14.2 更新联系信息直接复用 {@link ProfileService#updateContact}（见 {@link TeacherProfileController}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherProfileService {

    private final AuthService authService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CollegeRepository collegeRepository;

    /**
     * 获取教师个人信息（GET /teacher/profile，14.1）
     * <p>
     * 复用 {@link AuthService#getCurrentUser} 组装 userId / userNo / name / email / phone /
     * gender / genderLabel / roles / scopes / avatar 等公共字段（users / user_contact_infos /
     * user_roles / role_scopes），再补充教师特有字段：collegeName（teacher_profiles.college_id
     * → colleges.name）与 title（teacher_profiles.title）。教师档案缺失时两字段返回 null。
     *
     * @param userId 当前登录用户 ID
     * @return 教师个人信息响应
     */
    @Transactional(readOnly = true)
    public TeacherProfileResponse getTeacherProfile(Long userId) {
        UserInfoResponse base = authService.getCurrentUser(userId);

        TeacherProfile profile = teacherProfileRepository.findByUserId(userId).orElse(null);
        String collegeName = null;
        if (profile != null && profile.getCollegeId() != null) {
            College college = collegeRepository.findById(profile.getCollegeId()).orElse(null);
            collegeName = college != null ? college.getName() : null;
        }

        return TeacherProfileResponse.builder()
                .userId(base.getUserId())
                .userNo(base.getUserNo())
                .name(base.getName())
                .email(base.getEmail())
                .phone(base.getPhone())
                .gender(base.getGender())
                .genderLabel(base.getGenderLabel())
                .collegeName(collegeName)
                .title(profile != null ? profile.getTitle() : null)
                .roles(base.getRoles())
                .scopes(base.getScopes())
                .avatar(base.getAvatar())
                .build();
    }
}
