package com.shanzhu.biographical.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("job_category")
public class JobCategory {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;

    @TableField("create_time")
    private Date createTime;
}
