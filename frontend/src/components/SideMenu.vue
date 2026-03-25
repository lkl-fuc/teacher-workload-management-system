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
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { House } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  border-right: none;
  height: 100%;
}
</style>
