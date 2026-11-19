package com.example.studentarchives.dto.Fmy.guideconfig.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评选说明配置保存请求（PUT /admin/guide-configs/{kind}/{type}）
 * <p>
 * 局部更新语义：未出现的字段保持不变；{@code content}/{@code applyDesc} 传空串、
 * {@code requirements}/{@code notes} 传空数组表示清空。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuideConfigUpdateRequest {

    /** 类型中文名；不传时新增取枚举 label，更新时保持不变 */
    @Size(max = 50, message = "类型中文名长度不能超过50")
    private String typeName;

    /** 评选说明正文（evaluate_desc），空串表示清空 */
    private String content;

    /** 评选必填字段要求列表，空数组表示清空 */
    @Valid
    @Size(max = 100, message = "评选要求项不能超过100条")
    private List<GuideRequirementItem> requirements;

    /** 注意事项列表，空数组表示清空 */
    @Size(max = 100, message = "注意事项不能超过100条")
    private List<String> notes;

    /** 申报说明，空串表示清空 */
    private String applyDesc;

    /** 图标类名 */
    @Size(max = 100, message = "图标类名长度不能超过100")
    private String icon;

    /** 排序；不传时新增取当前最大值 + 1，更新时保持不变 */
    @Min(value = 0, message = "排序不能小于0")
    private Integer sort;

    /** 0=禁用 1=启用；不传时新增默认 1，更新时保持不变 */
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
