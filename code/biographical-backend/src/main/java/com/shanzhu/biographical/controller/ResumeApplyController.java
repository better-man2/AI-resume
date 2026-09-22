package com.shanzhu.biographical.controller;

import com.shanzhu.biographical.model.Resume;
import com.shanzhu.biographical.model.User;
import com.shanzhu.biographical.service.ResumeService;
import com.shanzhu.biographical.service.UserService;
import com.shanzhu.biographical.util.ResumeConverter;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把首页 AI 对话产出的描述性文本安全地写回简历。
 * <p>
 * 规则（避免对话内容破坏已录入的结构化信息）：
 * <ol>
 *   <li>只写"描述类"字段：自我评价、技能概述、经历描述；</li>
 *   <li>不碰基础信息（姓名/电话/邮箱/居住地）与学校、公司、时间、奖项等事实字段；</li>
 *   <li>已有描述不会被覆盖，只填空缺的描述；没有对应经历时新增一条"只有描述"的条目，由用户去编辑页补名称与时间。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/resume")
@CrossOrigin
public class ResumeApplyController {

    private static final Logger log = LoggerFactory.getLogger(ResumeApplyController.class);

    /** 描述类字段与 AI 输出数组的对应关系 */
    private static final String[][] DETAIL_FIELDS = {
            {"project", "projects", "项目经历"},
            {"internship", "internships", "实习经历"},
            {"work", "works", "工作经历"},
    };

    @Resource
    private ResumeService resumeService;

    @Resource
    private UserService userService;

    @Resource
    private ResumeConverter resumeConverter;

    @PostMapping("/apply")
    public Map<String, Object> apply(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = text(request.get("userId"));
            if (userId.isEmpty()) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            Map<String, Object> aiContent = asMap(request.get("content"));
            if (aiContent.isEmpty()) {
                throw new IllegalArgumentException("没有可写入的内容");
            }

            Resume latest = resumeService.getLatestResume(userId);
            Map<String, Object> current = latest == null
                    ? emptyResumeMap(userId)
                    : new LinkedHashMap<>(resumeConverter.toMap(latest));
            if (current.get("basicInfo") == null) {
                current.put("basicInfo", emptyBasicInfo(userId));
            }

            Map<String, Object> applied = new LinkedHashMap<>();
            List<String> messages = new ArrayList<>();

            // 1. 自我评价（描述类，允许覆盖，覆盖时在提示里说明）
            String evaluation = text(aiContent.get("self_evaluation"));
            if (evaluation.isEmpty()) {
                evaluation = text(aiContent.get("selfEvaluation"));
            }
            if (!evaluation.isEmpty()) {
                boolean overwritten = !text(current.get("selfEvaluation")).isEmpty();
                current.put("selfEvaluation", evaluation);
                applied.put("selfEvaluation", true);
                messages.add(overwritten ? "已更新自我评价（覆盖了原有内容）" : "已写入自我评价");
            }

            // 2. 技能概述
            String summary = text(asMap(aiContent.get("profession")).get("summary"));
            if (!summary.isEmpty()) {
                Map<String, Object> profession = asMap(current.get("profession"));
                profession.put("summary", summary);
                current.put("profession", profession);
                applied.put("professionSummary", true);
                messages.add("已写入技能概述");
            }

            // 3. 各段经历描述：只填空缺，不覆盖已有描述
            for (String[] mapping : DETAIL_FIELDS) {
                Map<String, Integer> counters = mergeDetails(current, mapping[0], aiContent, mapping[1]);
                if (counters.get("filled") > 0) {
                    messages.add("为 " + counters.get("filled") + " 条已有" + mapping[2] + "补充了描述");
                }
                if (counters.get("added") > 0) {
                    messages.add("新增了 " + counters.get("added") + " 条" + mapping[2] + "描述，请到编辑页补全名称和时间");
                }
                if (counters.get("filled") > 0 || counters.get("added") > 0) {
                    applied.put(mapping[0] + "Filled", counters.get("filled"));
                    applied.put(mapping[0] + "Added", counters.get("added"));
                }
            }

            if (applied.isEmpty()) {
                response.put("success", false);
                response.put("error", "AI 回复里没有可写入简历的描述内容");
                return response;
            }

            Resume saved = resumeService.updateResume(userId, current);
            response.put("success", true);
            response.put("applied", applied);
            response.put("message", String.join("；", messages));
            response.put("content", resumeConverter.toMap(saved));
            return response;
        } catch (Exception e) {
            log.error("写入简历失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    /** 按顺序把 AI 描述并入经历数组：已有描述不动，描述为空则补上，条数不够就新增 */
    private Map<String, Integer> mergeDetails(Map<String, Object> current, String currentKey,
                                              Map<String, Object> aiContent, String aiKey) {
        int filled = 0;
        int added = 0;
        List<Map<String, Object>> entries = new ArrayList<>(asList(current.get(currentKey)));
        List<Map<String, Object>> aiEntries = asList(aiContent.get(aiKey));

        for (int i = 0; i < aiEntries.size(); i++) {
            String details = text(aiEntries.get(i).get("details"));
            if (details.isEmpty()) {
                continue;
            }
            if (i < entries.size()) {
                Map<String, Object> entry = new LinkedHashMap<>(entries.get(i));
                if (text(entry.get("details")).isEmpty()) {
                    entry.put("details", details);
                    entries.set(i, entry);
                    filled++;
                }
            } else {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("details", details);
                entries.add(entry);
                added++;
            }
        }

        if (filled > 0 || added > 0) {
            current.put(currentKey, entries);
        }

        Map<String, Integer> counters = new LinkedHashMap<>();
        counters.put("filled", filled);
        counters.put("added", added);
        return counters;
    }

    private Map<String, Object> emptyResumeMap(String userId) {
        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("basicInfo", emptyBasicInfo(userId));
        resume.put("profession", new LinkedHashMap<String, Object>());
        return resume;
    }

    /** 基础信息只能来自注册资料，不来自 AI */
    private Map<String, Object> emptyBasicInfo(String userId) {
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("name", "");
        basic.put("phone", "");
        basic.put("email", "");
        basic.put("city", "");
        try {
            User user = userService.getUser(userId);
            if (user != null) {
                basic.put("name", text(user.getUsername()));
                basic.put("phone", text(user.getPhone()));
                basic.put("email", text(user.getEmail()));
            }
        } catch (Exception e) {
            log.warn("读取用户资料失败：{}", e.getMessage());
        }
        return basic;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                if (item instanceof Map) {
                    list.add((Map<String, Object>) item);
                }
            }
        } else if (value instanceof Map) {
            list.add((Map<String, Object>) value);
        }
        return list;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
