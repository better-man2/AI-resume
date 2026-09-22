/**
 * @projectName springAi
 * @package com.shanzhu.biographical.service
 * @className com.shanzhu.biographical.service.HistoryService
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shanzhu.biographical.model.History;

public interface HistoryService {

    void saveHistory(History history);

    Page<History> getHistoryList(String userId, int pageNum, int pageSize);

    History getHistoryDetail(String historyId);
}
