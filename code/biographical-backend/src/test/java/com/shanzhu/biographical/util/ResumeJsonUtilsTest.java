package com.shanzhu.biographical.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 大模型返回 JSON 的清洗逻辑（流式拼接产生的换行会拆断 key，导致解析失败）。
 */
class ResumeJsonUtilsTest {

    @Test
    void 应剥掉JSON前后的解释文字() {
        String raw = "好的，结果如下：\n```json\n{\"self_evaluation\":\"有实习经历\"}\n```\n以上。";

        String cleaned = ResumeJsonUtils.sanitizeJsonText(raw);

        assertEquals("{\"self_evaluation\":\"有实习经历\"}", cleaned);
    }

    @Test
    void 流式分片带进来的换行不应拆断key() {
        // 模拟 "data:" 帧尾分隔符混进文本：\"self\n\n_evaluation\"
        String raw = "{\"self\n\n_evaluation\": \"计算机专业\n本科\",\n\"profession\": {\"summary\": \"熟悉Java\"}}";

        String cleaned = ResumeJsonUtils.sanitizeJsonText(raw);

        assertTrue(cleaned.contains("\"self  _evaluation\""), "换行应被替换为空格: " + cleaned);
        assertTrue(cleaned.contains("计算机专业 本科"), "字符串内的换行应变为空格: " + cleaned);
        assertTrue(!cleaned.contains("\n"), "清洗后不应再有裸换行");
    }

    @Test
    void 没有JSON结构时返回空串() {
        assertEquals("", ResumeJsonUtils.sanitizeJsonText("面试时建议先介绍自己的学校和项目"));
        assertEquals("", ResumeJsonUtils.sanitizeJsonText(null));
    }

    @Test
    void 转义字符应原样保留() {
        String raw = "{\"details\":\"使用 \\\"Redis\\\" 做缓存\"}";

        String cleaned = ResumeJsonUtils.sanitizeJsonText(raw);

        assertEquals(raw, cleaned);
    }
}
