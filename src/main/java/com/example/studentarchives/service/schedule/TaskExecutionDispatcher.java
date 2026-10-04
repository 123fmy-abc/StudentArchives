package com.example.studentarchives.service.schedule;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.schedule.ScheduledTask;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ScheduledTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 定时任务通用执行分发器。
 * <p>
 * 统一负责：按任务编码/处理器名解析处理器、重试（max_retries / retry_delay_sec）、
 * 超时控制（timeout_sec）、执行结果回写（last_run_at / last_run_status / next_run_at）。
 * 处理器只需专注业务逻辑，无需重复实现上述横切能力。
 * <p>
 * 注意：本类方法<strong>不开启事务</strong>——部分处理器（如评分重算）会先落库再提交 {@code @Async}
 * 异步执行，若套在事务里会导致异步线程在提交前读不到数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionDispatcher {

    private final ScheduledTaskHandlerRegistry handlerRegistry;
    private final ScheduledTaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    /** 独立线程池：仅为需要超时控制（timeout_sec > 0）的任务提供隔离执行，避免占用调度线程。 */
    private final ExecutorService timeoutExecutor = Executors.newFixedThreadPool(8);

    @PreDestroy
    void shutdown() {
        timeoutExecutor.shutdownNow();
    }

    /**
     * 定时调度入口：按任务 ID 加载并执行，任何异常只记日志、不回抛给调度线程。
     */
    public void executeScheduled(Long taskId) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("定时任务不存在，跳过调度执行: taskId={}", taskId);
            return;
        }
        try {
            execute(task, null, "scheduled");
        } catch (Throwable e) {
            log.warn("定时任务调度执行结束（失败）: taskId={}, taskCode={}, err={}",
                    taskId, task.getTaskCode(), e.getMessage());
        }
    }

    /**
     * 执行任务。处理器未接入时抛业务异常；执行失败（重试耗尽）时回写失败状态并抛出异常。
     */
    public void execute(ScheduledTask task, Map<String, Object> runParams, String triggerType) {
        ScheduledTaskHandler handler = handlerRegistry
                .resolve(task.getTaskCode(), task.getTaskHandler())
                .orElseThrow(() -> new BusinessException(ResultCode.BIZ_OPERATION_FAILED,
                        "该任务处理器未接入执行器，暂不支持执行: " + task.getTaskHandler()));

        LocalDateTime runAt = LocalDateTime.now();
        TaskExecutionContext context = TaskExecutionContext.builder()
                .taskId(task.getId())
                .schoolId(task.getSchoolId())
                .taskCode(task.getTaskCode())
                .taskHandler(task.getTaskHandler())
                .taskParams(parseParams(task.getTaskParams()))
                .runParams(runParams)
                .triggerType(triggerType)
                .build();

        int maxRetries = task.getMaxRetries() == null ? 0 : Math.max(task.getMaxRetries(), 0);
        int retryDelaySec = task.getRetryDelaySec() == null ? 0 : Math.max(task.getRetryDelaySec(), 0);
        int timeoutSec = task.getTimeoutSec() == null ? 0 : Math.max(task.getTimeoutSec(), 0);

        Throwable lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                runWithTimeout(handler, context, timeoutSec);
                markRun(task, runAt, 1, null);
                log.info("定时任务执行成功: taskId={}, taskCode={}, triggerType={}",
                        task.getId(), task.getTaskCode(), triggerType);
                return;
            } catch (Throwable e) {
                lastError = e;
                log.warn("定时任务执行失败（第 {} 次）: taskId={}, taskCode={}, err={}",
                        attempt + 1, task.getId(), task.getTaskCode(), e.getMessage());
                if (attempt < maxRetries && retryDelaySec > 0) {
                    sleepQuietly(retryDelaySec);
                }
            }
        }

        markRun(task, runAt, 0, lastError == null ? "执行失败" : lastError.getMessage());
        throw new BusinessException(ResultCode.BIZ_OPERATION_FAILED,
                "任务执行失败: " + (lastError == null ? "未知错误" : lastError.getMessage()));
    }

    private void runWithTimeout(ScheduledTaskHandler handler, TaskExecutionContext context, int timeoutSec)
            throws Exception {
        if (timeoutSec <= 0) {
            handler.execute(context);
            return;
        }
        Future<Void> future = timeoutExecutor.submit(() -> {
            handler.execute(context);
            return null;
        });
        try {
            future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException(ResultCode.BIZ_OPERATION_FAILED, "任务执行超时（" + timeoutSec + "s）");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ResultCode.BIZ_OPERATION_FAILED,
                    "任务执行异常: " + (cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName()));
        }
    }

    private void markRun(ScheduledTask task, LocalDateTime runAt, int status, String error) {
        task.setLastRunAt(runAt);
        task.setLastRunStatus(status);
        boolean scheduled = task.getRunType() != null && task.getRunType() == 1;
        task.setNextRunAt(scheduled ? nextRun(task.getCronExpression(), runAt) : null);
        try {
            taskRepository.save(task);
        } catch (Exception e) {
            log.error("回写定时任务执行状态失败: taskId={}", task.getId(), e);
        }
        if (status == 0) {
            log.warn("定时任务执行失败: taskId={}, taskCode={}, err={}",
                    task.getId(), task.getTaskCode(), error);
        }
    }

    private LocalDateTime nextRun(String cron, LocalDateTime from) {
        if (cron == null || cron.isBlank()) {
            return null;
        }
        try {
            return CronExpression.parse(cron).next(from);
        } catch (Exception e) {
            log.warn("解析 cron 表达式失败: {}", cron);
            return null;
        }
    }

    private JsonNode parseParams(String params) {
        if (params == null || params.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(params);
        } catch (Exception e) {
            log.warn("定时任务 taskParams 解析失败，返回 null: {}", params);
            return null;
        }
    }

    private void sleepQuietly(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
