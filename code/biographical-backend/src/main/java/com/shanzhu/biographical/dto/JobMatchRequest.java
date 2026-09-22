package com.shanzhu.biographical.dto;

import lombok.Data;

/**
 * 基础岗位匹配入参。
 */
@Data
public class JobMatchRequest {

    /** 用户ID：不传 resumeText 时按该用户最新简历匹配 */
    private String userId;

    /** 粘贴导入的简历文本：非空时优先用它做匹配 */
    private String resumeText;

    /** 返回岗位数量上限，默认 10 */
    private Integer limit = 10;
}
