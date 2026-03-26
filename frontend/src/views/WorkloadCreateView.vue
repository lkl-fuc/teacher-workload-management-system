<template>
  <el-card>
    <template #header>
      <span>新增工作量</span>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 680px">
      <el-form-item label="工作类型" prop="typeId">
        <el-select
          v-model="form.typeId"
          placeholder="请选择工作类型"
          style="width: 100%"
          filterable
          clearable
          :loading="typeLoading"
        >
          <el-option
            v-for="item in typeOptions"
            :key="item.id"
            :label="item.typeName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

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
            :label="`${item.name}（${item.teacherNo}）`"
            :value="item.id"
          />
        </el-select>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref()
const submitLoading = ref(false)
const typeLoading = ref(false)
const teacherLoading = ref(false)
const typeOptions = ref([])
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
  form.title = ''
  form.score = 0
  form.description = ''
  form.workDate = ''
}

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
</script>
