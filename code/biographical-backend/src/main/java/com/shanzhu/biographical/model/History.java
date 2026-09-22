/**
 * @projectName springAi
 * @package com.shanzhu.biographical.model
 * @className com.shanzhu.biographical.model.History
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("history")
public class History {
    private String id;
    private String question;
    private String result;
    private String username;
    private String userId;
    private Date time;
}
