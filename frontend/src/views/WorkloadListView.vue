<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>我的工作量列表</span>
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
          <el-button link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-popconfirm title="确认删除该记录吗？" @confirm="handleDelete(scope.row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
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

  <el-dialog v-model="editVisible" title="编辑工作量" width="560px" destroy-on-close>
    <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
      <el-form-item label="工作标题" prop="workloadTitle">
        <el-input v-model="editForm.workloadTitle" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="editForm.description"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="分值" prop="amount">
        <el-input-number
          v-model="editForm.amount"
          :min="0"
          :max="999999"
          :precision="2"
          :step="0.5"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="提交日期" prop="submitDate">
        <el-date-picker
          v-model="editForm.submitDate"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSaveEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const tableLoading = ref(false)
const submitLoading = ref(false)
const rows = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)

const editVisible = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  workloadTitle: '',
  description: '',
  amount: 0,
  submitDate: ''
})

const editRules = {
  workloadTitle: [{ required: true, message: '请输入工作标题', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入分值', trigger: 'change' }],
  submitDate: [{ required: true, message: '请选择提交日期', trigger: 'change' }]
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

const filteredRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return rows.value

  return rows.value.filter((item) => {
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

function openEditDialog(row) {
  editForm.id = row.id
  editForm.workloadTitle = row.workloadTitle || ''
  editForm.description = row.description || ''
  editForm.amount = row.amount ?? 0
  editForm.submitDate = row.submitDate || ''
  editVisible.value = true
}

async function handleSaveEdit() {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = {
        workloadTitle: editForm.workloadTitle,
        description: editForm.description,
        amount: editForm.amount,
        submitDate: editForm.submitDate
      }
      await request(`/api/workloads/${editForm.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      })

      ElMessage.success('编辑成功')
      editVisible.value = false
      await loadRows()
    } catch (error) {
      ElMessage.error(normalizeError(error, '编辑失败'))
    } finally {
      submitLoading.value = false
    }
  })
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

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
