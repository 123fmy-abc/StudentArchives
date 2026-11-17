package com.example.studentarchives.dto.Lzw.achievement.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成果墙列表项（GET /achievements 响应）
 * <p>
 * 字段名与前端 {@code src/shared/api/achievements.ts} 的 AchievementListItem 一致（camelCase）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementListItemResponse {

    /** 成果 ID（字符串，避免前端 bigint 精度问题） */
    private String id;

    /** 成果标题 */
    private String title;

    /** 成果分类：honor / contest / practice / research / completion / other */
    private String category;

    /** 成果分类中文标签 */
    private String categoryLabel;

    /** 等级（获奖等级 / 项目级别等，无则为空串） */
    private String level;

    /** 颁发单位 / 主办方（无则为空串） */
    private String issuer;

    /** 成果发生日期（yyyy-MM-dd） */
    private String eventAt;

    /** 封面缩略图（当前无图片存储，固定 null） */
    private String thumbnailUrl;

    /** 附件数量 */
    private Integer fileCount;

    /** 材料状态：archived / missing / pending */
    private String materialStatus;

    /** 材料状态中文标签 */
    private String materialLabel;

    /** 来源类型（archives.archive_type 原值） */
    private String sourceType;

    /** 来源记录 ID */
    private Long sourceId;

    /** 最后更新时间（ISO 8601） */
    private String updatedAt;

    /** 实践类二级标签；非实践类为空数组 */
    private List<String> tags;

    /** 成果版本号 */
    private Integer version;
}
