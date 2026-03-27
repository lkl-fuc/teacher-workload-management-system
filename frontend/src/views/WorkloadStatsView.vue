<template>
  <div class="stats-page">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="年份">
          <el-date-picker
            v-model="selectedYear"
            type="year"
            value-format="YYYY"
            placeholder="选择年份"
            style="width: 140px"
            @change="refreshCharts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refreshCharts">刷新统计</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="8">
        <el-card>
          <div class="summary-item">
            <div class="summary-label">总记录数</div>
            <div class="summary-value">{{ statsSummary.totalCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card>
          <div class="summary-item">
            <div class="summary-label">总工作量</div>
            <div class="summary-value">{{ statsSummary.totalAmount.toFixed(2) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card>
          <div class="summary-item">
            <div class="summary-label">筛选年份</div>
            <div class="summary-value">{{ selectedYear || '全部' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="isAdmin" :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>按教师统计（柱状图）</template>
          <div ref="teacherBarRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>按教师统计（饼图）</template>
          <div ref="teacherPieRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>按类型统计（柱状图）</template>
          <div ref="typeBarRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>按类型统计（饼图）</template>
          <div ref="typePieRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>按月份统计（柱状图）</template>
          <div ref="monthBarRef" class="chart chart-lg"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const selectedYear = ref(new Date().getFullYear().toString())
const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())
const isAdmin = computed(() => role.value === 'ADMIN')

const workloads = ref([])
const teachers = ref([])
const workloadTypes = ref([])

const teacherBarRef = ref(null)
const teacherPieRef = ref(null)
const typeBarRef = ref(null)
const typePieRef = ref(null)
const monthBarRef = ref(null)

let teacherBarChart
let teacherPieChart
let typeBarChart
let typePieChart
let monthBarChart
let echartsLib

const teacherMap = computed(() => {
  const map = new Map()
  teachers.value.forEach((item) => {
    map.set(item.id, item.name)
  })
  return map
})

const typeMap = computed(() => {
  const map = new Map()
  workloadTypes.value.forEach((item) => {
    map.set(item.id, item.typeName)
  })
  return map
})

const filteredWorkloads = computed(() => {
  if (!selectedYear.value) {
    return workloads.value
  }
  return workloads.value.filter((item) => item.submitDate?.startsWith(selectedYear.value))
})

const teacherStats = computed(() => aggregateStats(filteredWorkloads.value, 'teacherId', teacherMap.value, '未知教师'))
const typeStats = computed(() => aggregateStats(filteredWorkloads.value, 'typeId', typeMap.value, '清空'))

const monthStats = computed(() => {
  const result = Array.from({ length: 12 }, (_, index) => ({
    name: `${index + 1}月`,
    value: 0
  }))

  filteredWorkloads.value.forEach((item) => {
    if (!item.submitDate) return
    const month = Number(item.submitDate.split('-')[1])
    if (month >= 1 && month <= 12) {
      result[month - 1].value += Number(item.amount || 0)
    }
  })

  return result
})

const statsSummary = computed(() => ({
  totalCount: filteredWorkloads.value.length,
  totalAmount: filteredWorkloads.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)
}))

function resolveTeacherId() {
  const candidates = [
    localStorage.getItem('teacherId'),
    localStorage.getItem('userId'),
    localStorage.getItem('id')
  ]

  for (const raw of candidates) {
    const id = Number(raw)
    if (!Number.isNaN(id) && id > 0) return id
  }
  return null
}


function loadEchartsScript() {
  if (window.echarts) {
    echartsLib = window.echarts
    return Promise.resolve(window.echarts)
  }

  return new Promise((resolve, reject) => {
    const exists = document.querySelector('script[data-echarts-cdn="true"]')
    if (exists) {
      exists.addEventListener('load', () => {
        echartsLib = window.echarts
        resolve(window.echarts)
      })
      exists.addEventListener('error', () => reject(new Error('ECharts 加载失败')))
      return
    }

    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js'
    script.async = true
    script.dataset.echartsCdn = 'true'
    script.onload = () => {
      echartsLib = window.echarts
      resolve(window.echarts)
    }
    script.onerror = () => reject(new Error('ECharts 加载失败'))
    document.head.appendChild(script)
  })
}

function aggregateStats(list, field, labelMap, fallbackName) {
  const map = new Map()
  list.forEach((item) => {
    const key = item[field] ?? `unknown_${field}`
    const label = labelMap.get(item[field]) || fallbackName
    const current = map.get(key) || { name: label, value: 0 }
    current.value += Number(item.amount || 0)
    map.set(key, current)
  })

  return Array.from(map.values()).sort((a, b) => b.value - a.value)
}

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

async function loadData() {
  const typeData = await request('/api/workload-types')
  workloadTypes.value = Array.isArray(typeData) ? typeData : []

  if (isAdmin.value) {
    const [workloadData, teacherData] = await Promise.all([
      request('/api/workloads'),
      request('/api/teachers')
    ])
    workloads.value = Array.isArray(workloadData) ? workloadData : []
    teachers.value = Array.isArray(teacherData) ? teacherData : []
    return
  }

  const teacherId = resolveTeacherId()
  if (!teacherId) {
    workloads.value = []
    teachers.value = []
    ElMessage.warning('未获取到教师ID，暂时无法加载个人工作量统计')
    return
  }

  const workloadData = await request(`/api/workloads/teacher/${teacherId}`)
  const teacherName = localStorage.getItem('name')
    || localStorage.getItem('username')
    || `教师#${teacherId}`
  workloads.value = Array.isArray(workloadData) ? workloadData : []
  teachers.value = [{ id: teacherId, name: teacherName }]
}

function initCharts() {
  if (isAdmin.value && teacherBarRef.value) teacherBarChart = echartsLib.init(teacherBarRef.value)
  if (isAdmin.value && teacherPieRef.value) teacherPieChart = echartsLib.init(teacherPieRef.value)
  if (typeBarRef.value) typeBarChart = echartsLib.init(typeBarRef.value)
  if (typePieRef.value) typePieChart = echartsLib.init(typePieRef.value)
  if (monthBarRef.value) monthBarChart = echartsLib.init(monthBarRef.value)
  renderCharts()
}

function renderCharts() {
  if (isAdmin.value) {
    renderBarChart(teacherBarChart, teacherStats.value, '教师工作量（按教师）')
    renderPieChart(teacherPieChart, teacherStats.value, '教师占比')
  }
  renderBarChart(typeBarChart, typeStats.value, '工作量（按类型）')
  renderPieChart(typePieChart, typeStats.value, '类型占比')

  monthBarChart?.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: monthStats.value.map((item) => item.name)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '工作量',
        type: 'bar',
        data: monthStats.value.map((item) => item.value),
        itemStyle: { color: '#67c23a' }
      }
    ]
  })
}

