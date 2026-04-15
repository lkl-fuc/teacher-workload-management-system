<template>
  <div class="home-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-content">
        <div>
          <h1>{{ heroTitle }}</h1>
          <p>{{ heroDesc }}</p>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="metrics-row">
      <el-col v-for="item in metricCards" :key="item.label" :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="intro-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-title">{{ introTitle }}</div>
          </template>
          <ul class="intro-list">
            <li v-for="item in introList" :key="item">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-title">{{ rightTitle }}</div>
          </template>
          <div class="tag-wrap" v-if="!isTeacher">
            <el-tag effect="plain" type="primary">统一蓝色主题</el-tag>
            <el-tag effect="plain" type="success">数据卡片总览</el-tag>
            <el-tag effect="plain" type="warning">响应式布局</el-tag>
            <el-tag effect="plain" type="info">管理流程清晰</el-tag>
          </div>
          <div v-else class="teacher-actions">
            <el-button type="primary" plain @click="goTo('/workloads/create')">填报工作量</el-button>
            <el-button type="success" plain @click="goTo('/teacher/post-tasks')">查看岗位任务</el-button>
            <el-button type="warning" plain @click="goTo('/teacher/notices')">查看预警通知</el-button>
          </div>
          <div v-if="isTeacher" class="focus-list">
            <div v-for="item in teacherFocusList" :key="item" class="focus-item">• {{ item }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const workloads = ref([])
const teachers = ref([])
const workloadTypes = ref([])
const router = useRouter()
const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())
const isTeacher = computed(() => role.value === 'TEACHER')
const teacherPost = computed(() => localStorage.getItem('teacherPost') || '行政岗')
const teacherName = computed(() => localStorage.getItem('name') || '老师')

const teacherFocusByPost = {
  行政岗: ['完善值班与巡检记录', '关注本周待处理通知', '按时提交流程型工作量'],
  管理岗: ['跟进分派任务进度', '复核关键工作量记录', '汇总团队周度完成情况'],
  教辅岗: ['保障教学资源与设备', '跟踪教辅任务响应时效', '归档服务型工作量证明']
}

const teacherFocusList = computed(() => teacherFocusByPost[teacherPost.value] || teacherFocusByPost.行政岗)

const heroTitle = computed(() =>
  isTeacher.value ? `${teacherName.value}，欢迎进入您的工作台` : '教师工作量管理系统'
)

const heroDesc = computed(() =>
  isTeacher.value
    ? `当前岗位：${teacherPost.value}。这里为您展示个人填报进展、审核状态与岗位关注事项，帮助您高效完成本岗位任务。`
    : '面向学院教学管理场景，支持工作量录入、审核、统计与分析。当前界面为毕业设计展示优化版，强调简洁布局、统一配色与核心数据可视化。'
)

const introTitle = computed(() => (isTeacher.value ? '岗位工作画像' : '系统简介'))
const rightTitle = computed(() => (isTeacher.value ? '快捷入口与本周关注' : '展示亮点'))

const introList = computed(() => {
  if (isTeacher.value) {
    const postText = teacherPost.value
    return [
      `您当前为${postText}，首页内容已按岗位职责个性化展示。`,
      '聚焦个人工作量填报、审核结果与任务截止时间，不再展示管理员全局统计。',
      '可通过右侧快捷入口直达岗位任务面板、通知公告与工作量填报页面。'
    ]
  }
  return [
    '覆盖教师工作量填报、审核流转、统计报表三大业务环节。',
    '按“录入-审核-归档”管理流程设计，支持教学管理标准化。',
    '提供按教师、类型、月份的统计视图，辅助管理决策。'
  ]
})

const metricCards = computed(() => {
  if (isTeacher.value) {
    const totalAmount = workloads.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    const approvedCount = workloads.value.filter((item) =>
      ['APPROVED', 'DONE', 'COMPLETED'].includes(String(item.status || '').toUpperCase())
    ).length
    const pendingCount = workloads.value.filter((item) =>
      ['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(String(item.status || '').toUpperCase())
    ).length
    const thisMonth = new Date().toISOString().slice(0, 7)
    const thisMonthCount = workloads.value.filter((item) => String(item.submitDate || '').startsWith(thisMonth)).length
    return [
      { label: '本月提交', value: thisMonthCount, desc: '本月新增工作量记录' },
      { label: '待处理', value: pendingCount, desc: '待审核或进行中的任务' },
      { label: '已通过', value: approvedCount, desc: '已完成审核的记录数' },
      { label: '个人总工作量', value: totalAmount.toFixed(2), desc: `岗位：${teacherPost.value}` }
    ]
  }

  const totalAmount = workloads.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)
  const pendingCount = workloads.value.filter((item) => item.status === 'PENDING').length

  return [
    { label: '教师总数', value: teachers.value.length, desc: '系统内已登记教师' },
    { label: '工作量类型', value: workloadTypes.value.length, desc: '已配置类型数量' },
    { label: '工作量记录', value: workloads.value.length, desc: '累计提交记录数' },
    { label: '总工作量', value: totalAmount.toFixed(2), desc: `待审核 ${pendingCount} 条` }
  ]
})

async function request(url, options = {}) {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message = typeof data === 'string' ? data : data?.message
    throw new Error(message || '请求失败')
  }

  return data
}

async function loadSummary() {
  try {
    if (isTeacher.value) {
      const teacherId = Number(localStorage.getItem('teacherId') || localStorage.getItem('userId') || localStorage.getItem('id'))
      if (!teacherId) {
        ElMessage.warning('未识别到教师身份，请重新登录')
        return
      }
      const [workloadData, typeData] = await Promise.all([
        request(`/api/workloads/teacher/${teacherId}`),
        request('/api/workload-types')
      ])
      workloads.value = Array.isArray(workloadData) ? workloadData : []
      workloadTypes.value = Array.isArray(typeData) ? typeData : []
      teachers.value = []
      return
    }

    const [workloadData, teacherData, typeData] = await Promise.all([request('/api/workloads'), request('/api/teachers'), request('/api/workload-types')])

    workloads.value = Array.isArray(workloadData) ? workloadData : []
    teachers.value = Array.isArray(teacherData) ? teacherData : []
    workloadTypes.value = Array.isArray(typeData) ? typeData : []
  } catch (error) {
    ElMessage.warning(error.message || '首页数据加载失败')
  }
}

function goTo(path) {
  router.push(path)
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card {
  border-radius: 14px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  background: linear-gradient(120deg, rgba(59, 130, 246, 0.12) 0%, rgba(14, 165, 233, 0.08) 100%);
}

.hero-content h1 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.hero-content p {
  margin: 10px 0 0;
  max-width: 780px;
  color: #475569;
  line-height: 1.75;
}

.metrics-row {
  margin-top: 2px;
}

.metric-card {
  border-radius: 12px;
}

.metric-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  color: #1d4ed8;
}

.metric-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.section-card {
  border-radius: 12px;
  min-height: 220px;
}

.section-title {
  font-weight: 600;
  color: #0f172a;
}

.intro-list {
  margin: 0;
  padding-left: 20px;
  color: #475569;
  line-height: 1.9;
}

.tag-wrap {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.teacher-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.focus-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #475569;
  font-size: 14px;
}

.focus-item {
  line-height: 1.6;
}
</style>
