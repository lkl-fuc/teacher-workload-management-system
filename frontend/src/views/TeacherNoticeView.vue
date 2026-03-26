<template>
  <div class="page-wrap">
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-title">通知公告</div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="item in notices"
          :key="item.title"
          :timestamp="item.date"
          placement="top"
          :type="item.type"
        >
          <el-card shadow="hover">
            <div class="notice-title">{{ item.title }}</div>
            <div class="notice-content">{{ item.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
const notices = [
  { date: '2026-03-20', title: '春季学期工作量填报开始', content: '请各位教师在 4 月 10 日前完成本月教学与科研工作量填报。', type: 'primary' },
  { date: '2026-03-18', title: '新增科研项目计分规则', content: '校级科研项目新增“阶段验收通过”积分项，请按新口径填报。', type: 'success' },
  { date: '2026-03-15', title: '系统维护通知', content: '本周六 22:00-23:00 系统进行维护，期间可能短暂不可用。', type: 'warning' }
]
</script>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  border-radius: 12px;
}

.section-title {
  font-weight: 600;
  color: #0f172a;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e3a8a;
}

.notice-content {
  margin-top: 8px;
  color: #475569;
  line-height: 1.8;
}
</style>
