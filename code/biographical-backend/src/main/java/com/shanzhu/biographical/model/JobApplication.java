package com.shanzhu.biographical.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 求职进度（投递 / 面试 / Offer / 已拒绝）。
 */
@Data
@TableName("job_application")
public class JobApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    /** 岗位库里的岗位 ID，手动添加的条目可以为空 */
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String salaryRange;
    private String location;
    /** 加入进度时的匹配分，便于复盘 */
    private Integer matchScore;
    /** APPLIED / INTERVIEW / OFFER / REJECTED */
    private String status;
    private Date interviewAt;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
