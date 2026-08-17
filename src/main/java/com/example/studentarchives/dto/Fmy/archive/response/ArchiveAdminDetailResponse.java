package com.example.studentarchives.dto.Fmy.archive.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 档案详情响应 DTO（GET /admin/archives/{archiveId}，文档 15.2）
 * <p>
 * 档案基表字段、学生基本信息与该档案类型扩展表的业务字段映射
 * （不同 archiveType 的 details 结构不同），佐证文件一并返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveAdminDetailResponse {

    /** 档案 ID */
    private Long archiveId;

    /** 档案类型编码，如 competition / scholarship */
    private String archiveType;

    /** 档案类型名称 */
    private String archiveTypeName;

    /** 档案标题 */
    private String title;

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称 */
    private String semesterName;

    /** 获得时间（yyyy-MM-dd） */
    private String obtainedAt;

    /** 档案状态：0=草稿 1=待审批 2=通过 3=已退回 4=已撤销 */
    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 退回原因（status=3 时非空） */
    private String rejectedReason;

    /** 提交时间（ISO 8601 带时区） */
    private String submittedAt;

    /** 审核时间（ISO 8601 带时区） */
    private String auditedAt;

    /** 审核人姓名 */
    private String auditorName;

    /** 学生基础信息 */
    private ArchiveStudentInfo student;

    /** 档案类型扩展表字段映射（含 proofFiles 佐证文件列表） */
    private Map<String, Object> details;
}
