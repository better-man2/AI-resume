package com.shanzhu.biographical.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.shanzhu.biographical.util.ResumeJsonUtils.asList;
import static com.shanzhu.biographical.util.ResumeJsonUtils.asMap;
import static com.shanzhu.biographical.util.ResumeJsonUtils.asStringList;
import static com.shanzhu.biographical.util.ResumeJsonUtils.appendText;
import static com.shanzhu.biographical.util.ResumeJsonUtils.splitTokens;
import static com.shanzhu.biographical.util.ResumeJsonUtils.text;

/**
 * 从简历（结构化数据或粘贴的简历文本）里抽取技能、学历、经验年限等标签，供基础岗位匹配使用。
 */
@Component
public class ResumeTagExtractor {

    /** 技能词典：用于从自由文本中识别技能标签（结构化的技能标签优先，词典只做补充） */
    private static final List<String> DICTIONARY = List.of(
            // 后端 / 语言
            "Java", "Python", "Go", "C++", "C#", "PHP", "Ruby", "Scala", "Kotlin", "Rust", "Shell", "Spring",
            "Spring Boot", "Spring Cloud", "MyBatis", "MyBatis-Plus", "Hibernate", "Netty", "Dubbo", "JVM",
            "Node.js", "Express", "Django", "Flask", "FastAPI", "Gin",
            // 前端
            "Vue", "React", "Angular", "JavaScript", "TypeScript", "HTML", "CSS", "Sass", "Webpack", "Vite",
            "jQuery", "Element UI", "Element Plus", "Ant Design", "小程序", "uni-app", "Axios",
            // 数据库 / 存储
            "MySQL", "Oracle", "SQL Server", "PostgreSQL", "Redis", "MongoDB", "Elasticsearch", "HBase",
            "ClickHouse", "SQLite", "SQL", "数据库设计", "索引优化",
            // 中间件 / 运维
            "Kafka", "RabbitMQ", "RocketMQ", "Nginx", "Tomcat", "Docker", "Kubernetes", "Jenkins", "Git",
            "Maven", "Gradle", "Linux", "Prometheus", "Grafana", "Zookeeper", "CI/CD", "阿里云", "AWS",
            // 大数据 / AI
            "Hadoop", "Hive", "Spark", "Flink", "MapReduce", "ETL", "TensorFlow", "PyTorch", "机器学习",
            "深度学习", "NLP", "数据分析", "数据挖掘", "爬虫",
            // 通用能力 / 方向
            "微服务", "分布式", "高并发", "多线程", "算法", "数据结构", "操作系统", "计算机网络", "设计模式",
            "性能优化", "单元测试", "敏捷开发", "需求分析", "项目管理", "团队协作", "沟通能力",
            // 办公 / 其他岗位
            "Excel", "Word", "PPT", "Photoshop", "Axure", "Figma", "Sketch", "文案撰写", "活动策划",
            "财务报表", "会计核算", "招聘", "绩效考核", "员工关系", "薪酬管理", "劳动合同"
    );

