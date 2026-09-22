<template>
  <div class="create-page">
    <!-- 顶部：返回 + 草稿状态 -->
    <div class="page-head">
      <el-button @click="goBack">返回上一页</el-button>
      <div class="page-title">AI 生成简历</div>
      <div class="draft-state">
        <span v-if="draftSavedAt" class="draft-saved">草稿已自动保存 {{ draftSavedAt }}</span>
        <el-button v-if="draftSavedAt" link type="danger" @click="resetAll">清空草稿</el-button>
      </div>
    </div>

    <el-steps :active="step" finish-status="success" align-center class="steps">
      <el-step v-for="item in STEPS" :key="item.key" :title="item.title" />
    </el-steps>

    <div class="create-body">
      <div class="form-panel">
        <!-- 固定表单栏：基础信息只由用户手工录入，不进入 AI 流程 -->
        <div class="basic-bar">
          <div class="basic-bar-head">
            <span class="basic-bar-title">基本信息</span>
            <el-tag size="small" type="success" effect="plain">手动填写 · AI 不会改动</el-tag>
          </div>
          <p class="basic-bar-desc">
            姓名、电话、邮箱、居住地由你直接录入，系统按独立字段结构化存储，不参与大模型生成；
            AI 只负责自我评价、实习/项目经历这类描述性文字。
          </p>
          <el-form label-width="70px" class="basic-form">
            <div class="basic-grid">
              <el-form-item label="姓名" required>
                <el-input v-model="form.basicInfo.name" maxlength="20" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="form.basicInfo.phone" maxlength="11" placeholder="用于简历投递联系" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="form.basicInfo.email" maxlength="50" placeholder="用于接收面试通知" />
              </el-form-item>
              <el-form-item label="居住地">
                <el-input v-model="form.basicInfo.city" maxlength="20" placeholder="例如：杭州（参与地点匹配）" />
              </el-form-item>
            </div>
          </el-form>
        </div>

        <h3 class="step-title">{{ currentStep.title }}</h3>
        <p class="step-desc">{{ currentStep.desc }}</p>

        <!-- ① 选择模板 -->
        <div v-if="currentStep.key === 'template'" class="template-grid">
          <div
            v-for="item in TEMPLATES"
            :key="item.key"
            class="template-card"
            :class="{ active: form.template === item.key }"
            @click="form.template = item.key"
          >
            <div class="thumb" :class="`thumb--${item.key}`">
              <div class="thumb-head" />
              <div class="thumb-body">
                <div class="thumb-line" />
                <div class="thumb-line short" />
                <div class="thumb-line" />
              </div>
            </div>
            <div class="template-name">{{ item.name }}</div>
            <div class="template-desc">{{ item.desc }}</div>
          </div>
        </div>

        <!-- ② 求职意向 -->
        <el-form v-else-if="currentStep.key === 'intent'" label-width="90px">
          <el-form-item label="求职状态">
            <el-select v-model="form.jobStatus" placeholder="请选择当前状态" style="width: 100%">
              <el-option v-for="item in JOB_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="期望职位" required>
            <el-input v-model="form.jobTitle" maxlength="30" placeholder="例如：Java 开发工程师" />
          </el-form-item>
          <el-form-item label="期望薪资">
            <el-input v-model="form.salaryExpectation" maxlength="20" placeholder="例如：10-15K" />
          </el-form-item>
        </el-form>

        <!-- ③ 教育经历 -->
        <div v-else-if="currentStep.key === 'education'">
          <div v-for="(item, index) in form.education" :key="index" class="entry-card">
            <div class="entry-head">
              <span>教育经历 {{ index + 1 }}</span>
              <el-button v-if="form.education.length > 1" link type="danger" @click="form.education.splice(index, 1)">
                删除
              </el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="学校">
                <el-input v-model="item.school" maxlength="40" placeholder="请输入学校名称" />
              </el-form-item>
              <el-form-item label="专业">
                <el-input v-model="item.major" maxlength="40" placeholder="请输入专业名称" />
              </el-form-item>
              <el-form-item label="学历">
                <el-select v-model="item.degree" placeholder="请选择学历" style="width: 100%">
                  <el-option v-for="degree in DEGREE_OPTIONS" :key="degree" :label="degree" :value="degree" />
                </el-select>
              </el-form-item>
              <el-form-item label="起止时间">
                <div class="range">
                  <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="入学时间" />
                  <span class="range-sep">至</span>
                  <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="毕业时间" />
                </div>
              </el-form-item>
            </el-form>
          </div>
          <el-button class="add-btn" @click="form.education.push(emptyEducation())">+ 添加教育经历</el-button>
        </div>

        <!-- ④ 专业技能 -->
        <div v-else-if="currentStep.key === 'skills'">
          <div class="skill-row">
            <el-input
              v-model="skillInput"
              placeholder="输入技能后回车，例如 Java"
              maxlength="30"
              @keyup.enter="addSkill(skillInput)"
            />
            <el-button type="primary" plain @click="addSkill(skillInput)">添加</el-button>
          </div>
          <div class="skill-tags">
            <el-tag
              v-for="skill in form.skills"
              :key="skill"
              closable
              size="large"
              @close="removeSkill(skill)"
            >
              {{ skill }}
            </el-tag>
            <span v-if="!form.skills.length" class="hint">还没有技能标签，岗位匹配会用到这些关键词</span>
          </div>
          <el-form label-width="90px" class="batch-form">
            <el-form-item label="批量粘贴">
              <el-input
                v-model="skillText"
                type="textarea"
                :rows="2"
                placeholder="例如：Java、MySQL、Redis，支持逗号/顿号/换行分隔"
              />
            </el-form-item>
            <el-form-item label="">
              <el-button @click="absorbSkillText">拆分为标签</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- ⑤ 项目经历 -->
        <div v-else-if="currentStep.key === 'project'">
          <el-alert type="warning" :closable="false" show-icon class="step-alert">
            <template #title>不填也可以：AI 会按你的专业与技能生成参考项目</template>
            留空时，AI 会根据专业、技能、求职意向生成 1~2 条参考项目经历（起止时间留空），
            并标注「AI 参考」提示你核实。建议直接填你真实做过的项目，写清楚你负责的部分。
          </el-alert>
          <div v-for="(item, index) in form.project" :key="index" class="entry-card">
            <div class="entry-head">
              <span>项目 {{ index + 1 }}</span>
              <el-button link type="danger" @click="form.project.splice(index, 1)">删除</el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="项目名称">
                <el-input v-model="item.name" maxlength="50" placeholder="请输入项目名称" />
              </el-form-item>
              <el-form-item label="担任角色">
                <el-input v-model="item.role" maxlength="30" placeholder="例如：后端开发" />
              </el-form-item>
              <el-form-item label="起止时间">
                <div class="range">
                  <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="开始时间" />
                  <span class="range-sep">至</span>
                  <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="结束时间" />
                </div>
              </el-form-item>
              <el-form-item label="项目内容">
                <el-input
                  v-model="item.details"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="你负责的部分、用到的技术、做的事情（AI 只会在此基础上整理措辞，不会编造内容）"
                />
              </el-form-item>
            </el-form>
          </div>
          <el-button class="add-btn" @click="form.project.push(emptyProject())">+ 添加项目经历</el-button>
        </div>

        <!-- ⑥ 实习经历 -->
        <div v-else-if="currentStep.key === 'internship'">
          <div v-for="(item, index) in form.internship" :key="index" class="entry-card">
            <div class="entry-head">
              <span>实习 {{ index + 1 }}</span>
              <el-button link type="danger" @click="form.internship.splice(index, 1)">删除</el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="单位名称">
                <el-input v-model="item.company" maxlength="50" placeholder="请输入实习单位" />
              </el-form-item>
              <el-form-item label="部门">
                <el-input v-model="item.department" maxlength="30" placeholder="请输入部门" />
              </el-form-item>
              <el-form-item label="职位">
                <el-input v-model="item.position" maxlength="30" placeholder="请输入职位" />
              </el-form-item>
              <el-form-item label="起止时间">
                <div class="range">
                  <el-date-picker v-model="item.start" type="month" value-format="YYYY-MM" placeholder="开始时间" />
                  <span class="range-sep">至</span>
                  <el-date-picker v-model="item.end" type="month" value-format="YYYY-MM" placeholder="结束时间" />
                </div>
              </el-form-item>
              <el-form-item label="实习内容">
                <el-input
                  v-model="item.details"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="做了什么、用了什么技术（同样不会被 AI 编造）"
                />
              </el-form-item>
            </el-form>
          </div>
          <el-button class="add-btn" @click="form.internship.push(emptyExperience())">+ 添加实习经历</el-button>
        </div>

        <!-- ⑦ 荣誉奖项 -->
        <div v-else-if="currentStep.key === 'award'">
          <div v-for="(item, index) in form.award" :key="index" class="entry-card">
            <div class="entry-head">
              <span>奖项 {{ index + 1 }}</span>
              <el-button link type="danger" @click="form.award.splice(index, 1)">删除</el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="奖项名称">
                <el-input v-model="item.name" maxlength="60" placeholder="例如：校级一等奖学金" />
              </el-form-item>
              <el-form-item label="获得时间">
                <el-date-picker v-model="item.date" type="month" value-format="YYYY-MM" placeholder="获得时间" />
              </el-form-item>
            </el-form>
          </div>
          <el-button class="add-btn" @click="form.award.push(emptyAward())">+ 添加奖项</el-button>
          <p class="hint">没有奖项可以直接进入下一步，这一项可以跳过</p>
        </div>

        <!-- ⑧ 确认生成 -->
        <div v-else-if="currentStep.key === 'confirm'">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">{{ form.basicInfo.name || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="居住地">{{ form.basicInfo.city || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">
              {{ form.basicInfo.phone || '未填电话' }} / {{ form.basicInfo.email || '未填邮箱' }}
            </el-descriptions-item>
            <el-descriptions-item label="期望职位">{{ form.jobTitle || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="教育经历">{{ form.education.filter(e => e.school).length }} 条</el-descriptions-item>
            <el-descriptions-item label="技能标签">{{ form.skills.length }} 个</el-descriptions-item>
            <el-descriptions-item label="项目经历">{{ form.project.length }} 条</el-descriptions-item>
            <el-descriptions-item label="实习经历">{{ form.internship.length }} 条</el-descriptions-item>
            <el-descriptions-item label="荣誉奖项">{{ form.award.length }} 条</el-descriptions-item>
            <el-descriptions-item label="简历模板">
              {{ TEMPLATES.find(item => item.key === form.template)?.name }}
            </el-descriptions-item>
          </el-descriptions>

          <el-form label-width="90px" class="extra-form">
            <el-form-item label="补充说明">
              <el-input
                v-model="form.extra"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="给 AI 生成自我评价的参考，例如：性格、优势、求职方向（不填也可以）"
              />
            </el-form-item>
          </el-form>

          <el-alert type="info" :closable="false" show-icon class="ai-notice">
            <template #title>基础信息不进大模型，AI 只写描述性文字</template>
            上面固定表单栏里的姓名、电话、邮箱、居住地会按独立字段直接入库，AI 完全不接触；
            学校、单位、时间、奖项等事实字段原样保留，AI 负责自我评价、个人优势、技能概述与经历描述；
            如果没填项目经历，AI 会按你的专业与技能生成带「AI 参考」标记的参考项目（时间由你补）；
            生成后先进入预览页，确认无误再进入编辑页修改、润色并导出 PDF。
          </el-alert>
        </div>

        <!-- 底部操作 -->
        <div class="step-actions">
          <el-button :disabled="step === 0" @click="prev">上一步</el-button>
          <el-button v-if="currentStep.key !== 'confirm'" type="primary" @click="next">下一步</el-button>
          <el-button v-else type="success" :loading="generating" @click="generate">AI 一键生成简历</el-button>
        </div>
      </div>

      <!-- 右侧实时预览 -->
      <div class="preview-panel">
        <div class="preview-head">实时预览（{{ TEMPLATES.find(item => item.key === form.template)?.name }}）</div>
        <div class="preview-scroll">
          <ResumeTemplate
            :resume="previewResume"
            :template="form.template"
            :accent="TEMPLATES.find(item => item.key === form.template)?.accent"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="showGenerating" :show-close="false" width="320px" center title="正在生成简历">
      <div class="generating">
        <el-progress type="circle" :percentage="progress" :indeterminate="true" :duration="2" :stroke-width="4" />
        <p>{{ progressText }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import ResumeTemplate from '../../components/ResumeTemplate.vue';
import axios from '../../utils/axios-config';
import {
  DEGREE_OPTIONS,
  JOB_STATUS_OPTIONS,
  STEPS,
  TEMPLATES,
  buildGenerateForm,
  clearDraft,
  emptyAward,
  emptyEducation,
  emptyExperience,
  emptyForm,
  emptyProject,
  hasGeneratableContent,
  loadDraft,
  normalizeResume,
  saveDraft,
  textToSkills,
  validateBasicInfo,
  validateStep,
} from '../../utils/resume-model';

const router = useRouter();
const userId = localStorage.getItem('userId');

const form = ref(emptyForm());
const step = ref(0);
const skillInput = ref('');
const skillText = ref('');
const draftSavedAt = ref('');
const generating = ref(false);
const showGenerating = ref(false);
const progress = ref(0);
const progressText = ref('正在读取你填写的信息…');

const currentStep = computed(() => STEPS[step.value] || STEPS[0]);

/** 预览用的简历结构由表单实时转换，保证所见即所得 */
const previewResume = computed(() => {
  const payload = buildGenerateForm(form.value);
  return normalizeResume({
    basicInfo: {
      name: payload.basic.name,
      phone: payload.basic.phone,
      email: payload.basic.email,
      city: payload.basic.city,
    },
    jobStatus: payload.intent.jobStatus,
    jobTitle: payload.intent.jobTitle,
    salaryExpectation: payload.intent.salaryExpectation,
    template: form.value.template,
    education: payload.educations,
    profession: { skills: payload.skills, summary: '' },
    project: payload.projects,
    internship: payload.internships,
    award: payload.awards,
    selfEvaluation: form.value.extra,
  });
});

// ------------------------------------------------------------ 草稿缓存

let saveTimer = null;

const scheduleSave = () => {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    draftSavedAt.value = saveDraft(userId, { form: form.value, step: step.value });
  }, 800);
};

watch([form, step], scheduleSave, { deep: true });

onMounted(() => {
  const draft = loadDraft(userId);
  if (!draft) return;

  const applyDraft = () => {
    form.value = draft.form;
    step.value = Math.min(Math.max(draft.step, 0), STEPS.length - 1);
    draftSavedAt.value = draft.savedAt || '';
  };

  // 从预览页「返回修改填写」进来的草稿直接回填，避免多一次弹窗
  if (draft.auto) {
    applyDraft();
    return;
  }

  ElMessageBox.confirm(
    `检测到上次填写的草稿（保存于 ${draft.savedAt || '未知时间'}），是否恢复？`,
    '恢复草稿',
    {
      confirmButtonText: '恢复填写',
      cancelButtonText: '重新开始',
      type: 'info',
      distinguishCancelAndClose: true,
    },
  )
    .then(applyDraft)
    .catch((action) => {
      if (action === 'cancel') {
        clearDraft(userId);
        form.value = emptyForm();
        step.value = 0;
      }
    });
});

onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer);
});

