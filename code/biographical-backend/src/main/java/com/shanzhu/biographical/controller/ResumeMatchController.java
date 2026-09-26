package com.shanzhu.biographical.controller;

import com.shanzhu.biographical.dto.JobMatchRequest;
import com.shanzhu.biographical.mapper.JobCategoryMapper;
import com.shanzhu.biographical.mapper.JobPositionMapper;
import com.shanzhu.biographical.model.JobCategory;
import com.shanzhu.biographical.model.JobPosition;
import com.shanzhu.biographical.model.Resume;
import com.shanzhu.biographical.service.ResumeService;
import com.shanzhu.biographical.util.ResumeConverter;
import com.shanzhu.biographical.util.ResumeTagExtractor;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * P0：基础岗位匹配。
 * <p>
 * 确定性规则（不调用大模型）：从简历/粘贴文本抽取技能、学历、经验等标签，与岗位 JD 的关键词做匹配，
 * 输出匹配分数 + 命中/缺失标签 + 匹配与不匹配原因。
 * 深度智能推荐（AI 推荐理由、职业建议）见 {@link RecommendController}，属于 P2。
 */
@RestController
@RequestMapping("/api/match")
@CrossOrigin
public class ResumeMatchController {

    private static final Logger log = LoggerFactory.getLogger(ResumeMatchController.class);

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @Resource
    private JobPositionMapper jobPositionMapper;

    @Resource
    private JobCategoryMapper jobCategoryMapper;

    @Resource
    private ResumeService resumeService;

    @Resource
    private ResumeConverter resumeConverter;

    @Resource
    private ResumeTagExtractor tagExtractor;

    @PostMapping("/jobs")
    public ResponseEntity<?> matchJobs(@RequestBody JobMatchRequest request) {
        try {
            String resumeText = text(request.getResumeText());
            boolean fromText = !resumeText.isEmpty();

            Map<String, Object> tags;
            if (fromText) {
                // "粘贴文本导入"
                tags = tagExtractor.extractFromText(resumeText);
            } else {
                String userId = text(request.getUserId());
                if (userId.isEmpty()) {
                    return ResponseEntity.badRequest().body(error("请提供 userId，或粘贴简历文本后再匹配"));
                }
                Resume resume = resumeService.getLatestResume(userId);
                if (resume == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(error("未找到你的简历，请先 AI 生成或保存一份简历"));
                }
                tags = tagExtractor.extractFromResume(resumeConverter.toMap(resume));
            }

            List<JobPosition> positions = enabledPositions();
            if (positions.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "岗位库暂无在招岗位，请先在职位库管理中添加岗位"));
            }

            List<String> skills = toStringList(tags.get("skills"));
            List<String> keywords = toStringList(tags.get("keywords"));
            String jobTitle = text(tags.get("jobTitle"));
            String educationLevel = text(tags.get("educationLevel"));
            double experienceYears = toDouble(tags.get("experienceYears"));
            String city = text(tags.get("city"));
            int userRank = tagExtractor.degreeRank(educationLevel);
            Map<Integer, String> categoryNames = categoryNameMap();

            List<Map<String, Object>> results = new ArrayList<>();
            for (JobPosition job : positions) {
                results.add(scoreJob(job, skills, keywords, jobTitle, educationLevel, userRank,
                        experienceYears, city, categoryNames));
            }
            results.sort(Comparator
                    .comparingInt((Map<String, Object> item) -> (Integer) item.get("matchScore")).reversed()
                    .thenComparing(item -> ((List<?>) item.get("matchedTags")).size(), Comparator.reverseOrder()));

