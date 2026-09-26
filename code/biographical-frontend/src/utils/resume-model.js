/**
 * 简历数据模型 / 模板定义 / 草稿缓存 / 分步表单校验。
 *
 * 数据契约（与后端 /api/resume/generate、/ai/resume/update 一致）：
 *   basicInfo    {name, phone, email, city}   —— 仅表单录入，独立 JSON 字段，不参与 AI 生成
 *   jobStatus jobTitle salaryExpectation template
 *   education[]  {school, major, degree, start, end}
 *   profession   {skills: [], summary: ''}
 *   project[]    {name, role, start, end, details}
 *   internship[] {company, department, position, start, end, details}
 *   work[]       {company, department, position, start, end, details}
 *   award[]      {name, date}
 *   selfEvaluation                        —— AI 生成的描述性文本
 *
 * 旧数据（平铺 name/phone/email + 单对象 + period/studyPeriod 数组 + profession.skill 文本）
 * 由 normalizeResume 统一成上面的结构。
 */

export const TEMPLATES = [
  { key: 'classic', name: '经典单栏', desc: '黑白简洁、模块分明，通用投递首选', accent: '#1f2d3d' },
  { key: 'modern', name: '现代双栏', desc: '左侧联系方式与技能，右侧经历，信息密度高', accent: '#2563eb' },
  { key: 'campus', name: '校园简约', desc: '应届生向，突出教育、项目与实习', accent: '#0f766e' },
];

export const ACCENT_COLORS = ['#1f2d3d', '#2563eb', '#0f766e', '#7c3aed', '#b45309'];

export const FONT_SIZES = [
  { key: 'small', label: '小', scale: 0.9 },
  { key: 'medium', label: '中', scale: 1 },
  { key: 'large', label: '大', scale: 1.12 },
];

/** 可排序/显隐的模块，key 与简历字段一致；基本信息不在此列（它是固定表单栏，始终展示） */
export const MODULES = [
  { key: 'intent', label: '求职意向' },
  { key: 'education', label: '教育经历' },
  { key: 'skills', label: '专业技能' },
  { key: 'strengths', label: '个人优势' },
  { key: 'project', label: '项目经历' },
  { key: 'internship', label: '实习经历' },
  { key: 'work', label: '工作经历' },
  { key: 'award', label: '荣誉奖项' },
  { key: 'evaluation', label: '自我评价' },
];

export const DEFAULT_ORDER = MODULES.map((item) => item.key);

/** 分步表单：第一步选模板，最后一步确认生成；基础信息是固定表单栏，不占步骤 */
export const STEPS = [
  { key: 'template', title: '选择模板', desc: '先定样式，后面可以随时换' },
  { key: 'intent', title: '求职意向', desc: '期望职位与薪资' },
  { key: 'education', title: '教育经历', desc: '学校、专业、时间' },
  { key: 'skills', title: '专业技能', desc: '用标签逐个添加' },
  { key: 'project', title: '项目经历', desc: '没填的话，AI 会按你的专业与技能生成参考项目' },
  { key: 'internship', title: '实习经历', desc: '实习单位与内容' },
  { key: 'award', title: '荣誉奖项', desc: '可跳过' },
  { key: 'confirm', title: '确认生成', desc: '核对后一键生成' },
];

export const DEGREE_OPTIONS = ['大专', '本科', '硕士', '博士'];
export const JOB_STATUS_OPTIONS = ['在校生', '应届生', '在职', '离职'];

// ------------------------------------------------------------------ 基础工具

export function safeParse(text) {
  if (typeof text !== 'string') return null;
  try {
    return JSON.parse(text);
  } catch (e) {
    return null;
  }
}

/**
 * 从大模型返回的文本里解析 JSON 对象（容错）。
 * 模型输出的 JSON 里可能夹着裸换行/制表符（JSON 不允许），流式拼接时也会带进分隔符，
 * 这里逐字符把控制字符换成空格：字符串里的换行变成空格，结构位置上的空白本来就不影响解析。
 *
 * @returns 解析成功且像简历内容时返回对象，否则返回 null
 */
