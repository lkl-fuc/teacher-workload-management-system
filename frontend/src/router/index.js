import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import MainLayout from '../layouts/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: HomeView,
        meta: { title: '首页', icon: 'House' }
      },
      {
        path: 'teacher-workload',
        name: 'TeacherWorkload',
        component: HomeView,
        meta: { title: '教师工作量', icon: 'DataAnalysis' }
      },
      {
        path: 'warning-rules',
        name: 'WarningRules',
        component: HomeView,
        meta: { title: '预警规则', icon: 'Bell' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  if (to.path === '/login' && token) {
    next('/home')
    return
  }

  document.title = to.meta?.title
    ? `${to.meta.title} - 教师工作量管理系统`
    : '教师工作量管理系统'

  next()
})

export default router
