package com.shanzhu.biographical.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzhu.biographical.config.uuid;
import com.shanzhu.biographical.dto.ResumeGenerateRequest;
import com.shanzhu.biographical.model.History;
import com.shanzhu.biographical.model.User;
import com.shanzhu.biographical.service.HistoryService;
import com.shanzhu.biographical.service.ResumeService;
import com.shanzhu.biographical.service.UserService;
import com.shanzhu.biographical.util.ResumeJsonUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.shanzhu.biographical.util.ResumeJsonUtils.asList;
import static com.shanzhu.biographical.util.ResumeJsonUtils.asMap;
import static com.shanzhu.biographical.util.ResumeJsonUtils.asStringList;
import static com.shanzhu.biographical.util.ResumeJsonUtils.text;

/**
 * P0：AI 一键生成简历。
 * <p>
 * 设计要点（防止 AI 编造经历）：
 * <ol>
 *   <li>提示词硬约束：只允许基于用户填写内容整理描述，禁止新增学校/公司/时间/数字指标等事实；</li>
 *   <li>代码侧二次校验：合并时所有"事实字段"一律以用户输入为准，AI 只能提供 details/summary 这类描述文本，
 *       AI 返回缺失或解析失败时自动回退为用户原文。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/resume")
@CrossOrigin
public class ResumeGenerateController {

    private static final Logger log = LoggerFactory.getLogger(ResumeGenerateController.class);

    private static final Set<String> SUPPORTED_TEMPLATES = Set.of("classic", "modern", "campus");
    private static final String DEFAULT_TEMPLATE = "classic";

    /** 各段经历里允许 AI 改写的字段（其余字段一律取用户输入） */
    private static final String[] PROJECT_FACTS = {"name", "role", "start", "end"};
    private static final String[] INTERNSHIP_FACTS = {"company", "department", "position", "start", "end"};
    private static final String[] WORK_FACTS = {"company", "department", "position", "start", "end"};

    @Resource
    private OpenAiChatModel openAiChatModel;

    @Resource
    private ResumeService resumeService;

    @Resource
    private HistoryService historyService;

    @Resource
    private UserService userService;

    @Resource
    private uuid.UuidGenerator uuidGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody ResumeGenerateRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = request.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("用户ID不能为空");
            }

            Map<String, Object> form = request.getForm() == null ? new HashMap<>() : request.getForm();
            String template = normalizeTemplate(request.getTemplate());

            String prompt = buildPrompt(form);

            // AI 只负责描述性文字；调用失败不阻断，描述回退用户原文
            String aiRaw = "";
            Map<String, Object> aiResult = new HashMap<>();
            boolean aiRewrite = false;
            try {
                aiRaw = openAiChatModel.call(prompt);
                aiResult = parseAiJson(aiRaw);
                aiRewrite = !aiResult.isEmpty();
            } catch (Exception e) {
                log.warn("AI 改写失败，将直接使用用户填写内容：{}", e.getMessage());
            }

            Map<String, Object> content = mergeWithFacts(form, aiResult, template);
            fillContactFromUser(content, userId);
            content.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            resumeService.saveResume(userId, content);
            saveHistory(userId, request.getUsername(), prompt, aiRaw);

            response.put("success", true);
            response.put("content", content);
            response.put("template", template);
            response.put("aiRewrite", aiRewrite);
            if (!aiRewrite) {
                response.put("notice", "AI 改写未生效，已按你填写的内容原样生成，可稍后重试或直接手动编辑");
            }
            return response;
        } catch (Exception e) {
            log.error("生成简历失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    // ---------------------------------------------------------------- prompt

    private String buildPrompt(Map<String, Object> form) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是简历文本整理助手。任务：把用户填写的经历信息整理成简历中的描述性文字。\n\n");
        prompt.append("【职责边界】\n");
        prompt.append("姓名、电话、邮箱、居住地属于基础信息，由系统表单直接录入，不在你的职责范围内——");
        prompt.append("你既不要输出这几个字段，也不要去改写它们。你只负责非结构化的描述性文本。\n\n");
        prompt.append("【硬性规则，必须全部遵守】\n");
        prompt.append("1. 只能使用下面「用户填写信息」中出现过的内容，严禁新增、推测、补全任何事实（唯一例外见第 8 条）。\n");
        prompt.append("2. 严禁编造或改写：学校、专业、学历、公司、部门、职位、项目名称、起止时间、奖项、证书、薪资。\n");
        prompt.append("3. 严禁编造任何数字指标（例如“提升30%”“服务10万用户”“并发1万”）以及用户未提及的技术栈、职责与成果。\n");
        prompt.append("3.1 整理某条经历（projects/internships/works）的 details 时，只能改写用户在该条经历里写过的内容；");
        prompt.append("不得把用户的技能标签（如 Java、MySQL、Redis）自动补进这条经历里，除非用户在这条描述中已经提到。");
        prompt.append("技术栈只允许出现在 profession.summary 和第 8 条的参考项目里。\n");
        prompt.append("4. 你只做这几件事：(a) 把 projects / internships / works 里的 details 整理得更简洁、专业、有条理；");
        prompt.append("(b) 生成 profession.summary，用一句话概括用户已填写的技能；");
        prompt.append("(c) 生成 self_evaluation 自我评价，1~3 句话，只能基于用户已填写的事实（技能、经历、求职意向）；");
        prompt.append("(d) 生成 strengths 个人优势，3~4 条短句，每条一句话，只能基于用户已填写的技能、专业、经历，不得夸大或编造。\n");
        prompt.append("5. 用户没有填写的内容一律留空，不要写“未知”“待补充”等占位文字。\n");
        prompt.append("6. 数组条数必须与用户填写的条数一致且顺序对应；某条没有可整理的内容时，details 返回空字符串。\n");
        prompt.append("7. suggested_projects 的用法见第 8 条。\n\n");
        prompt.append("【输出格式】只输出 JSON，不要解释文字、不要 markdown 代码块：\n");
        prompt.append("{\n");
        prompt.append("  \"self_evaluation\": \"\",\n");
        prompt.append("  \"strengths\": [ \"\", \"\" ],\n");
        prompt.append("  \"profession\": { \"summary\": \"\" },\n");
        prompt.append("  \"projects\": [ { \"details\": \"\" } ],\n");
        prompt.append("  \"internships\": [ { \"details\": \"\" } ],\n");
        prompt.append("  \"works\": [ { \"details\": \"\" } ],\n");
        prompt.append("  \"suggested_projects\": [ { \"name\": \"\", \"role\": \"\", \"details\": \"\" } ]\n");
        prompt.append("}\n\n");
        prompt.append("8. 【参考项目经历】如果用户填写的 projects 是空数组，说明用户还没有项目经历，");
        prompt.append("此时你可以基于用户的专业、技能、求职意向，生成 1~2 条“参考项目经历”放进 suggested_projects：\n");
        prompt.append("   - 项目名称要贴合该专业/技能的常见实践项目（例如计算机专业：校园二手交易平台、图书管理系统、博客系统）；\n");
        prompt.append("   - role 写常见角色（如后端开发、全栈开发）；\n");
        prompt.append("   - details 只写该类项目通用的技术实现与职责，不得编造公司名、上线数据、用户量、性能指标；\n");
        prompt.append("   - 不要返回起止时间（时间由用户自己补）；\n");
        prompt.append("   - 这些内容会被系统标注为“AI 参考”，提示用户核实或替换为真实经历。\n");
        prompt.append("   如果用户已经填写了 project 经历，suggested_projects 必须返回空数组。\n\n");
        prompt.append("【用户填写信息】\n");
        prompt.append(toJson(aiInput(form)));
        return prompt.toString();
    }

    /** 送给大模型的输入：剔除基础信息（姓名/电话/邮箱/居住地），保证 AI 完全不接触这些字段 */
    private Map<String, Object> aiInput(Map<String, Object> form) {
        Map<String, Object> input = new LinkedHashMap<>(form);
        input.remove("basic");
        input.remove("extra");
        input.put("补充说明（可参考，不得扩写为未提及的事实）", text(form.get("extra")));
        return input;
    }

    // ------------------------------------------------------- 合并（硬校验）

    private Map<String, Object> mergeWithFacts(Map<String, Object> form, Map<String, Object> ai, String template) {
        Map<String, Object> content = new LinkedHashMap<>();
        Map<String, Object> basic = asMap(form.get("basic"));
        Map<String, Object> intent = asMap(form.get("intent"));

        // 基础信息：独立 JSON 字段，原样使用用户表单输入，AI 完全不参与
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("name", text(basic.get("name")));
        basicInfo.put("phone", text(basic.get("phone")));
        basicInfo.put("email", text(basic.get("email")));
        basicInfo.put("city", text(basic.get("city")));
        content.put("basicInfo", basicInfo);

        content.put("jobStatus", text(intent.get("jobStatus")));
        content.put("jobTitle", text(intent.get("jobTitle")));
        content.put("salaryExpectation", text(intent.get("salaryExpectation")));
        content.put("template", template);

        // 自我评价：AI 生成的描述性文本；AI 未产出时留空，由用户自己写
        content.put("selfEvaluation", aiSelfEvaluation(ai));
        // 个人优势：AI 生成的短句列表（基于用户已填技能/专业/经历）
        content.put("strengths", aiStrengths(ai));

        // 教育经历：全部字段为事实，原样使用用户输入
        List<Map<String, Object>> educations = new ArrayList<>();
        for (Map<String, Object> item : asList(form.get("educations"))) {
            Map<String, Object> edu = new LinkedHashMap<>();
            edu.put("school", text(item.get("school")));
            edu.put("major", text(item.get("major")));
            edu.put("degree", text(item.get("degree")));
            edu.put("start", text(item.get("start")));
            edu.put("end", text(item.get("end")));
            educations.add(edu);
        }
        content.put("education", educations);

        // 技能：标签原样保留，AI 只补一句概述
        List<String> skills = asStringList(form.get("skills"));
        Map<String, Object> profession = new LinkedHashMap<>();
        profession.put("skills", skills);
        profession.put("summary", aiSummary(ai));
        // 兼容旧读取方（/api/recommend 的 AI 深度分析按 skill 字段取技能）
        profession.put("skill", String.join("、", skills));
        content.put("profession", profession);

        // 项目 / 实习 / 工作经历：名称与时间恒取用户输入，只有 details 允许 AI 改写
        List<Map<String, Object>> userProjects = asList(form.get("projects"));
        if (userProjects.isEmpty()) {
            // 用户没有项目经历：允许 AI 基于专业与技能生成"参考项目"，逐条打上 AI 参考标记，时间留空由用户补
            content.put("project", suggestedProjects(ai));
        } else {
            content.put("project", mergeEntries(form.get("projects"), aiEntries(ai, "projects"), PROJECT_FACTS));
        }
        content.put("internship", mergeEntries(form.get("internships"), aiEntries(ai, "internships"), INTERNSHIP_FACTS));
        content.put("work", mergeEntries(form.get("works"), aiEntries(ai, "works"), WORK_FACTS));

        // 荣誉奖项：事实字段，原样使用
        List<Map<String, Object>> awards = new ArrayList<>();
        for (Map<String, Object> item : asList(form.get("awards"))) {
            Map<String, Object> award = new LinkedHashMap<>();
            award.put("name", text(item.get("name")));
            award.put("date", text(item.get("date")));
            awards.add(award);
        }
        content.put("award", awards);

        return content;
    }

    /**
     * 按序号合并：事实字段取用户输入，details 优先取 AI 改写结果，AI 缺失则回退用户原文。
     */
    private List<Map<String, Object>> mergeEntries(Object userEntries, List<String> aiDetails, String[] factFields) {
        List<Map<String, Object>> userList = asList(userEntries);
        List<Map<String, Object>> merged = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            Map<String, Object> source = userList.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            for (String field : factFields) {
                entry.put(field, text(source.get(field)));
            }
            String aiText = i < aiDetails.size() ? aiDetails.get(i) : "";
            entry.put("details", aiText.isEmpty() ? text(source.get("details")) : aiText);
            merged.add(entry);
        }
        return merged;
    }

    private String aiSummary(Map<String, Object> ai) {
        Map<String, Object> profession = asMap(ai.get("profession"));
        String summary = text(profession.get("summary"));
        if (summary.isEmpty()) {
            return "";
        }
        // 概述只允许是短句，避免混入编造的长段落
        return summary.length() > 80 ? summary.substring(0, 80) : summary;
    }

    /** 自我评价：AI 生成的描述文本，限制长度，避免编造成长段落 */
    private String aiSelfEvaluation(Map<String, Object> ai) {
        String evaluation = text(ai.get("self_evaluation"));
        if (evaluation.isEmpty()) {
            evaluation = text(ai.get("selfEvaluation"));
        }
        return evaluation.length() > 300 ? evaluation.substring(0, 300) : evaluation;
    }

    /** 个人优势：AI 生成的短句列表，最多 5 条，每条不超过 60 字 */
    private List<String> aiStrengths(Map<String, Object> ai) {
        List<String> strengths = new ArrayList<>();
        for (String item : asStringList(ai.get("strengths"))) {
            String value = item.length() > 60 ? item.substring(0, 60) : item;
            if (!value.isEmpty()) {
                strengths.add(value);
            }
            if (strengths.size() >= 5) {
                break;
            }
        }
        return strengths;
    }

    /**
     * 用户没有项目经历时，把 AI 生成的参考项目转成项目条目。
     * 每条都打上 aiGenerated=true（前端据此显示"AI 参考"标记），起止时间一律留空，由用户自己核实与补全。
     */
    private List<Map<String, Object>> suggestedProjects(Map<String, Object> ai) {
        List<Map<String, Object>> projects = new ArrayList<>();
        for (Map<String, Object> item : asList(ai.get("suggested_projects"))) {
            String name = text(item.get("name"));
            String details = text(item.get("details"));
            if (name.isEmpty() && details.isEmpty()) {
                continue;
            }
            Map<String, Object> project = new LinkedHashMap<>();
            project.put("name", name);
            project.put("role", text(item.get("role")));
            project.put("start", "");
            project.put("end", "");
            project.put("details", details);
            project.put("aiGenerated", true);
            projects.add(project);
            if (projects.size() >= 2) {
                break;
            }
        }
        return projects;
    }

    private List<String> aiEntries(Map<String, Object> ai, String key) {
        List<String> details = new ArrayList<>();
        for (Map<String, Object> item : asList(ai.get(key))) {
            details.add(text(item.get("details")));
        }
        return details;
    }

    /** 用户没填姓名/电话/邮箱时，用注册资料补齐（仍属表单侧数据，不经过大模型） */
    private void fillContactFromUser(Map<String, Object> content, String userId) {
        try {
            User user = userService.getUser(userId);
            if (user == null) {
                return;
            }
            Map<String, Object> basic = asMap(content.get("basicInfo"));
            if (text(basic.get("name")).isEmpty()) {
                basic.put("name", text(user.getUsername()));
            }
            if (text(basic.get("phone")).isEmpty()) {
                basic.put("phone", text(user.getPhone()));
            }
            if (text(basic.get("email")).isEmpty()) {
                basic.put("email", text(user.getEmail()));
            }
            content.put("basicInfo", basic);
        } catch (Exception e) {
            log.warn("补齐用户联系方式失败：{}", e.getMessage());
        }
    }

    private void saveHistory(String userId, String username, String prompt, String aiRaw) {
        try {
            History history = new History();
            history.setId(uuidGenerator.generateUuid32());
            history.setQuestion(prompt);
            history.setResult(aiRaw);
            history.setUserId(userId);
            history.setUsername(username);
            history.setTime(new Date());
            historyService.saveHistory(history);
        } catch (Exception e) {
            log.warn("保存生成历史失败：{}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- utils

    private String normalizeTemplate(String template) {
        if (template == null || !SUPPORTED_TEMPLATES.contains(template)) {
            return DEFAULT_TEMPLATE;
        }
        return template;
    }

    private Map<String, Object> parseAiJson(String aiRaw) {
        // 统一清洗掉裸换行等控制字符，避免模型输出的 JSON 解析失败
        String cleaned = ResumeJsonUtils.sanitizeJsonText(aiRaw);
        if (cleaned.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.warn("AI 返回的 JSON 解析失败，将使用用户原文：{}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
