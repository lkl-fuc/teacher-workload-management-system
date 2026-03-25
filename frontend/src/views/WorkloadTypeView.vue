<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>工作量类型管理</span>
        <el-button type="primary" @click="openCreateDialog">新增类型</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="typeName" label="类型名称" min-width="180" />
      <el-table-column prop="unitValue" label="单位工作量" min-width="140" />
      <el-table-column prop="remark" label="备注" min-width="240" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工作量类型' : '新增工作量类型'" width="520px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="类型名称" prop="typeName">
        <el-input v-model="formData.typeName" placeholder="如：理论课时" />
      </el-form-item>
      <el-form-item label="单位工作量" prop="unitValue">
        <el-input-number v-model="formData.unitValue" :min="0" :precision="2" :step="0.1" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="可选" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()

const formData = reactive({
  typeName: '',
  unitValue: 0,
  remark: ''
})

const rules = {
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  unitValue: [{ required: true, message: '请输入单位工作量', trigger: 'change' }]
}

function resetForm() {
  formData.typeName = ''
  formData.unitValue = 0
  formData.remark = ''
  editingId.value = null
}

async function request(url, options = {}) {
  const response = await fetch(`${API_BASE}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message = typeof data === 'object' ? data.message : data
    throw new Error(message || '请求失败')
  }

  return data
}

async function fetchList() {
  loading.value = true
  try {
    tableData.value = await request('/api/workload-types')
  } catch (error) {
    ElMessage.error(error.message || '加载列表失败')
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
  editingId.value = row.id
  formData.typeName = row.typeName
  formData.unitValue = Number(row.unitValue)
  formData.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      typeName: formData.typeName,
      unitValue: formData.unitValue,
      remark: formData.remark
    }

    if (isEdit.value && editingId.value) {
      await request(`/api/workload-types/${editingId.value}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      })
      ElMessage.success('更新成功')
    } else {
      await request('/api/workload-types', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    await fetchList()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除类型「${row.typeName}」吗？`, '删除确认', {
      type: 'warning'
    })

    await request(`/api/workload-types/${row.id}`, {
      method: 'DELETE'
    })

    ElMessage.success('删除成功')
    await fetchList()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
