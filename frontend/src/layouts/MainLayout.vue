<template>
  <el-container class="layout-wrapper">
    <el-aside width="220px" class="aside">
      <div class="logo">工作量系统</div>
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
  background: #001529;
  color: #fff;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.header {
  padding: 0;
  height: 60px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 16px;
}
</style>
