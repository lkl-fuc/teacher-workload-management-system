<template>
  <div class="topbar">
    <div class="left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>系统首页</el-breadcrumb-item>
        <el-breadcrumb-item>{{ route.meta.title || '概览' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <span class="username">管理员</span>
      <el-button link type="danger" @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.topbar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #606266;
  font-size: 14px;
}
</style>
