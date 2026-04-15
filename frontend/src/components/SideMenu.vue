<template>
  <el-menu
    :default-active="activeMenu"
    class="side-menu"
    router
    unique-opened
    background-color="transparent"
    text-color="rgba(255,255,255,0.86)"
    active-text-color="#ffffff"
  >
    <template v-for="item in menuItems" :key="item.index || item.label">
      <el-sub-menu v-if="item.children?.length" :index="item.index">
        <template #title>
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </template>
        <el-menu-item
          v-for="child in item.children"
          :key="child.index"
          :index="child.index"
        >
          <el-icon><component :is="child.icon" /></el-icon>
          <span>{{ child.label }}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="item.index">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Bell,
  Calendar,
  Finished,
  Files,
  House,
  List,
  Management,
  PieChart,
  Plus,
  Tickets,
  User
} from '@element-plus/icons-vue'

const route = useRoute()

const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())
const teacherPost = computed(() => localStorage.getItem('teacherPost') || '专任教师岗')

const adminMenuItems = [
  { index: '/home', label: '首页', icon: House },
  { index: '/workload-types', label: '工作量类型管理', icon: List },
  { index: '/admin/teachers', label: '教师信息', icon: User },
  { index: '/workloads/new', label: '新增工作量', icon: Plus },
  { index: '/workloads/my', label: '全体教师工作量列表', icon: Tickets },
  { index: '/workloads/audit', label: '工作量审核', icon: Finished },
  { index: '/workloads/stats', label: '工作量统计分析', icon: PieChart }
]

const baseTeacherMenu = [
  { index: '/home', label: '首页', icon: House },
  {
    index: 'task-center',
    label: '任务中心',
    icon: Management,
    children: [
      { index: '/teacher/post-tasks', label: '岗位任务面板', icon: Files },
      { index: '/workloads/my', label: '我的提交记录', icon: Tickets },
      { index: '/workloads/stats', label: '个人工作量统计', icon: PieChart }
    ]
  },
  {
    index: 'teacher-tools',
    label: '个人工具',
    icon: User,
    children: [
      { index: '/teacher/notices', label: '通知公告（预警）', icon: Bell },
      { index: '/teacher/profile', label: '个人信息', icon: User }
    ]
  }
]

const teacherPostMenus = {
  专任教师岗: [
    { index: '/teacher/schedule', label: '教学安排', icon: Calendar }
  ],
  实验教师岗: [
    { index: '/teacher/schedule', label: '实验安排', icon: Calendar }
  ],
  辅导员岗: [{ index: '/teacher/schedule', label: '学生事务日程', icon: Calendar }],
  教辅岗: [{ index: '/teacher/post-tasks', label: '教辅服务任务看板', icon: Plus }],
  行政兼课岗: [{ index: '/teacher/schedule', label: '行政/兼课日程', icon: Calendar }],
  外聘教师岗: [{ index: '/teacher/schedule', label: '授课计划', icon: Calendar }]
}

const teacherMenuItems = computed(() => {
  const postItems = teacherPostMenus[teacherPost.value] || teacherPostMenus.专任教师岗
  return baseTeacherMenu.map((item) => {
    if (item.index !== 'teacher-tools') return item
    return {
      ...item,
      children: [...item.children, ...postItems]
    }
  })
})

const menuItems = computed(() => (role.value === 'TEACHER' ? teacherMenuItems.value : adminMenuItems))

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
