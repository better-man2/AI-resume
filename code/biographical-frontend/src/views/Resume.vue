<template>
  <div class="editor-page">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-button @click="goBack">返回上一页</el-button>
      <div class="toolbar-title">简历编辑器</div>
      <div class="toolbar-actions">
        <el-button @click="router.push('/resume/create')">去 AI 生成</el-button>
        <el-button type="success" :loading="saving" @click="saveResume">更新简历</el-button>
        <el-button type="primary" @click="exportPdf">导出 PDF</el-button>
        <el-button @click="exportWord">导出 Word</el-button>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左：模板与排版 -->
      <aside class="panel sidebar">
        <h3 class="panel-title">简历模板</h3>
        <div class="template-list">
          <div
            v-for="item in TEMPLATES"
            :key="item.key"
            class="template-item"
            :class="{ active: resume.template === item.key }"
            @click="resume.template = item.key"
          >
            <span class="dot" :style="{ background: item.accent }" />
            <span>{{ item.name }}</span>
          </div>
        </div>

        <h3 class="panel-title">主题色</h3>
        <div class="color-row">
          <span
            v-for="color in ACCENT_COLORS"
            :key="color"
            class="color-dot"
            :class="{ active: layout.accent === color }"
            :style="{ background: color }"
            @click="layout.accent = color"
          />
        </div>

        <h3 class="panel-title">字号</h3>
        <el-radio-group v-model="layout.fontSize" size="small">
          <el-radio-button v-for="item in FONT_SIZES" :key="item.key" :label="item.key">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>

        <h3 class="panel-title">模块顺序 / 显隐</h3>
        <div class="module-list">
          <div v-for="(key, index) in layout.order" :key="key" class="module-row">
            <el-checkbox
              :model-value="!layout.hidden.includes(key)"
              @change="toggleModule(key)"
            >
              {{ moduleLabel(key) }}
            </el-checkbox>
            <div class="module-actions">
              <el-button link :disabled="index === 0" @click="moveModule(index, -1)">↑</el-button>
              <el-button link :disabled="index === layout.order.length - 1" @click="moveModule(index, 1)">
                ↓
              </el-button>
            </div>
          </div>
        </div>
        <p class="hint">箭头调整顺序，取消勾选后该模块不会出现在简历和导出文件里</p>
      </aside>

      <!-- 中：内容编辑 -->
      <section class="panel form-panel" :class="{ loading }">
        <!-- 固定表单栏：基础信息手工录入，不参与 AI 生成 -->
        <div class="basic-bar">
          <div class="basic-bar-head">
            <span class="basic-bar-title">基本信息</span>
            <el-tag size="small" type="success" effect="plain">手动填写 · AI 不会改动</el-tag>
          </div>
          <el-form label-width="70px">
            <div class="basic-grid">
              <el-form-item label="姓名">
                <el-input v-model="resume.basicInfo.name" maxlength="20" />
              </el-form-item>
              <el-form-item label="电话">
                <el-input v-model="resume.basicInfo.phone" maxlength="11" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="resume.basicInfo.email" maxlength="50" />
              </el-form-item>
              <el-form-item label="居住地">
                <el-input v-model="resume.basicInfo.city" maxlength="20" placeholder="参与岗位地点匹配" />
              </el-form-item>
            </div>
          </el-form>
        </div>

        <h3 class="panel-title">求职意向</h3>
        <el-form label-width="90px">
          <el-form-item label="求职状态">
            <el-select v-model="resume.jobStatus" style="width: 100%" placeholder="请选择">
              <el-option v-for="item in JOB_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="期望职位">
            <el-input v-model="resume.jobTitle" maxlength="30" />
          </el-form-item>
          <el-form-item label="期望薪资">
            <el-input v-model="resume.salaryExpectation" maxlength="20" />
          </el-form-item>
        </el-form>

        <div class="panel-title-row">
          <h3 class="panel-title">自我评价</h3>
          <el-button size="small" type="primary" plain @click="polishEvaluation">AI 润色</el-button>
        </div>
        <el-form label-width="90px">
          <el-form-item label="内容">
            <el-input
              v-model="resume.selfEvaluation"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              placeholder="AI 会根据你填写的技能与经历生成，也可以自己改写"
            />
          </el-form-item>
        </el-form>

        <div class="panel-title-row">
          <h3 class="panel-title">个人优势</h3>
          <el-button size="small" type="primary" plain @click="resume.strengths.push('')">+ 添加一条</el-button>
        </div>
        <div v-for="(item, index) in resume.strengths" :key="`strength-${index}`" class="strength-row">
          <el-input v-model="resume.strengths[index]" maxlength="60" placeholder="例如：有完整的校园项目开发经验" />
          <el-button link type="primary" @click="polishStrength(index)">AI 润色</el-button>
          <el-button link type="danger" @click="resume.strengths.splice(index, 1)">删除</el-button>
        </div>
        <p v-if="!resume.strengths.length" class="hint">
          这一项由 AI 根据你的专业技能与专业自动生成，也可以手动添加
        </p>

        <!-- 教育经历 -->
        <h3 class="panel-title">教育经历</h3>
        <div v-for="(item, index) in resume.education" :key="`edu-${index}`" class="entry-card">
          <div class="entry-head">
            <span>教育 {{ index + 1 }}</span>
            <el-button link type="danger" @click="resume.education.splice(index, 1)">删除</el-button>
          </div>
          <el-form label-width="90px">
            <el-form-item label="学校">
              <el-input v-model="item.school" maxlength="40" />
            </el-form-item>
            <el-form-item label="专业">
              <el-input v-model="item.major" maxlength="40" />
            </el-form-item>
            <el-form-item label="学历">
              <el-select v-model="item.degree" style="width: 100%" placeholder="请选择">
                <el-option v-for="degree in DEGREE_OPTIONS" :key="degree" :label="degree" :value="degree" />
              </el-select>
            </el-form-item>
            <el-form-item label="起止时间">
              <div class="range">
                <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="开始" />
                <span class="range-sep">至</span>
                <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="结束" />
              </div>
            </el-form-item>
          </el-form>
        </div>
        <el-button class="add-btn" @click="resume.education.push(emptyEducation())">+ 添加教育经历</el-button>

        <!-- 技能 -->
        <div class="panel-title-row">
          <h3 class="panel-title">专业技能</h3>
          <el-button size="small" type="primary" plain @click="polishSummary">AI 润色概述</el-button>
        </div>
        <div class="skill-row">
          <el-input v-model="skillInput" placeholder="输入技能后回车" maxlength="30" @keyup.enter="addSkill" />
          <el-button type="primary" plain @click="addSkill">添加</el-button>
        </div>
        <div class="skill-tags">
          <el-tag
            v-for="skill in resume.profession.skills"
            :key="skill"
            closable
            @close="removeSkill(skill)"
          >
            {{ skill }}
          </el-tag>
        </div>
        <el-form label-width="90px">
          <el-form-item label="技能概述">
            <el-input v-model="resume.profession.summary" type="textarea" :rows="2" maxlength="120" show-word-limit />
          </el-form-item>
        </el-form>

        <!-- 项目经历 -->
        <div class="panel-title-row">
          <h3 class="panel-title">项目经历</h3>
        </div>
        <div v-for="(item, index) in resume.project" :key="`pro-${index}`" class="entry-card">
          <div class="entry-head">
            <span>
              项目 {{ index + 1 }}
              <el-tag v-if="item.aiGenerated" size="small" type="warning" effect="plain">AI 参考</el-tag>
            </span>
            <div>
              <el-button link type="primary" @click="polishDescription(item, '项目')">AI 润色</el-button>
              <el-button v-if="item.aiGenerated" link type="success" @click="item.aiGenerated = false">
                已核实
              </el-button>
              <el-button link type="danger" @click="resume.project.splice(index, 1)">删除</el-button>
            </div>
          </div>
          <p v-if="item.aiGenerated" class="hint ai-hint">
            AI 根据你的专业与技能生成的参考项目，起止时间需要你自己补；请改成你的真实经历，或点「已核实」去掉标记
          </p>
          <el-form label-width="90px">
            <el-form-item label="项目名称">
              <el-input v-model="item.name" maxlength="50" />
            </el-form-item>
            <el-form-item label="担任角色">
              <el-input v-model="item.role" maxlength="30" />
            </el-form-item>
            <el-form-item label="起止时间">
              <div class="range">
                <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="开始" />
                <span class="range-sep">至</span>
                <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="结束" />
              </div>
            </el-form-item>
            <el-form-item label="项目内容">
              <el-input v-model="item.details" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-form>
        </div>
        <el-button class="add-btn" @click="resume.project.push(emptyProject())">+ 添加项目经历</el-button>

        <!-- 实习经历 -->
        <h3 class="panel-title">实习经历</h3>
        <div v-for="(item, index) in resume.internship" :key="`int-${index}`" class="entry-card">
          <div class="entry-head">
            <span>实习 {{ index + 1 }}</span>
            <div>
              <el-button link type="primary" @click="polishDescription(item, '实习')">AI 润色</el-button>
              <el-button link type="danger" @click="resume.internship.splice(index, 1)">删除</el-button>
            </div>
          </div>
          <el-form label-width="90px">
            <el-form-item label="单位名称">
              <el-input v-model="item.company" maxlength="50" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input v-model="item.department" maxlength="30" />
            </el-form-item>
            <el-form-item label="职位">
              <el-input v-model="item.position" maxlength="30" />
            </el-form-item>
            <el-form-item label="起止时间">
              <div class="range">
                <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="开始" />
                <span class="range-sep">至</span>
                <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="结束" />
              </div>
            </el-form-item>
            <el-form-item label="实习内容">
              <el-input v-model="item.details" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-form>
        </div>
        <el-button class="add-btn" @click="resume.internship.push(emptyExperience())">+ 添加实习经历</el-button>

        <!-- 工作经历 -->
        <h3 class="panel-title">工作经历</h3>
        <div v-for="(item, index) in resume.work" :key="`work-${index}`" class="entry-card">
          <div class="entry-head">
            <span>工作 {{ index + 1 }}</span>
            <div>
              <el-button link type="primary" @click="polishDescription(item, '工作')">AI 润色</el-button>
              <el-button link type="danger" @click="resume.work.splice(index, 1)">删除</el-button>
            </div>
          </div>
          <el-form label-width="90px">
            <el-form-item label="公司名称">
              <el-input v-model="item.company" maxlength="50" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input v-model="item.department" maxlength="30" />
            </el-form-item>
            <el-form-item label="职位">
              <el-input v-model="item.position" maxlength="30" />
            </el-form-item>
            <el-form-item label="起止时间">
              <div class="range">
                <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="开始" />
                <span class="range-sep">至</span>
                <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="结束" />
              </div>
            </el-form-item>
            <el-form-item label="工作内容">
              <el-input v-model="item.details" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-form>
        </div>
        <el-button class="add-btn" @click="resume.work.push(emptyExperience())">+ 添加工作经历</el-button>

        <!-- 荣誉奖项 -->
        <h3 class="panel-title">荣誉奖项</h3>
        <div v-for="(item, index) in resume.award" :key="`award-${index}`" class="entry-card">
          <div class="entry-head">
            <span>奖项 {{ index + 1 }}</span>
            <el-button link type="danger" @click="resume.award.splice(index, 1)">删除</el-button>
          </div>
          <el-form label-width="90px">
            <el-form-item label="奖项名称">
              <el-input v-model="item.name" maxlength="60" />
            </el-form-item>
            <el-form-item label="获得时间">
              <el-date-picker v-model="item.date" type="month" value-format="YYYY-MM" placeholder="选择月份" />
            </el-form-item>
          </el-form>
        </div>
        <el-button class="add-btn" @click="resume.award.push(emptyAward())">+ 添加奖项</el-button>
      </section>

      <!-- 右：预览（导出即取这块 DOM） -->
      <section class="panel preview-panel" :class="{ loading }">
        <div class="preview-head">
          <span>预览效果（导出 PDF 与此一致）</span>
          <span v-if="loading">{{ loadingText }}</span>
        </div>
        <div ref="paperRef" class="paper">
          <ResumeTemplate
            :resume="resume"
            :template="resume.template"
            :accent="layout.accent"
            :font-size="layout.fontSize"
            :order="layout.order"
            :hidden="layout.hidden"
          />
        </div>
      </section>
    </div>

    <!-- AI 润色：先看对比，确认后再替换 -->
    <el-dialog v-model="polish.visible" title="AI 润色" width="600px">
      <div v-if="polish.loading" class="polish-loading">AI 正在润色，请稍候…</div>
      <template v-else>
        <div class="polish-block">
          <div class="polish-label">原文</div>
          <p class="polish-text">{{ polish.original }}</p>
        </div>
        <div class="polish-block">
          <div class="polish-label">润色后</div>
          <p class="polish-text polished">{{ polish.polished }}</p>
        </div>
        <p class="hint">润色只优化表达，不会新增你没写过的事实与数据</p>
      </template>
      <template #footer>
        <el-button @click="polish.visible = false">取消</el-button>
        <el-button type="primary" :disabled="polish.loading || !polish.polished" @click="applyPolish">
          应用到简历
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import html2pdf from 'html2pdf.js';
import { Document, Packer, Paragraph, TextRun } from 'docx';
import { saveAs } from 'file-saver';
import ResumeTemplate from '../components/ResumeTemplate.vue';
import axios from '../utils/axios-config';
import {
  ACCENT_COLORS,
  DEGREE_OPTIONS,
  FONT_SIZES,
  JOB_STATUS_OPTIONS,
  MODULES,
  TEMPLATES,
  emptyAward,
  emptyEducation,
  emptyExperience,
  emptyProject,
  emptyResume,
  loadLayout,
  normalizeResume,
  resumeToSections,
  saveLayout,
  textToSkills,
} from '../utils/resume-model';

