<template>
  <el-menu
    :default-active="activeMenu"
    class="side-menu"
    router
    background-color="transparent"
    text-color="rgba(255,255,255,0.86)"
    active-text-color="#ffffff"
  >
    <el-menu-item
      v-for="item in menuItems"
      :key="item.index"
      :index="item.index"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Calendar, Finished, House, List, PieChart, Plus, Tickets, User } from '@element-plus/icons-vue'

const route = useRoute()

const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())

const adminMenuItems = [
  { index: '/home', label: '首页', icon: House },
  { index: '/workload-types', label: '工作量类型管理', icon: List },
  { index: '/workloads/new', label: '新增工作量', icon: Plus },
  { index: '/workloads/my', label: '我的工作量列表', icon: Tickets },
  { index: '/workloads/audit', label: '工作量审核', icon: Finished },
  { index: '/workloads/stats', label: '工作量统计分析', icon: PieChart }
]

const teacherMenuItems = [
  { index: '/home', label: '首页', icon: House },
  { index: '/workloads/new', label: '填写工作量', icon: Plus },
  { index: '/workloads/my', label: '我的提交记录', icon: Tickets },
  { index: '/workloads/stats', label: '个人工作量统计', icon: PieChart },
  { index: '/teacher/notices', label: '通知公告', icon: Bell },
  { index: '/teacher/schedule', label: '教学安排', icon: Calendar },
  { index: '/teacher/profile', label: '个人信息', icon: User }
]

const menuItems = computed(() => (role.value === 'TEACHER' ? teacherMenuItems : adminMenuItems))

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  border-right: none;
  height: calc(100% - 74px);
  padding: 12px;
}

.side-menu :deep(.el-menu-item) {
  margin-bottom: 6px;
  border-radius: 10px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.14);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.2);
  font-weight: 600;
}
</style>
