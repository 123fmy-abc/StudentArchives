package com.example.studentarchives.dto.Fmy.guideconfig.response;

import com.example.studentarchives.dto.Fmy.guideconfig.request.GuideRequirementItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评选说明配置项（GET /admin/guide-configs 列表元素，PUT 响应）
 * <p>
 * 列表会补齐枚举中尚未配置的类型，此时 {@code id} 为 {@code null}、
 * {@code content}/{@code applyDesc} 为空、{@code requirements}/{@code notes} 为空数组。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideConfigItem {

    /** 配置行 ID（未配置的类型为 null） */
    private Long id;

    /** archive=档案类型 / award=奖项类型 */
    private String kind;

    /** 类型编码 */
    private String type;

    /** 类型中文名 */
    private String typeLabel;

    /** 弹窗标题：类型中文名 + 评选说明 */
    private String title;

    /** 评选说明正文 */
    private String content;

    /** 评选必填字段要求列表 */
    private List<GuideRequirementItem> requirements;

    /** 评选注意事项列表 */
    private List<String> notes;

    /** 申报说明 */
    private String applyDesc;

    /** 图标类名 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;

    /** 状态中文名 */
    private String statusLabel;

    /** 最近更新时间（ISO8601 带时区） */
    private String updatedAt;
}
