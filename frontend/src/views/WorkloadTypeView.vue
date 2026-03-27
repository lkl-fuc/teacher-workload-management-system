<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>工作量类型管理（支持大类 + 细分项）</span>
        <div class="header-actions">
          <el-button @click="seedDefaultTypes" :loading="seedLoading">一键生成建议类型</el-button>
          <el-button type="primary" @click="openCreateDialog">新增类型</el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="categoryName" label="大类" min-width="120" />
      <el-table-column prop="subTypeName" label="细分类型" min-width="180" />
      <el-table-column prop="typeName" label="显示名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="unitValue" label="单位分值" min-width="110" />
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
    width="560px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="工作大类" prop="categoryName">
        <el-select
          v-model="form.categoryName"
          allow-create
          filterable
          default-first-option
          clearable
          placeholder="请选择或输入大类"
          style="width: 100%"
          @change="handleCategoryChange"
        >
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="细分类型" prop="subTypeName">
        <el-select
          v-model="form.subTypeName"
          allow-create
          filterable
          default-first-option
          clearable
          placeholder="请选择或输入细分类型"
          style="width: 100%"
        >
          <el-option v-for="item in currentSubTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="显示名称" prop="typeName">
        <el-input v-model="form.typeName" placeholder="自动生成，可手动覆盖" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const seedLoading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const formRef = ref()

const categoryDict = {
  论文: ['SCI', 'EI', 'CSSCI', '普通期刊', '会议论文'],
  项目: ['国家级', '省部级', '厅局级', '校级'],
  专利: ['发明专利', '实用新型专利', '外观设计专利', '国际专利', '软件著作权'],
  竞赛: ['教学能力竞赛', '说课比赛', '微课比赛', '青教赛'],
  教学: ['理论教学', '实验教学', '实训教学', '课程设计', '毕业设计（论文）指导']
}

const defaultTypeList = [
  { categoryName: '论文', subTypeName: 'SCI', unitValue: 25, remark: '高质量国际检索论文' },
  { categoryName: '论文', subTypeName: 'EI', unitValue: 20, remark: '工程索引收录论文' },
  { categoryName: '论文', subTypeName: 'CSSCI', unitValue: 16, remark: 'CSSCI来源期刊论文' },
  { categoryName: '论文', subTypeName: '普通期刊', unitValue: 10, remark: '一般中文期刊论文' },
  { categoryName: '论文', subTypeName: '会议论文', unitValue: 8, remark: '学术会议论文' },
  { categoryName: '项目', subTypeName: '国家级', unitValue: 30, remark: '国家级纵向项目' },
  { categoryName: '项目', subTypeName: '省部级', unitValue: 20, remark: '省部级项目' },
  { categoryName: '项目', subTypeName: '厅局级', unitValue: 12, remark: '厅局级项目' },
  { categoryName: '项目', subTypeName: '校级', unitValue: 8, remark: '校级教改或科研项目' },
  { categoryName: '专利', subTypeName: '发明专利', unitValue: 24, remark: '已授权发明专利' },
  { categoryName: '专利', subTypeName: '实用新型专利', unitValue: 12, remark: '已授权实用新型专利' },
  { categoryName: '专利', subTypeName: '外观设计专利', unitValue: 8, remark: '已授权外观设计专利' },
  { categoryName: '专利', subTypeName: '国际专利', unitValue: 30, remark: 'PCT或境外授权专利' },
  { categoryName: '专利', subTypeName: '软件著作权', unitValue: 10, remark: '软件著作权登记' },
  { categoryName: '竞赛', subTypeName: '教学能力竞赛', unitValue: 12, remark: '教师教学能力类竞赛' },
  { categoryName: '竞赛', subTypeName: '说课比赛', unitValue: 8, remark: '说课类竞赛' },
  { categoryName: '竞赛', subTypeName: '微课比赛', unitValue: 8, remark: '微课制作比赛' },
  { categoryName: '竞赛', subTypeName: '青教赛', unitValue: 12, remark: '青年教师教学竞赛' },
  { categoryName: '教学', subTypeName: '理论教学', unitValue: 6, remark: '理论课课堂教学' },
  { categoryName: '教学', subTypeName: '实验教学', unitValue: 7, remark: '实验课教学' },
  { categoryName: '教学', subTypeName: '实训教学', unitValue: 8, remark: '实训课程教学' },
  { categoryName: '教学', subTypeName: '课程设计', unitValue: 9, remark: '课程设计指导' },
  { categoryName: '教学', subTypeName: '毕业设计（论文）指导', unitValue: 12, remark: '毕业设计或毕业论文指导' }
]

