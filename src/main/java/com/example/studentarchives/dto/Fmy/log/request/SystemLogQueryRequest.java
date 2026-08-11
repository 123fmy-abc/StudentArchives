package com.example.studentarchives.dto.Fmy.log.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统操作日志查询条件（GET /admin/logs/system，管理端文档 3.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogQueryRequest {

    /** 操作人用户 ID（system_logs.operator_id） */
    private Long operatorId;

    /** 操作人角色 ID（system_logs.role_id） */
    private Long roleId;

    /** 操作类型：create/update/delete/audit 等（system_logs.action） */
    private String action;

    /** 操作模块：archive/award/indicator/score 等（system_logs.module） */
    private String module;

    /** 日志级别：1=普通 2=用户动态 3=审计（system_logs.log_level） */
    private Integer logLevel;

    /** 关联模型类型（system_logs.related_type） */
    private String relatedType;

    /** 关联记录 ID（system_logs.related_id） */
    private Long relatedId;

    /** 起始时间（ISO 8601，含时区） */
    private String startTime;

    /** 结束时间（ISO 8601，含时区） */
    private String endTime;
}
