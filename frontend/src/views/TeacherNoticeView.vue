<template>
  <div class="page-wrap">
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-title">通知公告</div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="item in notices"
          :key="item.title"
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

const notices = ref([
  { date: '2026-03-20', title: '春季学期工作量填报开始', content: '请各位教师按时完成工作量任务。', type: 'primary' }
])

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })
  if (!response.ok) throw new Error('请求失败')
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
          date: item.submitDate,
          title: `任务《${item.workloadTitle}》审核通过`,
          content: '管理员已审核通过该任务。',
          type: 'success'
        }
      }
      if (status === 'REJECTED') {
        return {
          date: item.submitDate,
          title: `任务《${item.workloadTitle}》被驳回`,
          content: item.rejectReason || '请根据驳回原因重新完成任务并提交。',
          type: 'danger'
        }
      }
      return {
        date: item.submitDate,
        title: `收到新任务《${item.workloadTitle}》`,
        content: '请尽快填写完成情况并提交给管理员审核。',
        type: 'warning'
      }
    })

  notices.value = [...workloadNotices, ...notices.value]
}

onMounted(() => {
  loadWorkloadNotices()
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
</style>
