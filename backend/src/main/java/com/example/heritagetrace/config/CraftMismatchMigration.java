package com.example.heritagetrace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 历史挂错数据兜底（幂等，每次启动重跑）：
 * 关联名单过去只看人/物/项目还在不在，不校验工艺是否一路，
 * 可能留下「木雕凿子挂在刺绣项目下」这类记录。启动时按
 * 工具 craft_type 与项目 category 重新对账：
 *
 *  - ACTIVE 且对不上的，标成 MISMATCH，在名单上高亮，不算正常在用；
 *  - MISMATCH 后来又对得上的（例如工艺/分类被改对），恢复成 ACTIVE；
 *  - DELETED（已解开）不参与对账，流水照原样保留。
 *
 * 任一边工艺/分类留空时不参与比较，视为对得上，兼容历史档案。
 */
@Component
@Order(20)
public class CraftMismatchMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CraftMismatchMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public CraftMismatchMigration(JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("associations") || !tableExists("tools") || !tableExists("projects")) {
                log.warn("关联/工具/项目表尚未就绪，跳过工艺对账回填");
                return;
            }

            int marked = jdbcTemplate.update(
                    "UPDATE associations a "
                            + "JOIN tools t ON t.id = a.tool_id "
                            + "JOIN projects p ON p.id = a.project_id "
                            + "SET a.status = 'MISMATCH' "
                            + "WHERE a.status = 'ACTIVE' "
                            + "AND NULLIF(TRIM(t.craft_type), '') IS NOT NULL "
                            + "AND NULLIF(TRIM(p.category), '') IS NOT NULL "
                            + "AND LOWER(TRIM(t.craft_type)) <> LOWER(TRIM(p.category))");
            if (marked > 0) {
                log.info("已将 {} 条工艺对不上的在用关联标记为 MISMATCH", marked);
            }

            int restored = jdbcTemplate.update(
                    "UPDATE associations a "
                            + "JOIN tools t ON t.id = a.tool_id "
                            + "JOIN projects p ON p.id = a.project_id "
                            + "SET a.status = 'ACTIVE' "
                            + "WHERE a.status = 'MISMATCH' "
                            + "AND (NULLIF(TRIM(t.craft_type), '') IS NULL "
                            + "OR NULLIF(TRIM(p.category), '') IS NULL "
                            + "OR LOWER(TRIM(t.craft_type)) = LOWER(TRIM(p.category)))");
            if (restored > 0) {
                log.info("已将 {} 条工艺重新对上的关联恢复为 ACTIVE", restored);
            }

            // 关掉再打开后拦法/标记仍要生效：清掉溯源缓存里按旧状态算好的结果
            if (marked > 0 || restored > 0) {
                clearTraceCache();
            }
        } catch (Exception e) {
            log.error("关联工艺对账回填失败：{}", e.getMessage());
        }
    }

    private void clearTraceCache() {
        try {
            Set<String> keys = redisTemplate.keys("associations:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("工艺对账后清理溯源缓存失败：{}", e.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }
}
