<template>
  <div class="preview-page">
    <div class="page-head">
      <el-button @click="goBack">返回</el-button>
      <div class="page-title">简历预览</div>
      <div class="head-actions">
        <el-button @click="backToForm">返回修改填写</el-button>
        <el-button type="primary" @click="goEdit">去编辑完善</el-button>
      </div>
    </div>

    <el-alert v-if="!loading && resume" type="success" :closable="false" show-icon class="tip">
      <template #title>已按你填写的信息生成（模板：{{ templateName }}）</template>
      学校、单位、时间、奖项等事实字段均来自你的填写，AI 只整理了描述文字。
      可以先确认效果，导出 PDF 需要在编辑页完成调整后操作。
    </el-alert>

    <div v-if="loading" class="placeholder">正在加载简历…</div>
    <div v-else-if="!resume" class="placeholder">
      没有找到简历数据
      <div>
        <el-button type="primary" @click="router.push('/resume/create')">去填写并生成</el-button>
      </div>
    </div>

    <div v-else class="paper">
      <ResumeTemplate
        :resume="resume"
        :template="resume.template"
        :accent="accent"
        :font-size="layout.fontSize"
        :order="layout.order"
        :hidden="layout.hidden"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import ResumeTemplate from '../../components/ResumeTemplate.vue';
import axios from '../../utils/axios-config';
import {
  ACCENT_COLORS,
  TEMPLATES,
  loadLayout,
  normalizeResume,
  resumeToForm,
  saveDraft,
} from '../../utils/resume-model';

const router = useRouter();
const userId = localStorage.getItem('userId');

const resume = ref(null);
const loading = ref(true);
const layout = ref(loadLayout(userId));

const templateName = computed(
  () => TEMPLATES.find((item) => item.key === resume.value?.template)?.name || '经典单栏',
);

/** 生成接口不返回主题色，用模板自带主色 */
const accent = computed(() => {
  const matched = TEMPLATES.find((item) => item.key === resume.value?.template);
  return matched?.accent || ACCENT_COLORS[0];
});

const goBack = () => router.back();
const goEdit = () => router.push('/resume');

/** 回到分步表单：把当前简历回填成表单草稿，标记为自动恢复 */
const backToForm = () => {
  if (resume.value) {
    saveDraft(userId, { form: resumeToForm(resume.value), step: 0, auto: true });
  }
  router.push('/resume/create');
};

const loadGenerated = () => {
  const raw = localStorage.getItem('resumeData');
  if (!raw) return false;
  try {
    resume.value = normalizeResume(JSON.parse(raw));
    return true;
  } catch (e) {
    return false;
  }
};

const loadLatest = async () => {
  if (!userId) return;
  const { data } = await axios.get('/ai/resume/latest', { params: { userId } });
  if (data.success && data.content) {
    resume.value = normalizeResume(data.content);
  }
};

onMounted(async () => {
  try {
    if (!loadGenerated()) {
      await loadLatest();
    }
  } catch (error) {
    ElMessage.warning('加载简历失败：' + (error?.message || '未知错误'));
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.preview-page {
  min-height: 100vh;
  background: #f0f8ff;
  padding: 56px 20px 32px;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 900px;
  margin: 0 auto 12px;
}

.page-title {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #3186cb;
}

.tip {
  max-width: 900px;
  margin: 0 auto 12px;
}

.paper {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.placeholder {
  max-width: 900px;
  margin: 60px auto;
  text-align: center;
  color: #909399;
  display: grid;
  gap: 12px;
}
</style>
