/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : db_biographical

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 04/07/2025 10:28:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for history
-- ----------------------------
DROP TABLE IF EXISTS `history`;
CREATE TABLE `history` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of history
-- ----------------------------
BEGIN;
INSERT INTO `history` (`id`, `question`, `result`, `username`, `user_id`, `time`) VALUES ('4f709418e1614fba8c260e23bb343345', '1', '你好！你输入了数字“1”。有什么我可以帮助你的吗？例如：\n\n- 你需要关于数字1的数学信息吗？\n- 还是需要帮助完成某个任务（比如列表中的第一项）？\n- 或者这是一个测试消息？\n\n告诉我更多细节，我会尽力帮助你！ 😊', 'admin', '09d74e5584a448ada71f362ea420207c', '2025-07-04 00:58:51');
INSERT INTO `history` (`id`, `question`, `result`, `username`, `user_id`, `time`) VALUES ('2c5f0a5161a9423384411f17ac21fd09', '请生成一份结构化的简历信息，严格按照以下JSON格式返回：\n{\n  \"jobStatus\": \"在职/离职/应届生\",\n  \"jobTitle\": \"期望职位\",\n  \"salaryExpectation\": \"期望薪资\",\n  \"education\": {\n    \"school\": \"学校名称\",\n    \"major\": \"专业名称\",\n    \"degree\": \"学历\"\n  },\n  \"profession\": {\n    \"skill\": \"技能描述\"\n  },\n  \"work\": {\n    \"company\": \"公司名称\",\n    \"department\": \"部门名称\",\n    \"position\": \"职位名称\",\n    \"details\": \"工作内容描述\"\n  },\n  \"project\": {\n    \"name\": \"项目名称\",\n    \"details\": \"项目描述\"\n  },\n  \"award\": {\n    \"details\": \"获奖情况\"\n  }\n}\n\n基于以下信息生成应届生简历：\n专业：计算机软件工程\n期望职位：Java 开发\n补充信息：擅长 Java、Python、Mysql 等\n', '```json\n{\n  \"jobStatus\": \"应届生\",\n  \"jobTitle\": \"Java 开发\",\n  \"salaryExpectation\": \"\",\n  \"education\": {\n    \"school\": \"\",\n    \"major\": \"计算机软件工程\",\n    \"degree\": \"\"\n  },\n  \"profession\": {\n    \"skill\": \"擅长 Java、Python、Mysql 等\"\n  },\n  \"work\": {\n    \"company\": \"\",\n    \"department\": \"\",\n    \"position\": \"\",\n    \"details\": \"\"\n  },\n  \"project\": {\n    \"name\": \"\",\n    \"details\": \"\"\n  },\n  \"award\": {\n    \"details\": \"\"\n  }\n}\n```', 'admin', '09d74e5584a448ada71f362ea420207c', '2025-07-04 01:00:35');
INSERT INTO `history` (`id`, `question`, `result`, `username`, `user_id`, `time`) VALUES ('69dc197522c04cee8489c6d032946086', '你好，可以介绍下自己么', '你好！我是Qwen，是阿里巴巴集团旗下的通义实验室自主研发的超大规模语言模型。我能够帮助你回答问题、创作文字，比如写故事、写公文、写邮件、写剧本、逻辑推理、编程等等，还能表达观点，玩游戏等。我支持多种语言，包括但不限于中文、英文、德语、法语、西班牙语等。如果你有任何问题或需要帮助，欢迎随时向我提问！', 'admin', '09d74e5584a448ada71f362ea420207c', '2025-07-04 02:08:00');
INSERT INTO `history` (`id`, `question`, `result`, `username`, `user_id`, `time`) VALUES ('6f9f382091474b30a31b695f2fdc3aee', '请生成一份结构化的简历信息，严格按照以下JSON格式返回：\n{\n  \"jobStatus\": \"在职/离职/应届生\",\n  \"jobTitle\": \"期望职位\",\n  \"salaryExpectation\": \"期望薪资\",\n  \"education\": {\n    \"school\": \"学校名称\",\n    \"major\": \"专业名称\",\n    \"degree\": \"学历\"\n  },\n  \"profession\": {\n    \"skill\": \"技能描述\"\n  },\n  \"work\": {\n    \"company\": \"公司名称\",\n    \"department\": \"部门名称\",\n    \"position\": \"职位名称\",\n    \"details\": \"工作内容描述\"\n  },\n  \"project\": {\n    \"name\": \"项目名称\",\n    \"details\": \"项目描述\"\n  },\n  \"award\": {\n    \"details\": \"获奖情况\"\n  }\n}\n\n基于以下信息生成应届生简历：\n专业：计算机专业\n期望职位：1000\n补充信息：1、擅长 Java、Mysql等\n2、性格开朗\n', '```json\n{\n  \"jobStatus\": \"应届生\",\n  \"jobTitle\": \"软件工程师\",\n  \"salaryExpectation\": \"1000\",\n  \"education\": {\n    \"school\": \"XX大学\",\n    \"major\": \"计算机专业\",\n    \"degree\": \"本科\"\n  },\n  \"profession\": {\n    \"skill\": \"擅长 Java 编程语言，熟悉 Mysql 数据库\"\n  },\n  \"work\": {\n    \"company\": \"\",\n    \"department\": \"\",\n    \"position\": \"\",\n    \"details\": \"\"\n  },\n  \"project\": {\n    \"name\": \"\",\n    \"details\": \"\"\n  },\n  \"award\": {\n    \"details\": \"\"\n  }\n}\n```', 'admin', '09d74e5584a448ada71f362ea420207c', '2025-07-04 02:09:28');
COMMIT;

