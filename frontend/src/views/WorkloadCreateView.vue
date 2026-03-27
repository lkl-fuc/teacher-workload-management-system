<template>
  <el-card>
    <template #header>
      <span>新增工作量</span>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 680px">
      <el-form-item v-if="isAdmin" label="分发教师" prop="teacherId">
        <el-select
          v-model="form.teacherId"
          placeholder="请选择要分发的教师"
          style="width: 100%"
          filterable
          clearable
          :loading="teacherLoading"
        >
          <el-option
            v-for="item in teacherOptions"
            :key="item.id"
            :label="`${item.name}（${item.teacherNo} / ${item.postType || '未设置岗位'}）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

      <el-alert
        v-if="matchedTypeHint"
        class="mb-12"
        type="success"
        :closable="false"
        :title="matchedTypeHint"
      />

      <el-form-item label="工作类型" prop="typeId">
        <el-cascader
          v-model="selectedTypePath"
          :options="typeTreeOptions"
          :props="{ value: 'value', label: 'label', children: 'children', emitPath: true }"
          placeholder="请选择【大类 -> 小类】"
          style="width: 100%"
          filterable
          clearable
          :show-all-levels="true"
          :disabled="typeLoading"
          @change="handleTypePathChange"
        />
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入标题" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="分值" prop="score">
        <el-input-number
          v-model="form.score"
          :min="0"
          :max="999999"
          :precision="2"
          :step="0.5"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="工作日期" prop="workDate">
        <el-date-picker
          v-model="form.workDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择工作日期"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入描述（可选）"
          maxlength="300"
          show-word-limit
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref()
const submitLoading = ref(false)
const typeLoading = ref(false)
const teacherLoading = ref(false)
const typeOptions = ref([])
const selectedTypePath = ref([])
const teacherOptions = ref([])
const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())
const isAdmin = computed(() => role.value === 'ADMIN')

const form = reactive({
  teacherId: null,
  typeId: null,
  title: '',
  score: 0,
  description: '',
  workDate: ''
})

const rules = computed(() => ({
  teacherId: isAdmin.value ? [{ required: true, message: '请选择分发教师', trigger: 'change' }] : [],
  typeId: [{ required: true, message: '请选择工作类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  score: [{ required: true, message: '请输入分值', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择工作日期', trigger: 'change' }]
}))

const currentTeacherPost = computed(() => {
  if (!isAdmin.value) return localStorage.getItem('teacherPost') || '行政岗'
  const teacher = teacherOptions.value.find((item) => Number(item.id) === Number(form.teacherId))
  return teacher?.postType || ''
})

const filteredTypeOptions = computed(() => {
  const post = normalizeText(currentTeacherPost.value)
  if (!post) return typeOptions.value

  const matched = typeOptions.value.filter((item) => matchPostType(item, post))
  return matched.length > 0 ? matched : typeOptions.value
})

const typeTreeOptions = computed(() => {
  const grouped = filteredTypeOptions.value.reduce((acc, item) => {
    const category = item.categoryName || '未分类'
    if (!acc[category]) acc[category] = []
    acc[category].push(item)
    return acc
  }, {})

  return Object.keys(grouped).map((category) => ({
    value: `category-${category}`,
    label: category,
    children: grouped[category].map((item) => ({
      value: item.id,
      label: typeLabel(item)
    }))
  }))
})

const matchedTypeHint = computed(() => {
  if (!currentTeacherPost.value) return ''
  return `当前将按【${currentTeacherPost.value}】岗位匹配任务类型并分发到教师端任务面板。`
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

function resetFormModel() {
  form.teacherId = null
  form.typeId = null
  selectedTypePath.value = []
  form.title = ''
  form.score = 0
  form.description = ''
  form.workDate = ''
}

function handleTypePathChange(path) {
  if (!Array.isArray(path) || path.length < 2) {
    form.typeId = null
    return
  }
  form.typeId = Number(path[path.length - 1]) || null
}

watch(
  () => form.teacherId,
  () => {
    selectedTypePath.value = []
    form.typeId = null
  }
)

async function loadTypeOptions() {
  typeLoading.value = true
  try {
    const data = await request('/api/workload-types')
    typeOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(normalizeError(error, '获取工作量类型失败'))
  } finally {
    typeLoading.value = false
  }
}

async function loadTeacherOptions() {
  if (!isAdmin.value) return
  teacherLoading.value = true
  try {
    const data = await request('/api/teachers')
    teacherOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(normalizeError(error, '获取教师列表失败'))
  } finally {
    teacherLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const teacherId = isAdmin.value
      ? Number(form.teacherId)
      : Number(localStorage.getItem('teacherId') || localStorage.getItem('userId'))
    if (Number.isNaN(teacherId) || teacherId <= 0) {
      ElMessage.warning(isAdmin.value ? '请选择分发教师' : '当前未识别到教师身份，请先使用教师账号登录')
      return
    }

    submitLoading.value = true
    try {
      const payload = {
        teacherId,
        typeId: form.typeId,
        workloadTitle: form.title,
        amount: form.score,
        description: form.description,
        submitDate: form.workDate,
        status: isAdmin.value ? 'ASSIGNED' : 'PENDING'
      }

      await request('/api/workloads', {
        method: 'POST',
        body: JSON.stringify(payload)
      })

      ElMessage.success(isAdmin.value ? '工作量任务已分发给教师' : '新增工作量成功')
      handleReset()
    } catch (error) {
      ElMessage.error(normalizeError(error, '提交失败'))
    } finally {
      submitLoading.value = false
    }
  })
}

function handleReset() {
  resetFormModel()
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadTypeOptions()
  loadTeacherOptions()
})

function typeLabel(item) {
  const categoryText = item.categoryName ? `${item.categoryName}` : ''
  const subTypeText = item.subTypeName ? `${item.subTypeName}` : ''
  const nameText = item.typeName || [categoryText, subTypeText].filter(Boolean).join(' - ')
  const scoreText = item.unitValue != null ? `（建议分值：${item.unitValue}）` : ''
  return `${nameText}${scoreText}`
}

function matchPostType(item, normalizedPost) {
  const text = normalizeText([item.typeName, item.categoryName, item.subTypeName, item.remark].filter(Boolean).join(' '))
  if (normalizedPost.includes('行政')) {
    return ['行政', '管理', '服务', '支撑'].some((keyword) => text.includes(keyword))
  }
  if (normalizedPost.includes('管理')) {
    return ['管理', '审核', '质量', '规划', '督导'].some((keyword) => text.includes(keyword))
  }
  if (normalizedPost.includes('教辅')) {
    return ['教学', '教辅', '考试', '实验', '实训', '服务'].some((keyword) => text.includes(keyword))
  }
  return true
}

function normalizeText(text) {
  return String(text || '').toLowerCase().replace(/\s+/g, '')
}
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}
</style>
