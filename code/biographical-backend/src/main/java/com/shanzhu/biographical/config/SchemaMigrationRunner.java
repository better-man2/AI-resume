package com.shanzhu.biographical.config;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐 P0 需要的库表变更（幂等）。
 * <p>
 * 与 数据库/migration_p0.sql 等价：查 information_schema，缺什么补什么。
 * 迁移失败只记录 WARN，不阻断启动（例如本地库口令不对时服务仍可启动）。
 */
@Component
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** 需要确保存在的列：表名 -> (列名, 建列语句) */
    private static final String[][] REQUIRED_COLUMNS = {
            {"resume", "internship",
                    "ALTER TABLE `resume` ADD COLUMN `internship` text NULL COMMENT '实习经历(JSON)'"},
            {"resume", "template",
                    "ALTER TABLE `resume` ADD COLUMN `template` varchar(32) NOT NULL DEFAULT 'classic' COMMENT '简历模板标识(classic/modern/campus)'"},
            {"resume", "basic_info",
                    "ALTER TABLE `resume` ADD COLUMN `basic_info` text NULL COMMENT '基础信息(JSON，仅表单录入，AI 不参与)'"},
            {"resume", "self_evaluation",
                    "ALTER TABLE `resume` ADD COLUMN `self_evaluation` text NULL COMMENT '自我评价(AI 生成的描述文本)'"},
            {"resume", "strengths",
                    "ALTER TABLE `resume` ADD COLUMN `strengths` text NULL COMMENT '个人优势(AI 生成的短句数组 JSON)'"},
    };

    @Override
    public void run(ApplicationArguments args) {
        for (String[] column : REQUIRED_COLUMNS) {
            String table = column[0];
            String name = column[1];
            String ddl = column[2];
            try {
                if (columnExists(table, name)) {
                    continue;
                }
                jdbcTemplate.execute(ddl);
                log.info("[schema] 已补充字段 {}.{}", table, name);
            } catch (Exception e) {
                log.warn("[schema] 自动迁移跳过（{}.{}）：{}，如需手动执行请参考 数据库/migration_p0.sql",
                        table, name, e.getMessage());
            }
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }
}
