package com.example.studentarchives.enums;

import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.common.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * 动态记录来源类型枚举
 * <p>
 * 对应三张数据源表：archives、award_applications、career_plans。
 * 用于路径变量 {type} 的路由分发。
 */
@Getter
@AllArgsConstructor
public enum ActivityTypeEnum {

    ARCHIVE("archive", "档案", Archive.class),
    AWARD("award", "奖项", AwardApplication.class),
    CAREER_PLAN("career_plan", "职业规划", CareerPlan.class),
    ;

    /** 路径变量值（如 archive、award、career_plan） */
    private final String value;

    /** 中文标签 */
    private final String label;

    /** 对应的 JPA 实体类 */
    private final Class<?> entityClass;

    /** 所有有效 type 值集合（用于参数校验） */
    public static final Set<String> ALL_VALUES =
            Set.of(ARCHIVE.value, AWARD.value, CAREER_PLAN.value);

    /**
     * 根据路径变量解析枚举，无效值时直接抛业务异常
     */
    public static ActivityTypeEnum of(String type) {
        if (type == null || type.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "记录类型(type)不能为空，有效值：" + ALL_VALUES);
        }
        return Arrays.stream(values())
                .filter(e -> e.value.equals(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR,
                        "无效的记录类型：" + type + "，有效值：" + ALL_VALUES));
    }
}
