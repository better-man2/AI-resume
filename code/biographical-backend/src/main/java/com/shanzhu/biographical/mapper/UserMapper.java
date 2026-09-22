/**
 * @projectName springAi
 * @package com.shanzhu.biographical.mapper
 * @className com.shanzhu.biographical.mapper.UserMapper
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanzhu.biographical.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}