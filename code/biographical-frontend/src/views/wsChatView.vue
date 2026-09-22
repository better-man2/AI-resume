<template>
  <div class="app-container">
    <!-- 改进后的侧边栏 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- 可点击区域（仅标题） -->
      <div class="sidebar-header" @click.stop="toggleSidebar">
        <h3 class="logo">AI职场助手</h3>
        <div class="toggle-btn" @click.stop="toggleSidebar">
          {{ isCollapsed ? '>>' : '<<' }}
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="sidebar-content" v-if="!isCollapsed">
        <div class="nav-section">
          <h4>会话记录</h4>
          <div v-if="historyList.length === 0" class="no-history">
            暂无历史记录
          </div>
          <ul v-else class="history-list">
            <li v-for="history in historyList"
                :key="history.id"
                class="nav-item"
                @click="showHistoryDetail(history.id)">
              <div class="history-item">
                <span class="history-title">{{ history.question ? history.question.substring(0, 30) + '...' : '无内容' }}</span>
                <span class="history-time">{{ formatTime(history.time) }}</span>
              </div>
            </li>
          </ul>
        </div>

        <!-- 底部操作按钮 -->
        <div class="action-section">
          <el-popover
              placement="top-start"
              width="200"
              trigger="click"
              v-model:visible="profilePopoverVisible"
          >
            <div style="display: flex; flex-direction: column; gap: 10px;">
              <el-button type="text" @click="openSettingsDialog">系统设置</el-button>
              <el-button type="text" @click="openContactDialog">联系我们</el-button>
              <el-button type="text" @click="logout">退出登录</el-button>
            </div>

            <template #reference>
              <el-button type="primary" plain class="nav-btn">
                <el-icon><User /></el-icon>
                个人信息
              </el-button>
            </template>
          </el-popover>

          <!-- 系统设置弹窗 -->
          <el-dialog v-model="settingsDialogVisible" title="系统设置" width="30%">
            <el-radio-group v-model="theme" @change="changeTheme">
              <el-radio-button label="light">浅色模式</el-radio-button>
              <el-radio-button label="dark">深色模式</el-radio-button>
            </el-radio-group>
          </el-dialog>

          <!-- 联系我们弹窗 -->
          <el-dialog v-model="contactDialogVisible" title="联系我们" width="30%">
            <p>邮箱: example@email.com</p>
            <p>电话: 123-456-7890</p>
          </el-dialog>
        </div>
      </div>
    </div>

    <!-- 聊天主界面 -->
    <div class="chat-view">
      <div class="chat-panel">
        <!-- 消息记录面板 -->
        <div class="message-panel">
          <div class="message-list">
            <!-- 欢迎信息（条件渲染） -->
            <div v-if="messageList.length === 0" class="welcome-message">
              Hi~ 我是小枝 你身边的智能助手，可以为你答疑解惑、精读文档、尽情创作 让小枝助你轻松工作，多点生活
            </div>
            <!-- 消息循环列表 -->
            <div
              v-for="(message, index) in messageList"
              :key="index"
              :class="['message-row', message.sender]"
            >
              <div v-if="message.sender === 'ai'" class="message-avatar">
                <img :src="message.avatar" alt="Avatar" class="avatar" />
              </div>
              <div :class="['message', message.sender === 'user' ? 'user-message' : 'ai-message']">
                {{ message.text }}
                <!-- AI 按约定的 JSON 输出简历内容时，允许一键写回简历 -->
                <div v-if="message.resumePayload" class="apply-row">
                  <template v-if="!message.applied">
                    <el-button
                      size="small"
                      type="primary"
                      plain
                      :loading="applying"
                      @click="applyResume(message)"
                    >
                      应用到简历
                    </el-button>
                    <span class="apply-hint">检测到简历描述内容，可写入「我的简历」（不会覆盖已填的名称与时间）</span>
                  </template>
                  <template v-else>
                    <el-button size="small" type="success" plain @click="router.push('/resume')">
                      打开简历编辑页
                    </el-button>
                    <span class="apply-hint">已写入，可继续让 AI 补充其它经历</span>
                  </template>
                </div>
              </div>
              <div v-if="message.sender === 'user'" class="message-avatar">
                <img :src="message.avatar" alt="Avatar" class="avatar" />
              </div>
            </div>
          </div>

          <!-- 输入框和发送按钮 -->
          <div class="message-input">
            <el-button type="success" plain @click="clickHistory">历史记录</el-button>
            <el-input
              v-model="inputMessage"
              placeholder="开始和我聊天吧..."
              @keyup.enter="sendMessage"
              :loading="loading"
            />
            <el-button :loading="loading" @click="sendMessage" type="primary">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, nextTick, onMounted, onBeforeUnmount, computed} from 'vue';
