package com.example.studentarchives.service.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 定时任务执行上下文，由框架在调度/手动触发时组装后传给 {@link ScheduledTaskHandler}。
 */
@Value
@Builder
public class TaskExecutionContext {

    /** 任务记录 ID */
    Long taskId;

    /** 任务归属学校 ID */
    Long schoolId;

    /** 任务编码 */
    String taskCode;

    /** 处理器标识（Spring Bean 名称或全限定类名） */
    String taskHandler;

    /** 任务参数（scheduled_tasks.task_params 反序列化结果，可能为 null） */
    JsonNode taskParams;

    /** 手动触发时传入的运行时参数（可能为 null） */
    Map<String, Object> runParams;

    /** 触发来源：scheduled=定时调度 manual=手动触发 event=事件触发 */
    String triggerType;
}
