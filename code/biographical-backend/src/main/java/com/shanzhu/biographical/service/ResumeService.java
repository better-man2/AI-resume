package com.shanzhu.biographical.service;

import com.shanzhu.biographical.model.Resume;

import java.util.Map;

public interface ResumeService {
    Resume saveResume(String userId, Map<String, Object> resumeData);
    Resume getLatestResume(String userId);
    Resume updateResume(String userId, Map<String, Object> resumeData);
} 