const router = useRouter();
const userId = localStorage.getItem('userId');

const resume = ref(emptyResume());
const layout = ref(loadLayout(userId));
const loading = ref(false);
const loadingText = ref('');
const saving = ref(false);
const skillInput = ref('');
const paperRef = ref(null);

const moduleLabel = (key) => MODULES.find((item) => item.key === key)?.label || key;

const toggleModule = (key) => {
  layout.value.hidden = layout.value.hidden.includes(key)
    ? layout.value.hidden.filter((item) => item !== key)
    : [...layout.value.hidden, key];
};

const moveModule = (index, offset) => {
  const order = [...layout.value.order];
  const target = index + offset;
  if (target < 0 || target >= order.length) return;
  [order[index], order[target]] = [order[target], order[index]];
  layout.value.order = order;
};

watch(
  layout,
  (value) => saveLayout(userId, value),
  { deep: true },
);

const goBack = () => router.back();

const addSkill = () => {
  const skills = textToSkills(skillInput.value);
  skills.forEach((skill) => {
    if (!resume.value.profession.skills.includes(skill)) resume.value.profession.skills.push(skill);
  });
  skillInput.value = '';
};

const removeSkill = (skill) => {
  resume.value.profession.skills = resume.value.profession.skills.filter((item) => item !== skill);
};

