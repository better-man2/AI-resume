<template>
  <div
    class="resume-doc"
    :class="[`doc--${template}`, `doc--font-${fontSize}`]"
    :style="{ '--accent': accent }"
  >
    <!-- 头部：姓名 + 联系方式（均来自基础信息固定表单，AI 不参与） -->
    <header class="doc-header">
      <h1 class="doc-name">{{ resume.basicInfo.name || '姓名' }}</h1>
      <div class="doc-contact">
        <span v-if="resume.basicInfo.phone">📞 {{ resume.basicInfo.phone }}</span>
        <span v-if="resume.basicInfo.email">✉️ {{ resume.basicInfo.email }}</span>
        <span v-if="resume.basicInfo.city">📍 现居 {{ resume.basicInfo.city }}</span>
        <span v-if="resume.jobStatus">{{ resume.jobStatus }}</span>
      </div>
    </header>

    <div class="doc-body">
      <!-- 现代双栏：技能/奖项放侧栏 -->
      <aside v-if="isTwoColumn && sideSections.length" class="doc-aside">
        <section v-for="section in sideSections" :key="section.key" class="doc-section">
          <h2 class="doc-section-title">{{ section.label }}</h2>
          <SectionContent :section="section" :resume="resume" />
        </section>
      </aside>

      <div class="doc-main">
        <section v-for="section in mainSections" :key="section.key" class="doc-section">
          <h2 class="doc-section-title">{{ section.label }}</h2>
          <SectionContent :section="section" :resume="resume" />
        </section>
        <p v-if="!sections.length" class="doc-empty">
          还没有内容，填写左侧表单或点击「AI 一键生成」后会显示在这里
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, h } from 'vue';
import { resolveSections, toMonthString } from '../utils/resume-model';

const props = defineProps({
  resume: { type: Object, required: true },
  template: { type: String, default: 'classic' },
  accent: { type: String, default: '#1f2d3d' },
  fontSize: { type: String, default: 'medium' },
  order: { type: Array, default: () => [] },
  hidden: { type: Array, default: () => [] },
});

const isTwoColumn = computed(() => props.template === 'modern');

const sections = computed(() =>
  resolveSections(props.resume, {
    order: props.order,
    hidden: props.hidden,
  }),
);

/** modern 模板把技能/奖项放侧栏 */
const SIDE_KEYS = ['skills', 'award'];
const sideSections = computed(() =>
  isTwoColumn.value ? sections.value.filter((item) => SIDE_KEYS.includes(item.key)) : [],
);
const mainSections = computed(() =>
  isTwoColumn.value ? sections.value.filter((item) => !SIDE_KEYS.includes(item.key)) : sections.value,
);

/**
 * 模块内容渲染：一个模块一套标记，用渲染函数写比在模板里堆 v-if/v-else-if 更集中。
 */
const SectionContent = {
  name: 'SectionContent',
  props: {
    section: { type: Object, required: true },
    resume: { type: Object, required: true },
  },
  setup(sectionProps) {
    const range = (item) => {
      const from = toMonthString(item.start);
      const to = toMonthString(item.end);
      if (!from && !to) return '';
      return `${from || '待填'} ~ ${to || '至今'}`;
    };
    const entryTitle = (item, key) => (key === 'project' ? item.name || '项目' : item.company || '单位');
    const entrySub = (item, key) =>
      key === 'project'
        ? [item.role].filter(Boolean).join(' · ')
        : [item.department, item.position].filter(Boolean).join(' · ');

    const softLine = (className, value) => (value ? h('div', { class: className }, value) : null);

    return () => {
      const { section, resume } = sectionProps;
      const key = section.key;

      if (key === 'intent') {
        const items = [
          resume.jobStatus && `求职状态：${resume.jobStatus}`,
          resume.jobTitle && `期望职位：${resume.jobTitle}`,
          resume.salaryExpectation && `期望薪资：${resume.salaryExpectation}`,
        ].filter(Boolean);
        return h(
          'ul',
          { class: 'doc-list' },
          items.map((text) => h('li', { class: 'doc-list-item' }, text)),
        );
      }

      if (key === 'skills') {
        return h('div', { class: 'doc-skills' }, [
          resume.profession.summary
            ? h('p', { class: 'doc-skills-summary' }, resume.profession.summary)
            : null,
          h(
            'div',
            { class: 'doc-skill-tags' },
            resume.profession.skills.map((skill) => h('span', { class: 'doc-skill', key: skill }, skill)),
          ),
        ]);
      }

      if (key === 'education') {
        return h(
          'div',
          { class: 'doc-entries' },
          resume.education.map((item, index) =>
            h('div', { class: 'doc-entry', key: index }, [
              h('div', { class: 'doc-entry-head' }, [
                h('span', { class: 'doc-entry-title' }, item.school || '学校'),
                h('span', { class: 'doc-entry-time' }, range(item)),
              ]),
              softLine('doc-entry-sub', [item.major, item.degree].filter(Boolean).join(' · ')),
            ]),
          ),
        );
      }

      if (key === 'project' || key === 'internship' || key === 'work') {
        return h(
          'div',
          { class: 'doc-entries' },
          resume[key].map((item, index) =>
            h('div', { class: 'doc-entry', key: index }, [
              h('div', { class: 'doc-entry-head' }, [
                h('span', { class: 'doc-entry-title' }, [
                  entryTitle(item, key),
                  // AI 生成的参考项目留个可视标记，提醒用户核实或替换
                  key === 'project' && item.aiGenerated
                    ? h('span', { class: 'doc-ai-tag' }, 'AI 参考')
                    : null,
                ]),
                h('span', { class: 'doc-entry-time' }, range(item)),
              ]),
              softLine('doc-entry-sub', entrySub(item, key)),
              softLine('div', item.details),
            ]),
          ),
        );
      }

      if (key === 'strengths') {
        return h(
          'ul',
          { class: 'doc-list doc-strengths' },
          resume.strengths.map((item, index) => h('li', { class: 'doc-list-item', key: index }, item)),
        );
      }

      if (key === 'award') {
        return h(
          'ul',
          { class: 'doc-list' },
          resume.award.map((item, index) =>
            h('li', { class: 'doc-list-item', key: index }, [
              h('span', {}, item.name),
              item.date ? h('span', { class: 'doc-entry-time' }, toMonthString(item.date)) : null,
            ]),
          ),
        );
      }

      if (key === 'evaluation') {
        return h('p', { class: 'doc-evaluation' }, resume.selfEvaluation);
      }

      return null;
    };
  },
};
</script>

