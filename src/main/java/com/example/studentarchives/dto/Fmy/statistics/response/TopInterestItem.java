package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热门兴趣 DTO（统计看板）
 * <p>
 * 学生兴趣标签聚合项，interest 为标签名，count 为拥有该标签的学生数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopInterestItem {

    /** 兴趣标签，如 编程 */
    private String interest;

    /** 人数 */
    private Integer count;
}
