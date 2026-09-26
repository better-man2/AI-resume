<template>
  <div class="application-page">
    <div class="page-head">
      <el-button @click="goBack">返回上一页</el-button>
      <div class="page-title">求职进度</div>
      <el-button @click="router.push('/recommend')">去岗位匹配</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <!-- 状态统计 -->
    <div class="stats-row">
      <div v-for="item in STAT_CARDS" :key="item.key" class="stat-card">
        <b :style="{ color: item.color }">{{ stats[item.key] || 0 }}</b>
        <span>{{ item.label }}</span>
      </div>
    </div>

    <el-card v-if="list.length" class="card">
      <el-table :data="list" style="width: 100%">
        <el-table-column label="岗位" min-width="200">
          <template #default="{ row }">
            <div class="job-title">{{ row.jobTitle }}</div>
            <div class="job-sub">{{ row.companyName || '未填写公司' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="薪资 / 地点" min-width="150">
          <template #default="{ row }">
            <div>{{ row.salaryRange || '面议' }}</div>
            <div class="job-sub">{{ row.location || '地点不限' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="匹配分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.matchScore != null">{{ row.matchScore }}</span>
            <span v-else class="job-sub">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-select
              :model-value="row.status"
              size="small"
              style="width: 120px"
              @change="(value) => changeStatus(row, value)"
            >
              <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="面试时间" width="150">
          <template #default="{ row }">
            <span v-if="row.interviewAt">{{ formatTime(row.interviewAt) }}</span>
            <span v-else class="job-sub">未安排</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="job-sub">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else-if="!loading" description="还没有投递记录，去岗位匹配页把意向岗位加入进度吧">
      <el-button type="primary" @click="router.push('/recommend')">去岗位匹配</el-button>
    </el-empty>

    <!-- 编辑面试时间 / 备注 -->
    <el-dialog v-model="editDialog.visible" title="更新求职进度" width="520px">
      <el-form label-width="90px">
        <el-form-item label="岗位">
          <span>{{ editDialog.row?.jobTitle }}</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editDialog.status" style="width: 100%">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试时间">
          <el-date-picker
            v-model="editDialog.interviewAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm"
            placeholder="还没有面试安排可以留空"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="editDialog.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：已投递简历，等 HR 回复"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import axios from '../utils/axios-config';

const router = useRouter();
const userId = localStorage.getItem('userId');

const STATUS_OPTIONS = [
  { value: 'APPLIED', label: '已投递' },
  { value: 'INTERVIEW', label: '面试中' },
  { value: 'OFFER', label: '已录用' },
  { value: 'REJECTED', label: '已拒绝' },
];

const STAT_CARDS = [
  { key: 'applied', label: '已投递', color: '#3186cb' },
  { key: 'INTERVIEW', label: '面试中', color: '#e6a23c' },
  { key: 'OFFER', label: '已录用', color: '#67c23a' },
  { key: 'REJECTED', label: '已拒绝', color: '#909399' },
];

const loading = ref(false);
const saving = ref(false);
const list = ref([]);
const stats = ref({});
const editDialog = ref({ visible: false, row: null, status: 'APPLIED', interviewAt: '', remark: '' });

const goBack = () => router.back();

const formatTime = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const load = async () => {
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  try {
    loading.value = true;
    const { data } = await axios.get('/api/application/list', { params: { userId } });
    if (!data.success) throw new Error(data.error || '加载失败');
    list.value = data.list || [];
    // 后端返回的 applied 是状态码计数，这里统一成统计卡片的键
    stats.value = {
      applied: (data.stats && data.stats.APPLIED) || 0,
      INTERVIEW: (data.stats && data.stats.INTERVIEW) || 0,
      OFFER: (data.stats && data.stats.OFFER) || 0,
      REJECTED: (data.stats && data.stats.REJECTED) || 0,
    };
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || error?.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

const changeStatus = async (row, status) => {
  try {
    const { data } = await axios.post('/api/application/update', { id: row.id, userId, status });
    if (!data.success) throw new Error(data.error || '更新失败');
    row.status = status;
    ElMessage.success('状态已更新');
    load();
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || error?.message || '更新失败');
  }
};

const openEdit = (row) => {
  editDialog.value = {
    visible: true,
    row,
    status: row.status,
    interviewAt: row.interviewAt ? formatTime(row.interviewAt) : '',
    remark: row.remark || '',
  };
};

const saveEdit = async () => {
  try {
    saving.value = true;
    const { data } = await axios.post('/api/application/update', {
      id: editDialog.value.row.id,
      userId,
      status: editDialog.value.status,
      interviewAt: editDialog.value.interviewAt || '',
      remark: editDialog.value.remark,
    });
    if (!data.success) throw new Error(data.error || '保存失败');
    editDialog.value.visible = false;
    ElMessage.success('已保存');
    load();
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || error?.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const remove = (row) => {
  ElMessageBox.confirm(`确定把「${row.jobTitle}」从求职进度里删除吗？`, '删除记录', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      const { data } = await axios.delete(`/api/application/delete/${row.id}`, { params: { userId } });
      if (!data.success) throw new Error(data.error || '删除失败');
      ElMessage.success('已删除');
      load();
    })
    .catch(() => {});
};

onMounted(load);
</script>

<style scoped>
.application-page {
  min-height: 100vh;
  background: #f0f8ff;
  padding: 56px 20px 32px;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 1100px;
  margin: 0 auto 12px;
}

.page-title {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #3186cb;
}

.stats-row {
  display: flex;
  gap: 12px;
  max-width: 1100px;
  margin: 0 auto 12px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1 1 140px;
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.stat-card b {
  font-size: 22px;
  line-height: 1.2;
}

.stat-card span {
  color: #909399;
  font-size: 12px;
}

.card {
  max-width: 1100px;
  margin: 0 auto;
}

.job-title {
  font-weight: 600;
}

.job-sub {
  color: #909399;
  font-size: 12px;
}
</style>
