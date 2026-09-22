package com.shanzhu.biographical.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzhu.biographical.config.uuid;
import com.shanzhu.biographical.model.History;
import com.shanzhu.biographical.model.Resume;
import com.shanzhu.biographical.model.User;
import com.shanzhu.biographical.service.HistoryService;
import com.shanzhu.biographical.service.ResumeService;
import com.shanzhu.biographical.service.UserService;
import com.shanzhu.biographical.util.ResumeConverter;
import jakarta.annotation.Resource;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class ChatController {

    @Resource
    private OpenAiChatModel openAiChatModel;
    @Resource
    private HistoryService historyService;
    @Resource
    private UserService userService;
    @Resource
    private ResumeService resumeService;
    @Resource
    private uuid.UuidGenerator uuidGenerator;
    @Resource
    private ResumeConverter resumeConverter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String username = (String) request.get("username");
            String resumeVersion = (String) request.getOrDefault("resumeVersion", "");
            Map<String, Object> formData = (Map<String, Object>) request.getOrDefault("formData", new HashMap<>());

            String prompt = buildResumePrompt(resumeVersion, formData);
            String aiResponse = openAiChatModel.call(prompt);
            Map<String, Object> structuredResponse = parseAiResponse(aiResponse);

            User user = userService.getUser(userId);
            if (!structuredResponse.containsKey("error")) {
                structuredResponse.put("name", user.getUsername());
                structuredResponse.put("phone", user.getPhone());
                structuredResponse.put("email", user.getEmail());
                
                // 保存简历到数据库
                resumeService.saveResume(userId, structuredResponse);
            }

            // 保存历史记录
            History history = new History();
            history.setId(uuidGenerator.generateUuid32());
            history.setQuestion(prompt);
            history.setResult(aiResponse);
            history.setUserId(userId);
            history.setUsername(username);
            history.setTime(new Date());
            historyService.saveHistory(history);

            // 返回结构化的响应
            Map<String, Object> response = new HashMap<>();
            response.put("content", structuredResponse);
            response.put("success", true);
            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping("/resume/latest")
    public Map<String, Object> getLatestResume(@RequestParam String userId) {
        try {
            Resume resume = resumeService.getLatestResume(userId);
            if (resume == null) {
                throw new RuntimeException("No resume found");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", convertResumeToMap(resume));
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    @PostMapping("/resume/update")
    public Map<String, Object> updateResume(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Map<String, Object> resumeData = (Map<String, Object>) request.get("resumeData");
            
            Resume resume = resumeService.updateResume(userId, resumeData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", convertResumeToMap(resume));
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    private Map<String, Object> convertResumeToMap(Resume resume) {
        return resumeConverter.toMap(resume);
    }

    private String buildResumePrompt(String resumeVersion, Map<String, Object> formData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成一份结构化的简历信息，严格按照以下JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"jobStatus\": \"在职/离职/应届生\",\n");
        prompt.append("  \"jobTitle\": \"期望职位\",\n");
        prompt.append("  \"salaryExpectation\": \"期望薪资\",\n");
        prompt.append("  \"education\": {\n");
        prompt.append("    \"school\": \"学校名称\",\n");
        prompt.append("    \"major\": \"专业名称\",\n");
        prompt.append("    \"degree\": \"学历\"\n");
        prompt.append("  },\n");
        prompt.append("  \"profession\": {\n");
        prompt.append("    \"skill\": \"技能描述\"\n");
        prompt.append("  },\n");
        prompt.append("  \"work\": {\n");
        prompt.append("    \"company\": \"公司名称\",\n");
        prompt.append("    \"department\": \"部门名称\",\n");
        prompt.append("    \"position\": \"职位名称\",\n");
        prompt.append("    \"details\": \"工作内容描述\"\n");
        prompt.append("  },\n");
        prompt.append("  \"project\": {\n");
        prompt.append("    \"name\": \"项目名称\",\n");
        prompt.append("    \"details\": \"项目描述\"\n");
        prompt.append("  },\n");
        prompt.append("  \"award\": {\n");
        prompt.append("    \"details\": \"获奖情况\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");

        if ("应届生版".equals(resumeVersion)) {
            prompt.append("基于以下信息生成应届生简历：\n");
            prompt.append("专业：").append(formData.get("major")).append("\n");
            prompt.append("期望职位：").append(formData.get("position")).append("\n");
            prompt.append("补充信息：").append(formData.get("extra")).append("\n");
        } else {
            prompt.append("基于以下信息生成标准简历：\n");
            prompt.append("工作经历：").append(formData.get("experience")).append("\n");
            prompt.append("期望职位：").append(formData.get("position")).append("\n");
            prompt.append("补充信息：").append(formData.get("extra")).append("\n");
        }

        return prompt.toString();
    }

    private Map<String, Object> parseAiResponse(String aiResponse) {
        try {
            // 清理和预处理 AI 响应
            String cleanedResponse = cleanAiResponse(aiResponse);

            // 使用 ObjectMapper 解析 JSON
            Map<String, Object> parsedData = objectMapper.readValue(cleanedResponse, Map.class);

            // 验证必要的字段
            validateResumeData(parsedData);

            return parsedData;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "AI响应解析失败: " + e.getMessage());
            return fallback;
        }
    }

    private String cleanAiResponse(String aiResponse) {
        // 移除可能的前缀和后缀文本
        String cleaned = aiResponse.trim();

        // 查找第一个 { 和最后一个 } 的位置
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private void validateResumeData(Map<String, Object> data) {
        // 检查必要字段是否存在
        String[] requiredFields = {"jobStatus", "jobTitle", "salaryExpectation", "education", "profession", "work", "project", "award"};

        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                throw new IllegalArgumentException("缺少必要字段: " + field);
            }
        }

        // 验证嵌套对象
        validateNestedObject(data, "education", new String[]{"school", "major", "degree"});
        validateNestedObject(data, "profession", new String[]{"skill"});
        validateNestedObject(data, "work", new String[]{"company", "department", "position", "details"});
        validateNestedObject(data, "project", new String[]{"name", "details"});
        validateNestedObject(data, "award", new String[]{"details"});
    }

    private void validateNestedObject(Map<String, Object> data, String objectKey, String[] requiredFields) {
        Object obj = data.get(objectKey);
        if (!(obj instanceof Map)) {
            throw new IllegalArgumentException(objectKey + " 必须是一个对象");
        }

        Map<String, Object> nestedObj = (Map<String, Object>) obj;
        for (String field : requiredFields) {
            if (!nestedObj.containsKey(field)) {
                throw new IllegalArgumentException(objectKey + " 缺少必要字段: " + field);
            }
        }
    }
}
