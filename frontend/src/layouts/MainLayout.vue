<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="layout-aside">
      <div class="system-logo">教师工作量管理</div>
      <SidebarMenu />
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <TopBar />
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import SidebarMenu from '../components/SidebarMenu.vue'
import TopBar from '../components/TopBar.vue'
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.layout-aside {
  background: #001529;
  color: #fff;
}

.system-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #eceef2;
  padding: 0;
}

.layout-main {
  background: #f5f7fa;
  padding: 16px;
}
</style>