            int limit = request.getLimit() == null || request.getLimit() <= 0
                    ? DEFAULT_LIMIT : Math.min(request.getLimit(), MAX_LIMIT);
            List<Map<String, Object>> top = results.size() > limit
                    ? new ArrayList<>(results.subList(0, limit)) : results;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("source", fromText ? "text" : "resume");
            response.put("tags", tags);
            response.put("summary", buildSummary(results, top));
            response.put("jobs", top);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("岗位匹配失败", e);
            return ResponseEntity.badRequest().body(error("匹配失败：" + e.getMessage()));
        }
    }

    // ------------------------------------------------------------ 单个岗位打分

    private Map<String, Object> scoreJob(JobPosition job, List<String> skills, List<String> keywords,
                                         String jobTitle, String educationLevel, int userRank,
                                         double experienceYears, String city,
                                         Map<Integer, String> categoryNames) {
        // 1. 技能命中（50 分）：核心技能 40 + 加分技能 10
        List<String> matchReasons = new ArrayList<>();
        List<String> mismatchReasons = new ArrayList<>();

        ResumeTagExtractor.JobSkills jobSkills = tagExtractor.parseJobSkills(job.getRequiredSkills());
        List<String> coreSkills = jobSkills.core();
        List<String> plusSkills = jobSkills.plus();

        List<String> coreMatched = matchSkills(coreSkills, skills);
        List<String> coreMissing = subtract(coreSkills, coreMatched);
        List<String> plusMatched = matchSkills(plusSkills, skills);
        List<String> plusMissing = subtract(plusSkills, plusMatched);

        List<String> matched = new ArrayList<>(coreMatched);
        plusMatched.forEach(skill -> {
            if (!matched.contains(skill)) {
                matched.add(skill);
            }
        });
        List<String> missing = new ArrayList<>(coreMissing);
        plusMissing.forEach(skill -> {
            if (!missing.contains(skill)) {
                missing.add(skill);
            }
        });

        double skillScore;
        if (coreSkills.isEmpty() && plusSkills.isEmpty()) {
            skillScore = 25;
            matchReasons.add("岗位未明确技能要求，按学历与经验综合匹配");
        } else {
            double coreWeight = coreSkills.isEmpty() ? 0 : (plusSkills.isEmpty() ? 50 : 40);
            double plusWeight = coreSkills.isEmpty() ? 50 : 10;
            skillScore = coreWeight * ratio(coreMatched.size(), coreSkills.size())
                    + plusWeight * ratio(plusMatched.size(), plusSkills.size());
        }

        if (!coreSkills.isEmpty() && !coreMatched.isEmpty()) {
            matchReasons.add("命中核心技能 " + coreMatched.size() + "/" + coreSkills.size() + "："
                    + String.join("、", coreMatched));
        }
        if (!plusSkills.isEmpty() && !plusMatched.isEmpty()) {
            matchReasons.add("命中加分技能 " + plusMatched.size() + "/" + plusSkills.size() + "："
                    + String.join("、", plusMatched));
        }
        if (!coreSkills.isEmpty() && coreMatched.isEmpty()) {
            mismatchReasons.add("岗位核心技能（" + String.join("、", coreSkills) + "）与简历技能无重合");
        }
        if (!coreMissing.isEmpty()) {
            mismatchReasons.add("缺少核心技能：" + String.join("、", coreMissing));
        }
        if (!plusMissing.isEmpty()) {
            mismatchReasons.add("缺少加分技能：" + String.join("、", plusMissing));
        }

        // 2. 学历（15 分）
        String jobEducation = text(job.getEducationRequirement());
        int jobRank = tagExtractor.degreeRank(jobEducation);
        double educationScore;
        if (jobRank == 0) {
            educationScore = 15;
            matchReasons.add("学历要求不限");
        } else if (userRank >= jobRank) {
            educationScore = 15;
            matchReasons.add("学历满足：" + educationLevel + "（要求 " + jobEducation + "）");
        } else if (userRank == 0) {
            educationScore = 7;
            mismatchReasons.add("岗位要求 " + jobEducation + "，简历中未识别到学历信息");
        } else {
            educationScore = 0;
            mismatchReasons.add("学历要求 " + jobEducation + "，简历为 " + educationLevel);
        }

        // 3. 经验年限（15 分）
        double requiredYears = tagExtractor.parseRequiredYears(job.getExperienceRequirement());
        double experienceScore;
        if (requiredYears <= 0) {
            experienceScore = 15;
            matchReasons.add("经验要求不限");
        } else if (experienceYears >= requiredYears) {
            experienceScore = 15;
            matchReasons.add("经验满足：约 " + formatYears(experienceYears) + " 年（要求 "
                    + formatYears(requiredYears) + " 年）");
        } else if (experienceYears > 0) {
            experienceScore = Math.round(15 * experienceYears / requiredYears * 10) / 10.0;
            mismatchReasons.add("经验要求 " + formatYears(requiredYears) + " 年，简历估测 "
                    + formatYears(experienceYears) + " 年");
        } else {
            experienceScore = 7;
            mismatchReasons.add("经验要求 " + formatYears(requiredYears) + " 年，简历未识别到时间信息");
        }

        // 4. 居住地（10 分）：与岗位工作地点比对
        String jobLocation = text(job.getLocation());
        double locationScore;
        if (jobLocation.isEmpty()) {
            locationScore = 10;
            matchReasons.add("岗位未标注工作地点");
        } else if (city.isEmpty()) {
            locationScore = 5;
            mismatchReasons.add("简历未填写居住地，无法比对工作地点（岗位在 " + jobLocation + "）");
        } else if (locationMatches(city, jobLocation)) {
            locationScore = 10;
            matchReasons.add("地点匹配：居住地 " + city + "（岗位在 " + jobLocation + "）");
        } else {
            locationScore = 2;
            mismatchReasons.add("工作地点 " + jobLocation + "，居住地 " + city);
        }

        // 5. 关键词 / 期望职位（10 分）
        String jobText = (text(job.getTitle()) + " " + text(job.getDescription())).toLowerCase();
        List<String> hitKeywords = new ArrayList<>();
        for (String keyword : keywords) {
            if (!jobText.isEmpty() && jobText.contains(keyword.toLowerCase())) {
                hitKeywords.add(keyword);
            }
        }
        double keywordScore = 0;
        if (!jobTitle.isEmpty()) {
            String want = jobTitle.toLowerCase();
            String title = text(job.getTitle()).toLowerCase();
            if (!title.isEmpty() && (title.contains(want) || want.contains(title))) {
                keywordScore += 5;
                matchReasons.add("期望职位「" + jobTitle + "」与岗位名称一致");
            }
        }
        if (!hitKeywords.isEmpty()) {
            keywordScore += 5;
            matchReasons.add("职位描述命中关键词：" + String.join("、", hitKeywords));
        }

        int matchScore = (int) Math.round(Math.min(100,
                skillScore + educationScore + experienceScore + locationScore + keywordScore));

        // 建议补充的关键词：优先核心缺口，其次加分缺口（提示用户只补自己真的具备的）
        List<String> suggestedKeywords = new ArrayList<>(coreMissing);
        for (String skill : plusMissing) {
            if (suggestedKeywords.size() >= 5) {
                break;
            }
            suggestedKeywords.add(skill);
        }
        if (suggestedKeywords.size() > 5) {
            suggestedKeywords = new ArrayList<>(suggestedKeywords.subList(0, 5));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", job.getId());
        result.put("title", job.getTitle());
        result.put("companyName", job.getCompanyName());
        result.put("salaryRange", job.getSalaryRange());
        result.put("location", job.getLocation());
        result.put("educationRequirement", jobEducation);
        result.put("experienceRequirement", text(job.getExperienceRequirement()));
        result.put("categoryName", categoryNames.getOrDefault(job.getCategoryId(), ""));
        result.put("description", job.getDescription());
        result.put("matchScore", matchScore);
        result.put("grade", grade(matchScore));
        result.put("priority", priority(matchScore, coreMissing.size()));
        result.put("matchedTags", matched);
        result.put("missingTags", missing);
        result.put("coreSkills", coreSkills);
        result.put("coreMatchedTags", coreMatched);
        result.put("coreMissingTags", coreMissing);
        result.put("plusSkills", plusSkills);
        result.put("plusMatchedTags", plusMatched);
        result.put("plusMissingTags", plusMissing);
        result.put("suggestedKeywords", suggestedKeywords);
        result.put("matchReasons", matchReasons);
        result.put("mismatchReasons", mismatchReasons);
        return result;
    }

    // -------------------------------------------------------------- 打分辅助

    /** 用户技能命中 JD 技能的列表（保留 JD 侧的写法） */
    private List<String> matchSkills(List<String> requiredSkills, List<String> userSkills) {
        List<String> matched = new ArrayList<>();
        for (String requirement : requiredSkills) {
            for (String skill : userSkills) {
                if (tagExtractor.skillMatches(skill, requirement)) {
                    matched.add(requirement);
                    break;
                }
            }
        }
        return matched;
    }

    private List<String> subtract(List<String> source, List<String> remove) {
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (!remove.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private double ratio(int hit, int total) {
        return total <= 0 ? 0 : (double) hit / total;
    }

    /** 投递优先级：高分且核心技能基本补齐 = 优先投递 */
    private String priority(int score, int coreMissing) {
        if (score >= 75 && coreMissing <= 1) {
            return "优先投递";
        }
        if (score >= 60) {
            return "可以一试";
        }
        return "先补差距";
    }

    // -------------------------------------------------------------- 汇总信息

    private Map<String, Object> buildSummary(List<Map<String, Object>> all, List<Map<String, Object>> top) {
        int high = 0;
        int moderate = 0;
        int low = 0;
        int scoreSum = 0;
        Map<String, Integer> missingFrequency = new LinkedHashMap<>();
        for (Map<String, Object> job : all) {
            int score = (Integer) job.get("matchScore");
            scoreSum += score;
            if (score >= 75) {
                high++;
            } else if (score >= 60) {
                moderate++;
            } else {
                low++;
            }
            for (Object tag : (List<?>) job.get("missingTags")) {
                missingFrequency.merge(String.valueOf(tag), 1, Integer::sum);
            }
        }

        List<String> topMissing = missingFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", all.size());
        summary.put("returned", top.size());
        summary.put("highMatch", high);
        summary.put("moderateMatch", moderate);
        summary.put("lowMatch", low);
        summary.put("avgScore", all.isEmpty() ? 0 : Math.round((float) scoreSum / all.size()));
        summary.put("topMissingSkills", topMissing);
        return summary;
    }

    private List<JobPosition> enabledPositions() {
        return jobPositionMapper.selectList(null).stream()
                .filter(job -> job.getStatus() == null || job.getStatus() == 1)
                .collect(Collectors.toList());
    }

    private Map<Integer, String> categoryNameMap() {
        Map<Integer, String> names = new LinkedHashMap<>();
        try {
            for (JobCategory category : jobCategoryMapper.selectList(null)) {
                names.put(category.getId(), category.getName());
            }
        } catch (Exception e) {
            log.warn("读取岗位分类失败：{}", e.getMessage());
        }
        return names;
    }

    // ------------------------------------------------------------------ utils

    private String grade(int score) {
        if (score >= 75) {
            return "高匹配";
        }
        if (score >= 60) {
            return "较匹配";
        }
        return "差距较大";
    }

    /** 地点是否算匹配：去掉"市/区/县"等后缀后互相包含即视为同城 */
    private boolean locationMatches(String city, String jobLocation) {
        String left = city.replaceAll("[\\s市省区县]", "");
        String right = jobLocation.replaceAll("[\\s市省区县]", "");
        return left.equals(right) || left.contains(right) || right.contains(left);
    }

    private String formatYears(double years) {
        if (years == Math.floor(years)) {
            return String.valueOf((long) years);
        }
        return String.valueOf(years);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<String> toStringList(Object value) {
        List<String> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                String itemText = text(item);
                if (!itemText.isEmpty()) {
                    list.add(itemText);
                }
            }
        }
        return list;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(text(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        return body;
    }
}
