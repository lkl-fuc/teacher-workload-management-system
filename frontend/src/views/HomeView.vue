<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-title">{{ route.meta.title || '首页' }}</div>
    </template>

    <el-empty description="基础框架已搭建，可在此页面继续扩展业务模块。" />
  </el-card>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
