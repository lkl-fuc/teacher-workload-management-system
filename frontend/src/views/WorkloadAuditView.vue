<template>
  <div class="audit-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>管理员审核</span>
          <el-button type="primary" :loading="tableLoading" @click="loadPendingRows">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="tableLoading" :data="rows" border>
        <el-table-column prop="workloadTitle" label="工作标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="teacherId" label="教师ID" width="90" />
        <el-table-column prop="typeId" label="类型ID" width="90" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="amount" label="分值" width="100" />
        <el-table-column prop="submitDate" label="提交日期" width="120" />
        <el-table-column label="审核状态" width="110">
          <template #default="scope">
            <el-tag type="warning">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="success"
              :loading="actionLoadingId === scope.row.id && actionType === 'approve'"
              @click="handleApprove(scope.row)"
            >
              审核通过
            </el-button>
            <el-button link type="danger" @click="openRejectDialog(scope.row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!tableLoading && rows.length === 0" description="暂无待审核工作量" />
    </el-card>

    <el-card>
      <template #header>
        <div class="header-row">
          <span>智能核算与阈值预警</span>
          <el-button type="danger" :loading="analysisLoading" @click="recalculateWarnings">执行预警扫描</el-button>
        </div>
      </template>

      <el-row :gutter="12" class="summary-row">
        <el-col :span="8"><div class="summary-item">教师总数：{{ analysis.summary.teacherCount || 0 }}</div></el-col>
        <el-col :span="8"><div class="summary-item low">低负荷：{{ analysis.summary.lowCount || 0 }}</div></el-col>
        <el-col :span="8"><div class="summary-item high">高负荷：{{ analysis.summary.highCount || 0 }}</div></el-col>
      </el-row>

      <div ref="analysisChartRef" class="analysis-chart"></div>

      <el-table v-loading="analysisLoading" :data="analysis.items" border>
        <el-table-column prop="teacherName" label="教师" min-width="120" />
        <el-table-column prop="postType" label="岗位" min-width="100" />
        <el-table-column prop="title" label="职称" min-width="100" />
        <el-table-column prop="equivalentAmount" label="折算工作量" width="120" />
        <el-table-column label="阈值区间" min-width="150">
          <template #default="scope">{{ scope.row.minThreshold }} ~ {{ scope.row.maxThreshold }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="levelTagType(scope.row.level)">{{ levelText(scope.row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warningMessage" label="说明" min-width="260" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div class="header-row">
          <span>教师预警回执追踪</span>
          <el-button type="primary" :loading="warningLoading" @click="loadWarningRecords">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="warningLoading" :data="warningRows" border>
        <el-table-column prop="teacherName" label="教师" min-width="120" />
        <el-table-column prop="teacherPostType" label="岗位" min-width="100" />
        <el-table-column prop="warningMessage" label="预警内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createTime" label="预警时间" width="180" />
        <el-table-column label="回执状态" width="120">
          <template #default="scope">
            <el-tag :type="String(scope.row.status || '').toUpperCase() === 'ACKED' ? 'success' : 'warning'">
              {{ String(scope.row.status || '').toUpperCase() === 'ACKED' ? '教师已收到' : '待教师确认' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!warningLoading && warningRows.length === 0" description="暂无预警回执记录" />
    </el-card>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="500px" destroy-on-close>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写驳回原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleCloseRejectDialog">取消</el-button>
        <el-button
          type="danger"
          :loading="actionLoadingId === rejectForm.id && actionType === 'reject'"
          @click="handleReject"
        >
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const rows = ref([])
const tableLoading = ref(false)
const analysisLoading = ref(false)
const warningLoading = ref(false)
const analysis = ref({ summary: {}, items: [] })
const warningRows = ref([])
const analysisChartRef = ref(null)
let analysisChart
let echartsLib

const rejectVisible = ref(false)
const rejectFormRef = ref()
const actionLoadingId = ref(null)
const actionType = ref('')

const rejectForm = reactive({
  id: null,
  rejectReason: ''
})

const rejectRules = {
  rejectReason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

function normalizeError(error, fallback) {
  if (error?.message) return error.message
  return fallback
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
      // ignore parse errors
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

function statusText(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'PENDING') return '待审核'
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已驳回'
  return status || '未知'
}

function levelText(level) {
  const value = String(level || '').toUpperCase()
  if (value === 'LOW') return '过低'
  if (value === 'HIGH') return '过高'
  return '正常'
}

function levelTagType(level) {
  const value = String(level || '').toUpperCase()
  if (value === 'LOW') return 'warning'
  if (value === 'HIGH') return 'danger'
  return 'success'
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

async function loadPendingRows() {
  tableLoading.value = true
  try {
    const data = await request('/api/workloads')
    rows.value = (Array.isArray(data) ? data : []).filter(
      (item) => String(item.status || '').toUpperCase() === 'PENDING'
    )
  } catch (error) {
    ElMessage.error(normalizeError(error, '加载待审核工作量失败'))
  } finally {
    tableLoading.value = false
  }
}

async function loadAnalysis() {
  analysisLoading.value = true
  try {
    const data = await request('/api/warnings/analysis')
    analysis.value = data || { summary: {}, items: [] }
    await nextTick()
    renderChart()
  } catch (error) {
    ElMessage.error(normalizeError(error, '加载预警分析失败'))
  } finally {
    analysisLoading.value = false
  }
}

async function loadWarningRecords() {
  warningLoading.value = true
  try {
    const data = await request('/api/warnings/records')
    warningRows.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(normalizeError(error, '加载预警回执失败'))
  } finally {
    warningLoading.value = false
  }
}

function renderChart() {
  if (!echartsLib || !analysisChartRef.value) return
  if (!analysisChart) {
    analysisChart = echartsLib.init(analysisChartRef.value)
  }

  const items = Array.isArray(analysis.value.items) ? analysis.value.items : []
  analysisChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['折算工作量', '最低阈值', '最高阈值'] },
    grid: { left: 30, right: 20, top: 40, bottom: 90 },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.teacherName),
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '折算工作量',
        type: 'bar',
        data: items.map((item) => item.equivalentAmount),
        itemStyle: {
          color: (params) => {
            const level = String(items[params.dataIndex]?.level || '').toUpperCase()
            if (level === 'LOW') return '#e6a23c'
            if (level === 'HIGH') return '#f56c6c'
            return '#67c23a'
          }
        }
      },
      {
        name: '最低阈值',
        type: 'line',
        data: items.map((item) => item.minThreshold),
        smooth: true
      },
      {
        name: '最高阈值',
        type: 'line',
        data: items.map((item) => item.maxThreshold),
        smooth: true
      }
    ]
  })
}