// ------------------------------------------------------------ AI 润色（P1）

const polish = ref({ visible: false, loading: false, original: '', polished: '', apply: null });

/** 打开润色弹窗并请求润色结果；apply 是用户点「应用到简历」时执行的写回函数 */
const runPolish = async ({ text, kind, apply }) => {
  const original = (text || '').trim();
  if (!original) {
    ElMessage.warning('这段内容还是空的，先写点内容再润色');
    return;
  }
  polish.value = { visible: true, loading: true, original, polished: '', apply };
  try {
    const { data } = await axios.post('/api/resume/polish', { text: original, kind });
    if (!data.success) {
      throw new Error(data.error || '润色失败');
    }
    polish.value.polished = data.polished;
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || error?.message || '润色失败');
    polish.value.visible = false;
  } finally {
    polish.value.loading = false;
  }
};

const applyPolish = () => {
  if (polish.value.apply && polish.value.polished) {
    polish.value.apply(polish.value.polished);
    ElMessage.success('已应用润色结果，记得点「更新简历」保存');
  }
  polish.value.visible = false;
};

const polishEvaluation = () =>
  runPolish({
    text: resume.value.selfEvaluation,
    kind: 'evaluation',
    apply: (value) => {
      resume.value.selfEvaluation = value;
    },
  });