// ------------------------------------------------------------ 交互

const goBack = () => router.back();

const prev = () => {
  if (step.value > 0) step.value -= 1;
};

const next = () => {
  // 基础信息是页面上的固定表单栏，开始填经历前先校验一次
  if (currentStep.value.key === 'template') {
    const basicCheck = validateBasicInfo(form.value.basicInfo);
    if (!basicCheck.ok) {
      ElMessage.warning(basicCheck.message);
      return;
    }
  }
  const result = validateStep(currentStep.value, form.value);
  if (!result.ok) {
    ElMessage.warning(result.message);
    return;
  }
  if (step.value < STEPS.length - 1) step.value += 1;
};

const addSkill = (value) => {
  const skills = textToSkills(value);
  if (!skills.length) return;
  skills.forEach((skill) => {
    if (!form.value.skills.includes(skill)) form.value.skills.push(skill);
  });
  skillInput.value = '';
};

const removeSkill = (skill) => {
  form.value.skills = form.value.skills.filter((item) => item !== skill);
};

const absorbSkillText = () => {
  addSkill(skillText.value);
  skillText.value = '';
};

const resetAll = () => {
  ElMessageBox.confirm('清空后将丢弃本地草稿，确定继续吗？', '清空草稿', {
    type: 'warning',
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
  })
    .then(() => {
      clearDraft(userId);
      form.value = emptyForm();
      step.value = 0;
      draftSavedAt.value = '';
      ElMessage.success('草稿已清空');
    })
    .catch(() => {});
};

