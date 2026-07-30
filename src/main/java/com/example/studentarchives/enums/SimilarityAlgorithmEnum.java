package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 相似度算法枚举（对齐 duplicate_check_configs.similarity_algorithm）
 * <p>
 * 学生档案系统表 duplicate_check_configs.similarity_algorithm
 * exact/levenshtein/jaccard
 */
@Getter
@AllArgsConstructor
public enum SimilarityAlgorithmEnum {

    EXACT("exact", "精确匹配"),
    LEVENSHTEIN("levenshtein", "编辑距离"),
    JACCARD("jaccard", "Jaccard相似度"),
    ;

    private final String value;
    private final String label;

    public static SimilarityAlgorithmEnum of(String value) {
        if (value == null) return EXACT;
        for (SimilarityAlgorithmEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return EXACT;
    }
}