import { ElButton, ElInput, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import axios from 'axios';
import dayjs from 'dayjs';
import 'element-plus/dist/index.css';
import { parseAiJsonObject } from '../utils/resume-model';

const router = useRouter();
const messageList = ref([]); // 消息列表
const inputMessage = ref(''); // 输入框内容
const loading = ref(false); // 加载状态
const isCollapsed = ref(false); // 侧边栏默认保持展开状态
const toggleSidebar = () => isCollapsed.value = !isCollapsed.value;
const historyList = ref([]);
const currentHistory = ref(null);
const applying = ref(false);

// 用户和AI的头像
const userAvatar = 'src/assets/user.jpg'; // 用户头像链接
const aiAvatar = 'src/assets/ai.jpg'; // AI头像链接

let socket = null; // WebSocket对象

const clickHistory = () => {
    router.push('/history')
  }


const clickProfile = () => router.push('/profile');

/**
 * 从 AI 回复里提取简历描述 JSON（与 /api/resume/generate 的输出结构一致）。
 * 只有解析成功且确实包含描述内容时才返回，避免误判普通回答。
 */
const extractResumePayload = (text) => {
  const parsed = parseAiJsonObject(text);
  if (!parsed) return null;
  const hasEvaluation = Boolean(parsed.self_evaluation || parsed.selfEvaluation);
  const hasSummary = Boolean(parsed.profession && parsed.profession.summary);
  const hasDetails = ['projects', 'internships', 'works'].some(
    (key) => Array.isArray(parsed[key]) && parsed[key].some((item) => item && item.details),
  );
  return hasEvaluation || hasSummary || hasDetails ? parsed : null;
};

/** 把对话产出的描述写入简历（后端只补描述，不动基础信息与已填事实） */
const applyResume = async (message) => {
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  try {
    applying.value = true;
    const { data } = await axios.post('/api/resume/apply', {
      userId,
      content: message.resumePayload,
    });
    if (!data.success) {
      throw new Error(data.error || '写入失败');
    }
    message.applied = true;
    ElMessage.success(data.message || '已写入简历');
    const added = data.applied
      ? (data.applied.projectAdded || 0) + (data.applied.internshipAdded || 0) + (data.applied.workAdded || 0)
      : 0;
    if (added > 0) {
      ElMessage.info('新增的条目目前只有描述，请到简历编辑页补全名称与时间');
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || error?.message || '写入失败');
  } finally {
    applying.value = false;
  }
};

// 从 localStorage 获取用户信息
const userId = localStorage.getItem('userId');
const username = localStorage.getItem('username');
const parentEmail = localStorage.getItem('parentEmail');

// 发送消息到 WebSocket
const sendMessage = () => {
  if (inputMessage.value.trim() === '') return; // 输入为空时不发送消息

  // 将用户消息加入消息列表
  messageList.value.push({
    text: inputMessage.value,
    sender: 'user',
    avatar: userAvatar,
  });

  const userMessage = inputMessage.value; // 保存用户输入内容
  inputMessage.value = ''; // 清空输入框

  // 初始化AI消息（用于动态更新）
  const aiMessage = {
    text: '',
    sender: 'ai',
    avatar: aiAvatar,
  };
  messageList.value.push(aiMessage);

  loading.value = true;

  // 发送用户消息到 WebSocket 服务器，同时携带 userId 和 username
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({
      type: 'message',
      text: userMessage,
      userId: userId,
      username: username
    }));
  } else {
    ElMessage.error('WebSocket 连接未建立');
  }
};

