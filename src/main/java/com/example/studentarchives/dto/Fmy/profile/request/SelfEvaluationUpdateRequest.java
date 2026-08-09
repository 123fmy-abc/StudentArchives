package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新自我评价请求 DTO（PUT /profile/self-evaluation）
 * <p>
 * 全量更新语义：允许传空字符串清空自我评价，但字段不能缺失。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfEvaluationUpdateRequest {

    /** 自我评价内容 */
    @Size(max = 2000, message = "自我评价长度不能超过2000字")
    private String selfEvaluation;
}