    /**
     * 技能别名归一：把常见缩写/写法映射到统一名称，避免 "js" 对不上 "JavaScript"。
     * key 为归一化后的写法（小写、去掉空格与分隔符）。
     */
    private static final Map<String, String> SKILL_ALIAS = Map.ofEntries(
            Map.entry("js", "JavaScript"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("es6", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("typescript", "TypeScript"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("node", "Node.js"),
            Map.entry("springboot", "Spring Boot"),
            Map.entry("springcloud", "Spring Cloud"),
            Map.entry("springmvc", "Spring MVC"),
            Map.entry("mybatisplus", "MyBatis-Plus"),
            Map.entry("mariadb", "MySQL"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("pg", "PostgreSQL"),
            Map.entry("mssql", "SQL Server"),
            Map.entry("sqlserver", "SQL Server"),
            Map.entry("es", "Elasticsearch"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("mq", "消息队列"),
            Map.entry("vuejs", "Vue"),
            Map.entry("vue3", "Vue"),
            Map.entry("reactjs", "React"),
            Map.entry("wechatminiprogram", "小程序"),
            Map.entry("miniprogram", "小程序"),
            Map.entry("小程序开发", "小程序"),
            Map.entry("html5", "HTML"),
            Map.entry("css3", "CSS"),
            Map.entry("cicd", "CI/CD"),
            Map.entry("ml", "机器学习"),
            Map.entry("机器学习算法", "机器学习"),
            Map.entry("算法工程师", "算法"),
            Map.entry("数据结构与算法", "数据结构"),
            Map.entry("高并发场景", "高并发"),
            Map.entry("分布式系统", "分布式"));

    /** 判定"加分项"的关键词：命中后，该行及其之后的技能算加分技能 */
    private static final List<String> PLUS_MARKERS = List.of("加分", "优先", "有则更佳", "锦上添花");

    /** 学历从低到高，用于比较 */
    private static final List<String> DEGREE_ORDER = List.of("不限", "高中", "中专", "大专", "本科", "硕士", "博士");

    /** 匹配 "2023-06" / "2023/6" / "2023.06" / "2023年6月" / "2023" 这类时间，取到年和月 */
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\s*[-/.年]?\\s*(\\d{1,2})?");
    /** 匹配经验要求里的数字："10"、"3-5年"、"3年以上" -> 取第一个数字 */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    /**
     * 粘贴文本里的时间区间："2023-06 ~ 2024-06"、"2023.06-2024.06"、"2023-06 至 2024-06"。
     * 学历区间（后面跟着学校名称）会被排除，避免把在校时间算成工作经验。
     */
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(\\d{4}\\s*[-/.年]\\s*\\d{1,2}|\\d{4})\\s*(?:~|～|—|–|-|至|到|to)\\s*(\\d{4}\\s*[-/.年]\\s*\\d{1,2}|\\d{4})");
    private static final Pattern SCHOOL_PATTERN = Pattern.compile("^[\\s、，,。；;]*[^\\n]{0,12}(大学|学院|学校|高中|中学|职高|技校)");

    /**
     * 从结构化简历里抽取标签。
     *
     * @param resume 简历 Map（V2 数组结构或旧的对象结构均可）
     * @return skills / educationLevel / experienceYears / keywords / jobTitle / city
     */
    public Map<String, Object> extractFromResume(Map<String, Object> resume) {
        Set<String> skills = new LinkedHashSet<>();

        // 1. 结构化技能标签优先（summary 是整句概述，只参与下面的文本扫描，不当作标签）
        Map<String, Object> profession = asMap(resume.get("profession"));
        skills.addAll(asStringList(profession.get("skills")));
        skills.addAll(splitTokens(text(profession.get("skill"))));

        // 2. 自由文本兜底（项目/实习/工作描述里出现的技能）
        String freeText = collectFreeText(resume);
        skills.addAll(scanDictionary(freeText));

        String jobTitle = text(resume.get("jobTitle"));
        String educationLevel = extractEducationLevel(resume, freeText);
        double experienceYears = estimateExperienceYears(resume);
        // 居住地来自基础信息（表单录入的独立字段）
        String city = text(asMap(resume.get("basicInfo")).get("city"));

        List<String> keywords = new ArrayList<>(scanDictionary(freeText));
        List<String> titleKeywords = new ArrayList<>();
        if (!jobTitle.isEmpty()) {
            titleKeywords.add(jobTitle);
        }

        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("skills", new ArrayList<>(skills));
        tags.put("educationLevel", educationLevel);
        tags.put("experienceYears", experienceYears);
        tags.put("keywords", keywords);
        tags.put("jobTitle", jobTitle.isEmpty() ? "" : jobTitle);
        tags.put("titleKeywords", titleKeywords);
        tags.put("city", city);
        return tags;
    }

    /** 从粘贴的简历文本里抽取标签（"粘贴文本导入"用） */
    public Map<String, Object> extractFromText(String resumeText) {
        String text = resumeText == null ? "" : resumeText;
        Set<String> skills = new LinkedHashSet<>(scanDictionary(text));

        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("skills", new ArrayList<>(skills));
        tags.put("educationLevel", scanDegree(text));
        tags.put("experienceYears", extractYearsFromText(text));
        tags.put("keywords", new ArrayList<>(skills));
        tags.put("jobTitle", "");
        tags.put("titleKeywords", new ArrayList<>());
        tags.put("city", scanCity(text));
        return tags;
    }

    /** 从文本里识别现居/居住城市，例如 "现居杭州"、"居住地：杭州"、"base 上海" */
    public String scanCity(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher matcher = Pattern.compile("(?:现居|现住|居住地|所在地|base|Base)\\s*[:：]?\\s*([\\u4e00-\\u9fa5]{2,10})")
                .matcher(text);
        if (matcher.find()) {
            String city = matcher.group(1).replaceAll("(市|区|县)$", "");
            return city.length() > 4 ? city.substring(0, 4) : city;
        }
        return "";
    }

    /**
     * 解析 JD 里的所需技能：按换行/顿号/逗号切分，去掉 "1." "2、" 这类序号。
     */
    public List<String> parseRequiredSkills(String requiredSkills) {
        JobSkills skills = parseJobSkills(requiredSkills);
        List<String> all = new ArrayList<>(skills.core());
        for (String plus : skills.plus()) {
            if (!all.contains(plus)) {
                all.add(plus);
            }
        }
        return all;
    }

    /**
     * 解析 JD 技能并区分「必备」与「加分」：
     * <ul>
     *   <li>文本里出现"加分/优先"等字样后，该行起算加分技能；</li>
     *   <li>没有这种标记时，全部视为必备技能（不硬猜比例）。</li>
     * </ul>
     */
    public JobSkills parseJobSkills(String requiredSkills) {
        List<String> core = new ArrayList<>();
        List<String> plus = new ArrayList<>();
        if (requiredSkills == null || requiredSkills.trim().isEmpty()) {
            return new JobSkills(core, plus);
        }

        boolean plusSection = false;
        for (String line : requiredSkills.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (PLUS_MARKERS.stream().anyMatch(trimmed::contains)) {
                plusSection = true;
            }
            // 同一行里可能既有序号又有"加分"标注，先去掉标记词再拆
            String cleaned = trimmed;
            for (String marker : PLUS_MARKERS) {
                cleaned = cleaned.replace(marker + "项", "").replace(marker, "");
            }
            for (String marker : List.of("必备项", "必备", "必须", "硬性要求", "任职要求", "岗位要求")) {
                cleaned = cleaned.replace(marker, "");
            }
            for (String token : splitTokens(cleaned)) {
                // 去掉序号（1. / 2、）和残留的前导标点（：,，、）
                String skill = token.replaceAll("^\\s*\\d+\\s*[.、:：)）]\\s*", "")
                        .replaceAll("^[\\s:：,，、;；.\\-]+", "")
                        .trim();
                if (skill.isEmpty()) {
                    continue;
                }
                if (plusSection && !plus.contains(skill)) {
                    plus.add(skill);
                } else if (!plusSection && !core.contains(skill)) {
                    core.add(skill);
                }
            }
        }
        // 若"加分"标记在首行导致 core 为空，则把 plus 当作核心技能，避免权重错配
        if (core.isEmpty() && !plus.isEmpty()) {
            core.addAll(plus);
            plus.clear();
        }
        return new JobSkills(core, plus);
    }

    /** JD 技能分组结果 */
    public record JobSkills(List<String> core, List<String> plus) {
    }

    /**
     * 技能是否命中：先做别名归一，再比较。
     * <p>
     * 归一后相等，或一方是另一方的前缀且长度接近：Spring/Spring Boot 会命中，
     * Java/JavaScript（长度比 0.4）不会命中。
     */
    public boolean skillMatches(String userSkill, String requiredSkill) {
        String left = canonical(userSkill);
        String right = canonical(requiredSkill);
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        String shorter = left.length() <= right.length() ? left : right;
        String longer = shorter.equals(left) ? right : left;
        return shorter.length() >= 3
                && longer.startsWith(shorter)
                && shorter.length() * 10 >= longer.length() * 6;
    }

    /**
     * 归一化 + 别名映射，例如 "JS" / "js" / "JavaScript" -> "javascript"。
     * 别名值也会再归一化一次，保证大小写与分隔符口径一致（否则 "Vue" 与 "vue3" 会比不上）。
     */
    public String canonical(String skill) {
        String normalized = normalize(skill);
        String alias = SKILL_ALIAS.getOrDefault(normalized, normalized);
        return normalize(alias);
    }

    /** 学历等级序号，0 = 不限/未知 */
    public int degreeRank(String degree) {
        String value = text(degree);
        if (value.isEmpty()) {
            return 0;
        }
        for (int i = DEGREE_ORDER.size() - 1; i >= 0; i--) {
            if (value.contains(DEGREE_ORDER.get(i))) {
                return i;
            }
        }
        return 0;
    }

    /** 从"10 年"/"3-5年"/"不限"里取经验年限要求，取不到或表示不限时返回 0 */
    public double parseRequiredYears(String experienceRequirement) {
        String value = text(experienceRequirement);
        if (value.isEmpty() || value.contains("不限") || value.contains("应届") || value.contains("无")) {
            return 0;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------- 内部实现

    private String collectFreeText(Map<String, Object> resume) {
        StringBuilder buffer = new StringBuilder();
        appendText(buffer, resume.get("education"));
        appendText(buffer, resume.get("project"));
        appendText(buffer, resume.get("internship"));
        appendText(buffer, resume.get("work"));
        appendText(buffer, resume.get("award"));
        appendText(buffer, resume.get("profession"));
        return buffer.toString();
    }

    private List<String> scanDictionary(String text) {
        List<String> hits = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return hits;
        }
        for (String term : DICTIONARY) {
            if (containsTerm(text, term)) {
                hits.add(term);
            }
        }
        return hits;
    }

    /** 英文术语按前后非字母数字边界匹配，避免 "Go" 命中 "Google"；中文直接包含判断 */
    private boolean containsTerm(String text, String term) {
        if (term.codePoints().allMatch(cp -> cp < 128)) {
            String pattern = "(?i)(?<![a-z0-9+#])" + Pattern.quote(term) + "(?![a-z0-9+#])";
            return Pattern.compile(pattern).matcher(text).find();
        }
        return text.contains(term);
    }

    private String extractEducationLevel(Map<String, Object> resume, String freeText) {
        int best = 0;
        String bestLabel = "";
        for (Map<String, Object> item : asList(resume.get("education"))) {
            String degree = text(item.get("degree"));
            int rank = degreeRank(degree);
            if (rank > best) {
                best = rank;
                bestLabel = degree;
            }
        }
        if (best > 0) {
            return bestLabel;
        }
        return scanDegree(freeText);
    }

    private String scanDegree(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        for (int i = DEGREE_ORDER.size() - 1; i >= 1; i--) {
            String degree = DEGREE_ORDER.get(i);
            if (text.contains(degree)) {
                return degree;
            }
        }
        return "";
    }

    /** 经验年限：累计 实习 + 工作 的时间区间；都没有时用项目时间兜底（不做去重，按段累加） */
    private double estimateExperienceYears(Map<String, Object> resume) {
        double months = sumMonths(asList(resume.get("internship"))) + sumMonths(asList(resume.get("work")));
        if (months <= 0) {
            months = sumMonths(asList(resume.get("project")));
        }
        return Math.round(months / 12.0 * 10) / 10.0;
    }

    private double sumMonths(List<Map<String, Object>> entries) {
        double total = 0;
        for (Map<String, Object> entry : entries) {
            LocalDate start = parseDate(entryDate(entry, "start", 0));
            if (start == null) {
                continue;
            }
            LocalDate end = parseDate(entryDate(entry, "end", 1));
            LocalDate today = LocalDate.now();
            if (end == null) {
                end = today;
            }
            if (end.isAfter(today)) {
                end = today;
            }
            if (end.isAfter(start)) {
                total += (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue());
            }
        }
        return total;
    }

    /**
     * 取时间字段：优先 start/end，旧数据把时间放在 period / studyPeriod 数组里。
     *
     * @param index 0 取开始时间，1 取结束时间
     */
    private String entryDate(Map<String, Object> entry, String key, int index) {
        String value = text(entry.get(key));
        if (!value.isEmpty()) {
            return value;
        }
        for (String arrayKey : new String[]{"period", "studyPeriod"}) {
            Object raw = entry.get(arrayKey);
            if (raw instanceof List && ((List<?>) raw).size() > index) {
                String item = text(((List<?>) raw).get(index));
                if (!item.isEmpty()) {
                    return item;
                }
            }
        }
        return "";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));
            if (month < 1 || month > 12) {
                month = 1;
            }
            return LocalDate.of(year, month, 1);
        } catch (Exception e) {
            return null;
        }
    }

    /** 从文本里识别经验年限："3年经验" 优先，其次累计 "2023-06 ~ 2024-06" 这类时间区间 */
    private double extractYearsFromText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        double best = 0;
        Matcher explicit = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*年").matcher(text);
        while (explicit.find()) {
            try {
                double value = Double.parseDouble(explicit.group(1));
                if (value > best && value < 50) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的片段
            }
        }
        if (best > 0) {
            return best;
        }

        // 没有写"X年"时，按时间区间累计（排除学校区间）
        double months = 0;
        Matcher range = RANGE_PATTERN.matcher(text);
        while (range.find()) {
            String tail = text.substring(range.end(), Math.min(text.length(), range.end() + 16));
            if (SCHOOL_PATTERN.matcher(tail).find()) {
                continue;
            }
            LocalDate start = parseDate(range.group(1));
            LocalDate end = parseDate(range.group(2));
            if (start != null && end != null && end.isAfter(start)) {
                months += (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue());
            }
        }
        return Math.round(months / 12.0 * 10) / 10.0;
    }

    private String normalize(String value) {
        String normalized = text(value).toLowerCase();
        // 去掉空白与常见分隔符，让 "Spring Boot" / "springboot" / "Node.js" / "NodeJS" 能对上
        return normalized.replaceAll("[\\s._\\-·]+", "");
    }
}
