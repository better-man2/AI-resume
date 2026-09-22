package com.shanzhu.biographical.controller;

import com.shanzhu.biographical.dto.JobRecommendRequest;
import com.shanzhu.biographical.service.JobRecommendService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/job-recommend")
@CrossOrigin
public class JobRecommendController {

    @Resource
    private JobRecommendService jobRecommendService;

    @PostMapping("/recommend")
    public Map<String, Object> recommendJobs(@RequestBody JobRecommendRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> recommendations = jobRecommendService.recommendJobs(request);
            response.put("success", true);
            response.put("data", recommendations);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
} 