-- ----------------------------
-- Table structure for job_category
-- ----------------------------
DROP TABLE IF EXISTS `job_category`;
CREATE TABLE `job_category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `description` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位分类表';

-- ----------------------------
-- Records of job_category
-- ----------------------------
BEGIN;
INSERT INTO `job_category` (`id`, `name`, `description`, `create_time`) VALUES (1, '计算机软件开发', '计算机', '2025-07-04 01:30:28');
INSERT INTO `job_category` (`id`, `name`, `description`, `create_time`) VALUES (2, '人事', '人事', '2025-07-04 01:30:38');
INSERT INTO `job_category` (`id`, `name`, `description`, `create_time`) VALUES (3, '财务', '财务', '2025-07-04 01:30:42');
COMMIT;

-- ----------------------------
-- Table structure for job_position
-- ----------------------------
DROP TABLE IF EXISTS `job_position`;
CREATE TABLE `job_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位ID',
  `category_id` int NOT NULL COMMENT '分类ID',
  `title` varchar(100) NOT NULL COMMENT '职位名称',
  `description` text NOT NULL COMMENT '职位描述',
  `required_skills` text NOT NULL COMMENT '所需技能',
  `experience_requirement` varchar(50) DEFAULT NULL COMMENT '经验要求',
  `education_requirement` varchar(50) DEFAULT NULL COMMENT '学历要求',
  `salary_range` varchar(50) DEFAULT NULL COMMENT '薪资范围',
  `company_name` varchar(100) DEFAULT NULL COMMENT '公司名称',
  `location` varchar(200) DEFAULT NULL COMMENT '工作地点',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(1:启用 0:禁用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  CONSTRAINT `fk_job_category` FOREIGN KEY (`category_id`) REFERENCES `job_category` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位信息表';

-- ----------------------------
-- Records of job_position
-- ----------------------------
BEGIN;
INSERT INTO `job_position` (`id`, `category_id`, `title`, `description`, `required_skills`, `experience_requirement`, `education_requirement`, `salary_range`, `company_name`, `location`, `status`, `create_time`, `update_time`) VALUES (1, 1, 'Java开发', '从事软件开发设计', '1.Java\n2.Mysql', '10', '本科', '10000', '阿里', '杭州', 1, '2025-07-04 01:31:31', '2025-07-04 01:31:31');
INSERT INTO `job_position` (`id`, `category_id`, `title`, `description`, `required_skills`, `experience_requirement`, `education_requirement`, `salary_range`, `company_name`, `location`, `status`, `create_time`, `update_time`) VALUES (2, 1, 'Python开发', 'python 开发能力', 'python 开发能力', '10 年', '不限', '20000', '字节', '杭州', 1, '2025-07-04 01:32:03', '2025-07-04 01:32:03');
COMMIT;

-- ----------------------------
-- Table structure for resume
-- ----------------------------
DROP TABLE IF EXISTS `resume`;
CREATE TABLE `resume` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `job_status` varchar(20) DEFAULT NULL COMMENT '求职状态',
  `job_title` varchar(100) DEFAULT NULL COMMENT '期望职位',
  `salary_expectation` varchar(50) DEFAULT NULL COMMENT '期望薪资',
  `education` text COMMENT '教育经历(JSON)',
  `profession` text COMMENT '专业技能(JSON)',
  `work` text COMMENT '工作经历(JSON)',
  `project` text COMMENT '项目经历(JSON)',
  `award` text COMMENT '获奖情况(JSON)',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1940956089338187778 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历信息表';

-- ----------------------------
-- Records of resume
-- ----------------------------
BEGIN;
INSERT INTO `resume` (`id`, `user_id`, `name`, `phone`, `email`, `job_status`, `job_title`, `salary_expectation`, `education`, `profession`, `work`, `project`, `award`, `update_time`) VALUES (1940938754204311553, '09d74e5584a448ada71f362ea420207c', '杭州水果捞', '18666666666', '123@qq.com', '应届生', 'Java 开发', '', '{\"school\":\"\",\"major\":\"计算机软件工程\",\"degree\":\"\",\"studyPeriod\":[]}', '{\"skill\":\"擅长 Java、Python、Mysql 等\"}', '{\"company\":\"\",\"department\":\"\",\"position\":\"\",\"period\":[],\"details\":\"\"}', '{\"name\":\"\",\"period\":[],\"details\":\"\"}', '{\"details\":\"\"}', '2025-07-04 01:38:37');
INSERT INTO `resume` (`id`, `user_id`, `name`, `phone`, `email`, `job_status`, `job_title`, `salary_expectation`, `education`, `profession`, `work`, `project`, `award`, `update_time`) VALUES (1940956089338187777, '09d74e5584a448ada71f362ea420207c', 'admin', '18666666666', '123@qq.com', '应届生', '软件工程师', '1000', '{\"school\":\"XX大学\",\"major\":\"计算机专业\",\"degree\":\"本科\"}', '{\"skill\":\"擅长 Java 编程语言，熟悉 Mysql 数据库\"}', '{\"company\":\"\",\"department\":\"\",\"position\":\"\",\"details\":\"\"}', '{\"name\":\"\",\"details\":\"\"}', '{\"details\":\"\"}', '2025-07-04 02:09:28');
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `phone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`) VALUES ('09d74e5584a448ada71f362ea420207c', 'admin', '123456', '123@qq.com', '18666666666');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
