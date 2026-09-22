package com.shanzhu.biographical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzhu.biographical.mapper.ResumeMapper;
import com.shanzhu.biographical.model.Resume;
import com.shanzhu.biographical.service.ResumeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResumeServiceImpl implements ResumeService {

    @Resource
    private ResumeMapper resumeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Resume saveResume(String userId, Map<String, Object> resumeData) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        updateResumeFields(resume, resumeData);
        resume.setUpdateTime(new Date());

        resumeMapper.insert(resume);
        return resume;
    }

    @Override
    public Resume getLatestResume(String userId) {
        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdateTime)
                .last("LIMIT 1");
        return resumeMapper.selectOne(queryWrapper);
    }


    @Override
    @Transactional
    public Resume updateResume(String userId, Map<String, Object> resumeData) {
        Resume resume = getLatestResume(userId);
        if (resume == null) {
            return saveResume(userId, resumeData);
        }

        updateResumeFields(resume, resumeData);
        resume.setUpdateTime(new Date());

        resumeMapper.updateById(resume);
        return resume;
    }

    private void updateResumeFields(Resume resume, Map<String, Object> data) {
        try {
            // 基础信息：独立 JSON 字段，只由用户表单录入（AI 不参与），同时同步 name/phone/email 列保证旧读取方兼容
            if (data.get("basicInfo") != null) {
                Map<String, Object> basic = asMap(data.get("basicInfo"));
                resume.setBasicInfo(objectMapper.writeValueAsString(basic));
                resume.setName(text(basic.get("name")));
                resume.setPhone(text(basic.get("phone")));
                resume.setEmail(text(basic.get("email")));
            }
            // 兼容旧的平铺写法（老前端/老数据）
            if (data.get("name") != null) resume.setName((String) data.get("name"));
            if (data.get("phone") != null) resume.setPhone((String) data.get("phone"));
            if (data.get("email") != null) resume.setEmail((String) data.get("email"));

            if (data.get("jobStatus") != null) resume.setJobStatus((String) data.get("jobStatus"));
            if (data.get("jobTitle") != null) resume.setJobTitle((String) data.get("jobTitle"));
            if (data.get("salaryExpectation") != null) resume.setSalaryExpectation((String) data.get("salaryExpectation"));

            // 将复杂对象转换为JSON字符串存储
            if (data.get("education") != null) {
                resume.setEducation(objectMapper.writeValueAsString(data.get("education")));
            }
            if (data.get("profession") != null) {
                resume.setProfession(objectMapper.writeValueAsString(data.get("profession")));
            }
            if (data.get("work") != null) {
                resume.setWork(objectMapper.writeValueAsString(data.get("work")));
            }
            if (data.get("internship") != null) {
                resume.setInternship(objectMapper.writeValueAsString(data.get("internship")));
            }
            if (data.get("selfEvaluation") != null) {
                resume.setSelfEvaluation((String) data.get("selfEvaluation"));
            }
            if (data.get("strengths") != null) {
                resume.setStrengths(objectMapper.writeValueAsString(data.get("strengths")));
            }
            if (data.get("project") != null) {
                resume.setProject(objectMapper.writeValueAsString(data.get("project")));
            }
            if (data.get("award") != null) {
                resume.setAward(objectMapper.writeValueAsString(data.get("award")));
            }
            // 简历模板标识（纯文本，不转 JSON）
            if (data.get("template") != null && !text(data.get("template")).isEmpty()) {
                resume.setTemplate((String) data.get("template"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update resume fields", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
