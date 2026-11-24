package com.example.studentarchives.dto.Fmy.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学生注册页组织架构选项。
 * <p>
 * 返回「学校 → 学院 → 专业 → 班级」四层树，前端用级联选择器绑定；
 * 只返回启用中且存在下级数据的节点，避免出现选不到班级的死分支。
 * 空 children 不参与序列化，叶子节点在前端不会显示展开箭头。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOptionsResponse {

    private List<SchoolNode> schools;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class SchoolNode {
        private Long id;
        private String name;
        private List<CollegeNode> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class CollegeNode {
        private Long id;
        private String name;
        private List<MajorNode> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class MajorNode {
        private Long id;
        private String name;
        private List<ClassNode> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassNode {
        private Long id;
        private String name;
    }
}
