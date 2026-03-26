<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>我的工作量列表</span>
        <div class="header-actions">
          <el-select
            v-model="statusFilter"
            placeholder="按状态筛选"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="按标题/描述/审核状态搜索"
            clearable
            style="width: 280px"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>
    </template>

    <el-table v-loading="tableLoading" :data="pagedRows" border>
      <el-table-column prop="workloadTitle" label="工作标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="amount" label="分值" width="100" />
      <el-table-column prop="submitDate" label="提交日期" width="120" />
      <el-table-column label="审核状态" width="110">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驳回原因" min-width="200">
        <template #default="scope">
          <span>{{ scope.row.rejectReason || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="warning"
            :disabled="!canResubmit(scope.row.status)"
            @click="handleResubmit(scope.row)"
          >
            重新提交
          </el-button>
          <el-popconfirm
            title="确认删除该记录吗？"
            :disabled="!canModify(scope.row.status)"
            @confirm="handleDelete(scope.row.id)"
          >
            <template #reference>
              <el-button link type="danger" :disabled="!canModify(scope.row.status)">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[5, 10, 20, 50]"
        :total="filteredRows.length"
      />
    </div>
  </el-card>

</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const tableLoading = ref(false)
const submitLoading = ref(false)
const rows = ref([])
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)

function normalizeError(error, fallback) {
  if (error?.message) return error.message
  return fallback
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })

  if (!response.ok) {
    let message = '请求失败'
    try {
      const errorData = await response.json()
      message = errorData.message || message
    } catch {
      // ignore parse errors
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

const filteredRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()

  return rows.value.filter((item) => {
    if (statusFilter.value && String(item.status || '').toUpperCase() !== statusFilter.value) {
      return false
    }
    if (!q) return true
    const status = statusText(item.status)
    return [item.workloadTitle, item.description, status, item.rejectReason]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(q))
  })
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

function handleSearch() {
  page.value = 1
}

function canResubmit(status) {
  return String(status || '').toUpperCase() === 'REJECTED'
}

function canModify(status) {
  return String(status || '').toUpperCase() !== 'APPROVED'
}

function statusText(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已驳回'
  if (value === 'PENDING') return '待审核'
  return status || '未知'
}

function statusTagType(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  if (value === 'PENDING') return 'warning'
  return 'info'
}

function resolveTeacherId() {
  const candidates = [
    localStorage.getItem('teacherId'),
    localStorage.getItem('userId'),
    localStorage.getItem('id')
  ]

  for (const raw of candidates) {
    const id = Number(raw)
    if (!Number.isNaN(id) && id > 0) return id
  }
  return null
}

async function loadRows() {
  const teacherId = resolveTeacherId()
  if (!teacherId) {
    ElMessage.warning('未获取到教师ID，暂时无法加载我的工作量')
    rows.value = []
    return
  }

  tableLoading.value = true
  try {
    const data = await request(`/api/workloads/teacher/${teacherId}`)
    rows.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(normalizeError(error, '加载工作量失败'))
  } finally {
    tableLoading.value = false
  }
}

async function handleResubmit(row) {
  if (!canResubmit(row.status)) {
    ElMessage.warning('仅已驳回记录可重新提交')
    return
  }

  submitLoading.value = true
  try {
    await request(`/api/workloads/${row.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        status: 'PENDING',
        rejectReason: ''
      })
    })
    ElMessage.success('已重新提交，等待管理员审核')
    await loadRows()
  } catch (error) {
    ElMessage.error(normalizeError(error, '重新提交失败'))
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id) {
  try {
    await request(`/api/workloads/${id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    await loadRows()
  } catch (error) {
    ElMessage.error(normalizeError(error, '删除失败'))
  }
}

onMounted(() => {
  loadRows()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
