package com.example.studentarchives.dto.Fmy.delegation.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建审批委托请求 DTO（POST /teacher/delegations，《教师端接口文档》11/15.2）
 * <p>
 * delegateeId 必填；roleId / scopeType / scopeId 不传表示委托全部角色 / 全部范围。
 * startTime 必须晚于当前时间，endTime 必须晚于 startTime，最长委托期 180 天。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelegationCreateRequest {

    /** 受托人用户 ID（必填） */
    @NotNull(message = "受托人不能为空")
    private Long delegateeId;

    /** 委托角色范围（可选），不传则委托所有角色 */
    private Long roleId;

    /** 委托范围类型（可选）：2=学院 3=专业 4=班级，不传则委托全部范围 */
    private Integer scopeType;

    /** 委托范围 ID（可选），不传则委托该类型下所有范围 */
    private Long scopeId;

    /** 委托开始时间（必填，ISO 8601，时区偏移可带可不带：2026-07-10T00:00:00 或 2026-07-10T00:00:00+08:00） */
    @NotNull(message = "委托开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[XXX]")
    private LocalDateTime startTime;

    /** 委托结束时间（必填，ISO 8601，时区偏移可带可不带） */
    @NotNull(message = "委托结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[XXX]")
    private LocalDateTime endTime;

    /** 委托原因（可选，建议填写便于审计追溯） */
    @Size(max = 255, message = "委托原因长度不能超过255")
    private String reason;
}
