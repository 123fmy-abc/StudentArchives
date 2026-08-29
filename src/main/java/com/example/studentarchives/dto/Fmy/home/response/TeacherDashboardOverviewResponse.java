package com.example.studentarchives.dto.Fmy.home.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 教师首页数据概览响应 DTO（GET /teacher/dashboard，《教师端接口文档》3.1）
 * <p>
 * 数据来源：users、semesters、role_scopes、pending_approvals、audit_logs、user_messages。
 * 所有数值由上述真实数据表聚合返回，禁止写死演示数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherDashboardOverviewResponse {

    /** 教师姓名 */
    private String teacherName;

    /** 教师工号 */
    private String teacherNo;

    /** 当前学期 ID */
    private Long currentSemesterId;

    /** 当前学期名称 */
    private String currentSemesterName;

    /** 教师授权范围列表（role_scopes，按 scopeType+scopeId 去重） */
    private List<ScopeItem> scopes;

    /** 待办统计（pending_approvals 聚合） */
    private PendingStats pendingStats;

    /** 今日审核数（audit_logs 当日记录数） */
    private Long todayAudited;

    /** 最近审核动态（audit_logs 最近记录，附业务对象信息） */
    private List<RecentAuditItem> recentAudits;

    /** 未读消息数（user_messages，is_read=0 且 is_archived=0） */
    private Long unreadMessageCount;

    // ==================== 嵌套结构 ====================

    /**
     * 授权范围项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeItem {

        /** 范围类型：1学校 2学院 3专业 4班级 6年级 */
        private Integer scopeType;

        /** 范围 ID */
        private Long scopeId;

        /** 范围名称（班级/专业/学院名称） */
        private String scopeName;
    }

    /**
     * 待办统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingStats {

        /** 档案待审批数（pending_approvals.approvable_type=Archive） */
        private Long archivePending;

        /** 奖项申报待审批数（approvable_type=AwardApplication） */
        private Long awardPending;

        /** 职业规划待审批数（approvable_type=CareerPlan） */
        private Long careerPlanPending;

        /** 待审批总数 */
        private Long totalPending;
    }

    /**
     * 最近审核动态项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAuditItem {

        /** 审核记录 ID（audit_logs.id） */
        private Long id;

        /** 业务类型：archive / award / career_plan */
        private String type;

        /** 档案/奖项类型编码（职业规划无此字段为 null） */
        private String archiveType;

        /** 申报标题 */
        private String title;

        /** 申请人姓名 */
        private String studentName;

        /** 申请人学号 */
        private String studentNo;

        /** 审核动作：1=通过 2=退回 3=撤回 4=转交 */
        private Integer action;

        /** 审核动作中文标签 */
        private String actionLabel;

        /** 审核时间（ISO 8601 带时区） */
        private String auditedAt;
    }
}
