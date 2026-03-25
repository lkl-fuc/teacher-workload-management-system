<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>工作量类型管理</span>
        <el-button type="primary" @click="openCreateDialog">新增类型</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="typeName" label="类型名称" min-width="180" />
      <el-table-column prop="unitValue" label="单位分值" min-width="120" />
      <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑工作量类型' : '新增工作量类型'"
    width="500px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="类型名称" prop="typeName">
        <el-input v-model="form.typeName" placeholder="请输入类型名称" />
      </el-form-item>
      <el-form-item label="单位分值" prop="unitValue">
        <el-input-number
          v-model="form.unitValue"
          :min="0"
          :max="999999"
          :precision="2"
          :step="0.5"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const formRef = ref()

const form = reactive({
  typeName: '',
  unitValue: 0,
  remark: ''
})

const rules = {
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  unitValue: [{ required: true, message: '请输入单位分值', trigger: 'change' }]
}

const API_BASE = '/api/workload-types'

function resetForm() {
  form.typeName = ''
  form.unitValue = 0
  form.remark = ''
  currentId.value = null
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

async function loadTableData() {
  loading.value = true
  try {
    const data = await request(API_BASE)
    tableData.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(normalizeError(error, '获取列表失败'))
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  currentId.value = row.id
  form.typeName = row.typeName || ''
  form.unitValue = Number(row.unitValue ?? 0)
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = {
        typeName: form.typeName,
        unitValue: form.unitValue,
        remark: form.remark
      }

      if (isEdit.value && currentId.value) {
        await request(`${API_BASE}/${currentId.value}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        })
        ElMessage.success('编辑成功')
      } else {
        await request(API_BASE, {
          method: 'POST',
          body: JSON.stringify(payload)
        })
        ElMessage.success('新增成功')
      }

      dialogVisible.value = false
      await loadTableData()
    } catch (error) {
      ElMessage.error(normalizeError(error, '保存失败'))
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.typeName}”吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })

    await request(`${API_BASE}/${row.id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    await loadTableData()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(normalizeError(error, '删除失败'))
  }
}

onMounted(() => {
  loadTableData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
