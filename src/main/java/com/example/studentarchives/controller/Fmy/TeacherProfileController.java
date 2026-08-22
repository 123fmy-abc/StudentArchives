package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.profile.request.ContactUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.response.ContactUpdateResponse;
import com.example.studentarchives.dto.Fmy.profile.response.TeacherProfileResponse;
import com.example.studentarchives.service.Fmy.ProfileService;
import com.example.studentarchives.service.Fmy.TeacherProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端个人中心控制器
 * <p>
 * 提供教师端个人中心模块接口（《教师端接口文档》十四、个人中心模块）。
 * 个人信息获取（14.1）：复用 {@link TeacherProfileService#getTeacherProfile}——
 * 公共字段复用 {@link AuthService#getCurrentUser}（GET /auth/me）组装，
 * 教师特有字段 collegeName / title 补充自 teacher_profiles。
 * 联系信息更新（14.2）与学生端一致：直接复用 {@link ProfileService#updateContact}，
 * 数据写入 user_contact_infos 表（按 user_id 区分，学生/教师共用，无学生特有逻辑），
 * 请求/响应 DTO 复用 {@link ContactUpdateRequest} / {@link ContactUpdateResponse}。
 * <p>
 * 头像字段不在此接口维护：复用通用上传头像接口 POST /common/upload/avatar
 * （直接写入 user_contact_infos.avatar，与学生端一致）。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/profile")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final ProfileService profileService;
    private final TeacherProfileService teacherProfileService;

    /**
     * 获取教师个人信息（14.1）
     * <p>
     * 复用 {@link AuthService#getCurrentUser}（GET /auth/me）组装公共字段，
     * 再补充 teacher_profiles 的 collegeName（college_id → colleges.name）与 title；
     * 教师档案缺失时 collegeName / title 返回 null。
     *
     * @param userId 当前登录用户 ID
     * @return 教师个人信息响应
     */
    @GetMapping
    public ApiResult<TeacherProfileResponse> getProfile(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(teacherProfileService.getTeacherProfile(userId));
    }

    /**
     * 更新个人联系信息（14.2）
     * <p>
     * 复用学生端 PUT /profile/contact（4.1.1）的全量更新语义：
     * email / phone / address 三字段均必填，缺失任一字段返回参数校验错误，
     * 传空字符串表示清空对应字段；头像字段不在请求内，由 POST /common/upload/avatar 维护。
     *
     * @param userId  当前登录用户 ID
     * @param request 联系信息更新请求
     * @return 更新后的联系信息
     */
    @PutMapping("/contact")
    public ApiResult<ContactUpdateResponse> updateContact(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ContactUpdateRequest request) {
        return ApiResult.success("更新成功", profileService.updateContact(userId, request));
    }
}
