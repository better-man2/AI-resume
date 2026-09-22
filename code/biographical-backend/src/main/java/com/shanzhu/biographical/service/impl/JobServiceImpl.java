package com.shanzhu.biographical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shanzhu.biographical.mapper.JobCategoryMapper;
import com.shanzhu.biographical.mapper.JobPositionMapper;
import com.shanzhu.biographical.model.JobCategory;
import com.shanzhu.biographical.model.JobPosition;
import com.shanzhu.biographical.service.JobService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    @Resource
    private JobCategoryMapper categoryMapper;

    @Resource
    private JobPositionMapper positionMapper;

    @Override
    public List<JobCategory> getAllCategories() {
        return categoryMapper.selectList(null);
    }

    @Override
    @Transactional
    public JobCategory addCategory(JobCategory category) {
        category.setCreateTime(new Date());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public boolean deleteCategory(Integer id) {
        // 检查是否有职位使用该分类
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobPosition::getCategoryId, id);
        long count = positionMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该分类下还有职位，无法删除");
        }

        return categoryMapper.deleteById(id) > 0;
    }

    @Override
    public IPage<JobPosition> getJobList(int page, int size, String keyword, Integer categoryId) {
        Page<JobPosition> pageParam = new Page<>(page, size);
        return positionMapper.findJobs(pageParam, keyword, categoryId);
    }

    @Override
    public JobPosition getJobById(Long id) {
        // 基本查询
        JobPosition job = positionMapper.selectById(id);
        if (job != null) {
            // 获取分类名称
            JobCategory category = categoryMapper.selectById(job.getCategoryId());
            if (category != null) {
                job.setCategoryName(category.getName());
            }
        }
        return job;
    }

    @Override
    @Transactional
    public boolean addJob(JobPosition job) {
        Date now = new Date();
        job.setCreateTime(now);
        job.setUpdateTime(now);
        job.setStatus(job.getStatus() == null ? 1 : job.getStatus());
        return positionMapper.insert(job) > 0;
    }

    @Override
    @Transactional
    public boolean updateJob(JobPosition job) {
        job.setUpdateTime(new Date());
        return positionMapper.updateById(job) > 0;
    }

    @Override
    @Transactional
    public boolean deleteJob(Long id) {
        return positionMapper.deleteById(id) > 0;
    }
}
