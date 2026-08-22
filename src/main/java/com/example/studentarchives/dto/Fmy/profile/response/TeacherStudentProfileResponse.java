package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师端学生档案详情响应 DTO（GET /teacher/students/{studentId}/profile）
 * <p>
 * 结构与学生端 GET /profile/info 一致（复用 {@link ProfileInfoResponse}，
 * {@code @JsonUnwrapped} 平铺其全部字段），教师视角额外增加 {@code isInScope} 标记。
 * 越权学生已在 Service 层返回 20005，因此成功响应的 isInScope 恒为 true。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherStudentProfileResponse {

    /** 学生是否在教师授权范围内（恒为 true，越权返回 20005） */
    private Boolean isInScope;

    /** 档案主体（学籍/联系信息/画像分数/兴趣标签/学期成绩/个人奖项/短板分析等） */
    @JsonUnwrapped
    private ProfileInfoResponse profile;
}