const polishSummary = () =>
  runPolish({
    text: resume.value.profession.summary,
    kind: 'summary',
    apply: (value) => {
      resume.value.profession.summary = value;
    },
  });

const polishStrength = (index) =>
  runPolish({
    text: resume.value.strengths[index],
    kind: 'strength',
    apply: (value) => {
      resume.value.strengths[index] = value;
    },
  });

const polishDescription = (item, label) =>
  runPolish({
    text: item.details,
    kind: 'description',
    apply: (value) => {
      item.details = value;
      ElMessage.success(`${label}描述已更新`);
    },
  });

// ------------------------------------------------------------ 数据加载

/** 生成接口返回的数据优先（一次性消费）；否则读数据库里最新一份 */
const loadResume = async () => {
  const generated = localStorage.getItem('resumeData');
  if (generated) {
    try {
      resume.value = normalizeResume(JSON.parse(generated));
      localStorage.removeItem('resumeData');
      ElMessage.success('已载入刚生成的简历，修改后记得点「更新简历」保存');
      return;
    } catch (e) {
      localStorage.removeItem('resumeData');
    }
  }

  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }

  try {
    loading.value = true;
    loadingText.value = '正在加载最新简历…';
    const { data } = await axios.get('/ai/resume/latest', { params: { userId } });
    if (data.success && data.content) {
      resume.value = normalizeResume(data.content);
    } else {
      throw new Error(data.error || '加载失败');
    }
  } catch (error) {
    ElMessage.warning('还没有已保存的简历，可以先手动填写或去 AI 生成');
  } finally {
    loading.value = false;
    loadingText.value = '';
  }
};

