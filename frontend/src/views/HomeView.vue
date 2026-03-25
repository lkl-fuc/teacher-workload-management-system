<template>
  <div class="home-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-content">
        <div>
          <h1>教师工作量管理系统</h1>
          <p>
            面向学院教学管理场景，支持工作量录入、审核、统计与分析。
            当前界面为毕业设计展示优化版，强调简洁布局、统一配色与核心数据可视化。
          </p>
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
            <div class="section-title">系统简介</div>
          </template>
          <ul class="intro-list">
            <li>覆盖教师工作量填报、审核流转、统计报表三大业务环节。</li>
            <li>按“录入-审核-归档”管理流程设计，支持教学管理标准化。</li>
            <li>提供按教师、类型、月份的统计视图，辅助管理决策。</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-title">展示亮点</div>
          </template>
          <div class="tag-wrap">
            <el-tag effect="plain" type="primary">统一蓝色主题</el-tag>
            <el-tag effect="plain" type="success">数据卡片总览</el-tag>
            <el-tag effect="plain" type="warning">响应式布局</el-tag>
            <el-tag effect="plain" type="info">管理流程清晰</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const workloads = ref([])
const teachers = ref([])
const workloadTypes = ref([])

const metricCards = computed(() => {
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
    const [workloadData, teacherData, typeData] = await Promise.all([
      request('/api/workloads'),
      request('/api/teachers'),
      request('/api/workload-types')
    ])

    workloads.value = Array.isArray(workloadData) ? workloadData : []
    teachers.value = Array.isArray(teacherData) ? teacherData : []
    workloadTypes.value = Array.isArray(typeData) ? typeData : []
  } catch (error) {
    ElMessage.warning(error.message || '首页数据加载失败')
  }
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
</style>
