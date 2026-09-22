/**
 * @projectName springAi
 * @package com.shanzhu.biographical.service
 * @className com.shanzhu.biographical.service.UserService
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.service;

import com.shanzhu.biographical.model.User;

public interface UserService {

    boolean addUser(User user);

    User login (User user);

    User getUser(String id);
}
