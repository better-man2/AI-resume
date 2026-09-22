/**
 * @projectName springAi
 * @package com.shanzhu.biographical.service.impl
 * @className com.shanzhu.biographical.service.impl.UserServiceImpl
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shanzhu.biographical.config.uuid;
import com.shanzhu.biographical.mapper.UserMapper;
import com.shanzhu.biographical.model.User;
import com.shanzhu.biographical.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private uuid.UuidGenerator uuidGenerator;


    @Override
    public boolean addUser(User user) {
        // 检查用户名或邮箱是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername()).or().eq("email", user.getEmail());
        if (userMapper.selectCount(queryWrapper) > 0) {
            return false; // 用户名或邮箱已存在
        }
        user.setId(uuidGenerator.generateUuid32());
        return userMapper.insert(user) > 0;
    }

    @Override
    public User login(User user) {
        //通过email查询用户
        User dbUser = userMapper.selectOne(new QueryWrapper<User>().eq("email", user.getEmail()));
        if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
            return dbUser;
        } else {
            return null;
        }
    }

    @Override
    public User getUser(String id) {
        return userMapper.selectById(id);
    }
}
