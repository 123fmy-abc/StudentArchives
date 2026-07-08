package com.example.studentarchives.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用分页响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private Pagination pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int page;
        private int perPage;
        private long total;
        private int totalPages;
    }

    /** 创建空分页 */
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder()
                .list(Collections.emptyList())
                .pagination(Pagination.builder()
                        .page(1)
                        .perPage(20)
                        .total(0)
                        .totalPages(0)
                        .build())
                .build();
    }

    /** 从 PageParam 和数据创建 */
    public static <T> PageResult<T> of(List<T> list, long total, PageParam pageParam) {
        int perPage = pageParam.getPerPage();
        int totalPages = (int) Math.ceil((double) total / perPage);
        return PageResult.<T>builder()
                .list(list)
                .pagination(Pagination.builder()
                        .page(pageParam.getPage())
                        .perPage(perPage)
                        .total(total)
                        .totalPages(totalPages)
                        .build())
                .build();
    }

    /** 转换列表元素类型 */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedList = list.stream().map(mapper).collect(Collectors.toList());
        return PageResult.<R>builder()
                .list(mappedList)
                .pagination(pagination)
                .build();
    }

    /** 包装为 ApiResult */
    public ApiResult<PageResult<T>> toApiResult() {
        return ApiResult.success(this);
    }
}
