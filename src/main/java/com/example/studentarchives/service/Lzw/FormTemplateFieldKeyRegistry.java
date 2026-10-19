package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.enums.ArchiveTypeEnum;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 表单模板字段 key ↔ 档案申报 DTO 属性名注册表
 * <p>
 * 规范：{@code form_templates.fields[].key} = 档案申报 DTO 属性的小写驼峰名
 * （如 {@code competitionName} / {@code obtainTime} / {@code evidenceFileIds}）。
 * <p>
 * 本注册表维护「档案类型编码（{@link ArchiveTypeEnum#getValue()}） → 该类型申报请求 DTO 类」的映射，
 * 供管理端配置防错（S6：校验 field.key 是否为该 DTO 的合法属性名）使用；取值侧由
 * {@code BeanWrapperImpl} 在 DTO 实例上通用取值，无需在此维护字段清单。
 * <p>
 * 置于档案域（service/Lzw）并暴露为 public，避免 {@code Fmy → Lzw} 的反向耦合。
 */
public final class FormTemplateFieldKeyRegistry {

    /** 档案类型编码 → 申报请求 DTO 类 */
    private static final Map<String, Class<?>> CODE_TO_DTO = build();

    private FormTemplateFieldKeyRegistry() {
    }

    private static Map<String, Class<?>> build() {
        Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put(ArchiveTypeEnum.ACADEMIC_COMPETITION.getValue(), ApplicationService.CompetitionSubmitRequest.class);
        map.put(ArchiveTypeEnum.SCHOLARSHIP.getValue(), ApplicationService.ScholarshipSubmitRequest.class);
        map.put(ArchiveTypeEnum.INNOVATION_ENTREPRENEURSHIP.getValue(), ApplicationService.InnovationSubmitRequest.class);
        map.put(ArchiveTypeEnum.ACADEMIC_RESEARCH.getValue(), ApplicationService.ResearchSubmitRequest.class);
        map.put(ArchiveTypeEnum.HONOR_CERTIFICATE.getValue(), ApplicationService.CertificateSubmitRequest.class);
        map.put(ArchiveTypeEnum.INTERNSHIP.getValue(), ApplicationService.InternshipSubmitRequest.class);
        map.put(ArchiveTypeEnum.ORGANIZATION.getValue(), ApplicationService.OrganizationSubmitRequest.class);
        map.put(ArchiveTypeEnum.TRAINING_PROJECT.getValue(), ApplicationService.TrainingSubmitRequest.class);
        map.put(ArchiveTypeEnum.SOCIAL_PRACTICE.getValue(), ApplicationService.PracticeSubmitRequest.class);
        map.put(ArchiveTypeEnum.BOOK_REVIEW.getValue(), ApplicationService.BookReviewSubmitRequest.class);
        return Collections.unmodifiableMap(map);
    }

    /**
     * 按档案类型编码获取对应的申报请求 DTO 类。
     *
     * @param code 档案类型编码（{@link ArchiveTypeEnum#getValue()}）
     * @return DTO 类；未知编码返回 null
     */
    public static Class<?> dtoClass(String code) {
        return code != null ? CODE_TO_DTO.get(code) : null;
    }

    /**
     * 按档案类型编码获取该 DTO 的全部合法属性名集合。
     *
     * @param code 档案类型编码
     * @return 合法属性名集合；未知编码返回空集合
     */
    public static Set<String> supportedKeys(String code) {
        Class<?> dtoClass = dtoClass(code);
        if (dtoClass == null) {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (PropertyDescriptor pd : new BeanWrapperImpl(dtoClass).getPropertyDescriptors()) {
            keys.add(pd.getName());
        }
        return Collections.unmodifiableSet(keys);
    }

    /**
     * 判断某 key 是否为指定档案类型 DTO 的合法属性名。
     *
     * @param code 档案类型编码
     * @param key  字段 key
     * @return 合法返回 true
     */
    public static boolean isSupportedKey(String code, String key) {
        return key != null && supportedKeys(code).contains(key);
    }

    /** 已注册的档案类型编码集合（供防错提示） */
    public static Set<String> registeredCodes() {
        return Collections.unmodifiableSet(CODE_TO_DTO.keySet());
    }
}