export function parseAiJsonObject(text) {
  if (!text) return null;
  const start = text.indexOf('{');
  const end = text.lastIndexOf('}');
  if (start < 0 || end <= start) return null;

  const body = text.slice(start, end + 1);
  let cleaned = '';
  let escaped = false;
  for (const current of body) {
    if (escaped) {
      cleaned += current;
      escaped = false;
      continue;
    }
    if (current === '\\') {
      cleaned += current;
      escaped = true;
      continue;
    }
    cleaned += current === '\n' || current === '\r' || current === '\t' ? ' ' : current;
  }

  return safeParse(cleaned);
}

const text = (value) => (value === null || value === undefined ? '' : String(value).trim());

/** 任意日期值 -> 'YYYY-MM' */
export function toMonthString(value) {
  if (!value) return '';
  if (Array.isArray(value)) return '';
  const raw = String(value).trim();
  if (!raw) return '';
  if (/^\d{4}-\d{2}$/.test(raw)) return raw;
  const matched = raw.match(/^(\d{4})\D{0,2}(\d{1,2})?/);
  if (matched) {
    const month = matched[2] ? String(Math.min(12, Math.max(1, Number(matched[2])))).padStart(2, '0') : '01';
    return `${matched[1]}-${month}`;
  }
  const date = new Date(value);
  if (!Number.isNaN(date.getTime())) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }
  return '';
}

/** 时间区间展示：2023-06 ~ 2024-09，缺失一端时用"至今/待填" */
export function formatRange(start, end) {
  const from = toMonthString(start);
  const to = toMonthString(end);
  if (!from && !to) return '';
  return `${from || '待填'} ~ ${to || '至今'}`;
}

/** 技能文本框 -> 标签数组（支持中英文逗号、顿号、换行、分号） */
export function textToSkills(value) {
  return text(value)
    .split(/[\n,;，；、/|]/)
    .map((item) => item.trim())
    .filter((item, index, list) => item && list.indexOf(item) === index);
}

/** 字符串数组字段（个人优势）：兼容数组与单条文本 */
export function toStringList(value) {
  if (Array.isArray(value)) {
    return value.map((item) => text(item)).filter(Boolean);
  }
  return textToSkills(value);
}

// ------------------------------------------------------------------ 空数据

export function emptyEducation() {
  return { school: '', major: '', degree: '', start: '', end: '' };
}

export function emptyProject() {
  return { name: '', role: '', start: '', end: '', details: '', aiGenerated: false };
}

export function emptyExperience() {
  return { company: '', department: '', position: '', start: '', end: '', details: '' };
}

export function emptyAward() {
  return { name: '', date: '' };
}

export function defaultLayout() {
  return { accent: ACCENT_COLORS[0], fontSize: 'medium', order: [...DEFAULT_ORDER], hidden: [] };
}

export function emptyBasicInfo() {
  return { name: '', phone: '', email: '', city: '' };
}

/** 空白简历（分步表单初始值） */
export function emptyResume() {
  return {
    basicInfo: emptyBasicInfo(),
    jobStatus: '',
    jobTitle: '',
    salaryExpectation: '',
    template: 'classic',
    education: [],
    profession: { skills: [], summary: '' },
    project: [],
    internship: [],
    work: [],
    award: [],
    selfEvaluation: '',
    strengths: [],
    aiMeta: { pending: [] },
    generatedAt: '',
  };
}

/** 分步表单模型（比简历多一个 extra 补充说明；基础信息是固定表单栏） */
export function emptyForm() {
  return {
    template: 'classic',
    basicInfo: emptyBasicInfo(),
    jobStatus: '',
    jobTitle: '',
    salaryExpectation: '',
    education: [emptyEducation()],
    skills: [],
    skillText: '',
    project: [],
    internship: [],
    award: [],
    extra: '',
  };
}

// ------------------------------------------------------------------ 归一化

function toEntries(value, type) {
  const source = Array.isArray(value) ? value : value ? [value] : [];
  return source
    .filter((item) => item && typeof item === 'object')
    .map((item) => normalizeEntry(item, type))
    .filter((item) => Object.values(item).some((field) => text(field)));
}

