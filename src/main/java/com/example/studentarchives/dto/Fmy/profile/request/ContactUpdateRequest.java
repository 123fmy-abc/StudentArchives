package com.example.studentarchives.dto.Fmy.profile.request;

import com.example.studentarchives.common.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新个人联系信息请求 DTO（PUT /profile/contact）
 * <p>
 * 数据写入 user_contact_infos 表，全量更新语义：六个字段均必填，
 * 缺失任一字段直接报参数校验错误；传空字符串表示清空该字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactUpdateRequest {

    /** 邮箱（空串表示清空） */
    @NotNull(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255")
    private String email;

    /** 手机号（空串表示清空） */
    @NotNull(message = "手机号不能为空")
    @Size(max = 20, message = "手机号长度不能超过20")
    @Pattern(regexp = ValidationConstants.PHONE_PATTERN, message = ValidationConstants.PHONE_MESSAGE)
    private String phone;

    /** 通讯地址（空串表示清空） */
    @NotNull(message = "通讯地址不能为空")
    @Size(max = 255, message = "通讯地址长度不能超过255")
    private String address;

    /** 紧急联系人姓名（空串表示清空） */
    @NotNull(message = "紧急联系人姓名不能为空")
    @Size(max = 50, message = "紧急联系人姓名长度不能超过50")
    private String emergencyName;

    /** 与紧急联系人关系（空串表示清空） */
    @NotNull(message = "与紧急联系人关系不能为空")
    @Size(max = 30, message = "与紧急联系人关系长度不能超过30")
    private String emergencyRelation;

    /** 紧急联系人电话（空串表示清空） */
    @NotNull(message = "紧急联系人电话不能为空")
    @Size(max = 20, message = "紧急联系人电话长度不能超过20")
    @Pattern(regexp = ValidationConstants.PHONE_PATTERN, message = ValidationConstants.PHONE_MESSAGE)
    private String emergencyPhone;
}
