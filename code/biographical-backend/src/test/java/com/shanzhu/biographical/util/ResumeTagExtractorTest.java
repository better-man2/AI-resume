package com.shanzhu.biographical.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础岗位匹配的标签抽取逻辑（纯逻辑，不依赖数据库与 Spring 容器）。
 */
class ResumeTagExtractorTest {

    private final ResumeTagExtractor extractor = new ResumeTagExtractor();

    @Test
    void 解析JD技能时应去掉序号与分隔符() {
        List<String> skills = extractor.parseRequiredSkills("1.Java\n2.Mysql、3.Redis，4.Kafka");

        assertEquals(List.of("Java", "Mysql", "Redis", "Kafka"), skills);
    }

    @Test
    void 技能匹配应忽略大小写与分隔符() {
        assertTrue(extractor.skillMatches("Spring Boot", "springboot"));
        assertTrue(extractor.skillMatches("MyBatis-Plus", "mybatis plus"));
        assertTrue(extractor.skillMatches("Java", "Java"));
    }

    @Test
    void 技能匹配不应把Java当成JavaScript() {
        assertFalse(extractor.skillMatches("Java", "JavaScript"));
        assertFalse(extractor.skillMatches("Go", "Google Analytics"));
    }

    @Test
    void 学历等级应可比较() {
        assertTrue(extractor.degreeRank("本科") > extractor.degreeRank("大专"));
        assertTrue(extractor.degreeRank("硕士") > extractor.degreeRank("本科"));
        assertEquals(0, extractor.degreeRank("不限"));
        assertEquals(0, extractor.degreeRank(null));
    }

    @Test
    void 解析经验要求时应识别不限与数字() {
        assertEquals(10.0, extractor.parseRequiredYears("10"));
        assertEquals(10.0, extractor.parseRequiredYears("10 年"));
        assertEquals(3.0, extractor.parseRequiredYears("3-5年"));
        assertEquals(0.0, extractor.parseRequiredYears("不限"));
        assertEquals(0.0, extractor.parseRequiredYears("应届生"));
    }

    @Test
    void 从V2数组结构简历抽取标签() {
        Map<String, Object> resume = Map.of(
                "jobTitle", "Java开发",
                "education", List.of(Map.of("school", "XX大学", "major", "计算机", "degree", "本科",
                        "start", "2019-09", "end", "2023-06")),
                "profession", Map.of("skills", List.of("Java", "MySQL")),
                "internship", List.of(Map.of("company", "某公司", "position", "后端开发",
                        "start", "2022-07", "end", "2023-01", "details", "参与 Redis 缓存改造")),
                "project", List.of(),
                "work", List.of(),
                "award", List.of());

        Map<String, Object> tags = extractor.extractFromResume(resume);

        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) tags.get("skills");
        assertTrue(skills.contains("Java"), "应包含结构化技能 Java");
        assertTrue(skills.contains("MySQL"), "应包含结构化技能 MySQL");
        assertTrue(skills.contains("Redis"), "应能从经历描述里识别出 Redis");
        assertEquals("本科", tags.get("educationLevel"));
        assertEquals(0.5, tags.get("experienceYears"), "2022-07~2023-01 约 0.5 年");
        assertEquals("Java开发", tags.get("jobTitle"));
    }

    @Test
    void 兼容旧的对象结构与period数组() {
        Map<String, Object> resume = Map.of(
                "education", Map.of("school", "XX大学", "major", "软件工程", "degree", "硕士"),
                "profession", Map.of("skill", "擅长 Java、Mysql 等"),
                "work", Map.of("company", "某科技", "position", "开发",
                        "period", List.of("2023-07", "2024-07"), "details", "负责 Spring Boot 服务"));

        Map<String, Object> tags = extractor.extractFromResume(resume);

        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) tags.get("skills");
        assertTrue(skills.contains("Java"), "旧的 skill 文本应能拆成标签");
        assertTrue(skills.contains("MySQL"), "大小写不同的 Mysql 应归一为 MySQL");
        assertEquals("硕士", tags.get("educationLevel"));
        assertEquals(1.0, tags.get("experienceYears"), "旧数据 period 数组应能算出年限");
    }

    @Test
    void 从粘贴文本抽取标签() {
        Map<String, Object> tags = extractor.extractFromText(
                "熟悉 Java、Spring Boot、MySQL，本科毕业，有 3 年开发经验。");

        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) tags.get("skills");
        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("MySQL"));
        assertEquals("本科", tags.get("educationLevel"));
        assertEquals(3.0, tags.get("experienceYears"));
    }

    @Test
    void 文本没写年限时按时间区间估算并排除在校时间() {
        Map<String, Object> tags = extractor.extractFromText(
                "2019-09 ~ 2023-06 浙江某大学 计算机科学与技术专业；"
                        + "2023-07 ~ 2024-07 杭州某科技公司 Java 开发实习；熟悉 MySQL、Redis。");

        assertEquals(1.0, tags.get("experienceYears"), "只累计实习区间，排除在校区间");
    }

    @Test
    void 文本写明年限时优先使用写明值() {
        Map<String, Object> tags = extractor.extractFromText("有 3 年开发经验，2020-01 ~ 2021-01 在某公司实习");

        assertEquals(3.0, tags.get("experienceYears"));
    }

    @Test
    void 居住地取自基础信息独立字段() {
        Map<String, Object> resume = Map.of(
                "basicInfo", Map.of("name", "张三", "phone", "13800138000", "email", "a@b.com", "city", "杭州"),
                "profession", Map.of("skills", List.of("Java")),
                "education", List.of());

        Map<String, Object> tags = extractor.extractFromResume(resume);

        assertEquals("杭州", tags.get("city"));
    }

    @Test
    void 粘贴文本时可以从现居地描述里识别城市() {
        assertEquals("杭州", extractor.scanCity("现居杭州，熟悉 Java"));
        assertEquals("上海", extractor.scanCity("居住地：上海市"));
        assertEquals("", extractor.scanCity("熟悉 Java、MySQL"));
    }
}
