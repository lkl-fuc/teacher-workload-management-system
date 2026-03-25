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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref()
const submitLoading = ref(false)
const typeLoading = ref(false)
const typeOptions = ref([])

const form = reactive({
  typeId: null,
  title: '',
  score: 0,
  description: '',
  workDate: ''
})

const rules = {
  typeId: [{ required: true, message: '请选择工作类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  score: [{ required: true, message: '请输入分值', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择工作日期', trigger: 'change' }]
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

function resetFormModel() {
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

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = {
        typeId: form.typeId,
        title: form.title,
        score: form.score,
        description: form.description,
        workDate: form.workDate
      }

      await request('/api/workloads', {
        method: 'POST',
        body: JSON.stringify(payload)
      })

      ElMessage.success('新增工作量成功')
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
})
</script>
