package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典数据响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DictItemResponse {

    /** 字典编码/值（对应 dictionaries.dict_code） */
    private String value;

    /** 字典显示名称（对应 dictionaries.dict_name） */
    private String label;

    /** 排序 */
    private Integer sort;
}
