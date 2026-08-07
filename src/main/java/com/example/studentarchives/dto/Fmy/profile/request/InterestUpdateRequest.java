package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新个人兴趣标签请求 DTO（PUT /profile/interests）
 * <p>
 * id 存在则更新，否则新增；条件唯一索引 (user_id, tag_name, is_detail)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestUpdateRequest {

    @NotEmpty(message = "兴趣标签不能为空")
    @Valid
    private List<InterestItem> interests;

    /**
     * 兴趣标签项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestItem {

        /** 标签 ID，存在则更新，不存在则新增 */
        private Long id;

        /** 标签名称 */
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 100, message = "标签名称长度不能超过100")
        private String tagName;

        /** 熟练度：1=入门 2=一般 3=熟练 4=精通 */
        @NotNull(message = "熟练度不能为空")
        @Min(value = 1, message = "熟练度只能是1-4")
        @Max(value = 4, message = "熟练度只能是1-4")
        private Integer proficiencyLevel;

        /** 具体内容描述 */
        @Size(max = 255, message = "内容描述长度不能超过255")
        private String detailContent;

        /** 0=系统标签 1=用户手动添加，默认1 */
        private Integer isDetail = 1;
    }
}
