<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>教师信息（按岗位分组）</span>
        <el-button type="primary" :loading="loading" @click="loadTeachers">刷新</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab" v-loading="loading" stretch>
      <el-tab-pane
        v-for="group in groupedTeacherList"
        :key="group.key"
        :label="`${group.key}（${group.list.length}）`"
        :name="group.key"
      >
        <el-table :data="group.list" border>
          <el-table-column prop="teacherNo" label="工号" width="120" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="postType" label="岗位" width="100" />
          <el-table-column prop="gender" label="性别" width="90" />
          <el-table-column prop="department" label="院系" min-width="140" />
          <el-table-column prop="title" label="职称" width="120" />
          <el-table-column prop="phone" label="联系电话" width="150" />
          <el-table-column prop="email" label="邮箱" min-width="220" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const teachers = ref([])
const activeTab = ref('行政岗')
const postOrder = ['行政岗', '管理岗', '教辅岗']

const groupedTeacherList = computed(() => {
  const groupedMap = new Map(postOrder.map((key) => [key, []]))

  teachers.value.forEach((item) => {
    const post = postOrder.includes(item.postType) ? item.postType : '行政岗'
    groupedMap.get(post).push(item)
  })

  return postOrder.map((key) => ({
    key,
    list: groupedMap.get(key) || []
  }))
})

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
    teachers.value = (Array.isArray(data) ? data : []).map((item) => ({
      ...item,
      postType: postOrder.includes(item.postType) ? item.postType : '行政岗'
    }))
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
