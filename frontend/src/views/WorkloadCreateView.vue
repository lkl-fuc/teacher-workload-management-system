<template>
  <el-card>
    <template #header>
      <span>{{ isAdmin ? '新增工作量' : '每周固定任务填报' }}</span>
    </template>

    <template v-if="isAdmin">
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
    </template>

    <template v-else>
      <el-alert
        type="success"
        show-icon
        :closable="false"
        class="mb-12"
        :title="`当前岗位：${currentTeacherPost || '未设置岗位'}`"
        description="这里展示当前岗位每周固定任务。教师勾选完成后将自动折算工作量并写入个人记录，管理员可在工作量列表中实时查看完成情况。"
      />

      <el-form :inline="true" class="mb-12">
        <el-form-item label="任务周次">
          <el-date-picker
            v-model="fixedTaskWeekDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择周内任意一天"
            style="width: 220px"
          />
        </el-form-item>
      </el-form>

      <el-table :data="fixedTaskRows" border>
        <el-table-column label="完成" width="70" align="center">
          <template #default="scope">
            <el-checkbox v-model="scope.row.done" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="固定任务" min-width="220" />
        <el-table-column prop="expectedUnits" label="目标次数" width="120" />
        <el-table-column label="本周完成次数" width="160">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.doneUnits"
              :min="0"
              :max="scope.row.expectedUnits"
              :step="1"
              controls-position="right"
              style="width: 120px"
            />
          </template>
        </el-table-column>
        <el-table-column label="折算分值" width="130">
          <template #default="scope">
            {{ calculateTaskScore(scope.row).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="完成说明" min-width="280">
          <template #default="scope">
            <el-input
              v-model="scope.row.note"
              placeholder="如：完成班会2次，重点覆盖心理健康教育"
              maxlength="120"
              show-word-limit
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="fixed-footer">
        <span class="summary-text">已勾选 {{ selectedFixedTaskCount }} 项，预计新增 {{ totalFixedScore.toFixed(2) }} 分</span>
        <el-button @click="resetFixedTasks">重置</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitFixedTasks">提交固定任务</el-button>
      </div>
    </template>
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
const fixedTaskWeekDate = ref(todayText())
const fixedTaskRows = ref([])
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
  if (!isAdmin.value) return localStorage.getItem('teacherPost') || '专任教师岗'
  const teacher = teacherOptions.value.find((item) => Number(item.id) === Number(form.teacherId))
  return teacher?.postType || ''
})