function renderBarChart(chart, data, title) {
  chart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, bottom: 50, top: 30 },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      axisLabel: { rotate: 25 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: title,
        type: 'bar',
        data: data.map((item) => item.value),
        itemStyle: { color: '#409eff' }
      }
    ]
  })
}

function renderPieChart(chart, data, title) {
  chart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: title,
        type: 'pie',
        radius: '60%',
        data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.4)'
          }
        }
      }
    ]
  })
}

function resizeCharts() {
  teacherBarChart?.resize()
  teacherPieChart?.resize()
  typeBarChart?.resize()
  typePieChart?.resize()
  monthBarChart?.resize()
}

function destroyCharts() {
  teacherBarChart?.dispose()
  teacherPieChart?.dispose()
  typeBarChart?.dispose()
  typePieChart?.dispose()
  monthBarChart?.dispose()
}

function refreshCharts() {
  renderCharts()
}

onMounted(async () => {
  try {
    await Promise.all([loadData(), loadEchartsScript()])
    await nextTick()
    initCharts()
    window.addEventListener('resize', resizeCharts)
  } catch (error) {
    ElMessage.error(error.message || '加载统计数据失败')
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  destroyCharts()
})
</script>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  margin-bottom: 4px;
}

.summary-row :deep(.el-card__body) {
  padding: 14px 18px;
}

.summary-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.summary-label {
  color: #606266;
  font-size: 14px;
}

.summary-value {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.chart-row {
  margin-top: 0;
}

.chart-card :deep(.el-card__body) {
  padding: 12px;
}

.chart {
  height: 320px;
  width: 100%;
}

.chart-lg {
  height: 360px;
}
</style>
