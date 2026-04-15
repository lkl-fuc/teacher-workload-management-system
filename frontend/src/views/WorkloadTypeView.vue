<template>
  <el-card class="type-card" shadow="never">
    <template #header>
      <div class="card-header">
        <div class="header-title-group">
          <span class="header-title">工作量类型管理</span>
          <span class="header-subtitle">按「大类 - 小类」分组展示，支持快速维护。</span>
        </div>
        <div class="header-actions">
          <el-button @click="seedDefaultTypes" :loading="seedLoading">一键生成建议类型</el-button>
          <el-button type="primary" @click="openCreateDialog">新增类型</el-button>
        </div>
      </div>
    </template>

    <el-table
      v-loading="loading"
      :data="treeTableData"
      border
      stripe
      row-key="rowKey"
      :tree-props="{ children: 'children' }"
      default-expand-all
      class="type-table"
      :row-class-name="getRowClassName"
    >
      <el-table-column prop="id" label="ID" width="80">
        <template #default="scope">
          <span v-if="scope.row.isCategoryRow">—</span>
          <span v-else>{{ scope.row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="大类" min-width="120">
        <template #default="scope">
          <el-tag
            v-if="scope.row.categoryName"
            :type="scope.row.isCategoryRow ? 'primary' : 'info'"
            :effect="scope.row.isCategoryRow ? 'dark' : 'plain'"
            round
          >
            {{ scope.row.categoryName }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="subTypeName" label="细分类型" min-width="180">
        <template #default="scope">
          <span v-if="scope.row.isCategoryRow">请选择下拉箭头查看小类</span>
          <span v-else>{{ scope.row.subTypeName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="typeName" label="显示名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="unitValue" label="单位分值" min-width="110" />
      <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <template v-if="!scope.row.isCategoryRow">
            <el-button link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
          <span v-else>—</span>
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
          filterable
          clearable
          placeholder="请选择工作大类"
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
  专任教师岗: ['课堂授课', '教学准备', '教研活动'],
  实验教师岗: ['实验教学', '实验室指导', '实验室安全维护'],
  辅导员岗: ['学生管理', '思想教育', '日常事务'],
  教辅岗: ['图书资源服务', '设备保障服务', '教学秘书支持'],
  行政兼课岗: ['行政管理事务', '兼课教学任务', '行政教学协同'],
  外聘教师岗: ['外聘授课', '课程考核', '答疑辅导']
}

const defaultTypeList = [
  { categoryName: '专任教师岗', subTypeName: '课堂授课', unitValue: 8, remark: '专任教师每周授课工作量' },
  { categoryName: '专任教师岗', subTypeName: '教学准备', unitValue: 4, remark: '教案、备课与课程资源建设' },
  { categoryName: '专任教师岗', subTypeName: '教研活动', unitValue: 5, remark: '教研会议、听评课与教学改进' },
  { categoryName: '实验教师岗', subTypeName: '实验教学', unitValue: 8, remark: '实验课程教学与课堂组织' },
  { categoryName: '实验教师岗', subTypeName: '实验室指导', unitValue: 6, remark: '实验项目指导与答疑' },
  { categoryName: '实验教师岗', subTypeName: '实验室安全维护', unitValue: 5, remark: '实验室安全巡检与设备管理' },
  { categoryName: '辅导员岗', subTypeName: '学生管理', unitValue: 6, remark: '学生日常管理与数据维护' },
  { categoryName: '辅导员岗', subTypeName: '思想教育', unitValue: 7, remark: '主题班会、思政教育与谈心谈话' },
  { categoryName: '辅导员岗', subTypeName: '日常事务', unitValue: 5, remark: '请销假、奖助勤贷与突发事务处理' },
  { categoryName: '教辅岗', subTypeName: '图书资源服务', unitValue: 5, remark: '图书借阅管理、教材发放支持' },
  { categoryName: '教辅岗', subTypeName: '设备保障服务', unitValue: 6, remark: '教学设备巡检、报修与保障' },
  { categoryName: '教辅岗', subTypeName: '教学秘书支持', unitValue: 5, remark: '教学秘书排课、考试组织与资料归档' },
  { categoryName: '行政兼课岗', subTypeName: '行政管理事务', unitValue: 6, remark: '行政管理、流程审批与协调工作' },
  { categoryName: '行政兼课岗', subTypeName: '兼课教学任务', unitValue: 7, remark: '行政人员承担课程授课任务' },
  { categoryName: '行政兼课岗', subTypeName: '行政教学协同', unitValue: 5, remark: '教学检查、质量反馈与改进' },
  { categoryName: '外聘教师岗', subTypeName: '外聘授课', unitValue: 7, remark: '外聘教师课程授课任务' },
  { categoryName: '外聘教师岗', subTypeName: '课程考核', unitValue: 5, remark: '作业批改、考试命题与阅卷' },
  { categoryName: '外聘教师岗', subTypeName: '答疑辅导', unitValue: 4, remark: '课后答疑与学习辅导' }
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


const treeTableData = computed(() => {
  const grouped = new Map()
  const uncategorized = []

  tableData.value.forEach((item) => {
    const categoryName = String(item.categoryName || '').trim()
    if (!categoryName) {
      uncategorized.push({ ...item, rowKey: `type-${item.id}`, isCategoryRow: false })
      return
    }

    if (!grouped.has(categoryName)) {
      grouped.set(categoryName, [])
    }

    grouped.get(categoryName).push({ ...item, rowKey: `type-${item.id}`, isCategoryRow: false })
  })

  const categoryRows = [...grouped.entries()]
    .sort(([a], [b]) => a.localeCompare(b, 'zh-Hans-CN'))
    .map(([categoryName, children]) => ({
      id: null,
      rowKey: `category-${categoryName}`,
      isCategoryRow: true,
      categoryName,
      subTypeName: '',
      typeName: '',
      unitValue: '',
      remark: '',
      children: children.sort((a, b) => String(a.subTypeName || '').localeCompare(String(b.subTypeName || ''), 'zh-Hans-CN'))
    }))

  return [...uncategorized, ...categoryRows]
})

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

function getRowClassName({ row }) {
  return row.isCategoryRow ? 'category-row' : 'type-row'
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
.type-card {
  border: 0;
  border-radius: 16px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.header-subtitle {
  font-size: 13px;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

:deep(.type-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.type-table .category-row) {
  font-weight: 600;
  --el-table-tr-bg-color: #f8fafc;
}

:deep(.type-table .el-table__cell) {
  padding-top: 12px;
  padding-bottom: 12px;
}

:deep(.type-table th.el-table__cell) {
  background: #f1f5f9;
  color: #475569;
}

@media (max-width: 960px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
