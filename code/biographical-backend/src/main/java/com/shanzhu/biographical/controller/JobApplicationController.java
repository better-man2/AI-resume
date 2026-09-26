package com.shanzhu.biographical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzhu.biographical.mapper.JobApplicationMapper;
import com.shanzhu.biographical.model.JobApplication;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 求职进度管理（P1）：岗位匹配页「加入求职进度」的落地接口 + 进度页的增删改查。
 */
@RestController
@RequestMapping("/api/application")
@CrossOrigin
public class JobApplicationController {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationController.class);

    /** 状态白名单：已投递 / 面试中 / 已录用 / 已拒绝 */
    private static final Set<String> STATUSES = Set.of("APPLIED", "INTERVIEW", "OFFER", "REJECTED");

    @Resource
    private JobApplicationMapper jobApplicationMapper;

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody JobApplication request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = text(request.getUserId());
            if (userId.isEmpty()) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            if (text(request.getJobTitle()).isEmpty()) {
                throw new IllegalArgumentException("岗位名称不能为空");
            }

            // 同一个岗位不重复加入
            if (request.getJobId() != null) {
                LambdaQueryWrapper<JobApplication> exists = new LambdaQueryWrapper<>();
                exists.eq(JobApplication::getUserId, userId).eq(JobApplication::getJobId, request.getJobId());
                if (jobApplicationMapper.selectCount(exists) > 0) {
                    response.put("success", false);
                    response.put("error", "这个岗位已经在你的求职进度里了");
                    return response;
                }
            }

            Date now = new Date();
            request.setId(null);
            request.setUserId(userId);
            request.setStatus(STATUSES.contains(text(request.getStatus())) ? request.getStatus() : "APPLIED");
            request.setCreateTime(now);
            request.setUpdateTime(now);
            jobApplicationMapper.insert(request);

            response.put("success", true);
            response.put("data", request);
            return response;
        } catch (Exception e) {
            log.error("加入求职进度失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (text(userId).isEmpty()) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(JobApplication::getUserId, userId).orderByDesc(JobApplication::getUpdateTime);
            List<JobApplication> list = jobApplicationMapper.selectList(wrapper);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total", list.size());
            for (String status : STATUSES) {
                stats.put(status, list.stream().filter(item -> status.equals(item.getStatus())).count());
            }

            response.put("success", true);
            response.put("stats", stats);
            response.put("list", list);
            return response;
        } catch (Exception e) {
            log.error("查询求职进度失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long id = request.get("id") == null ? null : Long.parseLong(String.valueOf(request.get("id")));
            String userId = text(request.get("userId"));
            if (id == null || userId.isEmpty()) {
                throw new IllegalArgumentException("缺少 id 或 userId");
            }

            JobApplication existing = jobApplicationMapper.selectById(id);
            if (existing == null || !userId.equals(existing.getUserId())) {
                throw new IllegalArgumentException("记录不存在或无权修改");
            }

            String status = text(request.get("status"));
            if (!status.isEmpty()) {
                if (!STATUSES.contains(status)) {
                    throw new IllegalArgumentException("状态不合法");
                }
                existing.setStatus(status);
            }
            if (request.containsKey("remark")) {
                existing.setRemark(text(request.get("remark")));
            }
            if (request.containsKey("interviewAt")) {
                existing.setInterviewAt(parseDate(request.get("interviewAt")));
            }
            existing.setUpdateTime(new Date());
            jobApplicationMapper.updateById(existing);

            response.put("success", true);
            response.put("data", existing);
            return response;
        } catch (Exception e) {
            log.error("更新求职进度失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id, @RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            JobApplication existing = jobApplicationMapper.selectById(id);
            if (existing == null || !text(userId).equals(existing.getUserId())) {
                throw new IllegalArgumentException("记录不存在或无权删除");
            }
            jobApplicationMapper.deleteById(id);
            response.put("success", true);
            return response;
        } catch (Exception e) {
            log.error("删除求职进度失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    private Date parseDate(Object value) {
        String raw = text(value);
        if (raw.isEmpty()) {
            return null;
        }
        // 前端传 "yyyy-MM-dd HH:mm" 或 ISO 字符串
        String normalized = raw.replace(' ', 'T');
        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        try {
            return Date.from(java.time.LocalDateTime.parse(normalized).atZone(java.time.ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            try {
                return Date.from(java.time.Instant.parse(raw));
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
