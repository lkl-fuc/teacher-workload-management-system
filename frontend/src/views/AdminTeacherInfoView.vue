<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>教师信息（5位）</span>
        <el-button type="primary" :loading="loading" @click="loadTeachers">刷新</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="teachers" border>
      <el-table-column prop="teacherNo" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="90" />
      <el-table-column prop="department" label="院系" min-width="140" />
      <el-table-column prop="title" label="职称" width="120" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="220" />
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const teachers = ref([])

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })

  if (!response.ok) {
    throw new Error('请求失败')
  }

  return response.json()
}

async function loadTeachers() {
  loading.value = true
  try {
    const data = await request('/api/teachers')
    teachers.value = (Array.isArray(data) ? data : []).slice(0, 5)
  } catch (error) {
    ElMessage.error(error?.message || '加载教师信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTeachers()
})
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
