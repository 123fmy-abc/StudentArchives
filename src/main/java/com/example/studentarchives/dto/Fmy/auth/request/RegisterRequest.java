package com.example.studentarchives.dto.Fmy.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生自助注册请求 DTO。
 * <p>
 * 学生只能注册为学生账号，归属组织由「专业 + 班级名称」逐级推导（专业 → 学院 → 学校），
 * 因此请求中不接收 schoolId / roleId / collegeId，避免客户端伪造角色或跨校注册。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "学号不能为空")
    @Size(max = 50, message = "学号长度不能超过50个字符")
    private String userNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    /** 性别：0=未知 1=男 2=女 */
    @NotNull(message = "请选择性别")
    private Integer gender;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255个字符")
    private String email;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 所属专业 ID（必填，学生档案必须归属到一个专业） */
    @NotNull(message = "请选择专业")
    private Long majorId;

    /** 班级名称（手填；注册时按「专业 + 名称」匹配，不存在则自动创建） */
    @NotBlank(message = "请填写班级")
    @Size(max = 100, message = "班级名称不能超过100个字符")
    private String className;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @NotBlank(message = "验证码不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
