package com.example.studentarchives.dto.Fmy.announcement.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信息发布（公告）响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private Long id;
    private Long schoolId;
    private String title;
    private String content;
    private Long publisherId;
    private String targetType;
    private Long targetId;
    private String publishedAt;
    private Integer status;
}
