package com.example.studentarchives.config;

import com.example.studentarchives.service.Fmy.AdminScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 评分重算任务启动恢复器
 * <p>
 * 应用启动完成时调用 {@link AdminScoreService#recoverInterruptedTasks()}，把重启/崩溃后仍停留在
 * 0=排队中 / 1=执行中的 score_recalculation_tasks 统一标记为 3=失败并记录中断原因，
 * 避免任务永久卡死、占用同范围触发权（41005 冲突校验）。
 * <p>
 * 采用与 {@link ExportDefaultTemplateSeeder} 一致的 ApplicationRunner 启动模式。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRecalculationTaskRecoveryRunner implements ApplicationRunner {

    private final AdminScoreService adminScoreService;

    @Override
    public void run(ApplicationArguments args) {
        adminScoreService.recoverInterruptedTasks();
    }
}
