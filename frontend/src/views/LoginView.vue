<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <h2>教师工作量管理系统</h2>
      </template>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="登录角色">
          <el-radio-group v-model="form.role">
            <el-radio-button label="TEACHER">教师</el-radio-button>
            <el-radio-button label="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            :placeholder="form.role === 'ADMIN' ? '请输入管理员用户名' : '请输入教师工号'"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="submitLoading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const submitLoading = ref(false)

const form = reactive({
  username: '',
  password: '',
  role: 'TEACHER'
})

function normalizeError(error, fallback) {
  if (error?.message) return error.message
  return fallback
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })

  if (!response.ok) {
    let message = '请求失败'
    try {
      const errorData = await response.json()
      message = errorData.message || message
    } catch {
      // ignore parse errors
    }
    throw new Error(message)
  }

  return response.json()
}

function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('id')
  localStorage.removeItem('userId')
  localStorage.removeItem('teacherId')
  localStorage.removeItem('role')
  localStorage.removeItem('teacherPost')
  localStorage.removeItem('name')
  localStorage.removeItem('username')
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  submitLoading.value = true
  try {
    const result = await request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        username: form.username,
        password: form.password,
        role: form.role
      })
    })

    if (!result || typeof result !== 'object' || !result.id) {
      throw new Error('登录返回数据异常')
    }

    clearSession()
    localStorage.setItem('token', `mock-token-${Date.now()}`)
    localStorage.setItem('id', String(result.id))
    localStorage.setItem('userId', String(result.id))
    localStorage.setItem('role', result.role || form.role)
    localStorage.setItem('name', result.name || '')
    localStorage.setItem('username', result.username || form.username)
    if (String(result.role || form.role).toUpperCase() === 'TEACHER') {
      localStorage.setItem('teacherId', String(result.id))
      localStorage.setItem('teacherPost', result.teacherPost || '专任教师岗')
    }

    ElMessage.success('登录成功')
    router.push('/home')
  } catch (error) {
    clearSession()
    ElMessage.error(normalizeError(error, '登录失败，请检查账号密码'))
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.login-card {
  width: 360px;
}

.login-btn {
  width: 100%;
}

h2 {
  margin: 0;
  text-align: center;
}
</style>
