import { createRouter, createWebHashHistory } from 'vue-router'

import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import WorkloadTypeView from '../views/WorkloadTypeView.vue'
import WorkloadCreateView from '../views/WorkloadCreateView.vue'
import WorkloadListView from '../views/WorkloadListView.vue'
import WorkloadAuditView from '../views/WorkloadAuditView.vue'
import WorkloadStatsView from '../views/WorkloadStatsView.vue'
import TeacherProfileView from '../views/TeacherProfileView.vue'
import TeacherScheduleView from '../views/TeacherScheduleView.vue'
import TeacherNoticeView from '../views/TeacherNoticeView.vue'
import AdminTeacherInfoView from '../views/AdminTeacherInfoView.vue'
import MainLayout from '../layouts/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        redirect: '/home'
      },
      {
        path: 'home',
        name: 'home',
        component: HomeView,
        meta: { title: '首页', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'workload-types',
        name: 'workloadTypes',
        component: WorkloadTypeView,
        meta: { title: '工作量类型管理', roles: ['ADMIN'] }
      },
      {
        path: 'workloads/new',
        name: 'workloadCreate',
        component: WorkloadCreateView,
        meta: { title: '新增工作量', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'workloads/my',
        name: 'workloadList',
        component: WorkloadListView,
        meta: { title: '工作量列表', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'admin/teachers',
        name: 'adminTeachers',
        component: AdminTeacherInfoView,
        meta: { title: '教师信息', roles: ['ADMIN'] }
      },
      {
        path: 'workloads/audit',
        name: 'workloadAudit',
        component: WorkloadAuditView,
        meta: { title: '工作量审核', roles: ['ADMIN'] }
      },

      {
        path: 'workloads/stats',
        name: 'workloadStats',
        component: WorkloadStatsView,
        meta: { title: '工作量统计分析', roles: ['ADMIN', 'TEACHER'] }
      },

      {
        path: 'teacher/notices',
        name: 'teacherNotices',
        component: TeacherNoticeView,
        meta: { title: '通知公告', roles: ['TEACHER'] }
      },
      {
        path: 'teacher/schedule',
        name: 'teacherSchedule',
        component: TeacherScheduleView,
        meta: { title: '教学安排', roles: ['TEACHER'] }
      },
      {
        path: 'teacher/profile',
        name: 'teacherProfile',
        component: TeacherProfileView,
        meta: { title: '个人信息', roles: ['TEACHER'] }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  if (to.path === '/login' && token) {
    next('/home')
    return
  }

  if (to.path !== '/login') {
    const role = String(localStorage.getItem('role') || '').toUpperCase()
    const allowedRoles = to.meta?.roles || []

    if (Array.isArray(allowedRoles) && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
      next('/home')
      return
    }
  }

  next()
})

export default router
