<template>
  <header class="top-bar">
    <div class="system-name">教师工作量管理系统</div>
    <div class="right-area">
      <span class="admin">管理员</span>
      <el-button size="small" type="danger" plain @click="logout">退出登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.top-bar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.system-name {
  font-size: 18px;
  font-weight: 600;
}

.right-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin {
  color: #606266;
}
</style>
