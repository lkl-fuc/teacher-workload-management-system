<template>
  <div class="page-wrap">
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-title">本周教学安排</div>
      </template>
      <el-table :data="scheduleList" border stripe>
        <el-table-column prop="weekday" label="星期" width="100" />
        <el-table-column prop="time" label="时间" width="140" />
        <el-table-column prop="course" label="课程" min-width="180" />
        <el-table-column prop="classroom" label="地点" width="160" />
        <el-table-column prop="hours" label="学时" width="90" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const scheduleList = [
  { weekday: '周一', time: '08:30-10:05', course: '数据结构', classroom: '博学楼 A201', hours: 2 },
  { weekday: '周二', time: '14:00-15:35', course: '数据库原理', classroom: '博学楼 B306', hours: 2 },
  { weekday: '周四', time: '10:20-11:55', course: '算法设计与分析', classroom: '博学楼 A305', hours: 2 },
  { weekday: '周五', time: '16:00-17:35', course: '毕业设计指导', classroom: '实验楼 403', hours: 2 }
]
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
</style>
