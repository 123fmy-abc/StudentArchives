package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveDimensionItem;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveDimensionResponse;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.entity.growth.GrowthTimeline;
import com.example.studentarchives.entity.growth.GrowthTimelineAbility;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.GrowthTimelineAbilityRepository;
import com.example.studentarchives.repository.GrowthTimelineRepository;
import com.example.studentarchives.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 学生多维度能力画像 Service（GET /archive/dimensions，《学生端接口文档》4.1.7）
 * <p>
 * 补齐前端已在调用、后端此前缺失的接口 —— 后端没有 {@code /archive/**} 控制器。
 * <p>
 * 取数链路：维度字典 {@code ability_dimensions}（status=1，按 sort 升序）为左表，
 * 左连接该生「已通过」的成长事件能力得分 ——
 * {@code growth_timelines}（user_id + 可选 semester_id，status=2 已通过）
 * → {@code growth_timeline_abilities}（按 dimension_code 累加 score）。
 * 字典里有、该生尚无得分的维度补 0，避免画像缺角。
 * <p>
 * 口径（与《待实现接口文档》二确认一致）：
 * <ul>
 *   <li>score = 累计原始分，不做归一化、不按 event_type 拆分、不提供等级标签与横向对比；</li>
 *   <li>不传 semesterId = 全部学期累计，响应 semesterId 为 null；</li>
 *   <li>无数据不报错，返回 dimensions: [] / totalScore: 0。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentArchiveDimensionService {

    /** 得分保留 2 位小数（源列 growth_timeline_abilities.score 为 DECIMAL(5,2)） */
    private static final int SCORE_SCALE = 2;

    /** 占比保留 4 位小数 */
    private static final int RATIO_SCALE = 4;

    private final AbilityDimensionRepository abilityDimensionRepository;
    private final GrowthTimelineRepository growthTimelineRepository;
    private final GrowthTimelineAbilityRepository growthTimelineAbilityRepository;
    private final SemesterRepository semesterRepository;

    /**
     * 获取当前登录学生本人的多维度能力画像
     *
     * @param userId     当前登录用户 ID（本人）
     * @param semesterId 学期 ID，不传/为 null 表示全部学期累计
     * @return 画像汇总与各维度累计得分
     */
    @Transactional(readOnly = true)
    public ArchiveDimensionResponse getDimensions(Long userId, Long semesterId) {
        if (semesterId != null && !semesterRepository.existsById(semesterId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学期不存在");
        }

        List<AbilityDimension> dimensions = abilityDimensionRepository.findAllActive();
        Map<String, BigDecimal> scoreByCode = sumApprovedScoreByDimension(userId, semesterId);

        // 以维度字典为左表：字典里有、该生无得分的维度补 0。
        // totalScore 只累加字典内的维度 —— 得分明细里若存在字典中已删除/未配置的 dimension_code，
        // 它不会出现在 dimensions 里，计入总分会让 ratio 之和小于 1。
        BigDecimal totalScore = dimensions.stream()
                .map(d -> scoreOf(scoreByCode, d.getDimensionCode()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ArchiveDimensionItem> items = dimensions.stream()
                .map(d -> {
                    BigDecimal score = scoreOf(scoreByCode, d.getDimensionCode());
                    return ArchiveDimensionItem.builder()
                            .dimensionCode(d.getDimensionCode())
                            .dimensionName(d.getDimensionName())
                            .description(d.getDescription())
                            .score(score)
                            .ratio(ratio(score, totalScore))
                            .sort(d.getSort())
                            .build();
                })
                .collect(Collectors.toList());

        return ArchiveDimensionResponse.builder()
                .userId(userId)
                .semesterId(semesterId)
                .totalScore(totalScore.setScale(SCORE_SCALE, RoundingMode.HALF_UP))
                .dimensions(items)
                .build();
    }

    /**
     * 按 dimension_code 累加该生「已通过」成长事件的能力得分。
     * <p>
     * {@code GrowthTimelineRepository} 没有按 status 的查询方法，与
     * {@code ProfileService#getGrowthTimeline} 一样在内存里过滤。
     *
     * @return dimensionCode → 累计得分；无数据返回空 Map
     */
    private Map<String, BigDecimal> sumApprovedScoreByDimension(Long userId, Long semesterId) {
        List<GrowthTimeline> timelines = (semesterId != null)
                ? growthTimelineRepository.findByUserIdAndSemesterIdOrderByEventAtDesc(userId, semesterId)
                : growthTimelineRepository.findByUserIdOrderByEventAtDesc(userId);

        List<Long> timelineIds = timelines.stream()
                .filter(t -> Objects.equals(t.getStatus(), ApplyStatusEnum.APPROVED.getValue()))
                .map(GrowthTimeline::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (timelineIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (GrowthTimelineAbility ability : growthTimelineAbilityRepository.findByTimelineIdIn(timelineIds)) {
            if (ability.getDimensionCode() == null || ability.getScore() == null) {
                continue;
            }
            totals.merge(ability.getDimensionCode(), ability.getScore(), BigDecimal::add);
        }
        return totals;
    }

    /** 取某维度的累计得分，缺失补 0，统一保留 2 位小数 */
    private BigDecimal scoreOf(Map<String, BigDecimal> scoreByCode, String dimensionCode) {
        return scoreByCode.getOrDefault(dimensionCode, BigDecimal.ZERO)
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    /** 维度占比：总分为 0 时返回 0，避免除零 */
    private BigDecimal ratio(BigDecimal score, BigDecimal totalScore) {
        if (totalScore == null || totalScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(RATIO_SCALE, RoundingMode.HALF_UP);
        }
        return score.divide(totalScore, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
