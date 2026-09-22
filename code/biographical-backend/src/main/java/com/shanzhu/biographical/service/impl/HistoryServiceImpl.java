/**
 * @projectName springAi
 * @package com.shanzhu.biographical.service.impl
 * @className com.shanzhu.biographical.service.impl.HistoryServiceImpl
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shanzhu.biographical.mapper.HistoryMapper;
import com.shanzhu.biographical.model.History;
import com.shanzhu.biographical.service.HistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Resource
    private HistoryMapper historyMapper;

    @Override
    public Page<History> getHistoryList(String userId, int pageNum, int pageSize) {
        Page<History> page = new Page<>(pageNum, pageSize);
        QueryWrapper<History> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("time");
        return historyMapper.selectPage(page, wrapper);
    }

    @Override
    public History getHistoryDetail(String historyId) {
        return historyMapper.selectById(historyId);
    }

    @Override
    public void saveHistory(History history) {
        historyMapper.insert(history);
    }

}
