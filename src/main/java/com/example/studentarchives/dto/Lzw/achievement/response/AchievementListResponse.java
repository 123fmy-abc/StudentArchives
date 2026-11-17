package com.example.studentarchives.dto.Lzw.achievement.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成果墙列表响应（GET /achievements）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementListResponse {

    private List<AchievementListItemResponse> list;

    /** 各分类数量（不受筛选条件影响） */
    private Counts counts;

    /** 各材料状态数量（不受筛选条件影响） */
    private StatusCounts statusCounts;

    /** 分页信息（分页键保持契约的 snake_case） */
    private Pagination pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counts {
        private long all;
        private long honor;
        private long contest;
        private long practice;
        private long research;
        private long completion;
        private long other;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCounts {
        private long all;
        private long archived;
        private long missing;
        private long pending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int page;

        @JsonProperty("per_page")
        private int perPage;

        private long total;

        @JsonProperty("total_pages")
        private int totalPages;
    }
}