<style scoped>
.resume-doc {
  --scale: 1;
  width: 100%;
  max-width: 794px; /* A4 96dpi 宽度 */
  margin: 0 auto;
  background: #fff;
  color: #303133;
  font-size: calc(13px * var(--scale));
  line-height: 1.7;
  box-sizing: border-box;
  padding: 32px 36px;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
}

.doc--font-small {
  --scale: 0.9;
}

.doc--font-large {
  --scale: 1.12;
}

/* ---------------- 头部 ---------------- */
.doc-header {
  padding-bottom: 12px;
  margin-bottom: 16px;
  border-bottom: 2px solid var(--accent);
}

.doc-name {
  margin: 0;
  font-size: calc(24px * var(--scale));
  color: var(--accent);
  letter-spacing: 2px;
}

.doc-contact {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: calc(12px * var(--scale));
  color: #606266;
}

/* ---------------- 正文 ---------------- */
.doc-section + .doc-section {
  margin-top: 14px;
}

.doc-section-title {
  margin: 0 0 8px;
  font-size: calc(14px * var(--scale));
  color: var(--accent);
  padding-left: 8px;
  border-left: 4px solid var(--accent);
  line-height: 1.2;
}

.doc-list {
  margin: 0;
  padding-left: 4px;
  list-style: none;
}

.doc-list-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.doc-entry + .doc-entry {
  margin-top: 10px;
}

.doc-entry-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.doc-entry-title {
  font-weight: 700;
  color: #1f2d3d;
}

.doc-entry-time {
  font-size: calc(12px * var(--scale));
  color: #909399;
  white-space: nowrap;
}

.doc-entry-sub {
  color: #606266;
  font-size: calc(12.5px * var(--scale));
}

.doc-entry-details {
  margin: 2px 0 0;
  white-space: pre-wrap;
}

.doc-skills-summary {
  margin: 0 0 6px;
  color: #606266;
}

.doc-skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.doc-skill {
  display: inline-block;
  padding: 1px 8px;
  border: 1px solid var(--accent);
  border-radius: 10px;
  font-size: calc(12px * var(--scale));
  color: var(--accent);
}

.doc-empty {
  color: #c0c4cc;
  text-align: center;
  padding: 40px 0;
}

.doc-evaluation {
  margin: 0;
  white-space: pre-wrap;
}

/* AI 生成的参考内容标记 */
.doc-ai-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 0 6px;
  font-size: calc(10.5px * var(--scale));
  font-weight: 400;
  color: #d48806;
  border: 1px solid #ffd591;
  background: #fff7e6;
  border-radius: 8px;
  vertical-align: middle;
}

.doc-strengths .doc-list-item {
  display: block;
}

/* ---------------- 模板：现代双栏 ---------------- */
.doc--modern {
  padding: 0;
}

.doc--modern .doc-header {
  border-bottom: none;
  background: var(--accent);
  color: #fff;
  padding: 22px 30px 18px;
  margin-bottom: 0;
}

.doc--modern .doc-name,
.doc--modern .doc-contact {
  color: #fff;
}

.doc--modern .doc-body {
  display: flex;
  align-items: stretch;
}

.doc--modern .doc-aside {
  width: 32%;
  background: #f5f7fa;
  padding: 20px 18px;
}

.doc--modern .doc-main {
  width: 68%;
  padding: 20px 24px;
}

.doc--modern .doc-section-title {
  border-left: none;
  padding-left: 0;
  border-bottom: 1px dashed var(--accent);
  padding-bottom: 4px;
}

.doc--modern .doc-skill {
  background: #fff;
}

.doc--modern .doc-list-item {
  display: block;
}

/* ---------------- 模板：校园简约 ---------------- */
.doc--campus .doc-header {
  text-align: center;
  border-bottom: 1px solid #dcdfe6;
}

.doc--campus .doc-name,
.doc--campus .doc-contact {
  justify-content: center;
  text-align: center;
}

.doc--campus .doc-section-title {
  border-left: none;
  padding: 2px 12px;
  display: inline-block;
  background: var(--accent);
  color: #fff;
  border-radius: 12px;
}
</style>
