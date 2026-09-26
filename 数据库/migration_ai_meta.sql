/*
 迁移脚本：AI 生成内容溯源（事实台账）
 ------------------------------------------------------------------
 内容：
   1. resume.ai_meta —— 记录哪些内容是 AI 生成、还没被用户核实的，例如：
      {"pending":["selfEvaluation","strengths","summary","project.0.details","project.1"]}
      前端据此在编辑页显示「AI 内容核对」面板，逐条核实后从 pending 里移除。

 说明：
   - MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 "Duplicate column name"，可忽略；
     应用启动时 SchemaMigrationRunner 也会查 information_schema 自动补齐（幂等）。
*/

USE `db_biograhpical`;

ALTER TABLE `resume`
  ADD COLUMN `ai_meta` text NULL COMMENT 'AI 生成内容溯源(JSON，pending 待核实路径)' AFTER `strengths`;

-- SHOW COLUMNS FROM `resume`;
