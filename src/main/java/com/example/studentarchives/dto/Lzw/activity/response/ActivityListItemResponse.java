package com.example.studentarchives.dto.Lzw.activity.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态记录列表项（GET /activities 响应）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityListItemResponse {

    /** 记录 ID */
    private Long id;

    /** 记录类型：archive / award / career_plan */
    private String type;

    /** 档案/奖项类型编码 */
    @JsonProperty("archive_type")
    private String archiveType;

    /** 档案/奖项类型中文标签 */
    @JsonProperty("archive_type_label")
    private String archiveTypeLabel;

    /** 标题 */
    private String title;

    /** 摘要/简介 */
    private String content;

    /** 状态码 */
    private Integer status;

    /** 状态中文标签 */
    @JsonProperty("status_label")
    private String statusLabel;

    /** 学期 ID */
    @JsonProperty("semester_id")
    private Long semesterId;

    /** 学期名称 */
    @JsonProperty("semester_name")
    private String semesterName;

    /** 提交时间 (ISO 8601) */
    @JsonProperty("submit_time")
    private String submitTime;

    /** 当前版本号 */
    @JsonProperty("current_version")
    private Integer currentVersion;

    /** 提交次数 */
    @JsonProperty("submit_count")
    private Integer submitCount;

    /** 是否可编辑 */
    @JsonProperty("can_edit")
    private Boolean canEdit;

    /** 是否可删除 */
    @JsonProperty("can_delete")
    private Boolean canDelete;

    /** 是否可撤回（待审批→草稿） */
    @JsonProperty("can_withdraw")
    private Boolean canWithdraw;
}
