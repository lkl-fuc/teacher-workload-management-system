import { createRouter, createWebHashHistory } from 'vue-router'

import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import WorkloadTypeView from '../views/WorkloadTypeView.vue'
import WorkloadCreateView from '../views/WorkloadCreateView.vue'
import WorkloadListView from '../views/WorkloadListView.vue'
import WorkloadAuditView from '../views/WorkloadAuditView.vue'
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
        meta: { title: '首页' }
      },
      {
        path: 'workload-types',
        name: 'workloadTypes',
        component: WorkloadTypeView,
        meta: { title: '工作量类型管理' }
      },
      {
        path: 'workloads/new',
        name: 'workloadCreate',
        component: WorkloadCreateView,
        meta: { title: '新增工作量' }
      },
      {
        path: 'workloads/my',
        name: 'workloadList',
        component: WorkloadListView,
        meta: { title: '我的工作量列表' }
      },
      {
        path: 'workloads/audit',
        name: 'workloadAudit',
        component: WorkloadAuditView,
        meta: { title: '工作量审核' }
      }
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

  next()
})

export default router
