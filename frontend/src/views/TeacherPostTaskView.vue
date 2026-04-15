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

const teacherPost = computed(() => localStorage.getItem('teacherPost') || '专任教师岗')

const postMap = {
  专任教师岗: {
    title: '专任教师岗以“授课 + 教学 + 教研”为主',
    description: '每周重点填报课堂教学、教学改进、教研活动等任务，提交后由管理员按岗位规则自动折算工作量。'
  },
  实验教师岗: {
    title: '实验教师岗以“实验教学 + 实验室指导”为主',
    description: '重点填报实验课组织、实验指导与实验室安全管理任务，系统按实验岗位系数自动核算。'
  },
  辅导员岗: {
    title: '辅导员岗以“学生管理 + 思想教育 + 日常事务”为主',
    description: '重点填报学生事务、谈心谈话、主题教育活动及突发事件处置等任务。'
  },
  教辅岗: {
    title: '教辅岗以“教学保障 + 资源服务”为主',
    description: '重点填报图书与设备服务、教学秘书支持、资源调配等保障性任务。'
  },
  行政兼课岗: {
    title: '行政兼课岗以“行政管理 + 部分教学任务”为主',
    description: '需同时填报行政工作与兼课任务，系统将根据任务类型分别折算并汇总。'
  },
  外聘教师岗: {
    title: '外聘教师岗以“课程授课 + 指导答疑”为主',
    description: '重点填报校外聘任课程授课、答疑与考核指导任务，系统按外聘岗位规则折算。'
  },
}

const loading = ref(false)
const workloads = ref([])
const typeMap = ref(new Map())
const detailVisible = ref(false)
const currentMetric = ref('all')

const postDesc = computed(() => postMap[teacherPost.value] || postMap.专任教师岗)

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
  let weight = resolvePostWeight()
  if (/教学|考试|实训/.test(typeText)) weight = 1.15
  if (/实验|实验室|指导/.test(typeText)) weight += 0.08
  if (/学生|思政|班会|辅导/.test(typeText)) weight += 0.06
  if (/行政|管理|事务/.test(typeText)) weight += 0.04
  if (/图书|设备|教辅|秘书|保障/.test(typeText)) weight += 0.03
  return amount * weight
}

function resolvePostWeight() {
  if (teacherPost.value.includes('专任教师')) return 1.12
  if (teacherPost.value.includes('实验教师')) return 1.1
  if (teacherPost.value.includes('辅导员')) return 1.06
  if (teacherPost.value.includes('教辅')) return 1.02
  if (teacherPost.value.includes('行政兼课')) return 1.08
  if (teacherPost.value.includes('外聘')) return 1.04
  return 1
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