const payload = computed(() => ({
  basicInfo: { ...resume.value.basicInfo },
  jobStatus: resume.value.jobStatus,
  jobTitle: resume.value.jobTitle,
  salaryExpectation: resume.value.salaryExpectation,
  template: resume.value.template,
  education: resume.value.education,
  profession: resume.value.profession,
  project: resume.value.project,
  internship: resume.value.internship,
  work: resume.value.work,
  award: resume.value.award,
  selfEvaluation: resume.value.selfEvaluation,
  strengths: resume.value.strengths,
}));

const saveResume = async () => {
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  try {
    saving.value = true;
    const { data } = await axios.post('/ai/resume/update', { userId, resumeData: payload.value });
    if (!data.success) throw new Error(data.error || '保存失败');
    ElMessage.success('简历已更新');
  } catch (error) {
    ElMessage.error(error?.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

// ------------------------------------------------------------ 导出

const exportPdf = async () => {
  const element = paperRef.value;
  if (!element) return;
  try {
    await html2pdf()
      .set({
        margin: 0,
        filename: `${resume.value.name || '我的'}-简历.pdf`,
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      })
      .from(element)
      .save();
    ElMessage.success('PDF 已导出');
  } catch (error) {
    ElMessage.error('导出 PDF 失败：' + (error?.message || '未知错误'));
  }
};

/** Word 导出：用同一份数据生成段落，避免和预览不一致 */
const exportWord = async () => {
  const sections = resumeToSections(resume.value, layout.value);
  const basic = resume.value.basicInfo || {};
  const children = [
    new Paragraph({
      children: [new TextRun({ text: basic.name || '姓名', bold: true, size: 36 })],
    }),
    new Paragraph({
      children: [
        new TextRun({
          text: [
            basic.phone,
            basic.email,
            basic.city ? `现居 ${basic.city}` : '',
            resume.value.jobStatus,
          ]
            .filter(Boolean)
            .join('    '),
          size: 20,
          color: '606266',
        }),
      ],
      spacing: { after: 200 },
    }),
  ];

  sections.forEach((section) => {
    children.push(
      new Paragraph({
        children: [new TextRun({ text: section.label, bold: true, size: 26 })],
        spacing: { before: 200, after: 80 },
      }),
    );
    section.lines.forEach((line) => {
      children.push(new Paragraph({ children: [new TextRun({ text: line, size: 22 })] }));
    });
  });

  const doc = new Document({ sections: [{ properties: {}, children }] });
  const blob = await Packer.toBlob(doc);
  saveAs(blob, `${basic.name || '我的'}-简历.docx`);
  ElMessage.success('Word 已导出');
};

onMounted(loadResume);
</script>

<style scoped>
.editor-page {
  min-height: 100vh;
  background: #f0f8ff;
  padding: 56px 16px 16px;
  box-sizing: border-box;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1600px;
  margin: 0 auto 10px;
}

.toolbar-title {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #3186cb;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.editor-body {
  display: flex;
  gap: 10px;
  max-width: 1600px;
  margin: 0 auto;
  align-items: flex-start;
}

.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  padding: 16px;
}

.sidebar {
  flex: 0 0 220px;
  position: sticky;
  top: 10px;
}

.form-panel {
  flex: 1 1 460px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  position: relative;
}

.preview-panel {
  flex: 1 1 520px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  position: relative;
}

.panel-title {
  margin: 16px 0 8px;
  font-size: 14px;
  color: #303133;
  border-left: 3px solid #3186cb;
  padding-left: 8px;
}

.panel-title:first-child {
  margin-top: 0;
}

/* 标题右侧带操作按钮 */
.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-title-row .panel-title {
  margin-bottom: 8px;
}

.strength-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.ai-hint {
  margin: 0 0 10px;
  color: #d48806;
}

/* 润色对比弹窗 */
.polish-loading {
  padding: 24px 0;
  text-align: center;
  color: #909399;
}

.polish-block + .polish-block {
  margin-top: 12px;
}

.polish-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.polish-text {
  margin: 0;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  white-space: pre-wrap;
  line-height: 1.7;
}

.polish-text.polished {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}

/* 固定表单栏：基础信息手工录入，不参与 AI 生成 */
.basic-bar {
  position: sticky;
  top: 0;
  z-index: 3;
  background: #f2f8ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  padding: 12px 14px 2px;
  margin-bottom: 14px;
}

.basic-bar-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.basic-bar-title {
  font-size: 14px;
  font-weight: 700;
  color: #3186cb;
}

.basic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 0 12px;
}

.template-list {
  display: grid;
  gap: 6px;
}

.template-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}

.template-item.active {
  border-color: #3186cb;
  background: #f2f8ff;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.color-row {
  display: flex;
  gap: 8px;
}

.color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
}

.color-dot.active {
  border-color: #3186cb;
  box-shadow: 0 0 0 2px #d9ecff;
}

.module-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.module-actions {
  display: flex;
}

.hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.entry-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 14px 0;
  margin-bottom: 10px;
  background: #fafcff;
}

.entry-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #3186cb;
  font-weight: 600;
  margin-bottom: 6px;
}

.add-btn {
  width: 100%;
  border-style: dashed;
  margin-bottom: 8px;
}

.range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-sep {
  color: #909399;
}

.skill-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 30px;
}

.preview-head {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

.paper {
  background: #fff;
}

/* 加载时整体蒙一层 */
.form-panel.loading::before,
.preview-panel.loading::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.75);
  z-index: 5;
}

@media (max-width: 1280px) {
  .editor-body {
    flex-wrap: wrap;
  }

  .sidebar,
  .form-panel,
  .preview-panel {
    flex: 1 1 100%;
    position: static;
    max-height: none;
  }
}
</style>
