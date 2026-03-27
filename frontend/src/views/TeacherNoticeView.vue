<template>
  <div class="page-wrap">
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-title">通知公告</div>
      </template>

      <div v-if="warningCards.length" class="warning-box">
        <div class="warning-title">工作量预警</div>
        <el-card v-for="warning in warningCards" :key="warning.id" class="warning-item" shadow="hover">
          <div class="notice-title">{{ warning.title }}</div>
          <div class="notice-content">{{ warning.content }}</div>
          <div class="warning-footer">
            <span class="warning-date">{{ warning.date }}</span>
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="warning.status === 'ACKED'"
              @click="acknowledgeWarning(warning.id)"
            >
              {{ warning.status === 'ACKED' ? '已收到预警' : '已收到预警' }}
            </el-button>
          </div>
        </el-card>
      </div>

      <el-timeline>
        <el-timeline-item
          v-for="item in notices"
          :key="item.key"
          :timestamp="item.date"
          placement="top"
          :type="item.type"
        >
          <el-card shadow="hover">
            <div class="notice-title">{{ item.title }}</div>
            <div class="notice-content">{{ item.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const notices = ref([
  {
    key: 'system-default-notice',
    date: '2026-03-20',
    title: '春季学期工作量填报开始',
    content: '请各位教师按时完成工作量任务。',
    type: 'primary'
  }
])

const warningCards = ref([])

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
      const data = await response.json()
      message = data.message || message
    } catch {
      // ignore
    }
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

function resolveTeacherId() {
  const candidates = [localStorage.getItem('teacherId'), localStorage.getItem('userId'), localStorage.getItem('id')]
  for (const raw of candidates) {
    const id = Number(raw)
    if (!Number.isNaN(id) && id > 0) return id
  }
  return null
}

async function loadWorkloadNotices() {
  const teacherId = resolveTeacherId()
  if (!teacherId) return
  const data = await request(`/api/workloads/teacher/${teacherId}`)
  const workloadNotices = (Array.isArray(data) ? data : [])
    .filter((item) => ['APPROVED', 'REJECTED', 'ASSIGNED'].includes(String(item.status || '').toUpperCase()))
    .slice(0, 6)
    .map((item) => {
      const status = String(item.status || '').toUpperCase()
      if (status === 'APPROVED') {
        return {
          key: `workload-${item.id}`,
          date: item.submitDate,
          title: `任务《${item.workloadTitle}》审核通过`,
          content: '管理员已审核通过该任务。',
          type: 'success'
        }
      }
      if (status === 'REJECTED') {
        return {
          key: `workload-${item.id}`,
          date: item.submitDate,
          title: `任务《${item.workloadTitle}》被驳回`,
          content: item.rejectReason || '请根据驳回原因重新完成任务并提交。',
          type: 'danger'
        }
      }
      return {
        key: `workload-${item.id}`,
        date: item.submitDate,
        title: `收到新任务《${item.workloadTitle}》`,
        content: '请尽快填写完成情况并提交给管理员审核。',
        type: 'warning'
      }
    })

  notices.value = [...workloadNotices, ...notices.value]
}

async function loadWarningNotices() {
  const teacherId = resolveTeacherId()
  if (!teacherId) return
  const data = await request(`/api/warnings/teacher/${teacherId}`)
  warningCards.value = (Array.isArray(data) ? data : []).map((item) => ({
    id: item.id,
    date: item.createTime ? item.createTime.replace('T', ' ').slice(0, 16) : '实时',
    title: '工作量负荷预警',
    content: item.warningMessage || '系统检测到工作量异常，请及时确认。',
    status: String(item.status || 'NEW').toUpperCase()
  }))
}

async function acknowledgeWarning(warningId) {
  const teacherId = resolveTeacherId()
  if (!teacherId || !warningId) return
  try {
    await request(`/api/warnings/${warningId}/acknowledge?teacherId=${teacherId}`, { method: 'POST' })
    ElMessage.success('已回执管理员：你已收到预警信息')
    await loadWarningNotices()
  } catch (error) {
    ElMessage.error(error?.message || '预警回执失败')
  }
}

onMounted(() => {
  Promise.allSettled([loadWorkloadNotices(), loadWarningNotices()])
})
</script>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  border-radius: 12px;
}

.section-title {
  font-weight: 600;
  color: #0f172a;
}

.warning-box {
  margin-bottom: 16px;
}

.warning-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #b45309;
}

.warning-item + .warning-item {
  margin-top: 10px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e3a8a;
}

.notice-content {
  margin-top: 8px;
  color: #475569;
  line-height: 1.8;
}

.warning-footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.warning-date {
  color: #94a3b8;
  font-size: 12px;
}
</style>
