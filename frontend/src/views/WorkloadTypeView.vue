<template>
  <el-card class="type-card" shadow="never">
    <template #header>
      <div class="card-header">
        <div class="header-title-group">
          <span class="header-title">工作量类型管理</span>
          <span class="header-subtitle">按「岗位大类 - 细分任务 - 考核标准」展示，支持差异化维护。</span>
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
          <span v-if="scope.row.isCategoryRow">请选择下拉箭头查看细分考核项</span>
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
  专任教师岗: ['理论课授课达成', '课程资源建设', '作业批改与反馈', '教研听评课'],
  实验教师岗: ['实验课程实施', '实验项目指导', '实验室安全巡检', '仪器设备台账'],
  辅导员岗: ['学生分层管理', '思想教育活动', '谈心谈话干预', '奖助勤贷事务'],
  教辅岗: ['排课排考支持', '教材资料服务', '教学设备保障', '教学档案质检'],
  行政兼课岗: ['行政任务执行', '兼课授课质量', '流程优化改进', '教学检查整改'],
  外聘教师岗: ['协议课程授课', '课程考核阅卷', '课后答疑支持', '教学资料归档']
}

const defaultTypeList = [
  { categoryName: '专任教师岗', subTypeName: '理论课授课达成', unitValue: 9, remark: '考核：年度授课学时达标、课堂巡查合格率≥95%、教学事故为0' },
  { categoryName: '专任教师岗', subTypeName: '课程资源建设', unitValue: 6, remark: '考核：教案/课件更新完整，课程资源学期更新次数达标' },
  { categoryName: '专任教师岗', subTypeName: '作业批改与反馈', unitValue: 5, remark: '考核：批改及时率≥98%，学业预警反馈闭环率≥95%' },
  { categoryName: '专任教师岗', subTypeName: '教研听评课', unitValue: 6, remark: '考核：教研参与次数达标，听评课记录齐全并形成改进报告' },

  { categoryName: '实验教师岗', subTypeName: '实验课程实施', unitValue: 8, remark: '考核：实验开出率100%，课堂组织规范、无重大差错' },
  { categoryName: '实验教师岗', subTypeName: '实验项目指导', unitValue: 7, remark: '考核：实验项目指导覆盖完整，学生满意度≥90%' },
  { categoryName: '实验教师岗', subTypeName: '实验室安全巡检', unitValue: 6, remark: '考核：巡检台账完整，隐患整改闭环率100%' },
  { categoryName: '实验教师岗', subTypeName: '仪器设备台账', unitValue: 5, remark: '考核：设备完好率≥98%，台账一致性100%' },

  { categoryName: '辅导员岗', subTypeName: '学生分层管理', unitValue: 7, remark: '考核：重点学生台账完整，预警处置及时率100%' },
  { categoryName: '辅导员岗', subTypeName: '思想教育活动', unitValue: 7, remark: '考核：主题教育活动覆盖率100%，过程材料齐全' },
  { categoryName: '辅导员岗', subTypeName: '谈心谈话干预', unitValue: 6, remark: '考核：重点学生谈话与回访记录齐全，闭环率≥95%' },
  { categoryName: '辅导员岗', subTypeName: '奖助勤贷事务', unitValue: 5, remark: '考核：事务办理准确率100%，投诉率低于1%' },

  { categoryName: '教辅岗', subTypeName: '排课排考支持', unitValue: 6, remark: '考核：排课排考零重大差错，关键节点按时完成率100%' },
  { categoryName: '教辅岗', subTypeName: '教材资料服务', unitValue: 5, remark: '考核：教材发放及时准确，资源借用登记完整' },
  { categoryName: '教辅岗', subTypeName: '教学设备保障', unitValue: 6, remark: '考核：故障响应及时率≥95%，维修闭环率≥98%' },
  { categoryName: '教辅岗', subTypeName: '教学档案质检', unitValue: 5, remark: '考核：教学档案归档完整率100%，抽检合格率≥98%' },

  { categoryName: '行政兼课岗', subTypeName: '行政任务执行', unitValue: 6, remark: '考核：专项任务按期完成率≥98%，协同评价良好' },
  { categoryName: '行政兼课岗', subTypeName: '兼课授课质量', unitValue: 7, remark: '考核：兼课学时达标，学生评教达标率≥90%' },
  { categoryName: '行政兼课岗', subTypeName: '流程优化改进', unitValue: 6, remark: '考核：形成可量化流程优化成果并落地执行' },
  { categoryName: '行政兼课岗', subTypeName: '教学检查整改', unitValue: 5, remark: '考核：问题整改闭环率100%，复检通过率≥95%' },

  { categoryName: '外聘教师岗', subTypeName: '协议课程授课', unitValue: 7, remark: '考核：按合同完成授课学时，调停课流程合规率100%' },
  { categoryName: '外聘教师岗', subTypeName: '课程考核阅卷', unitValue: 6, remark: '考核：命题规范、阅卷及时，成绩提交准时率100%' },
  { categoryName: '外聘教师岗', subTypeName: '课后答疑支持', unitValue: 5, remark: '考核：答疑响应及时率≥95%，问题解决率≥90%' },
  { categoryName: '外聘教师岗', subTypeName: '教学资料归档', unitValue: 5, remark: '考核：教学资料提交完整率100%，归档符合规范' }
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
