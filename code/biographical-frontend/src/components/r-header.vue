<template>
  <div class="header-container">
    <div class="header-left">
      欢迎登录AI职场助手！
    </div>

    <el-button type="primary" plain @click="showMyResume" class="action-button" style="margin-right: 10px">
      我的简历
    </el-button>
    <el-button type="primary" plain @click="startResumeAi" class="action-button" style="margin-right: 10px">
      简历AI
    </el-button>

    <!-- 根据 username 判断是否显示按钮 -->
    <el-button
      v-if="isAdmin"
      type="success"
      plain
      @click="clickJobPool"
    >
    职位库管理
    </el-button>

    <el-button
      v-if="!isAdmin"
      type="success"
      plain
      @click="clickRecommend"
    >
    推荐职位
    </el-button>

    <el-button
      v-if="!isAdmin"
      type="warning"
      plain
      @click="clickApplications"
    >
    求职进度
    </el-button>

    <el-popconfirm
      confirmButtonText="好的"
      cancelButtonText="不用了"
      icon="el-icon-info"
      icon-color="red"
      title="确定退出登录吗？"
      @confirm="handleLogout"
    >
      <template #reference>
        <el-button type="danger" class="logout-button" round>
          退出登录
        </el-button>
      </template>
    </el-popconfirm>
  </div>
</template>

<script>
export default {
  name: 'r-header',
  data() {
    return {
      username: null,
      isAdmin: false,
    };
  },
  created() {
    this.username = localStorage.getItem('username');
    // 如果 username 为管理员，显示职位库管理按钮
    if (this.username === '管理员') {
      this.isAdmin = true;
    }
  },
  methods: {
    clickJobPool() {
      this.$router.push('/jobPool');
    },
    clickRecommend() {
      this.$router.push('/recommend');
    },
    clickApplications() {
      this.$router.push('/applications');
    },
    handleLogout() {
      localStorage.clear();
      this.$router.push('/login');
    },
    showMyResume() {
      // 清除本地存储的生成结果，以便加载数据库里保存的最新简历
      localStorage.removeItem('resumeData');
      this.$router.push('/resume');
    },
    // 进入分步表单：选模板 → 填写基础信息 → 一键生成
    startResumeAi() {
      this.$router.push('/resume/create');
    },
  },
};
</script>

<style scoped>
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #3186cb; /* 修改为更柔和的深色背景 */
  color: #ecf0f1; /* 调整文字颜色 */
  height: 40px; /* 固定高度 */
}
.header-nav {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 20px; /* 增加元素间距 */
  min-height: 40px; /* 新增最小高度 */
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px; /* 欢迎信息与导航按钮间距 */
  flex-grow: 1; /* 左侧自适应宽度 */
}

.welcome-text {
  font-size: 18px;
  font-weight: 500;
}

.nav-button {
  padding: 8px 12px !important; /* 调整按钮大小 */
  border-radius: 4px; /* 圆角按钮 */
  transition: all 0.3s; /* 添加过渡效果 */
  height: 32px; /* 统一按钮高度 */
}

.nav-button:hover {
  background: rgba(255, 255, 255, 0.05); /* 悬停效果 */
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  min-height: 40px; /* 新增最小高度 */
}

.action-button {
  padding: 8px 15px !important;
  border-radius: 20px;
}

.logout-button {
  padding: 8px 15px !important;
  border-radius: 4px; /* 移除圆形样式 */
  box-shadow: 0 2px 4px rgba(255, 69, 89, 0.3); /* 添加微阴影 */
}

.version-select {
  margin-bottom: 20px;
  text-align: center;
}
.resume-form {
  margin-top: 10px;
}

/* 新增响应式调整 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 10px;
  }

  .nav-button {
    padding: 6px 10px !important;
  }
}

.generating-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 20px;
}

.status-text {
  margin: 0;
  text-align: center;
  color: #606266;
  font-size: 16px;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: center;
}

/* 添加过渡效果 */
.el-dialog {
  transition: all 0.3s ease-in-out;
}

.el-progress {
  transition: all 0.5s ease-in-out;
}

/* 添加加载动画样式 */
:deep(.el-progress-circle) {
  width: 120px !important;
  height: 120px !important;
}

:deep(.el-progress__text) {
  display: none;
}

:deep(.el-progress-circle__track) {
  stroke: #e5e9f2;
}

:deep(.el-progress-circle__path) {
  stroke: #409EFF;
  animation: loading-rotate 2s linear infinite;
}

@keyframes loading-rotate {
  100% {
    transform: rotate(360deg);
  }
}
</style>
