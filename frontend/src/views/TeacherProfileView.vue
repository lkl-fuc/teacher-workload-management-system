<template>
  <div class="page-wrap">
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-title">个人信息</div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="工号">{{ profile.teacherNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ profile.name }}</el-descriptions-item>
        <el-descriptions-item label="院系">{{ profile.department }}</el-descriptions-item>
        <el-descriptions-item label="职称">{{ profile.title }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ profile.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ profile.email }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const profile = computed(() => ({
  teacherNo: localStorage.getItem('username') || '未设置',
  name: localStorage.getItem('name') || '教师用户',
  department: '计算机学院',
  title: '讲师',
  phone: '13800138000',
  email: 'teacher@example.com'
}))
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
