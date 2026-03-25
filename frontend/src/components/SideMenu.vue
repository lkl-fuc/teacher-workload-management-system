<template>
  <el-menu
    :default-active="activeMenu"
    class="side-menu"
    router
    text-color="#c0c4cc"
    active-text-color="#409eff"
    background-color="#001529"
  >
    <el-menu-item index="/home">
      <el-icon><House /></el-icon>
      <span>首页</span>
    </el-menu-item>

    <el-menu-item index="/workload-types">
      <el-icon><Collection /></el-icon>
      <span>工作量类型管理</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Collection, House } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  border-right: none;
  height: calc(100% - 60px);
}
</style>
