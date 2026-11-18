package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.enums.EventTypeEnum;
import com.example.studentarchives.enums.GrowthSourceTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * 成长时间轴事件类型推导器。
 * <p>
 * {@code growth_timelines.event_type} 由学生关联的来源记录客观决定，不接受前端自选：
 * <ul>
 *   <li>常规来源类型：直接取 {@link GrowthSourceTypeEnum#getEventType()} 的固定映射；</li>
 *   <li>通用档案来源（{@code archives} / 别名 {@code archive}）：一个来源类型覆盖全部档案子类型，
 *       需再读 {@code archives.archive_type} 细分（同时兼容 {@code ArchiveTypeEnum} 完整编码与
 *       历史种子数据的短编码，口径同《学生端接口文档》12.1）。</li>
 * </ul>
 * 无法推导时直接抛 {@code 10005}，不做静默兜底——避免新增档案类型/脏数据被悄悄归入错误分类。
 * 表名固定、参数化查询，无 SQL 注入风险。
 */
@Component
@RequiredArgsConstructor
public class GrowthTimelineEventTypeResolver {

    /** 来源类型别名 → 规范编码（与 ProfileGrowthTimelineService 的来源白名单别名一致） */
    private static final Map<String, String> SOURCE_TYPE_ALIASES = Map.of(
            "archive", "archives",
            "award_application", "award_applications");

    /**
     * archives.archive_type → 成长事件类型。
     * <p>
     * 键同时覆盖 ArchiveTypeEnum 完整编码与历史种子数据的短编码：
     * 1=奖项（竞赛 / 奖学金 / 荣誉证书）、3=实践（社会实践 / 实习 / 组织任职 / 实训 / 创新创业）、
     * 6=能力提升（学术研究 / 图书心得）。未命中视为脏数据或未登记的新档案类型，调用方直接报错。
     */
    private static final Map<String, Integer> ARCHIVE_TYPE_EVENT_TYPE = Map.ofEntries(
            // 1=奖项
            Map.entry("academic_competition", EventTypeEnum.AWARD.getValue()),
            Map.entry("competition", EventTypeEnum.AWARD.getValue()),
            Map.entry("scholarship", EventTypeEnum.AWARD.getValue()),
            Map.entry("honor_certificate", EventTypeEnum.AWARD.getValue()),
            Map.entry("certificate", EventTypeEnum.AWARD.getValue()),
            // 3=实践
            Map.entry("social_practice", EventTypeEnum.PRACTICE.getValue()),
            Map.entry("internship", EventTypeEnum.PRACTICE.getValue()),
            Map.entry("organization", EventTypeEnum.PRACTICE.getValue()),
            Map.entry("training_project", EventTypeEnum.PRACTICE.getValue()),
            Map.entry("training", EventTypeEnum.PRACTICE.getValue()),
            Map.entry("innovation_entrepreneurship", EventTypeEnum.PRACTICE.getValue()),
            Map.entry("innovation", EventTypeEnum.PRACTICE.getValue()),
            // 6=能力提升
            Map.entry("academic_research", EventTypeEnum.ABILITY_UPGRADE.getValue()),
            Map.entry("research", EventTypeEnum.ABILITY_UPGRADE.getValue()),
            Map.entry("book_review", EventTypeEnum.ABILITY_UPGRADE.getValue()));

    /** 查询档案类型（来源记录已在业务层校验归属与软删除，此处仅取分类编码） */
    private static final String ARCHIVE_TYPE_SQL =
            "SELECT archive_type FROM archives WHERE id = :id AND deleted_at IS NULL";

    private final EntityManager entityManager;

    /**
     * 由来源记录推导成长事件类型。
     *
     * @param sourceType 来源类型（支持别名 archive / award_application）
     * @param sourceId   来源记录 ID
     * @return 事件类型 1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升
     */
    public int resolve(String sourceType, Long sourceId) {
        String canonical = canonicalize(sourceType);
        GrowthSourceTypeEnum typeEnum = GrowthSourceTypeEnum.of(canonical);
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ILLEGAL, "不支持的来源类型: " + sourceType);
        }
        if (typeEnum == GrowthSourceTypeEnum.ARCHIVES) {
            return resolveFromArchiveType(canonical, sourceId);
        }
        EventTypeEnum eventType = typeEnum.getEventType();
        if (eventType == null) {
            throw new BusinessException(ResultCode.PARAM_ILLEGAL,
                    "来源类型 " + canonical + " 未配置事件类型映射");
        }
        return eventType.getValue();
    }

    /**
     * 宽松推导：仅在可推导时返回事件类型，来源记录已删除 / 档案类型未登记 / 来源类型不支持时返回 null。
     * <p>
     * 供修改场景使用：来源引用未变更时沿用节点原分类，避免存量脏数据阻塞名称、描述等字段的编辑；
     * 新增与来源切换必须使用 {@link #resolve(String, Long)}，无法推导即报错。
     *
     * @param sourceType 来源类型
     * @param sourceId   来源记录 ID
     * @return 事件类型 1-6；无法推导返回 null
     */
    public Integer tryResolve(String sourceType, Long sourceId) {
        try {
            return resolve(sourceType, sourceId);
        } catch (BusinessException e) {
            return null;
        }
    }

    /** 通用档案来源：按 archives.archive_type 细分；无法匹配时报错而非静默归类 */
    private int resolveFromArchiveType(String sourceType, Long sourceId) {
        String archiveType = queryArchiveType(sourceId);
        if (archiveType == null || archiveType.isBlank()) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "来源记录不存在或不属于当前用户");
        }
        Integer eventType = ARCHIVE_TYPE_EVENT_TYPE.get(archiveType.trim().toLowerCase(Locale.ROOT));
        if (eventType == null) {
            throw new BusinessException(ResultCode.PARAM_ILLEGAL,
                    "无法由档案类型(" + archiveType + ")推导事件类型，请同步维护来源类型映射（sourceType=" + sourceType + "）");
        }
        return eventType;
    }

    /** 读取档案类型编码，记录不存在（含软删除）返回 null */
    private String queryArchiveType(Long sourceId) {
        try {
            Object value = entityManager.createNativeQuery(ARCHIVE_TYPE_SQL)
                    .setParameter("id", sourceId)
                    .getSingleResult();
            return value != null ? value.toString() : null;
        } catch (NoResultException e) {
            return null;
        }
    }

    /** 别名归一为规范来源类型编码 */
    private String canonicalize(String sourceType) {
        if (sourceType == null) {
            return null;
        }
        String trimmed = sourceType.trim();
        return SOURCE_TYPE_ALIASES.getOrDefault(trimmed, trimmed);
    }
}
