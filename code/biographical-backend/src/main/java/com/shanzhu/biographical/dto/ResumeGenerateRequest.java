package com.shanzhu.biographical.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 一键生成简历入参（对应前端分步表单的一次性提交）。
 */
@Data
public class ResumeGenerateRequest {

    private String userId;

    private String username;

    /** 简历模板标识：classic/modern/campus */
    private String template = "classic";

    /**
     * 分步表单数据（基础信息与经历分开）：
     * basic{name,phone,email,city}      —— 基础信息，仅表单录入，不进入大模型
     * intent{jobStatus,jobTitle,salaryExpectation}
     * educations[{school,major,degree,start,end}]
     * skills["Java","MySQL"]
     * projects[{name,role,start,end,details}]
     * internships[{company,department,position,start,end,details}]
     * works[{company,department,position,start,end,details}]
     * awards[{name,date}]
     * extra 补充说明（供 AI 生成自我评价时参考）
     */
    private Map<String, Object> form = new HashMap<>();
}
