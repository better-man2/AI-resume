/**
 * @projectName springAi
 * @package com.shanzhu.biographical.model
 * @className com.shanzhu.biographical.model.User
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.model;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")  // 绑定数据库中的 'user' 表
public class User {

    private String id;
    private String username;
    private String password;
    private String email;
    private String phone;
}
