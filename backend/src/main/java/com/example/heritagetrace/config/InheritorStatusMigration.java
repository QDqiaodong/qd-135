package com.example.heritagetrace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 传承人在册状态字段兜底：
 * ddl-auto=update 会给 inheritors 表补建 status 列，但历史行的值为 NULL，
 * 启动时把 NULL 一律回填为在册（ACTIVE），幂等可重复执行。
 * 停档标记与看板人数都以数据库这一份为准，关掉再打开仍然对得上。
 */
@Component
public class InheritorStatusMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InheritorStatusMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public InheritorStatusMigration(JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!columnExists()) {
                log.warn("inheritors 表缺少 status 列，跳过在册状态回填");
                return;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE inheritors SET status = 'ACTIVE' WHERE status IS NULL");
            if (updated > 0) {
                log.info("已回填 {} 个历史传承人的状态为在册", updated);
                clearCaches();
            }
        } catch (Exception e) {
            log.error("传承人在册状态历史数据回填失败：{}", e.getMessage());
        }
    }

    private boolean columnExists() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inheritors' "
                        + "AND COLUMN_NAME = 'status' LIMIT 1");
        return !rows.isEmpty();
    }

    /** 回填后清掉按旧结构缓存的传承人档案与溯源结果，重启后标记立即对得上 */
    private void clearCaches() {
        try {
            Set<String> keys = redisTemplate.keys("inheritors:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            Set<String> traceKeys = redisTemplate.keys("associations:*");
            if (traceKeys != null && !traceKeys.isEmpty()) {
                redisTemplate.delete(traceKeys);
            }
        } catch (Exception e) {
            log.warn("在册状态回填后清理缓存失败：{}", e.getMessage());
        }
    }
}
