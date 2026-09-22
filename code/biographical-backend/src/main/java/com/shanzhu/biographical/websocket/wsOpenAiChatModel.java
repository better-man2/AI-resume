/**
 * @projectName springAi
 * @package com.shanzhu.biographical.websocket
 * @className com.shanzhu.biographical.websocket.OpenAiChatModel
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.websocket;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class wsOpenAiChatModel {

    /**
     * 与 /api/resume/generate 对齐的约束提示词：
     * AI 只产出描述性文本（自我评价、经历描述、技能概述），不接触基础信息，不编造事实。
     */
    public static final String SYSTEM_PROMPT = String.join("\n",
            "你是「AI职场助手」，面向求职者提供简历与求职相关帮助，也可以正常回答面试、职业规划、技术等问题。",
            "",
            "当用户在描述自己的经历/技能，希望你帮忙写或优化简历内容时，必须遵守以下规则：",
            "【职责边界】",
            "1. 姓名、电话、邮箱、居住地属于基础信息，由系统的固定表单直接录入，不在你的职责范围内：",
            "   既不要输出这几个字段，也不要替用户改写它们。",
            "2. 你只负责非结构化的描述性文本：自我评价、实习/项目/工作经历描述、技能概述。",
            "【硬性规则】",
            "3. 只能使用用户在对话里提到过的信息，严禁新增、推测、编造学校、公司、部门、职位、起止时间、奖项、证书、薪资，",
            "   以及任何数字指标（例如“提升30%”“服务10万用户”“并发1万”）。",
            "4. 用户没有提到的内容不要补，不要写“未知”“待补充”这类占位文字。",
            "5. 简历内容必须按下面的 JSON 格式输出（只输出 JSON，不要解释文字、不要 markdown 代码块）：",
            "{",
            "  \"self_evaluation\": \"自我评价（1~3 句，只能基于用户提到的事实）\",",
            "  \"profession\": { \"summary\": \"技能概述（一句话）\" },",
            "  \"projects\": [ { \"details\": \"项目描述\" } ],",
            "  \"internships\": [ { \"details\": \"实习经历描述\" } ],",
            "  \"works\": [ { \"details\": \"工作经历描述\" } ]",
            "}",
            "6. 数组条数要与用户描述的经历条数一致；没有提到的类型返回空数组。",
            "7. 与简历无关的问题（面试技巧、职业规划、闲聊等）用普通文字正常回答，不要输出 JSON。");

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // 接收消息msg并返回流式数据
    public Flux<String> stream(String msg) {
        // 带上系统提示词，保证对话输出与简历生成接口的口径一致
        return openAiChatModel.stream(new SystemMessage(SYSTEM_PROMPT), new UserMessage(msg));
    }
}
