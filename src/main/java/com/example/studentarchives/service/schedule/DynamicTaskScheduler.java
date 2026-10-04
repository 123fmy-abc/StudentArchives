package com.example.studentarchives.service.schedule;

import com.example.studentarchives.entity.schedule.ScheduledTask;
import com.example.studentarchives.repository.ScheduledTaskRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态 Cron 调度器：读取 {@code scheduled_tasks} 表中启用且为定时自动（status=1 且 run_type=1）的任务，
 * 动态注册/取消 Spring 调度触发，替代此前每个任务写一个硬编码 {@code @Scheduled} 的做法。
 * <p>
 * 新增/修改/删除/启停任务后，由 {@code ScheduledTaskManageService} 在事务提交后调用 {@link #reload()} 刷新调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicTaskScheduler {

    private final ScheduledTaskRepository taskRepository;
    private final TaskExecutionDispatcher dispatcher;
    private final ScheduledTaskHandlerRegistry handlerRegistry;

    private ThreadPoolTaskScheduler scheduler;
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    @PostConstruct
    void start() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        reload();
        log.info("动态定时任务调度器已启动");
    }

    @PreDestroy
    void stop() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * 全量重载：取消既有触发，按库中「启用且定时自动」的任务重建调度。
     */
    public synchronized void reload() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();

        List<ScheduledTask> tasks = taskRepository.findByStatusAndRunType(1, 1);
        for (ScheduledTask task : tasks) {
            if (handlerRegistry.resolve(task.getTaskCode(), task.getTaskHandler()).isEmpty()) {
                log.warn("跳过未接入处理器的定时任务: taskId={}, taskCode={}, taskHandler={}",
                        task.getId(), task.getTaskCode(), task.getTaskHandler());
                continue;
            }
            try {
                CronTrigger trigger = new CronTrigger(task.getCronExpression());
                ScheduledFuture<?> future = scheduler.schedule(
                        () -> dispatcher.executeScheduled(task.getId()), trigger);
                futures.put(task.getId(), future);
                log.info("已注册定时任务调度: taskId={}, taskCode={}, cron={}",
                        task.getId(), task.getTaskCode(), task.getCronExpression());
            } catch (Exception e) {
                log.warn("注册定时任务失败（忽略该任务）: taskId={}, taskCode={}, cron={}, err={}",
                        task.getId(), task.getTaskCode(), task.getCronExpression(), e.getMessage());
            }
        }
        log.info("动态定时任务调度重载完成，共注册 {} 个任务", futures.size());
    }
}
