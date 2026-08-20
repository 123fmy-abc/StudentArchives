package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统计快照刷新响应 DTO（POST /admin/statistics/refresh）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotRefreshResponse {

    /** 学校 ID */
    private Long schoolId;

    /** 学期 ID */
    private Long semesterId;

    /** 统计日期 */
    private LocalDate statDate;

    /** 刷新时间 */
    private LocalDateTime refreshedAt;

    /** 学生总数 */
    private Integer studentCount;

    /** 档案总数 */
    private Integer archiveCount;

    /** 获奖总数 */
    private Integer awardCount;

    /** 平均绩点 */
    private Double avgGpa;
}
