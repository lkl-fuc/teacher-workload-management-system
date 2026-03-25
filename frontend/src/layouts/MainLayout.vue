<template>
  <el-container class="layout-wrapper">
    <el-aside width="236px" class="aside">
      <div class="logo-wrap">
        <div class="logo-badge">TW</div>
        <div>
          <div class="logo-title">工作量系统</div>
          <div class="logo-sub">Teacher Workload</div>
        </div>
      </div>
      <SideMenu />
    </el-aside>

    <el-container>
      <el-header class="header">
        <TopBar />
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import SideMenu from '../components/SideMenu.vue'
import TopBar from '../components/TopBar.vue'
</script>

<style scoped>
.layout-wrapper {
  width: 100%;
  height: 100%;
}

.aside {
  background: linear-gradient(180deg, #1d4ed8 0%, #1e3a8a 100%);
  color: #fff;
  border-right: 1px solid rgba(255, 255, 255, 0.16);
}

.logo-wrap {
  height: 74px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
}

.logo-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  font-weight: 700;
}

.logo-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.logo-sub {
  margin-top: 2px;
  font-size: 12px;
  opacity: 0.76;
}

.header {
  padding: 0;
  height: 68px;
  border-bottom: 1px solid var(--border-color);
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
}

.main-content {
  padding: 20px;
  background: transparent;
}
</style>
