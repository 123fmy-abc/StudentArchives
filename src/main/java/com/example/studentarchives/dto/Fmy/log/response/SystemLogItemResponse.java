package com.example.studentarchives.dto.Fmy.log.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统操作日志列表项 DTO（GET /admin/logs/system，管理端文档 3.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogItemResponse {

    /** 日志 ID（system_logs.id） */
    private Long id;

    /** 操作人用户 ID（system_logs.operator_id） */
    private Long operatorId;

    /** 操作人姓名（联查 users 表，非 system_logs 列） */
    private String operatorName;

    /** 操作人角色 ID（system_logs.role_id） */
    private Long roleId;

    /** 操作人角色名称快照（system_logs.role_name） */
    private String roleName;

    /** 操作类型（system_logs.action） */
    private String action;

    /** 操作模块（system_logs.module） */
    private String module;

    /** 操作描述（system_logs.description） */
    private String description;

    /** 修改前数据快照（system_logs.before_data，JSON） */
    private JsonNode beforeData;

    /** 修改后数据快照（system_logs.after_data，JSON） */
    private JsonNode afterData;

    /** 客户端 IP（system_logs.ip_address） */
    private String ipAddress;

    /** 创建时间（system_logs.created_at，ISO 8601 带时区） */
    private String createdAt;
}
