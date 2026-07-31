package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学期下拉选项响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SemesterItemResponse {

    /** 学期 ID（对应 semesters.id） */
    private Integer value;

    /** 学期展示名称，如 "2022-2023第一学期" */
    private String label;

    /** 学期原始名称，如 "2022-2023-1"（对应 semesters.name） */
    private String name;

    /** 是否为当前学期：0=否 1=是 */
    private Integer isCurrent;
}