const progressTimer = ref(null);

const startProgressAnimation = () => {
  progress.value = 8;
  progressText.value = '正在读取你填写的信息…';
  progressTimer.value = setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 12;
    }
    if (progress.value > 60) {
      progressText.value = 'AI 正在整理描述文字（不会编造经历）…';
    } else if (progress.value > 30) {
      progressText.value = '正在生成结构化简历…';
    }
  }, 600);
};

const stopProgressAnimation = () => {
  if (progressTimer.value) {
    clearInterval(progressTimer.value);
    progressTimer.value = null;
  }
  progress.value = 100;
};

const generate = async () => {
  const basicCheck = validateBasicInfo(form.value.basicInfo);
  if (!basicCheck.ok) {
    ElMessage.warning(basicCheck.message + '（基本信息栏）');
    return;
  }
  if (!hasGeneratableContent(form.value)) {
    ElMessage.warning('请补充教育经历、技能、项目或实习中的至少一项');
    return;
  }

  generating.value = true;
  showGenerating.value = true;
  startProgressAnimation();

  try {
    const { data } = await axios.post('/api/resume/generate', {
      userId,
      username: localStorage.getItem('username'),
      template: form.value.template,
      form: buildGenerateForm(form.value),
    });

    if (!data.success) {
      throw new Error(data.error || '生成失败');
    }

    // 交给预览页展示；草稿使命结束，清掉避免下次误恢复
    localStorage.setItem('resumeData', JSON.stringify(data.content));
    clearDraft(userId);
    draftSavedAt.value = '';

    stopProgressAnimation();
    if (data.aiRewrite === false) {
      ElMessage.warning(data.notice || 'AI 改写未生效，已按填写内容原样生成');
    } else {
      ElMessage.success('生成完成，请先预览');
    }
    router.push('/resume/preview');
  } catch (error) {
    stopProgressAnimation();
    ElMessage.error(error?.message || '生成失败，请稍后重试');
  } finally {
    generating.value = false;
    showGenerating.value = false;
  }
};
</script>

