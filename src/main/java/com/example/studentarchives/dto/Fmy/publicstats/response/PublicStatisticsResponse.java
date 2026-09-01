package com.example.studentarchives.dto.Fmy.publicstats.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录页公开统计响应 DTO
 * <p>
 * 对应登录页品牌区底部统计数字（学生端与管理端登录页共用）：
 * <ul>
 *   <li>学生端登录页：在校学生 {@link #studentCount}、档案条目 {@link #archiveCount}、服务可用 {@link #serviceAvailability}</li>
 *   <li>管理端登录页：待审申请 {@link #pendingApplicationCount}、学生档案 {@link #archiveCount}、系统稳定 {@link #systemStability}</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicStatisticsResponse {

    /** 在校学生数：COUNT(student_profiles) 全部未软删除行（每名学生一行） */
    private long studentCount;

    /** 档案条目数 / 学生档案数：COUNT(archives) 全部未软删除档案 */
    private long archiveCount;

    /** 待审申请数：archives.status=1 与 award_applications.status=1 之和 */
    private long pendingApplicationCount;

    /** 服务可用率（配置项 public.stats.service-availability，默认 99.9%） */
    private String serviceAvailability;

    /** 系统稳定率（配置项 public.stats.system-stability，默认 99.99%） */
    private String systemStability;

    /** 统计时间（yyyy-MM-dd HH:mm:ss） */
    private String statTime;
}