// WebSocket连接及接收处理
const setupWebSocket = () => {
  socket = new WebSocket('ws://localhost:9090/ws/chat'); // 根据需要修改WebSocket服务器地址

  socket.onopen = () => {
    console.log('WebSocket 已连接');
  };

  socket.onmessage = (event) => {
    try {
      let message = event.data;
      // 检查消息是否包含 "data:" 前缀并去除它
      if (message.startsWith("data:")) {
        message = message.substring(5); // 移除 "data:" 前缀
        if (message.includes("[DONE]")) {
          loading.value = false; // 停止加载状态
          // 流结束：检查这次回复是不是简历描述 JSON，是则给出「应用到简历」入口
          const finished = messageList.value[messageList.value.length - 1];
          if (finished && finished.sender === 'ai') {
            finished.resumePayload = extractResumePayload(finished.text);
          }
          console.log('AI 流已完成');
          return; // 结束处理
        }
        // 找到最新的 AI 消息并更新其文本内容
        // 注意：后端每个分片都带 "data:" 前缀和 "\n\n" 分隔符，累加时要剥掉分隔符，
        // 否则 JSON 输出的 key 会被换行拆断（导致无法解析、无法回填简历）
        const aiMessage = message.replace(/\n{2}$/, '');
        const latestMessage = messageList.value[messageList.value.length - 1];
        if (latestMessage && latestMessage.sender === 'ai') {
          latestMessage.text += aiMessage; // 累积更新 AI 回复
          nextTick(); // 强制 DOM 更新
        }
      }
    } catch (error) {
      console.error('处理 WebSocket 消息时出错:', error);
    }
  };

  socket.onerror = (error) => {
    console.error('WebSocket 错误:', error);
    ElMessage.error('WebSocket 连接出错');
  };

  socket.onclose = (event) => {
    if (event.wasClean) {
      console.log('WebSocket 连接关闭');
    } else {
      console.error('WebSocket 连接异常关闭');
    }
  };
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

// 加载历史记录列表
const loadHistoryList = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  try {
    console.log('开始加载历史记录...');
    const response = await axios.get('/api/history/list', {
      params: {
        userId,
        pageNum: 1,
        pageSize: 10
      }
    });
    console.log('历史记录数据:', response.data);
    if (response.data && response.data.records) {
      historyList.value = response.data.records;
    }
  } catch (error) {
    console.error('加载历史记录失败:', error);
    ElMessage.error('加载历史记录失败: ' + (error.response?.data?.message || error.message));
  }
};

// 显示历史记录详情
const showHistoryDetail = async (historyId) => {
  try {
    console.log('加载历史记录详情:', historyId);
    const response = await axios.get(`/api/history/detail/${historyId}`);
    const history = response.data;

    // 清空当前消息列表
    messageList.value = [];

    // 添加历史对话到消息列表
    messageList.value.push({
      text: history.question,
      sender: 'user',
      avatar: userAvatar
    });

    messageList.value.push({
      text: history.result,
      sender: 'ai',
      avatar: aiAvatar
    });

    currentHistory.value = history;
  } catch (error) {
    console.error('加载对话详情失败:', error);
    ElMessage.error('加载对话详情失败: ' + (error.response?.data?.message || error.message));
  }
};

onMounted(async () => {
  console.log('组件挂载，准备加载历史记录...');
  await loadHistoryList();
  setupWebSocket();
});

onBeforeUnmount(() => {
  if (socket) {
    socket.close(); // 在组件卸载时关闭 WebSocket 连接
  }
});

// 新增计算属性用于布局
const sidebarWidth = computed(() =>
    isCollapsed.value ? '60px' : '240px'
);

const profilePopoverVisible = ref(false);
const settingsDialogVisible = ref(false);
const contactDialogVisible = ref(false);
const theme = ref('light'); // 默认浅色

const openSettingsDialog = () => {
  profilePopoverVisible.value = false;
  settingsDialogVisible.value = true;
};

const openContactDialog = () => {
  profilePopoverVisible.value = false;
  contactDialogVisible.value = true;
};

const changeTheme = (val) => {
  if (val === 'dark') {
    document.documentElement.setAttribute('class', 'dark');
  } else {
    document.documentElement.removeAttribute('class');
  }
};

const logout = () => {
  localStorage.clear();
  router.push('/login'); // 跳转到登录页
};

</script>



<style scoped>
:root {
  --bg-color: #ffffff;
  --text-color: #88888f;
  --sidebar-bg: #e3f2fd;
  --border-color: #bbdefb;
  --card-bg: #ffffff;
}

.dark {
  --bg-color: #2d2d2d;        /* 更柔和的深色背景 */
  --text-color: #e0e0e0;      /* 提高文字对比度 */
  --sidebar-bg: #333333;      /* 侧边栏背景 */
  --border-color: #444444;    /* 边框颜色 */
  --card-bg: #3a3a3a;         /* 卡片背景 */
}

body {
  background-color: var(--bg-color);
  color: var(--text-color);
}

.dark .chat-view,
.dark .sidebar,
.dark .message-panel {
  background-color: var(--sidebar-bg) !important; /* 统一使用侧边栏背景色 */
}

.dark .user-message {
  background-color: #444444;   /* 较浅的深色背景 */
  color: #e0e0e0;
  border-bottom-right-radius: 8px;
}

.dark .ai-message {
  background-color: #333333;   /* 更柔和的AI消息背景 */
  color: #e0e0e0;
  border-bottom-left-radius: 8px;
}

.dark .el-dialog {
  background-color: #2d2d2d !important; /* 弹窗背景 */
  color: #e0e0e0;
  border: 1px solid #444444;
}

.el-dialog {
  background-color: var(--card-bg) !important;
  color: var(--text-color);
}

