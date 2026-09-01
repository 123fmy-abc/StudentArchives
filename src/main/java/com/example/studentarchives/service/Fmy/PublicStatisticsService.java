package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.publicstats.response.PublicStatisticsResponse;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.Fmy.PublicStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 登录页公开统计服务
 * <p>
 * 提供登录页品牌区底部统计数字（GET /public/statistics，免鉴权）。
 * 数据口径（与前端 LoginHero.vue 登录页文案对应）：
 * <ul>
 *   <li>studentCount → COUNT(student_profiles) 全部未软删除行（在校学生 / 学生档案）</li>
 *   <li>archiveCount → COUNT(archives) 全部未软删除档案（档案条目 / 学生档案）</li>
 *   <li>pendingApplicationCount → archives.status=1 与 award_applications.status=1 之和（待审申请）</li>
 *   <li>serviceAvailability / systemStability → 配置项返回，默认 99.9% / 99.99%（无法从学生档案库算出真实值）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicStatisticsService {

    /** 待审核状态（ApplyStatusEnum：1=待审核） */
    private static final int STATUS_PENDING = 1;

    /** 统计时间格式：2026-09-01 10:00:00 */
    private static final DateTimeFormatter STAT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StudentProfileRepository studentProfileRepository;
    private final ArchiveRepository archiveRepository;
    private final PublicStatisticsRepository publicStatisticsRepository;

    /** 服务可用率（配置项 public.stats.service-availability，默认 99.9%） */
    @Value("${public.stats.service-availability:99.9%}")
    private String serviceAvailability;

    /** 系统稳定率（配置项 public.stats.system-stability，默认 99.99%） */
    @Value("${public.stats.system-stability:99.99%}")
    private String systemStability;

    /**
     * 获取登录页公开统计概览（免鉴权，未登录可访问）
     *
     * @return 登录页统计概览
     */
    @Transactional(readOnly = true)
    public PublicStatisticsResponse getStatistics() {
        long studentCount = studentProfileRepository.count();
        long archiveCount = archiveRepository.count();
        long pendingApplicationCount = publicStatisticsRepository.countArchivesByStatus(STATUS_PENDING)
                + publicStatisticsRepository.countAwardApplicationsByStatus(STATUS_PENDING);

        return PublicStatisticsResponse.builder()
                .studentCount(studentCount)
                .archiveCount(archiveCount)
                .pendingApplicationCount(pendingApplicationCount)
                .serviceAvailability(serviceAvailability)
                .systemStability(systemStability)
                .statTime(LocalDateTime.now().format(STAT_TIME_FORMAT))
                .build();
    }
}
