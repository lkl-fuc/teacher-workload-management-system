<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>{{ isAdmin ? '全体教师工作量列表' : '我的提交记录' }}</span>
        <div class="header-actions">
          <el-select
            v-model="statusFilter"
            placeholder="按状态筛选"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="按标题/描述/状态搜索"
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
      <el-table-column v-if="isAdmin" prop="teacherName" label="教师" width="140" />
      <el-table-column prop="workloadTitle" label="工作标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="完成情况描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="amount" label="分值" width="100" />
      <el-table-column prop="submitDate" label="提交日期" width="120" />
      <el-table-column label="审核状态" width="120">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驳回原因" min-width="200">
        <template #default="scope">
          <span>{{ scope.row.rejectReason || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="isAdmin ? 180 : 220" fixed="right">
        <template #default="scope">
          <template v-if="isAdmin">
            <el-button
              link
              type="success"
              :disabled="!canAudit(scope.row.status)"
              @click="handleApprove(scope.row)"
            >
              审核通过
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="!canAudit(scope.row.status)"
              @click="openRejectDialog(scope.row)"
            >
              驳回
            </el-button>
          </template>
          <template v-else>
            <el-button
              link
              type="primary"
              :disabled="!canFill(scope.row.status)"
              @click="openSubmitDialog(scope.row)"
            >
              {{ submitBtnText(scope.row.status) }}
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

  <el-dialog v-model="submitVisible" title="填写任务完成情况" width="560px" destroy-on-close>
    <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="100px">
      <el-form-item label="完成情况" prop="description">
        <el-input
          v-model="submitForm.description"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          placeholder="请填写任务完成情况"
        />
      </el-form-item>
      <el-form-item label="完成分值" prop="amount">
        <el-input-number
          v-model="submitForm.amount"
          :min="0"
          :max="999999"
          :precision="2"
          :step="0.5"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="完成日期" prop="submitDate">
        <el-date-picker
          v-model="submitForm.submitDate"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          placeholder="请选择完成日期"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeSubmitDialog">取消</el-button>
      <el-button type="primary" @click="handleTeacherSubmit">提交给管理员</el-button>
    </template>
  </el-dialog>

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
      <el-button @click="closeRejectDialog">取消</el-button>
      <el-button type="danger" @click="handleReject">确认驳回</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())
const isAdmin = computed(() => role.value === 'ADMIN')

const tableLoading = ref(false)
const rows = ref([])
const teacherMap = ref({})
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)

const submitVisible = ref(false)
const submitFormRef = ref()
const submitForm = reactive({
  id: null,
  description: '',
  amount: 0,
  submitDate: ''
})

const rejectVisible = ref(false)
const rejectFormRef = ref()
const rejectForm = reactive({
  id: null,
  rejectReason: ''
})

const submitRules = {
  description: [{ required: true, message: '请填写完成情况', trigger: 'blur' }],
  amount: [{ required: true, message: '请填写完成分值', trigger: 'change' }],
  submitDate: [{ required: true, message: '请选择完成日期', trigger: 'change' }]
}

const rejectRules = {
  rejectReason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

const statusOptions = computed(() => {
  const common = [
    { label: '待审核', value: 'PENDING' },
    { label: '已通过', value: 'APPROVED' },
    { label: '已驳回', value: 'REJECTED' }
  ]
  if (isAdmin.value) {
    return [{ label: '已分发', value: 'ASSIGNED' }, ...common]
  }
  return [{ label: '待填写', value: 'ASSIGNED' }, ...common]
})

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

const displayRows = computed(() => rows.value.map((item) => ({
  ...item,
  teacherName: teacherMap.value[item.teacherId] || `教师#${item.teacherId}`
})))

const filteredRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()

  return displayRows.value.filter((item) => {
    if (statusFilter.value && String(item.status || '').toUpperCase() !== statusFilter.value) {
      return false
    }
    if (!q) return true
    const status = statusText(item.status)
    return [item.workloadTitle, item.description, status, item.rejectReason, item.teacherName]
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
  if (value === 'ASSIGNED') return isAdmin.value ? '已分发' : '待填写'
  return status || '未知'
}

function statusTagType(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  if (value === 'PENDING') return 'warning'
  if (value === 'ASSIGNED') return 'info'
  return 'info'
}

function canAudit(status) {
  return String(status || '').toUpperCase() === 'PENDING'
}

function canFill(status) {
  const value = String(status || '').toUpperCase()
  return value === 'ASSIGNED' || value === 'REJECTED'
}

function canModify(status) {
  return !['APPROVED', 'PENDING'].includes(String(status || '').toUpperCase())
}

function submitBtnText(status) {
  return String(status || '').toUpperCase() === 'REJECTED' ? '重新完成并提交' : '填写完成并提交'
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

async function loadTeacherMap() {
  const data = await request('/api/teachers')
  const map = {}
  ;(Array.isArray(data) ? data : []).forEach((item) => {
    map[item.id] = item.name
  })
  teacherMap.value = map
}

async function loadRows() {
  tableLoading.value = true
  try {
    await loadTeacherMap()
    if (isAdmin.value) {
      const data = await request('/api/workloads')
      rows.value = Array.isArray(data) ? data : []
    } else {
      const teacherId = resolveTeacherId()
      if (!teacherId) {
        ElMessage.warning('未获取到教师ID，暂时无法加载我的工作量')
        rows.value = []
        return
      }
      const data = await request(`/api/workloads/teacher/${teacherId}`)
      rows.value = Array.isArray(data) ? data : []
    }
  } catch (error) {
    ElMessage.error(normalizeError(error, '加载工作量失败'))
  } finally {
    tableLoading.value = false
  }
}

function openSubmitDialog(row) {
  submitForm.id = row.id
  submitForm.description = row.description || ''
  submitForm.amount = Number(row.amount || 0)
  submitForm.submitDate = row.submitDate || ''
  submitVisible.value = true
}

function closeSubmitDialog() {
  submitVisible.value = false
  submitFormRef.value?.clearValidate()
}

async function handleTeacherSubmit() {
  if (!submitFormRef.value) return
  await submitFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await request(`/api/workloads/${submitForm.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          description: submitForm.description,
          amount: submitForm.amount,
          submitDate: submitForm.submitDate,
          status: 'PENDING',
          rejectReason: ''
        })
      })
      ElMessage.success('提交成功，已发送给管理员审核')
      submitVisible.value = false
      await loadRows()
    } catch (error) {
      ElMessage.error(normalizeError(error, '提交失败'))
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

async function handleApprove(row) {
  try {
    await request(`/api/workloads/${row.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        status: 'APPROVED',
        rejectReason: ''
      })
    })
    ElMessage.success('审核通过，已通知教师')
    await loadRows()
  } catch (error) {
    ElMessage.error(normalizeError(error, '审核通过失败'))
  }
}

function openRejectDialog(row) {
  rejectForm.id = row.id
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

function closeRejectDialog() {
  rejectVisible.value = false
  rejectFormRef.value?.clearValidate()
}

async function handleReject() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      await request(`/api/workloads/${rejectForm.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          status: 'REJECTED',
          rejectReason: rejectForm.rejectReason
        })
      })
      ElMessage.success('已驳回并通知教师重新完成')
      rejectVisible.value = false
      await loadRows()
    } catch (error) {
      ElMessage.error(normalizeError(error, '驳回失败'))
    }
  })
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
