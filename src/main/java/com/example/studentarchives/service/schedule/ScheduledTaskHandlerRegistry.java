package com.example.studentarchives.service.schedule;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 定时任务处理器注册表。
 * <p>
 * 启动时收集所有 {@link ScheduledTaskHandler} Bean，按 {@link ScheduledTaskHandler#getTaskCode()}
 * 建立索引，供 {@link TaskExecutionDispatcher} 按任务编码/处理器名解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskHandlerRegistry {

    private final ApplicationContext applicationContext;

    private final Map<String, ScheduledTaskHandler> byCode = new HashMap<>();

    @PostConstruct
    void init() {
        Map<String, ScheduledTaskHandler> beans = applicationContext.getBeansOfType(ScheduledTaskHandler.class);
        beans.values().forEach(handler -> {
            String code = handler.getTaskCode();
            if (code == null || code.isBlank()) {
                log.warn("定时任务处理器缺少 taskCode，跳过注册: {}", handler.getClass().getName());
                return;
            }
            byCode.put(code, handler);
        });
        log.info("定时任务处理器注册完成: {}", byCode.keySet());
    }

    /**
     * 解析处理器：优先按 {@code taskCode} 匹配，其次按 {@code taskHandler}（Bean 名）匹配。
     */
    public Optional<ScheduledTaskHandler> resolve(String taskCode, String taskHandler) {
        ScheduledTaskHandler handler = taskCode != null ? byCode.get(taskCode) : null;
        if (handler != null) {
            return Optional.of(handler);
        }
        if (taskHandler != null && !taskHandler.isBlank() && applicationContext.containsBean(taskHandler)) {
            Object bean = applicationContext.getBean(taskHandler);
            if (bean instanceof ScheduledTaskHandler sh) {
                return Optional.of(sh);
            }
        }
        return Optional.empty();
    }

    /**
     * 判断指定 Bean 名称是否对应一个已注册的通用/业务处理器。
     */
    public boolean isHandlerAvailable(String handlerName) {
        return resolve(null, handlerName).isPresent();
    }

    /**
     * 返回所有已注册的处理器元信息，供创建任务时前端下拉选择。
     */
    public List<HandlerMeta> listHandlers() {
        List<HandlerMeta> result = new ArrayList<>();
        byCode.forEach((code, handler) -> result.add(HandlerMeta.builder()
                .taskCode(code)
                .handler(handler.getHandlerName())
                .name(handler.getHandlerName())
                .description(handler.getDescription())
                .build()));
        return result;
    }

    /**
     * 处理器元信息（供前端下拉列表展示）。
     */
    @Value
    @Builder
    public static class HandlerMeta {
        String taskCode;
        String handler;
        String name;
        String description;
    }
}
