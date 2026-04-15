<template>
  <header class="top-bar">
    <div>
      <div class="system-name">教师工作量管理系统</div>
      <div class="system-sub">毕业设计演示版本</div>
    </div>
    <div class="right-area">
      <div class="admin-chip">{{ displayIdentity }}</div>
      <el-button size="small" type="primary" plain @click="logout">退出登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const displayIdentity = computed(() => {
  const role = String(localStorage.getItem('role') || '').toUpperCase()
  const name = localStorage.getItem('name') || ''
  const teacherPost = localStorage.getItem('teacherPost') || '专任教师岗'
  const roleLabel = role === 'TEACHER' ? `教师（${teacherPost}）` : '管理员'
  return name ? `${roleLabel}：${name}` : roleLabel
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('id')
  localStorage.removeItem('userId')
  localStorage.removeItem('teacherId')
  localStorage.removeItem('role')
  localStorage.removeItem('teacherPost')
  localStorage.removeItem('name')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.top-bar {
  height: 68px;
  padding: 0 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.system-name {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.system-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

.right-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-chip {
  font-size: 13px;
  color: #1d4ed8;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.22);
  padding: 5px 10px;
  border-radius: 999px;
}
</style>
