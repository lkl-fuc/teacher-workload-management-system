<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>岗位任务面板</span>
        <el-tag type="success" effect="dark">当前岗位：{{ teacherPost }}</el-tag>
      </div>
    </template>

    <el-alert
      :title="postDesc.title"
      :description="postDesc.description"
      type="info"
      show-icon
      :closable="false"
      class="mb-16"
    />

    <el-row :gutter="16" class="mb-16">
      <el-col v-for="metric in metrics" :key="metric.key" :span="8">
        <el-card shadow="hover" class="metric-card" @click="openMetric(metric.key)">
          <div class="metric-value">{{ metric.value }}</div>
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-tip">点击查看明细</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="displayTasks" border v-loading="loading">
      <el-table-column prop="category" label="任务大类" width="160" />
      <el-table-column prop="subTask" label="细分任务" min-width="220" />
      <el-table-column prop="deadline" label="截止日期" width="120" />
      <el-table-column prop="statusText" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.statusText)">{{ scope.row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="equivalentAmount" label="折算工作量" width="120" />
      <el-table-column prop="owner" label="协同对象" min-width="160" />
    </el-table>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="880px" destroy-on-close>
      <el-table :data="metricTaskRows" border>
        <el-table-column prop="subTask" label="任务" min-width="260" />
        <el-table-column prop="deadline" label="截止日期" width="120" />
        <el-table-column prop="statusText" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.statusText)">{{ scope.row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="原始分值" width="100" />
        <el-table-column prop="equivalentAmount" label="折算工作量" width="120" />
      </el-table>
      <el-empty v-if="metricTaskRows.length === 0" description="暂无任务" />
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const teacherPost = computed(() => localStorage.getItem('teacherPost') || '行政岗')

const postMap = {
  行政岗: {
    title: '行政岗以“流程执行 + 数据上报”为主',
    description: '重点关注值班、材料归档、考勤统计等流程型任务，可按周完成并提交记录。'
  },
  管理岗: {
    title: '管理岗以“统筹管理 + 审核监督”为主',
    description: '重点关注部门任务分派、过程监督、绩效审核，任务通常关联多人协同。'
  },
  教辅岗: {
    title: '教辅岗以“教学保障 + 资源服务”为主',
    description: '重点关注教室设备、实训资源、考试组织等教辅任务，强调及时响应。'
  }
}

const loading = ref(false)
const workloads = ref([])
const typeMap = ref(new Map())
const detailVisible = ref(false)
const currentMetric = ref('all')

const postDesc = computed(() => postMap[teacherPost.value] || postMap.行政岗)

const displayTasks = computed(() => buildTaskRows(workloads.value))

const metrics = computed(() => {
  const rows = displayTasks.value
  return [
    { key: 'todo', label: '待完成', value: rows.filter((item) => ['未开始', '进行中', '已分发'].includes(item.statusText)).length },
    { key: 'done', label: '已完成', value: rows.filter((item) => item.statusText === '已完成').length },
    { key: 'overdue', label: '逾期任务', value: rows.filter((item) => item.isOverdue).length }
  ]
})

const metricTaskRows = computed(() => {
  if (currentMetric.value === 'todo') {
    return displayTasks.value.filter((item) => ['未开始', '进行中', '已分发'].includes(item.statusText))
  }
  if (currentMetric.value === 'done') {
    return displayTasks.value.filter((item) => item.statusText === '已完成')
  }
  if (currentMetric.value === 'overdue') {
    return displayTasks.value.filter((item) => item.isOverdue)
  }
  return displayTasks.value
})

const detailTitle = computed(() => {
  if (currentMetric.value === 'todo') return '待完成任务明细'
  if (currentMetric.value === 'done') return '已完成任务明细'
  if (currentMetric.value === 'overdue') return '逾期任务明细'
  return '任务明细'
})

function resolveTeacherId() {
  const candidates = [localStorage.getItem('teacherId'), localStorage.getItem('userId'), localStorage.getItem('id')]
  for (const raw of candidates) {
    const id = Number(raw)
    if (!Number.isNaN(id) && id > 0) return id
  }
  return null
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })

  if (!response.ok) {
    let message = '请求失败'
    try {
      const errorData = await response.json()
      message = errorData.message || message
    } catch {
      // ignore
    }
    throw new Error(message)
  }

  return response.json()
}

async function loadData() {
  const teacherId = resolveTeacherId()
  if (!teacherId) {
    ElMessage.warning('未识别到教师身份，请重新登录')
    return
  }

  loading.value = true
  try {
    const [workloadData, typeData] = await Promise.all([
      request(`/api/workloads/teacher/${teacherId}`),
      request('/api/workload-types')
    ])
    workloads.value = Array.isArray(workloadData) ? workloadData : []
    typeMap.value = new Map((Array.isArray(typeData) ? typeData : []).map((item) => [Number(item.id), item]))
  } catch (error) {
    ElMessage.error(error?.message || '加载岗位任务失败')
  } finally {
    loading.value = false
  }
}

function openMetric(metric) {
  currentMetric.value = metric
  detailVisible.value = true
}

function buildTaskRows(rows) {
  const today = new Date().toISOString().slice(0, 10)
  return (Array.isArray(rows) ? rows : []).map((item) => {
    const type = typeMap.value.get(Number(item.typeId))
    const amount = Number(item.amount || 0)
    return {
      id: item.id,
      category: type?.categoryName || '岗位任务',
      subTask: item.workloadTitle || '-',
      deadline: item.submitDate || '-',
      statusText: statusText(item.status),
      owner: type?.subTypeName || '教研组',
      amount: amount.toFixed(2),
      equivalentAmount: calcEquivalent(amount, type).toFixed(2),
      isOverdue: Boolean(item.submitDate && item.submitDate < today && !['APPROVED', 'DONE', 'COMPLETED'].includes(String(item.status || '').toUpperCase()))
    }
  })
}

function calcEquivalent(amount, type) {
  const typeText = `${type?.typeName || ''}${type?.categoryName || ''}${type?.subTypeName || ''}`
  let weight = 1
  if (/教学|考试|实训/.test(typeText)) weight = 1.15
  if (/管理|审核|规划|行政/.test(typeText)) weight = 1.05
  if (/服务|支撑|保障/.test(typeText)) weight = 0.95
  return amount * weight
}

function statusText(status) {
  const value = String(status || '').toUpperCase()
  if (['APPROVED', 'DONE', 'COMPLETED'].includes(value)) return '已完成'
  if (value === 'ASSIGNED') return '已分发'
  if (['IN_PROGRESS', 'PENDING'].includes(value)) return '进行中'
  return '未开始'
}

function statusType(status) {
  if (status === '已完成') return 'success'
  if (status === '进行中' || status === '已分发') return 'warning'
  return 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-card {
  text-align: center;
  cursor: pointer;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: #1d4ed8;
}

.metric-label {
  margin-top: 6px;
  color: var(--text-secondary);
}

.metric-tip {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
