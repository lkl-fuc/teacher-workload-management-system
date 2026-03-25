<template>
  <el-menu
    router
    :default-active="activePath"
    background-color="#001529"
    text-color="#bfcbd9"
    active-text-color="#409eff"
    class="menu"
  >
    <el-menu-item
      v-for="item in menuItems"
      :key="item.path"
      :index="item.path"
    >
      <el-icon><component :is="item.meta.icon" /></el-icon>
      <span>{{ item.meta.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const menuItems = computed(() => {
  const rootRoute = router.options.routes.find((item) => item.path === '/')
  return rootRoute?.children || []
})

const activePath = computed(() => route.path)
</script>

<style scoped>
.menu {
  border-right: none;
  height: calc(100vh - 56px);
}
</style>