function normalizeEntry(item, type) {
  // 旧数据把时间放在 period/studyPeriod 数组里
  const range = Array.isArray(item.period) ? item.period : Array.isArray(item.studyPeriod) ? item.studyPeriod : [];
  const start = toMonthString(item.start || range[0]);
  const end = toMonthString(item.end || range[1]);

  if (type === 'education') {
    return {
      school: text(item.school),
      major: text(item.major),
      degree: text(item.degree),
      start,
      end,
    };
  }
  if (type === 'project') {
    return {
      name: text(item.name),
      role: text(item.role),
      start,
      end,
      details: text(item.details),
      // AI 生成的参考项目会带这个标记，前端据此显示「AI 参考」并要求用户核实
      aiGenerated: item.aiGenerated === true,
    };
  }
  if (type === 'award') {
    return { name: text(item.name || item.details), date: toMonthString(item.date) };
  }
  return {
    company: text(item.company),
    department: text(item.department),
    position: text(item.position),
    start,
    end,
    details: text(item.details),
  };
}

/** AI 生成内容溯源：{ pending: ["selfEvaluation", "project.0.details", ...] } */
export function normalizeAiMeta(value) {
  if (!value || typeof value !== 'object' || !Array.isArray(value.pending)) {
    return { pending: [] };
  }
  return { pending: value.pending.map((item) => text(item)).filter(Boolean) };
}

function normalizeProfession(value) {  const profession = { skills: [], summary: '' };
  if (!value) return profession;
  if (typeof value === 'string') {
    profession.skills = textToSkills(value);
    return profession;
  }
  const rawSkills = value.skills;
  if (Array.isArray(rawSkills)) {
    profession.skills = rawSkills.map((item) => text(item)).filter(Boolean);
  } else if (rawSkills) {
    profession.skills = textToSkills(rawSkills);
  } else if (value.skill) {
    // 旧结构：{skill: "擅长 Java、Mysql 等"}
    profession.skills = textToSkills(String(value.skill).replace(/擅长|熟悉|精通|了解|等/g, ' '));
  }
  profession.summary = text(value.summary);
  return profession;
}

function normalizeBasicInfo(source) {
  const stored = source.basicInfo && typeof source.basicInfo === 'object' ? source.basicInfo : {};
  const basic = emptyBasicInfo();
  // 优先取独立的 basicInfo，其次兼容旧的平铺 name/phone/email
  basic.name = text(stored.name || source.name);
  basic.phone = text(stored.phone || source.phone);
  basic.email = text(stored.email || source.email);
  basic.city = text(stored.city);
  return basic;
}

/** 把任意来源（后端返回 / localStorage / 旧数据）的简历数据统一成当前结构 */export function normalizeResume(raw) {
  const source = typeof raw === 'string' ? safeParse(raw) : raw;
  const resume = emptyResume();
  if (!source || typeof source !== 'object') return resume;

  resume.basicInfo = normalizeBasicInfo(source);
  resume.jobStatus = text(source.jobStatus);
  resume.jobTitle = text(source.jobTitle);
  resume.salaryExpectation = text(source.salaryExpectation);
  resume.selfEvaluation = text(source.selfEvaluation);
  resume.strengths = toStringList(source.strengths);
  resume.aiMeta = normalizeAiMeta(source.aiMeta);
  resume.generatedAt = text(source.generatedAt);
  resume.template = TEMPLATES.some((item) => item.key === source.template) ? source.template : 'classic';
  resume.education = toEntries(source.education, 'education');
  resume.project = toEntries(source.project, 'project');
  resume.internship = toEntries(source.internship, 'internship');
  resume.work = toEntries(source.work, 'work');
  resume.award = toEntries(source.award, 'award');
  resume.profession = normalizeProfession(source.profession);
  return resume;
}

/** 简历 -> 分步表单模型（编辑页/预览页跳回表单时用） */
export function resumeToForm(resume) {
  const source = normalizeResume(resume);
  return {
    template: source.template,
    basicInfo: { ...source.basicInfo },
    jobStatus: source.jobStatus,
    jobTitle: source.jobTitle,
    salaryExpectation: source.salaryExpectation,
    education: source.education.length ? source.education : [emptyEducation()],
    skills: [...source.profession.skills],
    skillText: '',
    project: source.project,
    internship: source.internship,
    award: source.award,
    extra: source.profession.summary || '',
  };
}

