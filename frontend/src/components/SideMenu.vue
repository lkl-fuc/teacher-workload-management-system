<template>
  <el-menu
    :default-active="activeMenu"
    class="side-menu"
    router
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
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { House, List, Plus, Tickets } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  border-right: none;
  height: 100%;
}
</style>
