package com.example.studentarchives.dto.Fmy.announcement.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信息发布（公告）发布请求
 * <p>
 * 对应图片“管理员 → 表单自定义 → 发布信息”。
 * 明确与“表单模板发布”区分：信息发布面向用户发布公告/通知。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementPublishRequest {

    @NotNull(message = "schoolId 不能为空")
    private Long schoolId;

    @NotBlank(message = "title 不能为空")
    @Size(max = 255, message = "title 长度不能超过255")
    private String title;

    @NotBlank(message = "content 不能为空")
    private String content;

    /** 发布对象类型：all/student/teacher/admin */
    @NotBlank(message = "targetType 不能为空")
    @Size(max = 50, message = "targetType 长度不能超过50")
    private String targetType;

    /** 发布对象 ID，targetType=all 时可空 */
    private Long targetId;
}