/** 分步表单模型 -> 生成接口入参（basic 是独立字段，后端不会把它送给大模型） */
export function buildGenerateForm(form) {
  const clean = (entries, fields) =>
    entries
      .map((entry) => {
        const result = {};
        fields.forEach((field) => {
          result[field] = text(entry[field]);
        });
        return result;
      })
      .filter((entry) => Object.values(entry).some((field) => field));

  const basic = form.basicInfo || {};

  return {
    basic: {
      name: text(basic.name),
      phone: text(basic.phone),
      email: text(basic.email),
      city: text(basic.city),
    },
    intent: {
      jobStatus: text(form.jobStatus),
      jobTitle: text(form.jobTitle),
      salaryExpectation: text(form.salaryExpectation),
    },
    educations: clean(form.education, ['school', 'major', 'degree', 'start', 'end']),
    skills: form.skills.map((item) => text(item)).filter(Boolean),
    projects: clean(form.project, ['name', 'role', 'start', 'end', 'details']),
    internships: clean(form.internship, ['company', 'department', 'position', 'start', 'end', 'details']),
    awards: clean(form.award, ['name', 'date']),
    extra: text(form.extra),
  };
}

/** 表单是否已经有可生成的内容 */
export function hasGeneratableContent(form) {
  const payload = buildGenerateForm(form);
  return Boolean(
    payload.basic.name &&
      (payload.educations.length || payload.projects.length || payload.internships.length || payload.skills.length),
  );
}

// ------------------------------------------------------------------ 分步校验

const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const EMAIL_PATTERN = /^[\w.+-]+@[\w-]+\.[\w.-]+$/;

/** 基础信息校验（固定表单栏，不属于分步流程，生成前单独校验） */
export function validateBasicInfo(basicInfo) {
  const basic = basicInfo || {};
  if (!text(basic.name)) return { ok: false, message: '请填写姓名' };
  if (basic.phone && !PHONE_PATTERN.test(text(basic.phone))) return { ok: false, message: '手机号格式不正确' };
  if (basic.email && !EMAIL_PATTERN.test(text(basic.email))) return { ok: false, message: '邮箱格式不正确' };
  return { ok: true };
}

/** 返回 { ok, message }，只做轻校验，不阻断用户往下填 */
export function validateStep(stepKey, form) {
  const fail = (message) => ({ ok: false, message });
  switch (stepKey) {
    case 'intent':
      if (!text(form.jobTitle)) return fail('请填写期望职位，岗位匹配会用到它');
      return { ok: true };
    case 'education': {
      const filled = form.education.filter((item) => text(item.school) || text(item.major));
      if (!filled.length) return fail('至少填写一条教育经历（学校或专业）');
      if (form.education.some((item) => (text(item.school) || text(item.major)) && !text(item.school)))
        return fail('请补全学校名称');
      return { ok: true };
    }
    case 'skills':
      if (!form.skills.length) return fail('至少添加一个技能标签，岗位匹配依赖技能关键词');
      return { ok: true };
    default:
      return { ok: true };
  }
}

// ------------------------------------------------------------------ 草稿缓存

const DRAFT_PREFIX = 'resume_draft_v1:';
const LAYOUT_PREFIX = 'resume_layout_v1:';

export function draftKey(userId) {
  return `${DRAFT_PREFIX}${userId || 'anonymous'}`;
}

export function loadDraft(userId) {
  const raw = safeParse(localStorage.getItem(draftKey(userId)));
  if (!raw || !raw.form) return null;

  const form = { ...emptyForm(), ...raw.form };
  // 兼容旧版草稿：基础信息以前平铺在 name/phone/email 上
  form.basicInfo = { ...emptyBasicInfo(), ...(form.basicInfo || {}) };
  if (!form.basicInfo.name && raw.form.name) form.basicInfo.name = text(raw.form.name);
  if (!form.basicInfo.phone && raw.form.phone) form.basicInfo.phone = text(raw.form.phone);
  if (!form.basicInfo.email && raw.form.email) form.basicInfo.email = text(raw.form.email);
  delete form.name;
  delete form.phone;
  delete form.email;

  return {
    form,
    step: Number.isInteger(raw.step) ? raw.step : 0,
    savedAt: raw.savedAt || '',
    // auto = true 表示是"返回修改填写"这类明确动作写入的，进入页面直接回填、不再弹窗询问
    auto: Boolean(raw.auto),
  };
}

export function saveDraft(userId, payload) {
  const savedAt = new Date().toLocaleTimeString('zh-CN', { hour12: false });
  localStorage.setItem(draftKey(userId), JSON.stringify({ ...payload, savedAt }));
  return savedAt;
}