<style scoped>
.create-page {
  min-height: 100vh;
  background: #f0f8ff;
  padding: 56px 20px 24px;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1400px;
  margin: 0 auto 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #3186cb;
  flex: 1;
}

.draft-saved {
  color: #67c23a;
  font-size: 13px;
  margin-right: 8px;
}

.steps {
  max-width: 1400px;
  margin: 0 auto 12px;
  background: #fff;
  padding: 14px 10px;
  border-radius: 8px;
}

.create-body {
  display: flex;
  gap: 12px;
  max-width: 1400px;
  margin: 0 auto;
  align-items: flex-start;
}

.form-panel {
  flex: 1 1 620px;
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  min-height: 520px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

/* 固定表单栏：基础信息手工录入，不参与 AI 生成 */
.basic-bar {
  position: sticky;
  top: 0;
  z-index: 3;
  background: #f2f8ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  padding: 12px 16px 4px;
  margin-bottom: 16px;
}

.basic-bar-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.basic-bar-title {
  font-size: 15px;
  font-weight: 700;
  color: #3186cb;
}

.basic-bar-desc {
  margin: 6px 0 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.basic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 0 16px;
}

@media (max-width: 720px) {
  .basic-grid {
    grid-template-columns: 1fr;
  }
}

.step-title {
  margin: 0 0 4px;
  font-size: 16px;
  color: #303133;
}

.step-desc {
  margin: 0 0 16px;
  color: #909399;
  font-size: 13px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

/* 模板卡片 */
.template-grid {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.template-card {
  width: 190px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.template-card.active {
  border-color: #3186cb;
  box-shadow: 0 4px 12px rgba(49, 134, 203, 0.2);
}

.thumb {
  height: 130px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.thumb-head {
  height: 26px;
  background: #1f2d3d;
}

.thumb-body {
  padding: 10px;
  display: grid;
  gap: 6px;
}

.thumb-line {
  height: 6px;
  background: #e4e7ed;
  border-radius: 3px;
}

.thumb-line.short {
  width: 60%;
}

.thumb--modern .thumb-head {
  background: #2563eb;
}

.thumb--modern .thumb-body {
  grid-template-columns: 1fr 2fr;
}

.thumb--campus .thumb-head {
  background: #0f766e;
}

.template-name {
  margin-top: 8px;
  font-weight: 600;
}

.template-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

/* 多条经历卡片 */
.entry-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px 0;
  margin-bottom: 12px;
  background: #fafcff;
}

.entry-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #3186cb;
  margin-bottom: 8px;
}

.add-btn {
  width: 100%;
  border-style: dashed;
}

.range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-sep {
  color: #909399;
}

/* 技能 */
.skill-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 34px;
  margin-bottom: 8px;
}

.hint {
  color: #909399;
  font-size: 13px;
}

.batch-form {
  margin-top: 8px;
}

.ai-notice {
  margin-top: 12px;
}

.step-alert {
  margin-bottom: 12px;
}

/* 预览 */
.preview-panel {
  flex: 1 1 460px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 12px;
}

.preview-head {
  padding: 10px 16px;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  font-size: 13px;
}

.preview-scroll {
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  padding: 12px;
}

.generating {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 10px 0 20px;
  color: #606266;
}

@media (max-width: 1100px) {
  .create-body {
    flex-direction: column;
  }

  .preview-panel {
    position: static;
    width: 100%;
  }
}
</style>
