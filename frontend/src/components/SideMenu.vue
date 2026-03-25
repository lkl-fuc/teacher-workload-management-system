<template>
  <el-menu
    :default-active="activeMenu"
    class="side-menu"
    router
    background-color="transparent"
    text-color="rgba(255,255,255,0.86)"
    active-text-color="#ffffff"
  >
    <el-menu-item index="/home">
      <el-icon><House /></el-icon>
      <span>首页</span>
    </el-menu-item>
    <el-menu-item index="/workload-types">
      <el-icon><List /></el-icon>
      <span>工作量类型管理</span>
    </el-menu-item>
    <el-menu-item index="/workloads/new">
      <el-icon><Plus /></el-icon>
      <span>新增工作量</span>
    </el-menu-item>
    <el-menu-item index="/workloads/my">
      <el-icon><Tickets /></el-icon>
      <span>我的工作量列表</span>
    </el-menu-item>
    <el-menu-item index="/workloads/audit">
      <el-icon><Finished /></el-icon>
      <span>工作量审核</span>
    </el-menu-item>
    <el-menu-item index="/workloads/stats">
      <el-icon><PieChart /></el-icon>
      <span>工作量统计分析</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Finished, House, List, PieChart, Plus, Tickets } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  border-right: none;
  height: calc(100% - 74px);
  padding: 12px;
}

.side-menu :deep(.el-menu-item) {
  margin-bottom: 6px;
  border-radius: 10px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.14);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.2);
  font-weight: 600;
}
</style>
