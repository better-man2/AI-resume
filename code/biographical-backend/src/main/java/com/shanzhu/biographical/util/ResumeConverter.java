package com.shanzhu.biographical.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzhu.biographical.model.Resume;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简历实体 -> 前端/匹配用的 Map。
 * <p>
 * 历史上只支持对象结构，V2 结构里经历是数组，这里统一按 Object 读，读取失败的单字段置空而不影响其它字段。
 */
@Component
public class ResumeConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> toMap(Resume resume) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (resume == null) {
            return result;
        }
        result.put("basicInfo", basicInfoOf(resume));
        result.put("jobStatus", resume.getJobStatus());
        result.put("jobTitle", resume.getJobTitle());
        result.put("salaryExpectation", resume.getSalaryExpectation());
        result.put("education", readJson(resume.getEducation()));
        result.put("profession", readJson(resume.getProfession()));
        result.put("work", readJson(resume.getWork()));
        result.put("internship", readJson(resume.getInternship()));
        result.put("selfEvaluation", resume.getSelfEvaluation());
        result.put("strengths", readJson(resume.getStrengths()));
        result.put("project", readJson(resume.getProject()));
        result.put("award", readJson(resume.getAward()));
        result.put("template", resume.getTemplate());
        result.put("updateTime", resume.getUpdateTime());
        return result;
    }

    /**
     * 基础信息：优先取独立的 basic_info JSON；老数据没有该字段时，用 name/phone/email 列兜底。
     * 这部分内容只由用户表单录入，不参与大模型生成。
     */
    public Map<String, Object> basicInfoOf(Resume resume) {
        Map<String, Object> basic = new LinkedHashMap<>();
        Map<String, Object> stored = asMap(readJson(resume.getBasicInfo()));
        basic.put("name", firstNonBlank(stored.get("name"), resume.getName()));
        basic.put("phone", firstNonBlank(stored.get("phone"), resume.getPhone()));
        basic.put("email", firstNonBlank(stored.get("email"), resume.getEmail()));
        basic.put("city", text(stored.get("city")));
        return basic;
    }

    private String firstNonBlank(Object primary, String fallback) {
        String value = text(primary);
        return value.isEmpty() ? text(fallback) : value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Object readJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}