async function recalculateWarnings() {
  analysisLoading.value = true
  try {
    const data = await request('/api/warnings/recalculate', { method: 'POST' })
    analysis.value = data || { summary: {}, items: [] }
    await nextTick()
    renderChart()
    ElMessage.success('预警扫描完成，已向教师端同步预警')
  } catch (error) {
    ElMessage.error(normalizeError(error, '执行预警扫描失败'))
  } finally {
    analysisLoading.value = false
  }
}

async function handleApprove(row) {
  actionLoadingId.value = row.id
  actionType.value = 'approve'
  try {
    await request(`/api/workloads/${row.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        status: 'APPROVED',
        rejectReason: ''
      })
    })
    ElMessage.success('审核通过成功')
    await Promise.all([loadPendingRows(), loadAnalysis()])
  } catch (error) {
    ElMessage.error(normalizeError(error, '审核通过失败'))
  } finally {
    actionLoadingId.value = null
    actionType.value = ''
  }
}

function openRejectDialog(row) {
  rejectForm.id = row.id
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

function handleCloseRejectDialog() {
  rejectVisible.value = false
  rejectFormRef.value?.clearValidate()
}

async function handleReject() {
  if (!rejectFormRef.value) return

  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return

    actionLoadingId.value = rejectForm.id
    actionType.value = 'reject'

    try {
      await request(`/api/workloads/${rejectForm.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          status: 'REJECTED',
          rejectReason: rejectForm.rejectReason
        })
      })
      ElMessage.success('驳回成功')
      rejectVisible.value = false
      await Promise.all([loadPendingRows(), loadAnalysis()])
    } catch (error) {
      ElMessage.error(normalizeError(error, '驳回失败'))
    } finally {
      actionLoadingId.value = null
      actionType.value = ''
    }
  })
}

function handleResize() {
  analysisChart?.resize()
}

onMounted(async () => {
  await Promise.all([loadPendingRows(), loadEchartsScript()])
  await Promise.all([loadAnalysis(), loadWarningRecords()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  analysisChart?.dispose()
})
</script>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-row {
  margin-bottom: 12px;
}

.summary-item {
  border-radius: 8px;
  padding: 10px 12px;
  background: #ecf5ff;
  color: #1f2d3d;
}

.summary-item.low {
  background: #fdf6ec;
  color: #e6a23c;
}

.summary-item.high {
  background: #fef0f0;
  color: #f56c6c;
}

.analysis-chart {
  height: 320px;
  margin-bottom: 14px;
}
</style>
