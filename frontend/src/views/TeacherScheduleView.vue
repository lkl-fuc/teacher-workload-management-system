<template>
  <div class="page-wrap">
    <el-row :gutter="12">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">本周总课时</div>
          <div class="stat-value">{{ totalHours }} 学时</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">本周课程数</div>
          <div class="stat-value">{{ scheduleList.length }} 节</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">今日安排</div>
          <div class="stat-value">{{ todayCourseCount }} 节</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="header-row">
          <div class="section-title">本周教学安排</div>
          <div class="header-actions">
            <el-tag type="info" effect="plain">教师ID：{{ currentTeacherId || '未识别' }}</el-tag>
            <el-tag type="success" effect="plain">下一节：{{ nextCourseText }}</el-tag>
            <el-button type="primary" plain @click="copyWeeklySummary">复制周安排摘要</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="filter-bar">
        <el-form-item label="星期筛选">
          <el-select v-model="weekdayFilter" placeholder="全部" style="width: 120px">
            <el-option label="全部" value="ALL" />
            <el-option v-for="day in weekdayOptions" :key="day" :label="day" :value="day" />
          </el-select>
        </el-form-item>

        <el-form-item label="课程关键词">
          <el-input
            v-model="keyword"
            placeholder="输入课程名/地点"
            clearable
            style="width: 220px"
          />
        </el-form-item>

        <el-form-item>
          <el-switch v-model="onlyToday" inline-prompt active-text="仅今日" inactive-text="全周" />
        </el-form-item>
      </el-form>

      <el-table :data="filteredScheduleList" border stripe>
        <el-table-column prop="weekday" label="星期" width="100" />
        <el-table-column prop="time" label="时间" width="140" />
        <el-table-column prop="course" label="课程" min-width="180" />
        <el-table-column prop="classroom" label="地点" width="160" />
        <el-table-column prop="hours" label="学时" width="90" />
      </el-table>
      <el-empty v-if="!filteredScheduleList.length" description="当前筛选条件下暂无教学安排" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const scheduleList = ref([])
const currentTeacherId = ref(null)

const teacherSchedules = {
  1: [
    { weekday: '周一', time: '08:30-10:05', course: '数据结构', classroom: '博学楼 A201', hours: 2 },
    { weekday: '周二', time: '14:00-15:35', course: '数据库原理', classroom: '博学楼 B306', hours: 2 },
    { weekday: '周四', time: '10:20-11:55', course: '算法设计与分析', classroom: '博学楼 A305', hours: 2 },
    { weekday: '周五', time: '16:00-17:35', course: '毕业设计指导', classroom: '实验楼 403', hours: 2 }
  ],
  2: [
    { weekday: '周一', time: '10:20-11:55', course: '计算机网络', classroom: '博学楼 C205', hours: 2 },
    { weekday: '周三', time: '08:30-10:05', course: '操作系统', classroom: '博学楼 C401', hours: 2 },
    { weekday: '周四', time: '14:00-15:35', course: 'Linux系统管理', classroom: '实验楼 202', hours: 2 },
    { weekday: '周五', time: '08:30-10:05', course: '网络安全实践', classroom: '实验楼 105', hours: 2 }
  ],
  3: [
    { weekday: '周二', time: '08:30-10:05', course: '软件工程', classroom: '博学楼 D101', hours: 2 },
    { weekday: '周三', time: '14:00-15:35', course: '项目管理', classroom: '博学楼 D302', hours: 2 },
    { weekday: '周四', time: '16:00-17:35', course: '需求分析与建模', classroom: '博学楼 D206', hours: 2 },
    { weekday: '周五', time: '10:20-11:55', course: '软件测试', classroom: '实验楼 305', hours: 2 }
  ],
  4: [
    { weekday: '周一', time: '14:00-15:35', course: '机器学习', classroom: '智算楼 A503', hours: 2 },
    { weekday: '周二', time: '16:00-17:35', course: '深度学习', classroom: '智算楼 A501', hours: 2 },
    { weekday: '周四', time: '08:30-10:05', course: '计算机视觉', classroom: '智算楼 B402', hours: 2 },
    { weekday: '周五', time: '14:00-15:35', course: '人工智能导论', classroom: '智算楼 B201', hours: 2 }
  ],
  5: [
    { weekday: '周一', time: '16:00-17:35', course: '数据挖掘', classroom: '数据楼 301', hours: 2 },
    { weekday: '周三', time: '10:20-11:55', course: '大数据处理技术', classroom: '数据楼 208', hours: 2 },
    { weekday: '周四', time: '14:00-15:35', course: '统计学习方法', classroom: '数据楼 402', hours: 2 },
    { weekday: '周五', time: '08:30-10:05', course: 'Python数据分析', classroom: '实验楼 204', hours: 2 }
  ]
}

