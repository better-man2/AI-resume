<template>
  <div class="match-page">
    <div class="page-head">
      <el-button @click="goBack">返回上一页</el-button>
      <div class="page-title">岗位匹配</div>
      <el-button :loading="deepLoading" @click="runDeepAnalysis">AI 深度分析（P2）</el-button>
    </div>

    <!-- 导入简历 -->
    <el-card class="card">
      <template #header>
        <div class="card-head">
          <span>导入简历</span>
          <el-radio-group v-model="mode" size="small">
            <el-radio-button label="resume">使用我的简历</el-radio-button>
            <el-radio-button label="text">粘贴简历文本</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="import-body">
        <p v-if="mode === 'resume'" class="hint">
          将读取你在简历编辑页保存的最新简历，提取技能、学历、经历时间等标签，与岗位库中的 JD 做关键词匹配。
        </p>
        <template v-else>
          <el-input
            v-model="resumeText"
            type="textarea"
            :rows="6"
            placeholder="直接把简历内容粘贴进来，例如：熟悉 Java、Spring Boot、MySQL，本科，2023-06 ~ 2024-06 在 xx 公司实习…"
          />
          <p class="hint">粘贴文本时会从中识别技能关键词、学历与经验年限，结果仅供参考。</p>
        </template>
      </div>

      <el-button type="primary" :loading="matching" @click="startMatch">开始匹配</el-button>
    </el-card>

    <!-- 抽取到的标签 -->
    <el-card v-if="tags" class="card">
      <template #header><span>从简历中抽取的标签</span></template>
      <div class="tag-block">
        <div class="tag-line">
          <span class="tag-label">技能</span>
          <el-tag v-for="skill in tags.skills" :key="skill" type="info" class="tag-item">{{ skill }}</el-tag>
          <span v-if="!tags.skills || !tags.skills.length" class="hint">
            未识别到技能，建议在简历里补充技能标签
          </span>
        </div>
        <div class="tag-line">
          <span class="tag-label">学历</span>
          <span>{{ tags.educationLevel || '未识别' }}</span>
          <span class="tag-label">经验</span>
          <span>{{ tags.experienceYears ? `${tags.experienceYears} 年（按经历时间累计）` : '未识别' }}</span>
          <span class="tag-label">居住地</span>
          <span>{{ tags.city || '未填写' }}</span>
          <span class="tag-label">期望职位</span>
          <span>{{ tags.jobTitle || '未填写' }}</span>
        </div>
      </div>
    </el-card>

    <!-- 匹配汇总 -->
    <div v-if="summary" class="summary-row">
      <div class="summary-item"><b>{{ summary.total }}</b><span>在招岗位</span></div>
      <div class="summary-item"><b class="ok">{{ summary.highMatch }}</b><span>高匹配 ≥75</span></div>
      <div class="summary-item"><b class="mid">{{ summary.moderateMatch }}</b><span>较匹配 60-74</span></div>
      <div class="summary-item"><b class="low">{{ summary.lowMatch }}</b><span>差距较大</span></div>
      <div class="summary-item"><b>{{ summary.avgScore }}</b><span>平均匹配分</span></div>
      <div v-if="summary.topMissingSkills && summary.topMissingSkills.length" class="summary-item wide">
        <span class="tag-label">高频缺失技能</span>
        <el-tag v-for="skill in summary.topMissingSkills" :key="skill" type="danger" effect="plain">
          {{ skill }}
        </el-tag>
      </div>
    </div>

    <!-- 岗位列表 -->
    <div v-if="jobs.length" class="job-list">
      <el-card v-for="job in jobs" :key="job.id" class="job-card" shadow="hover">
        <div class="job-head">
          <div>
            <div class="job-title">
              {{ job.title }}
              <el-tag :type="gradeType(job.matchScore)" size="small" effect="dark">{{ job.grade }}</el-tag>
            </div>
            <div class="job-company">
              {{ job.companyName || '未填写公司' }} · {{ job.salaryRange || '薪资面议' }}
            </div>
          </div>
          <div class="job-score">
            <div class="score-value" :style="{ color: scoreColor(job.matchScore) }">{{ job.matchScore }}</div>
            <div class="score-label">匹配分</div>
          </div>
        </div>

        <el-progress :percentage="job.matchScore" :color="scoreColor(job.matchScore)" :show-text="false" />

        <div class="job-meta">
          <span>📍 {{ job.location || '地点不限' }}</span>
          <span>🎓 {{ job.educationRequirement || '学历不限' }}</span>
          <span>💼 {{ formatExperience(job.experienceRequirement) }}</span>
          <span v-if="job.categoryName">🏷️ {{ job.categoryName }}</span>
        </div>

        <div class="tag-block">
          <div class="tag-line">
            <span class="tag-label">命中</span>
            <el-tag v-for="tag in job.matchedTags" :key="tag" type="success" class="tag-item">{{ tag }}</el-tag>
            <span v-if="!job.matchedTags.length" class="hint">无</span>
          </div>
          <div class="tag-line">
            <span class="tag-label">缺失</span>
            <el-tag v-for="tag in job.missingTags" :key="tag" type="danger" effect="plain" class="tag-item">
              {{ tag }}
            </el-tag>
            <span v-if="!job.missingTags.length" class="hint">无</span>
          </div>
        </div>

        <div class="reasons">
          <p v-for="reason in job.matchReasons" :key="reason" class="reason-ok">✓ {{ reason }}</p>
          <p v-for="reason in job.mismatchReasons" :key="reason" class="reason-bad">✕ {{ reason }}</p>
        </div>

        <el-collapse v-if="job.description">
          <el-collapse-item title="查看岗位 JD">
            <p class="jd">{{ job.description }}</p>
          </el-collapse-item>
        </el-collapse>

        <div class="job-actions">
          <el-tooltip content="求职进度管理属于 P1 阶段功能" placement="top">
            <span>
              <el-button type="primary" plain size="small" disabled>加入求职进度</el-button>
            </span>
          </el-tooltip>
        </div>
      </el-card>
    </div>

    <el-empty v-else-if="matched" description="没有匹配到岗位，请确认岗位库中存在「招聘中」的岗位" />

    <!-- AI 深度分析（P2） -->
    <el-card v-if="deepResult" class="card">
      <template #header>
        <div class="card-head">
          <span>AI 深度分析（P2 阶段功能）</span>
          <el-button link @click="deepResult = null">收起</el-button>
        </div>
      </template>
      <div v-if="deepResult.career_advice" class="advice">
        <p><b>短期目标：</b>{{ deepResult.career_advice.short_term }}</p>
        <p><b>技能提升：</b>{{ deepResult.career_advice.skills_improvement }}</p>
        <p><b>长期规划：</b>{{ deepResult.career_advice.long_term }}</p>
      </div>
      <div v-for="job in deepResult.jobs || []" :key="job.id" class="deep-job">
        <div class="job-title">{{ job.title }} <span class="hint">AI 参考分 {{ job.matchScore }}</span></div>
        <p v-if="job.recommendation_reason" class="reason-ok">✓ {{ job.recommendation_reason }}</p>
        <p v-if="job.resume_improvement" class="reason-bad">✎ {{ job.resume_improvement }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import axios from '../utils/axios-config';

const router = useRouter();
const userId = localStorage.getItem('userId');

const mode = ref('resume');
const resumeText = ref('');
const matching = ref(false);
const matched = ref(false);
const tags = ref(null);
const summary = ref(null);
const jobs = ref([]);

const deepLoading = ref(false);
const deepResult = ref(null);

const goBack = () => router.back();

const scoreColor = (score) => {
  if (score >= 75) return '#67c23a';
  if (score >= 60) return '#e6a23c';
  return '#f56c6c';
};

const gradeType = (score) => {
  if (score >= 75) return 'success';
  if (score >= 60) return 'warning';
  return 'danger';
};

/** 经验要求展示：纯数字补上"年"，已经带单位或写"不限"的原样显示 */
const formatExperience = (value) => {
  const raw = (value || '').trim();
  if (!raw) return '经验不限';
  if (/^\d+(\.\d+)?$/.test(raw)) return `${raw} 年`;
  return raw;
};

const startMatch = async () => {
  if (mode.value === 'text' && !resumeText.value.trim()) {
    ElMessage.warning('请先粘贴简历文本');
    return;
  }
  if (mode.value === 'resume' && !userId) {
    ElMessage.warning('请先登录');
    return;
  }

  try {
    matching.value = true;
    const { data } = await axios.post('/api/match/jobs', {
      userId,
      resumeText: mode.value === 'text' ? resumeText.value : '',
      limit: 20,
    });

    matched.value = true;

    if (data.message) {
      ElMessage.warning(data.message);
      tags.value = null;
      summary.value = null;
      jobs.value = [];
      return;
    }

    tags.value = data.tags;
    summary.value = data.summary;
    jobs.value = data.jobs || [];
    if (!jobs.value.length) {
      ElMessage.info('没有匹配到岗位');
    }
  } catch (error) {
    const message = error?.response?.data?.error || error?.message || '匹配失败';
    ElMessage.error(message);
    matched.value = true;
    jobs.value = [];
    tags.value = null;
    summary.value = null;
  } finally {
    matching.value = false;
  }
};

/** 复用原有的 AI 深度推荐接口（P2），需要已保存的简历 */
const runDeepAnalysis = async () => {
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  try {
    deepLoading.value = true;
    const { data } = await axios.post('/api/recommend/jobs', { userId });
    if (data.error) {
      throw new Error(data.error);
    }
    deepResult.value = Array.isArray(data) ? { jobs: data } : data;
    ElMessage.success('AI 深度分析完成');
  } catch (error) {
    ElMessage.error(error?.message || 'AI 深度分析失败');
  } finally {
    deepLoading.value = false;
  }
};
</script>

<style scoped>
.match-page {
  min-height: 100vh;
  background: #f0f8ff;
  padding: 56px 20px 32px;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1000px;
  margin: 0 auto 12px;
}

.page-title {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #3186cb;
}

.card {
  max-width: 1000px;
  margin: 0 auto 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.import-body {
  margin-bottom: 12px;
}

.hint {
  color: #909399;
  font-size: 13px;
  margin: 6px 0 0;
}

.tag-block {
  display: grid;
  gap: 6px;
}

.tag-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-label {
  color: #909399;
  font-size: 13px;
  margin-right: 4px;
}

.summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 1000px;
  margin: 0 auto 12px;
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 90px;
}

.summary-item b {
  font-size: 20px;
  color: #3186cb;
}

.summary-item b.ok {
  color: #67c23a;
}

.summary-item b.mid {
  color: #e6a23c;
}

.summary-item b.low {
  color: #f56c6c;
}

.summary-item span {
  color: #909399;
  font-size: 12px;
}

.summary-item.wide {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.job-list {
  max-width: 1000px;
  margin: 0 auto;
  display: grid;
  gap: 12px;
}

.job-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.job-title {
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.job-company {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.job-score {
  text-align: center;
  min-width: 64px;
}

.score-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 12px;
  color: #909399;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #606266;
  font-size: 13px;
  margin: 10px 0;
}

.reasons {
  margin: 8px 0;
}

.reason-ok,
.reason-bad {
  margin: 2px 0;
  font-size: 13px;
}

.reason-ok {
  color: #529b2e;
}

.reason-bad {
  color: #c45656;
}

.jd {
  color: #606266;
  white-space: pre-wrap;
  margin: 0;
}

.job-actions {
  margin-top: 10px;
}

.advice p {
  margin: 6px 0;
  color: #606266;
}

.deep-job {
  padding: 8px 0;
  border-top: 1px dashed #ebeef5;
}
</style>
