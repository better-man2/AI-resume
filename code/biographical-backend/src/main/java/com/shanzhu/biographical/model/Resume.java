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
    private String project;
    private String award;
    /** 简历模板标识：classic/modern/campus */
    private String template;
    private Date updateTime;

}
