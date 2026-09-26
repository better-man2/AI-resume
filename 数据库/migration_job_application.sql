/*
 迁移脚本：求职进度管理（P1）
 ------------------------------------------------------------------
 内容：
   1. 新表 job_application —— 记录投递/面试/offer/已拒绝状态，支持从岗位匹配页一键加入。

 说明：
   - 建表语句是幂等的（CREATE TABLE IF NOT EXISTS），重复执行无副作用；
     应用启动时 SchemaMigrationRunner 也会自动建表。
*/

USE `db_biograhpical`;

CREATE TABLE IF NOT EXISTS `job_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `job_id` bigint DEFAULT NULL COMMENT '岗位库ID(可空，允许手动添加)',
  `job_title` varchar(100) NOT NULL COMMENT '岗位名称',
  `company_name` varchar(100) DEFAULT NULL COMMENT '公司名称',
  `salary_range` varchar(50) DEFAULT NULL COMMENT '薪资范围',
  `location` varchar(100) DEFAULT NULL COMMENT '工作地点',
  `match_score` int DEFAULT NULL COMMENT '加入时的匹配分，便于复盘',
  `status` varchar(20) NOT NULL DEFAULT 'APPLIED' COMMENT '状态：APPLIED已投递/INTERVIEW面试中/OFFER已录用/REJECTED已拒绝',
  `interview_at` datetime DEFAULT NULL COMMENT '面试时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '加入时间(投递时间)',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求职进度(投递/面试/offer)';

-- SELECT * FROM `job_application`;
