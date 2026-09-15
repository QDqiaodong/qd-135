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
 * 项目阶段字段兜底：
 * ddl-auto=update 会给 projects 表补建 stage 列，但历史行的值为 NULL，
 * 启动时把 NULL 一律回填为在研（IN_PROGRESS），幂等可重复执行。
 */
@Component
public class ProjectStageMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProjectStageMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public ProjectStageMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!columnExists()) {
                log.warn("projects 表缺少 stage 列，跳过项目阶段回填");
                return;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE projects SET stage = 'IN_PROGRESS' WHERE stage IS NULL");
            if (updated > 0) {
                log.info("已回填 {} 个历史项目的阶段为在研", updated);
            }
        } catch (Exception e) {
            log.error("项目阶段历史数据回填失败：{}", e.getMessage());
        }
    }

    private boolean columnExists() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'projects' "
                        + "AND COLUMN_NAME = 'stage' LIMIT 1");
        return !rows.isEmpty();
    }
}
