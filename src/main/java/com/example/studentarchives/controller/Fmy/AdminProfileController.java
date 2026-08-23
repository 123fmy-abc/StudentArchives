package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.profile.request.ContactUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.response.ContactUpdateResponse;
import com.example.studentarchives.service.Fmy.ProfileService;
import com.example.studentarchives.service.common.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端个人中心控制器
 * <p>
 * 提供管理端个人中心模块接口（《管理端接口文档》个人中心模块）。
 * 个人信息获取复用通用 `GET /auth/me`（`AuthService#getCurrentUser`）。
 * 联系信息更新（PUT /admin/profile/contact）与学生端/教师端一致：直接复用
 * {@link ProfileService#updateContact}，数据写入 user_contact_infos 表
 * （按 user_id 区分，学生/教师/管理员共用，无角色特有逻辑），
 * 请求/响应 DTO 复用 {@link ContactUpdateRequest} / {@link ContactUpdateResponse}。
 * <p>
 * 头像字段不在此接口维护：复用通用上传头像接口 POST /common/upload/avatar
 * （直接写入 user_contact_infos.avatar，与其他端一致）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final ProfileService profileService;
    private final AdminAuthService adminAuthService;

    /**
     * 更新个人联系信息
     * <p>
     * 复用学生端 PUT /profile/contact（4.1.1）的全量更新语义：
     * email / phone / address / emergencyName / emergencyRelation / emergencyPhone
     * 六字段均必填，缺失任一字段返回参数校验错误，传空字符串表示清空对应字段。
     * 操作严格限定为当前登录用户自身记录（userId 取自 JWT principal），不接收他人 ID，
     * 故无数据越权风险；遵循管理端「/admin/* 需 admin 角色」约定校验后复用同一服务。
     *
     * @param userId  当前登录用户 ID
     * @param request 联系信息更新请求
     * @return 更新后的联系信息
     */
    @PutMapping("/contact")
    public ApiResult<ContactUpdateResponse> updateContact(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ContactUpdateRequest request) {
        adminAuthService.requireAdmin(userId);
        return ApiResult.success("更新成功", profileService.updateContact(userId, request));
    }
}
