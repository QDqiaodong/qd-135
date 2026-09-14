package com.example.heritagetrace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工具编号唯一约束兜底：
 * ddl-auto=update 对历史表不会补建唯一约束，启动时按 INFORMATION_SCHEMA 检查后幂等补建。
 */
@Component
public class ToolNumberUniqueConstraintMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ToolNumberUniqueConstraintMigration.class);

    private static final String TABLE_NAME = "tools";
    private static final String COLUMN_NAME = "tool_number";
    private static final String CONSTRAINT_NAME = "uk_tool_number";

    private final JdbcTemplate jdbcTemplate;

    public ToolNumberUniqueConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (constraintExists()) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + CONSTRAINT_NAME
                    + " UNIQUE (" + COLUMN_NAME + ")");
            log.info("已补建工具编号唯一约束 {}", CONSTRAINT_NAME);
        } catch (Exception e) {
            log.error("工具编号唯一约束初始化失败，请检查 tools 表中是否存在重复 tool_number：{}", e.getMessage());
        }
    }

    private boolean constraintExists() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                        + "AND COLUMN_NAME = ? AND NON_UNIQUE = 0 LIMIT 1",
                TABLE_NAME, COLUMN_NAME);
        return !rows.isEmpty();
    }
}
