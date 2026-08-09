package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新增/提交职业规划请求 DTO（POST /profile/career-plans）
 * <p>
 * 支持嵌套 goals → actions → milestones 结构，并可选绑定 evidenceFileIds 附件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerPlanCreateRequest {

    /** 学期 ID */
    @NotNull(message = "学期不能为空")
    private Long semesterId;

    /** 规划标题 */
    @NotBlank(message = "规划标题不能为空")
    @Size(max = 255, message = "规划标题长度不能超过255")
    private String title;

    /** 规划总述 */
    private String content;

    /** 要求/目标 */
    private String requirement;

    /** 0=提交（status=1待审批） 1=保存草稿（status=0），默认0 */
    private Integer isDraft = 0;

    /** 规划文件附件 ID 列表（先上传再传入 fileId） */
    private List<Long> evidenceFileIds;

    /** 目标列表 */
    @Valid
    private List<GoalItem> goals;

    /**
     * 目标子对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalItem {

        @NotBlank(message = "目标标题不能为空")
        @Size(max = 255, message = "目标标题长度不能超过255")
        private String goalTitle;

        private String goalDesc;

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
        private String targetDate;

        @Valid
        private List<ActionItem> actions;
    }

    /**
     * 行动子对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {

        @NotBlank(message = "行动标题不能为空")
        @Size(max = 255, message = "行动标题长度不能超过255")
        private String actionTitle;

        private String actionDesc;

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
        private String startDate;

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
        private String endDate;

        @Valid
        private List<MilestoneItem> milestones;
    }

    /**
     * 里程碑子对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneItem {

        @NotBlank(message = "里程碑标题不能为空")
        @Size(max = 255, message = "里程碑标题长度不能超过255")
        private String milestoneTitle;

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
        private String milestoneDate;
    }
}
