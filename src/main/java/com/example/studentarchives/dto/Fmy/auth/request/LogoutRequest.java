package com.example.studentarchives.dto.Fmy.auth.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退出登录请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    /** 是否退出所有设备，默认 false */
    private Boolean all = false;
}