export function clearDraft(userId) {
  localStorage.removeItem(draftKey(userId));
}

// 排版偏好（主题色/字号/模块顺序）属于本地展示设置，存在浏览器里
export function loadLayout(userId) {
  const raw = safeParse(localStorage.getItem(`${LAYOUT_PREFIX}${userId || 'anonymous'}`));
  if (!raw) return defaultLayout();
  return {
    accent: ACCENT_COLORS.includes(raw.accent) ? raw.accent : ACCENT_COLORS[0],
    fontSize: FONT_SIZES.some((item) => item.key === raw.fontSize) ? raw.fontSize : 'medium',
    order: Array.isArray(raw.order) && raw.order.length ? raw.order : [...DEFAULT_ORDER],
    hidden: Array.isArray(raw.hidden) ? raw.hidden : [],
  };
}

export function saveLayout(userId, layout) {
  localStorage.setItem(`${LAYOUT_PREFIX}${userId || 'anonymous'}`, JSON.stringify(layout));
}

// ------------------------------------------------------------------ 模板渲染数据

/** 按顺序/显隐整理出要渲染的模块列表 */
export function resolveSections(resume, layout) {
  const order = layout?.order?.length ? layout.order : DEFAULT_ORDER;
  const hidden = layout?.hidden || [];
  return order
    .filter((key) => MODULES.some((item) => item.key === key))
    .map((key) => ({ key, label: MODULES.find((item) => item.key === key).label }))
    .filter((section) => !hidden.includes(section.key))
    .filter((section) => sectionHasContent(resume, section.key));
}

/** 模块是否有内容（没内容的模块不渲染） */
export function sectionHasContent(resume, key) {
  switch (key) {
    case 'intent':
      return Boolean(resume.jobStatus || resume.jobTitle || resume.salaryExpectation);
    case 'education':
      return resume.education.length > 0;
    case 'skills':
      return resume.profession.skills.length > 0 || Boolean(resume.profession.summary);
    case 'strengths':
      return resume.strengths.length > 0;
    case 'project':
      return resume.project.length > 0;
    case 'internship':
      return resume.internship.length > 0;
    case 'work':
      return resume.work.length > 0;
    case 'award':
      return resume.award.length > 0;
    case 'evaluation':
      return Boolean(resume.selfEvaluation);
    default:
      return false;
  }
}

/** 简历 -> 纯文本模块（Word 导出、复制文本用），与模板渲染保持同样的模块顺序 */
export function resumeToSections(resume, layout) {
  return resolveSections(resume, layout).map(({ key, label }) => {
    const lines = [];
    if (key === 'intent') {
      if (resume.jobStatus) lines.push(`求职状态：${resume.jobStatus}`);
      if (resume.jobTitle) lines.push(`期望职位：${resume.jobTitle}`);
      if (resume.salaryExpectation) lines.push(`期望薪资：${resume.salaryExpectation}`);
    } else if (key === 'education') {
      resume.education.forEach((item) => {
        const sub = [item.major, item.degree].filter(Boolean).join(' · ');
        lines.push([item.school, sub, formatRange(item.start, item.end)].filter(Boolean).join('｜'));
      });
    } else if (key === 'skills') {
      if (resume.profession.summary) lines.push(resume.profession.summary);
      if (resume.profession.skills.length) lines.push(resume.profession.skills.join('、'));
    } else if (key === 'strengths') {
      resume.strengths.forEach((item) => lines.push(`· ${item}`));
    } else if (key === 'award') {
      resume.award.forEach((item) => {
        lines.push([item.name, item.date ? toMonthString(item.date) : ''].filter(Boolean).join('｜'));
      });
    } else if (key === 'evaluation') {
      if (resume.selfEvaluation) lines.push(resume.selfEvaluation);
    } else {
      resume[key].forEach((item) => {
        const title = key === 'project' ? item.name || '项目' : item.company || '单位';
        const sub = key === 'project' ? item.role : [item.department, item.position].filter(Boolean).join(' · ');
        lines.push([title, sub, formatRange(item.start, item.end)].filter(Boolean).join('｜'));
        if (item.details) lines.push(item.details);
      });
    }
    return { key, label, lines: lines.filter(Boolean) };
  });
}
