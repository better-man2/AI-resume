/*
 迁移脚本：个人优势 + AI 参考项目标记
 ------------------------------------------------------------------
 内容：
   1. resume.strengths —— 个人优势（AI 生成的短句数组，JSON 字符串，例如 ["...", "..."]）

 说明：
   - 项目经历/实习经历条目里新增的 "aiGenerated": true 是 JSON 内部字段
     存在原有的 project / internship 文本列里，不需要新增数据库列。
   - MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 "Duplicate column name"，可忽略；
     应用启动时 SchemaMigrationRunner 也会查 information_schema 自动补齐（幂等）
*/

USE `db_biograhpical`;

ALTER TABLE `resume`
  ADD COLUMN `strengths` text NULL COMMENT '个人优势(AI 生成的短句数组 JSON)' AFTER `self_evaluation`;

-- SHOW COLUMNS FROM `resume`;