const form = reactive({
  typeName: '',
  categoryName: '',
  subTypeName: '',
  unitValue: 0,
  remark: ''
})

const rules = {
  categoryName: [{ required: true, message: '请选择或输入工作大类', trigger: 'change' }],
  subTypeName: [{ required: true, message: '请选择或输入细分类型', trigger: 'change' }],
  typeName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  unitValue: [{ required: true, message: '请输入单位分值', trigger: 'change' }]
}

const API_BASE = '/api/workload-types'

const categoryOptions = computed(() => {
  const fromTable = tableData.value.map((item) => item.categoryName).filter(Boolean)
  return [...new Set([...Object.keys(categoryDict), ...fromTable])]
})

const currentSubTypeOptions = computed(() => {
  const selected = form.categoryName
  const fromDict = selected ? categoryDict[selected] || [] : []
  const fromTable = tableData.value
    .filter((item) => item.categoryName === selected)
    .map((item) => item.subTypeName)
    .filter(Boolean)
  return [...new Set([...fromDict, ...fromTable])]
})

function normalizeError(error, fallback) {
  if (error?.message) return error.message
  return fallback
}

function buildDisplayName(categoryName, subTypeName) {
  if (categoryName && subTypeName) return `${categoryName} - ${subTypeName}`
  return categoryName || subTypeName || ''
}

function syncTypeName() {
  form.typeName = buildDisplayName(form.categoryName, form.subTypeName)
}

function handleCategoryChange() {
  syncTypeName()
}

watch(
  () => [form.categoryName, form.subTypeName],
  () => {
    syncTypeName()
  }
)

function resetForm() {
  form.typeName = ''
  form.categoryName = ''
  form.subTypeName = ''
  form.unitValue = 0
  form.remark = ''
  currentId.value = null
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
    const rows = Array.isArray(data) ? data : []
    tableData.value = rows.sort((a, b) => {
      const categoryCompare = String(a.categoryName || '').localeCompare(String(b.categoryName || ''), 'zh-Hans-CN')
      if (categoryCompare !== 0) return categoryCompare
      return String(a.subTypeName || '').localeCompare(String(b.subTypeName || ''), 'zh-Hans-CN')
    })
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
  form.categoryName = row.categoryName || ''
  form.subTypeName = row.subTypeName || ''
  form.typeName = row.typeName || buildDisplayName(form.categoryName, form.subTypeName)
  form.unitValue = Number(row.unitValue ?? 0)
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return

  form.typeName = buildDisplayName(form.categoryName, form.subTypeName) || form.typeName

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = {
        categoryName: form.categoryName,
        subTypeName: form.subTypeName,
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

async function seedDefaultTypes() {
  seedLoading.value = true
  try {
    let createdCount = 0
    for (const item of defaultTypeList) {
      try {
        const payload = {
          ...item,
          typeName: buildDisplayName(item.categoryName, item.subTypeName)
        }
        await request(API_BASE, {
          method: 'POST',
          body: JSON.stringify(payload)
        })
        createdCount += 1
      } catch (error) {
        const message = normalizeError(error, '')
        if (!message.includes('已存在')) {
          throw error
        }
      }
    }

    await loadTableData()
    ElMessage.success(createdCount > 0 ? `已新增 ${createdCount} 条建议类型` : '建议类型已存在，无需重复新增')
  } catch (error) {
    ElMessage.error(normalizeError(error, '生成建议类型失败'))
  } finally {
    seedLoading.value = false
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
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
