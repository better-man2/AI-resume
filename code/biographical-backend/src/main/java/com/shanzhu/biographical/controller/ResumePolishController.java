package com.shanzhu.biographical.controller;

import jakarta.annotation.Resource;import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 简历片段 AI 润色（编辑页里对单个模块/单条描述点「AI 润色」）。
 * <p>
 * 只做措辞优化：更专业、更简洁、有条理；不得新增任何事实与数字指标，
 * 也不能改掉原有的技术栈、职责与成果。
 */
@RestController
@RequestMapping("/api/resume")
@CrossOrigin
public class ResumePolishController {

    private static final Logger log = LoggerFactory.getLogger(ResumePolishController.class);

    /** 支持的润色场景 */
    private static final Map<String, String> KINDS = Map.of(
            "description", "简历中的经历描述（项目/实习/工作内容）",
            "evaluation", "简历中的自我评价",
            "strength", "简历中的个人优势（单条）",
            "summary", "简历中的技能概述（一句话）");
    private static final Set<String> SUPPORTED = KINDS.keySet();
    private static final int MAX_INPUT_LENGTH = 1000;

    @Resource
    private OpenAiChatModel openAiChatModel;

    @PostMapping("/polish")
    public Map<String, Object> polish(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String original = text(request.get("text"));
            if (original.isEmpty()) {
                throw new IllegalArgumentException("没有需要润色的内容");
            }
            if (original.length() > MAX_INPUT_LENGTH) {
                throw new IllegalArgumentException("内容过长，请分段润色（单段不超过 " + MAX_INPUT_LENGTH + " 字）");
            }
            String kind = text(request.get("kind"));
            if (!SUPPORTED.contains(kind)) {
                kind = "description";
            }

            String polished = openAiChatModel.call(buildPrompt(original, kind));
            polished = cleanOutput(polished);

            if (polished.isEmpty()) {
                throw new IllegalStateException("AI 没有返回润色结果，请稍后重试");
            }

            response.put("success", true);
            response.put("original", original);
            response.put("polished", polished);
            return response;
        } catch (Exception e) {
            log.error("润色失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    private String buildPrompt(String original, String kind) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是简历文字润色助手。请对下面这段文字做**措辞润色**，不要改变事实。\n\n");
        prompt.append("【硬性规则】\n");
        prompt.append("1. 只优化表达：更专业、更简洁、更有条理，动词开头，去掉口语化与冗余表述。\n");
        prompt.append("2. 严禁新增任何事实：不得添加原文没有的学校、公司、时间、技术栈、职责、成果。\n");
        prompt.append("3. 严禁编造数字指标（如“提升30%”“服务10万用户”）；原文没有数字就不要加数字。\n");
        prompt.append("4. 严禁拔高或替换原意：不要把“参与/做了”改成“主导/负责核心”，不要把关/了解改成精通；");
        prompt.append("   原文提到的品质与要点（如能吃苦、性格开朗）必须保留，原文没说的品质（如学习能力强、抗压能力好）不要添加。\n");
        prompt.append("5. 保持原意，长度与原文相当（不超过原文长度的 1.3 倍）。\n");
        prompt.append("6. 直接输出润色后的文字，不要解释、不要引号、不要 markdown 标记，不要输出 JSON。\n\n");
        prompt.append("【场景】").append(KINDS.get(kind)).append("\n\n");
        prompt.append("【原文】\n").append(original);
        return prompt.toString();
    }

    /** 去掉模型可能加的引号/代码块/前缀说明 */
    private String cleanOutput(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        cleaned = cleaned.replaceAll("^(润色后|润色结果|修改后)[:：]\\s*", "");
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
