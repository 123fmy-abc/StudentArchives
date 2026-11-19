package com.example.studentarchives.dto.Fmy.guideconfig.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评选说明配置列表响应（GET /admin/guide-configs）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideConfigListResponse {

    /** archive=档案类型 / award=奖项类型 */
    private String kind;

    /** 配置项列表（含尚未入库的枚举类型，id 为 null） */
    private List<GuideConfigItem> list;
}
