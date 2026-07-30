package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 导出任务类型枚举（对齐 export_tasks.task_type）
 * <p>
 * 学生档案系统表 export_tasks.task_type
 * 1=指定学生 2=指定班级 3=指定学期 4=全量 5=指定专业
 */
@Getter
@AllArgsConstructor
public enum ExportTaskTypeEnum {

    SPECIFIED_STUDENT(1, "指定学生"),
    SPECIFIED_CLASS(2, "指定班级"),
    SPECIFIED_SEMESTER(3, "指定学期"),
    FULL(4, "全量"),
    SPECIFIED_MAJOR(5, "指定专业"),
    ;

    private final int value;
    private final String label;

    public static ExportTaskTypeEnum of(Integer value) {
        if (value == null) return null;
        for (ExportTaskTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
