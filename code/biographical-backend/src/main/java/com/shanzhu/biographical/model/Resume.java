package com.shanzhu.biographical.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("resume")

public class Resume {

    private Long id;

    private String userId;

    /**
     * 基础信息（独立 JSON：{name,phone,email,city}）。
     * 只由用户表单录入，不参与大模型生成，AI 也不允许改动。
     */
    private String basicInfo;

    private String name;
    private String phone;
    private String email;
    private String jobStatus;
    private String jobTitle;
    private String salaryExpectation;
    private String education;
    private String profession;
    private String work;
    private String internship;
    /** 自我评价（AI 生成的描述性文本） */
    private String selfEvaluation;
    /** 个人优势（AI 生成的短句数组，JSON） */
    private String strengths;
    /**
     * AI 生成内容溯源：{"pending":["selfEvaluation","project.0.details"]}
     * 记录哪些内容是 AI 写的、用户还没核实，用于编辑页的「AI 内容核对」面板。
     */
    private String aiMeta;
    private String project;
    private String award;
    /** 简历模板标识：classic/modern/campus */
    private String template;
    private Date updateTime;

}