const filteredTypeOptions = computed(() => {
  const post = normalizeText(currentTeacherPost.value)
  if (!post) return typeOptions.value

  return typeOptions.value.filter((item) => matchPostType(item, post))
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

const selectedFixedTaskCount = computed(() => fixedTaskRows.value.filter((item) => item.done).length)
const totalFixedScore = computed(() => fixedTaskRows.value
  .filter((item) => item.done)
  .reduce((sum, item) => sum + calculateTaskScore(item), 0))

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

watch(
  () => [currentTeacherPost.value, typeOptions.value.length],
  () => {
    if (isAdmin.value) return
    rebuildFixedTasks()
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

function typeLabel(item) {
  const categoryText = item.categoryName ? `${item.categoryName}` : ''
  const subTypeText = item.subTypeName ? `${item.subTypeName}` : ''
  const nameText = item.typeName || [categoryText, subTypeText].filter(Boolean).join(' - ')
  const scoreText = item.unitValue != null ? `（建议分值：${item.unitValue}）` : ''
  return `${nameText}${scoreText}`
}

function matchPostType(item, normalizedPost) {
  const category = normalizeText(item.categoryName)
  const postAliases = getPostAliases(normalizedPost)
  if (postAliases.length === 0) return true
  return postAliases.some((alias) => category.includes(alias))
}

function getPostAliases(normalizedPost) {
  const relation = [
    { aliases: ['专任教师岗', '专任教师'], keywords: ['专任教师岗', '专任教师'] },
    { aliases: ['实验教师岗', '实验教师'], keywords: ['实验教师岗', '实验教师'] },
    { aliases: ['辅导岗', '辅导员岗', '辅导员'], keywords: ['辅导岗', '辅导员岗', '辅导员'] },
    { aliases: ['教辅岗', '教辅'], keywords: ['教辅岗', '教辅'] },
    { aliases: ['行政兼课岗', '行政兼课'], keywords: ['行政兼课岗', '行政兼课'] },
    { aliases: ['外聘教师岗', '外聘教师'], keywords: ['外聘教师岗', '外聘教师'] }
  ]

  const matched = relation.find((item) => item.keywords.some((keyword) => normalizedPost.includes(normalizeText(keyword))))
  return matched ? matched.aliases.map((alias) => normalizeText(alias)) : []
}

function normalizeText(text) {
  return String(text || '').toLowerCase().replace(/\s+/g, '')
}

function todayText() {
  return new Date().toISOString().slice(0, 10)
}

function getWeekRangeText(dateText) {
  const date = new Date(dateText || todayText())
  const day = date.getDay() || 7
  const monday = new Date(date)
  monday.setDate(date.getDate() - day + 1)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `${toDateText(monday)}~${toDateText(sunday)}`
}

function toDateText(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function workloadTemplateMap(post) {
  const templates = {
    专任教师岗: [
      { key: 'teach', title: '课堂授课与教学实施', expectedUnits: 4, baseScore: 2.5, keyword: '教学' },
      { key: 'prepare', title: '备课与课程资料更新', expectedUnits: 2, baseScore: 1.5, keyword: '教研' },
      { key: 'answer', title: '答疑与作业反馈', expectedUnits: 2, baseScore: 1.2, keyword: '授课' }
    ],
    实验教师岗: [
      { key: 'lab', title: '实验课程组织与指导', expectedUnits: 3, baseScore: 2.4, keyword: '实验' },
      { key: 'safety', title: '实验室安全巡检', expectedUnits: 2, baseScore: 1.6, keyword: '实验室' },
      { key: 'record', title: '实验记录归档', expectedUnits: 2, baseScore: 1.2, keyword: '指导' }
    ],
    辅导员岗: [
      { key: 'class', title: '主题班会/年级会', expectedUnits: 2, baseScore: 2.0, keyword: '学生' },
      { key: 'talk', title: '学生谈心谈话', expectedUnits: 4, baseScore: 1.2, keyword: '思政' },
      { key: 'daily', title: '学生日常事务处理', expectedUnits: 5, baseScore: 0.8, keyword: '事务' }
    ],
    教辅岗: [
      { key: 'service', title: '教学资源服务支持', expectedUnits: 4, baseScore: 1.5, keyword: '教辅' },
      { key: 'device', title: '设备与资料维护', expectedUnits: 2, baseScore: 1.8, keyword: '设备' },
      { key: 'coord', title: '教学保障协调工作', expectedUnits: 2, baseScore: 1.3, keyword: '保障' }
    ],
    行政兼课岗: [
      { key: 'admin', title: '行政事务执行', expectedUnits: 4, baseScore: 1.4, keyword: '行政' },
      { key: 'teach', title: '兼课授课', expectedUnits: 2, baseScore: 2.1, keyword: '兼课' },
      { key: 'coord', title: '跨部门协调', expectedUnits: 2, baseScore: 1.2, keyword: '管理' }
    ],
    外聘教师岗: [
      { key: 'course', title: '外聘课程授课', expectedUnits: 3, baseScore: 2.2, keyword: '外聘' },
      { key: 'qa', title: '课程答疑与指导', expectedUnits: 2, baseScore: 1.3, keyword: '答疑' },
      { key: 'eval', title: '考核与教学反馈', expectedUnits: 1, baseScore: 1.5, keyword: '教学' }
    ]
  }
  return templates[post] || templates.专任教师岗
}

function rebuildFixedTasks() {
  fixedTaskRows.value = workloadTemplateMap(currentTeacherPost.value).map((item) => ({
    ...item,
    done: false,
    doneUnits: item.expectedUnits,
    note: ''
  }))
}

function calculateTaskScore(task) {
  const expectedUnits = Number(task.expectedUnits || 0)
  const doneUnits = Math.max(0, Number(task.doneUnits || 0))
  if (expectedUnits <= 0) return 0
  const ratio = Math.min(doneUnits / expectedUnits, 1)
  return Number(task.baseScore || 0) * expectedUnits * ratio
}

function resolveTeacherId() {
  const candidates = [localStorage.getItem('teacherId'), localStorage.getItem('userId'), localStorage.getItem('id')]
  for (const raw of candidates) {
    const id = Number(raw)
    if (!Number.isNaN(id) && id > 0) return id
  }
  return null
}

function matchFixedTypeId(task) {
  const aliases = getPostAliases(normalizeText(currentTeacherPost.value))
  const keyword = normalizeText(task.keyword)
  const matched = typeOptions.value.find((type) => {
    const searchText = normalizeText(`${type.typeName || ''}${type.subTypeName || ''}${type.categoryName || ''}${type.remark || ''}`)
    const postMatched = aliases.length === 0 || aliases.some((alias) => searchText.includes(alias))
    return postMatched && (!keyword || searchText.includes(keyword))
  })
  if (matched) return Number(matched.id)
  return Number(filteredTypeOptions.value[0]?.id || typeOptions.value[0]?.id || 0)
}

function fixedTaskDescription(task, weekRange) {
  const doneUnits = Number(task.doneUnits || 0)
  const expectedUnits = Number(task.expectedUnits || 0)
  const note = task.note?.trim()
  const base = `固定任务周报：${task.title}（${weekRange}，完成 ${doneUnits}/${expectedUnits} 次）`
  return note ? `${base}。备注：${note}` : base
}

async function submitFixedTasks() {
  const doneTasks = fixedTaskRows.value.filter((item) => item.done)
  if (doneTasks.length === 0) {
    ElMessage.warning('请先勾选已完成的固定任务')
    return
  }

  const teacherId = resolveTeacherId()
  if (!teacherId) {
    ElMessage.warning('当前未识别到教师身份，请重新登录')
    return
  }

  submitLoading.value = true
  try {
    const weekRange = getWeekRangeText(fixedTaskWeekDate.value)
    const submitDate = fixedTaskWeekDate.value || todayText()
    for (const task of doneTasks) {
      const typeId = matchFixedTypeId(task)
      if (!typeId) {
        throw new Error('未找到可用的工作量类型，请联系管理员先配置工作量类型')
      }
      const payload = {
        teacherId,
        typeId,
        workloadTitle: `${task.title}（${weekRange}）`,
        amount: Number(calculateTaskScore(task).toFixed(2)),
        description: fixedTaskDescription(task, weekRange),
        submitDate,
        status: 'PENDING'
      }
      await request('/api/workloads', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
    }

    ElMessage.success(`已提交 ${doneTasks.length} 项固定任务，管理员可在工作量列表查看`)
    resetFixedTasks()
  } catch (error) {
    ElMessage.error(normalizeError(error, '固定任务提交失败'))
  } finally {
    submitLoading.value = false
  }
}

function resetFixedTasks() {
  rebuildFixedTasks()
}

onMounted(async () => {
  await loadTypeOptions()
  await loadTeacherOptions()
  if (!isAdmin.value) rebuildFixedTasks()
})
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}

.fixed-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.summary-text {
  margin-right: auto;
  color: var(--text-secondary);
}
</style>
