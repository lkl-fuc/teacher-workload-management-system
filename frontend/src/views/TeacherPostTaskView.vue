<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>岗位任务面板</span>
        <el-tag type="success" effect="dark">当前岗位：{{ teacherPost }}</el-tag>
      </div>
    </template>

    <el-alert
      :title="postDesc.title"
      :description="postDesc.description"
      type="info"
      show-icon
      :closable="false"
      class="mb-16"
    />

    <el-row :gutter="16" class="mb-16">
      <el-col v-for="metric in postMetrics" :key="metric.label" :span="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value">{{ metric.value }}</div>
          <div class="metric-label">{{ metric.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="postTasks" border>
      <el-table-column prop="category" label="任务大类" width="150" />
      <el-table-column prop="subTask" label="细分任务" min-width="220" />
      <el-table-column prop="deadline" label="截止日期" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="协同对象" min-width="160" />
    </el-table>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const teacherPost = computed(() => localStorage.getItem('teacherPost') || '行政岗')

const postMap = {
  行政岗: {
    title: '行政岗以“流程执行 + 数据上报”为主',
    description: '重点关注值班、材料归档、考勤统计等流程型任务，可按周完成并提交记录。',
    metrics: [
      { label: '本周待办', value: 5 },
      { label: '已完成', value: 12 },
      { label: '逾期任务', value: 1 }
    ],
    tasks: [
      { category: '行政事务', subTask: '学院周例会纪要整理并上传', deadline: '2026-03-29', status: '进行中', owner: '办公室' },
      { category: '行政事务', subTask: '值班登记与巡检台账维护', deadline: '2026-03-30', status: '未开始', owner: '值班教师' },
      { category: '支撑服务', subTask: '教学材料归档与盖章流转', deadline: '2026-04-02', status: '已完成', owner: '教务秘书' }
    ]
  },
  管理岗: {
    title: '管理岗以“统筹管理 + 审核监督”为主',
    description: '重点关注部门任务分派、过程监督、绩效审核，任务通常关联多人协同。',
    metrics: [
      { label: '待审核', value: 8 },
      { label: '本月已审', value: 34 },
      { label: '需驳回', value: 2 }
    ],
    tasks: [
      { category: '管理工作', subTask: '二级学院工作量月度审核', deadline: '2026-03-31', status: '进行中', owner: '系主任' },
      { category: '质量督导', subTask: '春季课程质量巡查复盘', deadline: '2026-04-03', status: '未开始', owner: '督导组' },
      { category: '战略规划', subTask: '年度任务目标拆解确认', deadline: '2026-04-05', status: '已完成', owner: '分管领导' }
    ]
  },
  教辅岗: {
    title: '教辅岗以“教学保障 + 资源服务”为主',
    description: '重点关注教室设备、实训资源、考试组织等教辅任务，强调及时响应。',
    metrics: [
      { label: '今日工单', value: 6 },
      { label: '已处理', value: 18 },
      { label: '紧急任务', value: 2 }
    ],
    tasks: [
      { category: '教学保障', subTask: '多媒体教室设备巡检', deadline: '2026-03-28', status: '进行中', owner: '实验中心' },
      { category: '考试服务', subTask: '期中考试排考资源准备', deadline: '2026-04-01', status: '未开始', owner: '教务处' },
      { category: '实训支持', subTask: '实训室耗材补给申请', deadline: '2026-03-30', status: '已完成', owner: '实验员' }
    ]
  }
}

const currentPostData = computed(() => postMap[teacherPost.value] || postMap.行政岗)
const postDesc = computed(() => ({
  title: currentPostData.value.title,
  description: currentPostData.value.description
}))
const postMetrics = computed(() => currentPostData.value.metrics)
const postTasks = computed(() => currentPostData.value.tasks)

function statusType(status) {
  if (status === '已完成') return 'success'
  if (status === '进行中') return 'warning'
  return 'info'
}
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-card {
  text-align: center;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: #1d4ed8;
}

.metric-label {
  margin-top: 6px;
  color: var(--text-secondary);
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
