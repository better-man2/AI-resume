/*
 P0 迁移脚本：AI 简历生成 / 简历模板 / 基础岗位匹配
 ------------------------------------------------------------------
 内容：
   1. resume 表新增 internship（实习经历）、template（简历模板标识）

 说明：
   - 原 dump（db_biographical.sql）保持不动，本脚本只做增量变更。
   - MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报
     "Duplicate column name"，可忽略；应用启动时 SchemaMigrationRunner
     也会查 information_schema 自动补齐缺失列（幂等）。
   - 库名注意：dump 文件头写的是 db_biographical，而 application.yml 配的、
     实际能连上的是 db_biograhpical（少了一个 r），本脚本按实际库名执行。
*/

-- 与 application.yml 的 datasource 保持一致
USE `db_biograhpical`;

ALTER TABLE `resume`
  ADD COLUMN `internship` text NULL COMMENT '实习经历(JSON)' AFTER `work`,
  ADD COLUMN `template` varchar(32) NOT NULL DEFAULT 'classic' COMMENT '简历模板标识(classic/modern/campus)' AFTER `award`;

-- 检查结果
-- SHOW COLUMNS FROM `resume`;
