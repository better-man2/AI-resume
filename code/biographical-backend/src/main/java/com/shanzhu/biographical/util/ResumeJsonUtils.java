package com.shanzhu.biographical.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历 JSON（Map/List 混合结构）读取工具。
 * <p>
 * 历史数据里 education/work/project 存的是单个对象，V2 结构存的是数组，这里统一兼容。
 */
public final class ResumeJsonUtils {

    private ResumeJsonUtils() {
    }

    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    /** 统一成"条目列表"：数组逐项取，单对象当成一条，字符串包装成 {details: ...} */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> asList(Object value) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                if (item instanceof Map) {
                    list.add((Map<String, Object>) item);
                } else if (item != null && !text(item).isEmpty()) {
                    Map<String, Object> wrapper = new HashMap<>();
                    wrapper.put("details", text(item));
                    list.add(wrapper);
                }
            }
        } else if (value instanceof Map) {
            list.add((Map<String, Object>) value);
        }
        return list;
    }

    /** 字符串列表：支持数组，也支持 "Java、MySQL;Redis" 这类分隔文本 */
    public static List<String> asStringList(Object value) {
        List<String> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                String itemText = text(item);
                if (!itemText.isEmpty()) {
                    list.add(itemText);
                }
            }
        } else {
            list.addAll(splitTokens(text(value)));
        }
        return list;
    }

    /** 按常见分隔符切分（换行、中英文逗号/分号/顿号、斜杠） */
    public static List<String> splitTokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String part : text.split("[\\n,;，；、/|]")) {
            String token = part.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** 把一段自由文本的所有字符串值拼起来，便于做关键词扫描 */
    public static void appendText(StringBuilder buffer, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Map) {
            for (Object item : ((Map<?, ?>) value).values()) {
                appendText(buffer, item);
            }
        } else if (value instanceof List) {
            for (Object item : (List<?>) value) {
                appendText(buffer, item);
            }
        } else {
            buffer.append(text(value)).append('\n');
        }
    }

    /**
     * 把大模型返回的文本清洗成可解析的 JSON 字符串。
     * <p>
     * 模型的 JSON 里可能出现裸换行/制表符（JSON 字符串字面量不允许），流式输出拼接时也会夹带分隔符，
     * 这里统一把控制字符替换成空格：字符串内的换行变成空格，结构位置上的空白本来就不影响解析。
     *
     * @return 清理后的 JSON 文本，找不到 { } 时返回空串
     */
    public static String sanitizeJsonText(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }

        String body = trimmed.substring(start, end + 1);
        StringBuilder cleaned = new StringBuilder(body.length());
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char current = body.charAt(i);
            if (escaped) {
                cleaned.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                cleaned.append(current);
                escaped = true;
                continue;
            }
            if (current == '\n' || current == '\r' || current == '\t') {
                cleaned.append(' ');
                continue;
            }
            cleaned.append(current);
        }
        return cleaned.toString();
    }
}
