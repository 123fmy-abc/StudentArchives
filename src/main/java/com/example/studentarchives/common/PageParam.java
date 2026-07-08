package com.example.studentarchives.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 通用分页请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageParam {

    @Min(value = 1, message = "页码最小为 1")
    @Builder.Default
    private int page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Builder.Default
    private int perPage = 20;

    private String sortBy;

    @Builder.Default
    private String sortOrder = "desc";

    /** 计算 SQL offset */
    public int getOffset() {
        return (page - 1) * perPage;
    }

    /** 获取排序方向（asc/desc），默认 desc */
    public String getSortOrder() {
        return (sortOrder != null && (sortOrder.equalsIgnoreCase("asc") || sortOrder.equalsIgnoreCase("desc")))
                ? sortOrder.toLowerCase()
                : "desc";
    }

    /** 校验 sortBy 是否在允许字段白名单内（空值视为合法） */
    public boolean isSortByValid(Set<String> allowedFields) {
        if (sortBy == null || sortBy.isEmpty()) return true;
        return allowedFields != null && allowedFields.contains(sortBy.trim());
    }

    /** 获取安全的 sortBy，不在白名单内时返回 null */
    public String getSafeSortBy(Set<String> allowedFields) {
        if (sortBy == null || sortBy.isEmpty()) return null;
        String trimmed = sortBy.trim();
        return (allowedFields != null && allowedFields.contains(trimmed)) ? trimmed : null;
    }
}