const weekdayOptions = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const weekdayMap = {
  1: '周一',
  2: '周二',
  3: '周三',
  4: '周四',
  5: '周五',
  6: '周六',
  0: '周日'
}

const weekdayFilter = ref('ALL')
const keyword = ref('')
const onlyToday = ref(false)

function resolveTeacherId() {
  const candidates = [localStorage.getItem('teacherId'), localStorage.getItem('userId'), localStorage.getItem('id')]
  for (const raw of candidates) {
    const id = Number(raw)
    if (!Number.isNaN(id) && id > 0) return id
  }
  return null
}

function loadTeacherSchedule() {
  const teacherId = resolveTeacherId()
  currentTeacherId.value = teacherId

  if (!teacherId) {
    scheduleList.value = []
    ElMessage.warning('未识别到教师身份，请重新登录后再查看教学安排')
    return
  }

  const schedule = teacherSchedules[teacherId]
  if (Array.isArray(schedule)) {
    scheduleList.value = schedule.map((item) => ({ ...item }))
    return
  }

  scheduleList.value = [
    { weekday: '周一', time: '08:30-10:05', course: `教师${teacherId}课程A`, classroom: `教学楼 A${200 + teacherId}`, hours: 2 },
    { weekday: '周三', time: '14:00-15:35', course: `教师${teacherId}课程B`, classroom: `教学楼 B${300 + teacherId}`, hours: 2 }
  ]
}

const todayWeekday = computed(() => weekdayMap[new Date().getDay()])
const totalHours = computed(() => scheduleList.value.reduce((sum, item) => sum + Number(item.hours || 0), 0))
const todayCourseCount = computed(() => scheduleList.value.filter((item) => item.weekday === todayWeekday.value).length)

const filteredScheduleList = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return scheduleList.value.filter((item) => {
    if (onlyToday.value && item.weekday !== todayWeekday.value) return false
    if (weekdayFilter.value !== 'ALL' && item.weekday !== weekdayFilter.value) return false
    if (!key) return true
    return `${item.course}${item.classroom}`.toLowerCase().includes(key)
  })
})

const nextCourseText = computed(() => {
  const today = todayWeekday.value
  const todayCourses = scheduleList.value.filter((item) => item.weekday === today)
  if (todayCourses.length) return `${today} ${todayCourses[0].time} ${todayCourses[0].course}`
  const fallback = scheduleList.value[0]
  return fallback ? `${fallback.weekday} ${fallback.time} ${fallback.course}` : '暂无'
})

async function copyWeeklySummary() {
  const text = scheduleList.value
    .map((item) => `${item.weekday} ${item.time} ${item.course} @ ${item.classroom}`)
    .join('\n')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('周安排摘要已复制，可直接发到群里')
  } catch {
    ElMessage.warning('复制失败，请检查浏览器剪贴板权限')
  }
}

onMounted(() => {
  loadTeacherSchedule()
})
</script>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-card {
  border-radius: 12px;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

.stat-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.section-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  font-weight: 600;
  color: #0f172a;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-bar {
  margin-bottom: 8px;
}
</style>
