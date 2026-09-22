/*
 迁移脚本：基础信息独立存储 + 自我评价
 ------------------------------------------------------------------
 内容：
   1. resume.basic_info     —— 基础信息独立 JSON 字段
      { "name": "张三", "phone": "138...", "email": "a@b.com", "city": "杭州" }
      该字段只由用户表单录入，永远不参与大模型生成，AI 也不允许修改。
   2. resume.self_evaluation —— 自我评价（AI 生成的描述性文本，非结构化内容）

 说明：
   - 原 dump 与 migration_p0.sql 保持不动，本脚本只做增量变更。
   - MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报
     "Duplicate column name"，可忽略；应用启动时 SchemaMigrationRunner
     也会查 information_schema 自动补齐（幂等）。
   - 库名与 application.yml 的 datasource 保持一致。
*/

USE `db_biograhpical`;

ALTER TABLE `resume`
  ADD COLUMN `basic_info` text NULL COMMENT '基础信息(JSON，仅表单录入，AI 不参与)' AFTER `id`,
  ADD COLUMN `self_evaluation` text NULL COMMENT '自我评价(AI 生成的描述文本)' AFTER `internship`;

-- 历史数据回填：把已有的 name/phone/email 迁进 basic_info
-- UPDATE `resume`
-- SET `basic_info` = JSON_OBJECT('name', IFNULL(`name`, ''), 'phone', IFNULL(`phone`, ''), 'email', IFNULL(`email`, ''), 'city', '')
-- WHERE `basic_info` IS NULL;
