package com.example.studentarchives.service.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通用 SQL 脚本处理器（task_code = sql_script）。
 * <p>
 * 定时执行一段 SQL 脚本，适用于数据归档、状态批量更新、统计表刷新等。
 * 出于安全考虑，仅允许 DML（SELECT/INSERT/UPDATE/DELETE），禁止 DDL 与危险关键字。
 * <p>
 * 任务参数（scheduled_tasks.task_params）：
 * <pre>
 * {
 *   "sql": "UPDATE users SET status = 1 WHERE status = 0 AND created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR)",
 *   "expectAffectedMin": 0,
 *   "expectAffectedMax": 100000
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlScriptHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "sql_script";

    private static final Set<String> ALLOWED_PREFIXES = new HashSet<>(
            Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE"));
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(
            Arrays.asList("DROP", "TRUNCATE", "ALTER", "CREATE", "RENAME", "GRANT", "REVOKE"));

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getHandlerName() {
        return "sqlScriptHandler";
    }

    @Override
    public String getDescription() {
        return "定时执行 SQL 脚本，仅允许 SELECT/INSERT/UPDATE/DELETE";
    }

    @Override
    @Transactional
    public void execute(TaskExecutionContext context) {
        SqlConfig config = SqlConfig.from(context.getTaskParams());
        if (config.sql == null || config.sql.isBlank()) {
            throw new IllegalArgumentException("taskParams.sql 不能为空");
        }
        String normalized = config.sql.trim().toUpperCase();
        String firstWord = normalized.split("\\s+", 2)[0];
        if (!ALLOWED_PREFIXES.contains(firstWord)) {
            throw new IllegalArgumentException("SQL 脚本必须以 SELECT/INSERT/UPDATE/DELETE 开头");
        }
        if (FORBIDDEN_KEYWORDS.stream().anyMatch(normalized::contains)) {
            throw new IllegalArgumentException("SQL 脚本包含禁止的关键字（DROP/TRUNCATE/ALTER/CREATE/RENAME/GRANT/REVOKE）");
        }

        log.info("SQL 脚本定时任务执行: taskId={}, firstWord={}", context.getTaskId(), firstWord);
        int affected;
        if (firstWord.equals("SELECT")) {
            List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(config.sql);
            affected = rows.size();
            log.info("SQL 脚本查询完成: rows={}", affected);
        } else {
            affected = jdbcTemplate.update(config.sql);
            log.info("SQL 脚本执行完成: affected={}", affected);
        }

        if (affected < config.expectAffectedMin) {
            throw new RuntimeException("SQL 影响行数 " + affected + " 低于预期最小值 " + config.expectAffectedMin);
        }
        if (config.expectAffectedMax != null && affected > config.expectAffectedMax) {
            throw new RuntimeException("SQL 影响行数 " + affected + " 超过预期最大值 " + config.expectAffectedMax);
        }
    }

    private static class SqlConfig {
        String sql;
        int expectAffectedMin = 0;
        Integer expectAffectedMax;

        static SqlConfig from(JsonNode taskParams) {
            SqlConfig config = new SqlConfig();
            if (taskParams == null) {
                return config;
            }
            if (taskParams.hasNonNull("sql")) {
                config.sql = taskParams.get("sql").asText().trim();
            }
            if (taskParams.hasNonNull("expectAffectedMin")) {
                config.expectAffectedMin = taskParams.get("expectAffectedMin").asInt(0);
            }
            if (taskParams.hasNonNull("expectAffectedMax")) {
                config.expectAffectedMax = taskParams.get("expectAffectedMax").asInt();
            }
            return config;
        }
    }
}
