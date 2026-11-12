package com.example.studentarchives.service.Fmy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成长时间轴事件审核状态来源解析器。
 * <p>
 * 事件 growth_timelines.status 是独立快照（新建恒为 2=已通过），无法反映来源申报的真实状态。
 * 本组件在读取时联动：若事件关联的 sourceType 在申报状态白名单内，则以来源记录当前申报状态
 * 覆盖自身快照；否则（如成绩、画像、反馈、建议等无申报态来源）回退事件自身快照。
 * 表名来自固定白名单，参数化查询，无 SQL 注入风险。
 */
@Component
@RequiredArgsConstructor
public class GrowthTimelineStatusResolver {

    /** sourceType → 取来源申报状态的原生查询（主键 = sourceId） */
    private static final Map<String, String> SOURCE_STATUS_SQL;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        // 直接带申报状态的来源主表
        m.put("archives", "SELECT status FROM archives WHERE id = :id AND deleted_at IS NULL");
        m.put("award_applications", "SELECT status FROM award_applications WHERE id = :id AND deleted_at IS NULL");
        m.put("career_plans", "SELECT status FROM career_plans WHERE id = :id AND deleted_at IS NULL");
        // archive 子表：经 archive_id 关联父表取申报状态
        m.put("archive_competitions", "SELECT a.status FROM archive_competitions s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL");
        m.put("archive_social_practices", "SELECT a.status FROM archive_social_practices s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL");
        m.put("archive_internships", "SELECT a.status FROM archive_internships s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL");
        m.put("archive_organizations", "SELECT a.status FROM archive_organizations s JOIN archives a ON a.id = s.archive_id WHERE s.id = :id AND s.deleted_at IS NULL");
        // 奖项子表：经 application_id 关联父表取申报状态
        m.put("award_competition_stars", "SELECT a.status FROM award_competition_stars s JOIN award_applications a ON a.id = s.application_id WHERE s.id = :id AND s.deleted_at IS NULL");
        // 《学生端接口文档》4.2 来源别名
        m.put("archive", m.get("archives"));
        m.put("award_application", m.get("award_applications"));
        SOURCE_STATUS_SQL = Collections.unmodifiableMap(m);
    }

    /** 来源记录已通过状态（仅该状态的来源允许加入成长轴） */
    public static final int APPROVED_STATUS = 2;

    private final EntityManager entityManager;

    /**
     * 联动来源申报状态。
     *
     * @param sourceId   事件来源记录 ID
     * @param sourceType 事件来源类型
     * @param fallback   默认态（事件自身快照 status）
     * @return 来源申报状态；无来源 / 不在白名单 / 记录已删或不存在时回退 fallback
     */
    public Integer resolve(Long sourceId, String sourceType, Integer fallback) {
        Integer status = querySourceStatus(sourceType, sourceId);
        return status != null ? status : fallback;
    }

    /**
     * 该来源类型是否存在申报审批状态（档案 / 奖项 / 职业规划及其子表）。
     *
     * @param sourceType 事件来源类型
     * @return true=该来源带申报状态；false=无审批态来源（成绩/画像/短板等）或来源类型为空
     */
    public boolean hasApprovalStatus(String sourceType) {
        return sourceType != null && SOURCE_STATUS_SQL.containsKey(sourceType);
    }

    /**
     * 查询来源记录当前申报状态。
     *
     * @param sourceType 事件来源类型
     * @param sourceId   事件来源记录 ID
     * @return 来源申报状态；无审批态来源（成绩/画像/短板等）或记录已删/不存在时返回 null
     */
    public Integer querySourceStatus(String sourceType, Long sourceId) {
        String sql = (sourceId == null || sourceType == null) ? null : SOURCE_STATUS_SQL.get(sourceType);
        if (sql == null) {
            return null;
        }
        try {
            Number status = (Number) entityManager.createNativeQuery(sql)
                    .setParameter("id", sourceId)
                    .getSingleResult();
            return status == null ? null : status.intValue();
        } catch (NoResultException e) {
            return null;
        }
    }
}
