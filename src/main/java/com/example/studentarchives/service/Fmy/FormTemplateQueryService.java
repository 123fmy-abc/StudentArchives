package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateRenderResponse;
import com.example.studentarchives.entity.foundation.FormTemplate;
import com.example.studentarchives.repository.AdminFormTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 表单模板取用服务（学生端/教师端）
 * <p>
 * 为学生端/教师端提供按「学校 + 类别 + 编码」解析生效模板的能力，供提交校验与表单渲染使用。
 * 无启用模板时返回 {@code null}（不抛异常），由调用方放行（向后兼容未配置模板的场景）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateQueryService {

    private final AdminFormTemplateRepository adminFormTemplateRepository;
    private final ObjectMapper objectMapper;

    /**
     * 解析默认模板：同校同 code 同 category 中 {@code status=1 且 is_default=1} → 无则取最新一条启用模板
     * → 均无返回 {@code null}（不抛异常）。
     *
     * @param schoolId 学校 ID
     * @param category 适用类别（如 archive）
     * @param code     模板编码（如 academic_competition）
     * @return 生效模板实体，无则 null
     */
    @Transactional(readOnly = true)
    public FormTemplate resolveDefault(Long schoolId, String category, String code) {
        if (schoolId == null || category == null || code == null) {
            return null;
        }
        List<FormTemplate> enabled = adminFormTemplateRepository
                .findBySchoolIdAndCategoryAndCode(schoolId, category, code)
                .stream()
                .filter(t -> Objects.equals(t.getStatus(), 1))
                .collect(Collectors.toList());
        if (enabled.isEmpty()) {
            return null;
        }
        return enabled.stream()
                .filter(t -> Objects.equals(t.getIsDefault(), 1))
                .findFirst()
                .orElseGet(() -> enabled.stream()
                        .max(Comparator.comparing(FormTemplate::getUpdatedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(null));
    }

    /**
     * 解析并转换为渲染响应 DTO：无生效模板时返回 {@code null}。
     *
     * @param schoolId 学校 ID
     * @param category 适用类别
     * @param code     模板编码
     * @return 渲染响应 DTO，无则 null
     */
    @Transactional(readOnly = true)
    public FormTemplateRenderResponse resolveRenderTemplate(Long schoolId, String category, String code) {
        FormTemplate template = resolveDefault(schoolId, category, code);
        if (template == null) {
            return null;
        }
        return FormTemplateRenderResponse.builder()
                .id(template.getId())
                .code(template.getCode())
                .category(template.getCategory())
                .templateName(template.getTemplateName())
                .version(template.getVersion())
                .fields(parseJson(template.getFields()))
                .layoutConfig(parseJson(template.getLayoutConfig()))
                .applicableRoles(parseRoles(template.getApplicableRoles()))
                .build();
    }

    /** 解析 JSON 字符串为 JsonNode（空白返回 null） */
    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /** 解析适用角色 JSON 数组为 List&lt;String&gt;（非数组/空白返回 null） */
    private List<String> parseRoles(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return null;
            }
            List<String> roles = new ArrayList<>();
            node.forEach(n -> {
                if (n.isTextual()) {
                    roles.add(n.asText());
                }
            });
            return roles;
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", json, e);
            return null;
        }
    }
}