/* 联系弹窗样式 */
.contact-info p {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}

.app-container {  display: flex;
  height: 100vh;
  gap: 0; /* 移除默认间隙 */
  background-color: var(--bg-color);
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
}

.sidebar {
  width: v-bind(sidebarWidth);
  background: #e3f2fd; /* 浅蓝色背景 */
  border-right: none; /* 移除右边框 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar.collapsed {
  width: 60px;
  background: var(--sidebar-bg);
}

.sidebar-header {
  padding: 16px;
  background: #3186cb; /* 深蓝色头部 */
  cursor: pointer;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.logo {
  margin: 0;
  font-size: 1.2em;
  font-weight: 600;
  user-select: none; /* 防止文字选中 */
}

.toggle-btn {
  cursor: pointer;
  font-size: 20px;
  padding: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  transition: all 0.3s;
}

.sidebar-content {
  margin-top: 10px;
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  transition: opacity 0.3s;
}

.sidebar.collapsed .sidebar-content{
  opacity: 0;
  transform: translateX(-20px);
  pointer-events: none;
}

.sidebar.collapsed .toggle-btn{
  transform: rotate(90deg);
}

/* 保留展开时的历史记录样式 */
.sidebar:not(.collapsed) .history-list {
  max-height: 200px;
  overflow-y: auto;
}

/* 收起时隐藏非必要元素 */
.sidebar.collapsed .user-message,
.sidebar.collapsed .ai-message {
  display: none;
}

/* 弹窗样式调整 */
.el-dialog {
  background-color: var(--card-bg) !important;
  color: var(--text-color);
}

.nav-section {
  margin-bottom: 24px;
}

.history-list {
  list-style: none;
  padding: 0;
  margin: 0;
  min-height: 400px;
  overflow-y: auto;
}

.nav-item {
  padding: 12px;
  margin: 4px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

.nav-item:hover {
  background: rgba(255,255,255,0.2);
}

.action-section {
  padding: 16px;
  border-top: 1px solid #bbdefb;
}

/* 按钮对齐优化 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nav-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
}

.welcome-message {
  text-align: center;
  padding: 20px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 15px;
  color: #666;
  font-size: 1.2em;
  position: relative;
  z-index: 1; /* 确保在最上层显示 */
}

.chat-view {
  margin-left: 0;
  flex: 1;
  transition: margin-left 0.3s;
  background: #e3f2fd;
}

.avatar {
  width: 48px;
  height: 48px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* 消息气泡优化 */
.user-message {
  border-bottom-right-radius: 4px;
  background-color: #d3d4ee;
  color: var(--text-color);
}

.ai-message {
  border-bottom-left-radius: 4px;
  background-color: #cbd4f1;
  color: var(--text-color);
}

.chat-panel {
  width: 100%;
  height: 85%;
  max-height: calc(100vw - 60px); /* 侧边栏最小宽度时适配 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background-color: var(--bg-color);
}

.message-panel {
  display: flex;
  flex-direction: column;
  padding: 10px;
}

.message-list {
  overflow-y: auto;
  max-height: 500px;  /* 设置消息列表最大高度，限制面板的高度 */
  min-height: 500px;
  min-width: 500px;
  flex: 1;
  padding: 10px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
  justify-content: space-between;  /* Space between message elements */
}

.message-avatar {
  margin-right: 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.message {
  max-width: 70%;
  padding: 10px;
  border-radius: 10px;
  font-size: 16px;
}

.user-message {
  background-color: #e1f7d5;
  margin-left: auto; /* Align to the right for user */
}

.ai-message {
  background-color: #f0f0f0;
}

/* AI 输出简历描述 JSON 时的「应用到简历」操作区 */
.apply-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #d9d9d9;
}

.apply-hint {
  font-size: 12px;
  color: #909399;
}

.message-input {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: rgba(255, 255, 255, 0.1);   /* 输入框背景 */
  border: 1px solid #d3d4ee;
}

.el-input {
  flex: 1;
  margin-right: 10px;
}

.message-row.user {
  justify-content: flex-end;
}

.el-input__inner {
  background-color: #444444 !important; /* 输入框文字背景 */
  color: #e0e0e0 !important;
  border: 1px solid #444444 !important;
}

.message-row.ai {
  justify-content: flex-start;
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.1);
  margin-bottom: 8px;
}

.history-title {
  font-size: 14px;
  color: var(--text-color);
  word-break: break-all;
}

.history-time {
  font-size: 12px;
  color: #999;
}

.nav-item {
  cursor: pointer;
  transition: transform 0.2s;
}

.nav-item:hover {
  transform: translateX(4px);
  background-color: rgba(0, 0, 0, 0.05);
}

.no-history {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}
</style>

