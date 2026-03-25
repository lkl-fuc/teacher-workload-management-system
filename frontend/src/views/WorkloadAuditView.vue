<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>管理员审核</span>
        <el-button type="primary" :loading="tableLoading" @click="loadPendingRows">刷新</el-button>
      </div>
    </template>

    <el-table v-loading="tableLoading" :data="rows" border>
      <el-table-column prop="workloadTitle" label="工作标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="teacherId" label="教师ID" width="90" />
      <el-table-column prop="typeId" label="类型ID" width="90" />
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="amount" label="分值" width="100" />
      <el-table-column prop="submitDate" label="提交日期" width="120" />
      <el-table-column label="审核状态" width="110">
        <template #default="scope">
          <el-tag type="warning">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="success"
            :loading="actionLoadingId === scope.row.id && actionType === 'approve'"
            @click="handleApprove(scope.row)"
          >
            审核通过
          </el-button>
          <el-button link type="danger" @click="openRejectDialog(scope.row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!tableLoading && rows.length === 0" description="暂无待审核工作量" />
  </el-card>

  <el-dialog v-model="rejectVisible" title="驳回原因" width="500px" destroy-on-close>
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
      <el-form-item label="驳回原因" prop="rejectReason">
        <el-input
          v-model="rejectForm.rejectReason"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="请填写驳回原因"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCloseRejectDialog">取消</el-button>
      <el-button
        type="danger"
        :loading="actionLoadingId === rejectForm.id && actionType === 'reject'"
        @click="handleReject"
      >
        确认驳回
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const rows = ref([])
const tableLoading = ref(false)
const rejectVisible = ref(false)
const rejectFormRef = ref()
const actionLoadingId = ref(null)
const actionType = ref('')

const rejectForm = reactive({
  id: null,
  rejectReason: ''
})

const rejectRules = {
  rejectReason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

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

function statusText(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'PENDING') return '待审核'
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已驳回'
  return status || '未知'
}

async function loadPendingRows() {
  tableLoading.value = true
  try {
    const data = await request('/api/workloads')
    rows.value = (Array.isArray(data) ? data : []).filter(
      (item) => String(item.status || '').toUpperCase() === 'PENDING'
    )
  } catch (error) {
    ElMessage.error(normalizeError(error, '加载待审核工作量失败'))
  } finally {
    tableLoading.value = false
  }
}

async function handleApprove(row) {
  actionLoadingId.value = row.id
  actionType.value = 'approve'
  try {
    await request(`/api/workloads/${row.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        status: 'APPROVED',
        rejectReason: ''
      })
    })
    ElMessage.success('审核通过成功')
    await loadPendingRows()
  } catch (error) {
    ElMessage.error(normalizeError(error, '审核通过失败'))
  } finally {
    actionLoadingId.value = null
    actionType.value = ''
  }
}

function openRejectDialog(row) {
  rejectForm.id = row.id
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

function handleCloseRejectDialog() {
  rejectVisible.value = false
  rejectFormRef.value?.clearValidate()
}

async function handleReject() {
  if (!rejectFormRef.value) return

  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return

    actionLoadingId.value = rejectForm.id
    actionType.value = 'reject'

    try {
      await request(`/api/workloads/${rejectForm.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          status: 'REJECTED',
          rejectReason: rejectForm.rejectReason
        })
      })
      ElMessage.success('驳回成功')
      rejectVisible.value = false
      await loadPendingRows()
    } catch (error) {
      ElMessage.error(normalizeError(error, '驳回失败'))
    } finally {
      actionLoadingId.value = null
      actionType.value = ''
    }
  })
}

onMounted(() => {
  loadPendingRows()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
