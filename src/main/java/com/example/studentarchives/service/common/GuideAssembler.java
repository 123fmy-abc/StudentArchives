package com.example.studentarchives.service.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 评选说明统一组装器
 * <p>
 * 档案侧（{@code GET /applications/{type}/guide}）与奖项侧（{@code GET /awards/{type}/guide}）
 * 共用，将配置表中的 evaluate_* 原始字段归一化为同一响应结构，
 * 避免同一后端出现两套评选说明响应形状。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuideAssembler {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final ObjectMapper objectMapper;

    /**
     * 组装归一化评选说明。
     *
     * @param type                 类型编码
     * @param typeLabel            类型中文名
     * @param evaluateDesc         评选说明正文
     * @param evaluateRequirements 评选要求 JSON 字符串（对象数组）
     * @param evaluateNotes        注意事项 JSON 字符串（字符串数组）
     * @param applyDesc            申报说明
     * @param icon                 图标类名
     * @param updatedAt            配置更新时间
     */
    public GuideResponse assemble(String type,
                                  String typeLabel,
                                  String evaluateDesc,
                                  String evaluateRequirements,
                                  String evaluateNotes,
                                  String applyDesc,
                                  String icon,
                                  LocalDateTime updatedAt) {
        return GuideResponse.builder()
                .type(type)
                .typeLabel(typeLabel)
                .title(typeLabel + "评选说明")
                .content(evaluateDesc)
                .requirements(parseRequirements(evaluateRequirements))
                .notes(parseNotes(evaluateNotes))
                .applyDesc(applyDesc)
                .icon(icon)
                .updatedAt(updatedAt != null
                        ? updatedAt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                        : null)
                .build();
    }

    private List<Requirement> parseRequirements(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Requirement>>() {});
        } catch (Exception e) {
            log.warn("解析评选要求配置失败，已按空列表返回：{}", json, e);
            return new ArrayList<>();
        }
    }

    private List<String> parseNotes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析评选注意事项配置失败，已按空列表返回：{}", json, e);
            return new ArrayList<>();
        }
    }

    @Data
    @Builder
    public static class GuideResponse {
        private String type;
        private String typeLabel;
        private String title;
        private String content;
        private List<Requirement> requirements;
        private List<String> notes;
        private String applyDesc;
        private String icon;
        private String updatedAt;
    }

    @Data
    public static class Requirement {
        private String field;
        private String label;
        private boolean required;
        private String description;
    }
}
