<template>
  <el-card>
    <template #header>
      <span>{{ isAdmin ? '岗位任务分发' : '年度固定任务填报' }}</span>
    </template>

    <template v-if="isAdmin">
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        class="mb-12"
        title="管理员分发任务将独立于年度固定任务，教师需回填完成情况后再由管理员人工审核、打分与折算。"
      />

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

        <el-form-item label="专项类型" prop="specialType">
          <el-input
            v-model="form.specialType"
            placeholder="请输入专项任务类型（如：迎评整改、重点督导、专项活动）"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="任务模板">
          <el-select
            v-model="selectedTaskTemplate"
            placeholder="按岗位选择常见任务模板（可选）"
            style="width: 100%"
            clearable
            @change="applyTaskTemplate"
          >
            <el-option
              v-for="item in taskTemplateOptions"
              :key="item.title"
              :label="`${item.title}（建议${item.score}分）`"
              :value="item.title"
            />
          </el-select>
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
        description="这里展示当前岗位年度固定任务与考核标准。教师勾选完成后将自动折算工作量并写入个人记录，管理员可在工作量列表中实时查看完成情况。"
      />

      <el-form :inline="true" class="mb-12">
        <el-form-item label="任务年度">
          <el-date-picker
            v-model="fixedTaskYear"
            type="year"
            value-format="YYYY"
            placeholder="选择任务年度"
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
        <el-table-column label="目标要求" width="170">
          <template #default="scope">
            {{ formatExpectedTarget(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="本年完成次数" width="160">
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
        <el-table-column prop="assessment" label="考核标准" min-width="260" show-overflow-tooltip />
        <el-table-column label="完成说明" min-width="280">
          <template #default="scope">
            <el-input
              v-model="scope.row.note"
              :placeholder="scope.row.completionHint || '请填写可量化的完成说明'"
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
const teacherOptions = ref([])
const selectedTaskTemplate = ref('')
const fixedTaskYear = ref(String(new Date().getFullYear()))
const fixedTaskRows = ref([])
const role = computed(() => String(localStorage.getItem('role') || '').toUpperCase())
const isAdmin = computed(() => role.value === 'ADMIN')

const form = reactive({
  teacherId: null,
  typeId: null,
  specialType: '',
  title: '',
  score: 0,
  description: '',
  workDate: ''
})

const rules = computed(() => ({
  teacherId: isAdmin.value ? [{ required: true, message: '请选择分发教师', trigger: 'change' }] : [],
  specialType: isAdmin.value ? [{ required: true, message: '请输入专项类型', trigger: 'blur' }] : [],
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

const selectedFixedTaskCount = computed(() => fixedTaskRows.value.filter((item) => item.done).length)
const totalFixedScore = computed(() => fixedTaskRows.value
  .filter((item) => item.done)
  .reduce((sum, item) => sum + calculateTaskScore(item), 0))
const taskTemplateOptions = computed(() => {
  const post = normalizeText(currentTeacherPost.value)
  if (post.includes('专任教师')) {
    return [
      { title: '科研成果展示', score: 12, description: '组织并完成院系科研成果展，提交展示材料与总结。' },
      { title: '论文发表与检索证明', score: 15, description: '完成高质量论文发表并提交检索/录用证明。' },
      { title: '教改项目结题汇报', score: 10, description: '完成教改项目结题材料与现场汇报。' }
    ]
  }
  if (post.includes('实验教师')) {
    return [
      { title: '实验项目成果展示', score: 10, description: '面向学院组织实验项目成果展示与示范。' },
      { title: '实验室安全专题整改', score: 8, description: '完成安全检查整改闭环并提交佐证。' }
    ]
  }
  if (post.includes('辅导员')) {
    return [
      { title: '学风建设专题活动', score: 8, description: '组织学风建设活动并形成效果评估。' },
      { title: '重点学生帮扶案例', score: 10, description: '提交重点学生帮扶过程台账与成效复盘。' }
    ]
  }
  return [
    { title: '岗位专项任务', score: 8, description: '请结合岗位职责填写任务要求与验收标准。' }
  ]
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
  form.specialType = ''
  selectedTaskTemplate.value = ''
  form.title = ''
  form.score = 0
  form.description = ''
  form.workDate = ''
}

function applyTaskTemplate(templateTitle) {
  const template = taskTemplateOptions.value.find((item) => item.title === templateTitle)
  if (!template) return
  form.title = template.title
  if (!form.description) {
    form.description = template.description
  }
  if (!form.score || Number(form.score) <= 0) {
    form.score = template.score
  }
}

watch(
  () => form.teacherId,
  () => {
    form.specialType = ''
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
        typeId: isAdmin.value ? null : form.typeId,
        workloadTitle: isAdmin.value ? `【${form.specialType}】${form.title}` : form.title,
        amount: form.score,
        description: form.description,
        submitDate: form.workDate,
        status: isAdmin.value ? 'ASSIGNED' : 'PENDING',
        sourceType: isAdmin.value ? 'ADMIN_ASSIGNED' : 'SELF_REPORTED'
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

function resolveTaskYear() {
  const year = Number(fixedTaskYear.value || new Date().getFullYear())
  if (Number.isNaN(year) || year < 2000) return String(new Date().getFullYear())
  return String(year)
}

function workloadTemplateMap(post) {
  const templates = {
    专任教师岗: [
      { key: 'teach-core', title: '核心课程授课达成', expectedUnits: 96, targetUnit: '课时', targetCycle: '年度', baseScore: 0.25, keyword: '课堂授课', assessment: '按教学计划完成≥96课时，教学事故为0，课堂巡查合格率≥95%', completionHint: '如：完成96课时《高等数学》授课，听课抽检优良率96%' },
      { key: 'prepare-resource', title: '课程资源建设与更新', expectedUnits: 24, targetUnit: '份', targetCycle: '年度', baseScore: 0.3, keyword: '课程资源', assessment: '每学期至少更新12份教案/课件，资源合规率100%，课程思政元素覆盖核心章节', completionHint: '如：上传课件14份、案例库8份，资源审核通过率100%' },
      { key: 'research-teach', title: '教研活动与质量改进', expectedUnits: 12, targetUnit: '次', targetCycle: '年度', baseScore: 0.5, keyword: '教研活动', assessment: '参与教研≥12次，形成可追溯改进记录，至少落地2项课堂改进措施', completionHint: '如：参加教研12次，完成2轮课堂改进复盘并归档' },
      { key: 'student-feedback', title: '作业批改与学业反馈', expectedUnits: 32, targetUnit: '批次', targetCycle: '年度', baseScore: 0.22, keyword: '作业批改', assessment: '批改及时率≥98%，学业预警学生反馈闭环率≥95%，错题分析覆盖主要知识点', completionHint: '如：完成32批次作业批改，预警学生回访闭环率100%' }
    ],
    实验教师岗: [
      { key: 'lab-course', title: '实验课程组织实施', expectedUnits: 72, targetUnit: '课时', targetCycle: '年度', baseScore: 0.28, keyword: '实验教学', assessment: '完成实验教学≥72课时，实验开出率100%，无重大教学差错', completionHint: '如：完成72课时实验教学，开出率100%，无安全事故' },
      { key: 'lab-guide', title: '实验项目指导与答疑', expectedUnits: 40, targetUnit: '人次', targetCycle: '学期', baseScore: 0.24, keyword: '实验指导', assessment: '项目指导覆盖重点实验班，答疑响应及时率≥95%，学生满意度≥90%', completionHint: '如：累计指导43人次，答疑24小时内响应率97%' },
      { key: 'lab-safety', title: '实验室安全巡检与整改', expectedUnits: 48, targetUnit: '次', targetCycle: '年度', baseScore: 0.18, keyword: '安全巡检', assessment: '每周巡检并留痕，危化品台账规范，隐患整改闭环率100%', completionHint: '如：完成48次巡检，发现隐患12项并全部闭环' },
      { key: 'lab-assets', title: '仪器设备维护与台账', expectedUnits: 24, targetUnit: '批次', targetCycle: '年度', baseScore: 0.2, keyword: '设备管理', assessment: '设备完好率≥98%，借还记录准确率100%，关键设备有维护记录', completionHint: '如：完成24批次设备维护，设备完好率99%' }
    ],
    辅导员岗: [
      { key: 'student-case', title: '学生分层管理与预警干预', expectedUnits: 80, targetUnit: '人次', targetCycle: '年度', baseScore: 0.2, keyword: '学生管理', assessment: '重点学生台账完整，预警处置及时率100%，高风险学生跟踪有月度记录', completionHint: '如：重点学生干预82人次，红色预警处置及时率100%' },
      { key: 'ideology-edu', title: '思想教育与主题班会', expectedUnits: 24, targetUnit: '场', targetCycle: '年度', baseScore: 0.35, keyword: '思想教育', assessment: '主题教育不少于24场次，覆盖率100%，活动材料归档完整', completionHint: '如：组织主题班会26场，覆盖全体学生并完成照片/纪要归档' },
      { key: 'talk-heart', title: '谈心谈话与心理关怀', expectedUnits: 60, targetUnit: '人次', targetCycle: '年度', baseScore: 0.2, keyword: '谈心谈话', assessment: '重点人群谈话不少于60人次，回访闭环率≥95%，心理转介流程规范', completionHint: '如：完成68人次谈话，回访闭环率97%' },
      { key: 'daily-affairs', title: '奖助勤贷与日常事务办理', expectedUnits: 36, targetUnit: '批次', targetCycle: '年度', baseScore: 0.22, keyword: '日常事务', assessment: '事务办理准确率100%，投诉率低于1%，资助审核全流程留痕', completionHint: '如：办理39批次事务，零差错，学生投诉率0.3%' }
    ],
    教辅岗: [
      { key: 'schedule-support', title: '教学运行与排考支持', expectedUnits: 36, targetUnit: '批次', targetCycle: '年度', baseScore: 0.26, keyword: '教学秘书', assessment: '排课排考零重大差错，关键节点按时完成率100%，教师反馈满意度≥90%', completionHint: '如：完成36批次排课排考支持，关键节点按时率100%' },
      { key: 'resource-service', title: '教材资料与资源服务', expectedUnits: 48, targetUnit: '批次', targetCycle: '年度', baseScore: 0.18, keyword: '资源服务', assessment: '教材发放及时率100%，资源借用登记准确率100%，需求响应周期可追踪', completionHint: '如：完成教材与资料服务49批次，登记准确率100%' },
      { key: 'device-service', title: '教学设备保障与报修跟进', expectedUnits: 50, targetUnit: '单', targetCycle: '年度', baseScore: 0.2, keyword: '设备保障', assessment: '设备故障响应及时率≥95%，修复闭环率≥98%，重点教室保障不中断', completionHint: '如：处理报修54单，48小时内闭环率98.5%' },
      { key: 'archive-quality', title: '教学档案整理与质检', expectedUnits: 30, targetUnit: '批次', targetCycle: '年度', baseScore: 0.22, keyword: '资料归档', assessment: '档案归档完整率100%，校内抽检合格率≥98%，材料检索方便', completionHint: '如：完成30批次档案归整，抽检合格率100%' }
    ],
    行政兼课岗: [
      { key: 'admin-execution', title: '行政专项任务执行', expectedUnits: 60, targetUnit: '项', targetCycle: '年度', baseScore: 0.2, keyword: '行政管理', assessment: '专项任务按期完成率≥98%，跨部门协同评价良好，过程文档齐全', completionHint: '如：完成63项行政专项，按期完成率98.4%' },
      { key: 'part-time-teach', title: '兼课课程授课质量', expectedUnits: 48, targetUnit: '课时', targetCycle: '年度', baseScore: 0.26, keyword: '兼课教学', assessment: '完成兼课学时≥48，学生评教达标率≥90%，教学资料按时提交', completionHint: '如：兼课50课时，评教达标率93%，资料按时提交' },
      { key: 'process-opt', title: '流程优化与制度落实', expectedUnits: 20, targetUnit: '项', targetCycle: '年度', baseScore: 0.3, keyword: '流程优化', assessment: '形成制度优化成果，流程执行偏差率持续下降，至少输出2份优化报告', completionHint: '如：推动流程优化22项，输出制度优化报告2份' },
      { key: 'quality-feedback', title: '教学检查与反馈整改', expectedUnits: 24, targetUnit: '次', targetCycle: '年度', baseScore: 0.24, keyword: '教学检查', assessment: '检查问题整改闭环率100%，复检通过率≥95%，台账记录完整', completionHint: '如：完成24次检查，整改闭环率100%，复检通过率96%' }
    ],
    外聘教师岗: [
      { key: 'outsource-teach', title: '协议课程授课完成', expectedUnits: 64, targetUnit: '课时', targetCycle: '年度', baseScore: 0.24, keyword: '外聘授课', assessment: '严格按合同完成授课学时，调停课流程合规率100%，课堂纪律反馈良好', completionHint: '如：按合同完成64课时，调停课流程合规率100%' },
      { key: 'course-eval', title: '课程考核与阅卷反馈', expectedUnits: 20, targetUnit: '批次', targetCycle: '年度', baseScore: 0.3, keyword: '课程考核', assessment: '命题规范、阅卷及时，成绩提交准时率100%，试卷分析完整', completionHint: '如：完成20批次考核阅卷，成绩准时提交率100%' },
      { key: 'qa-support', title: '课后答疑与学习支持', expectedUnits: 36, targetUnit: '次', targetCycle: '年度', baseScore: 0.2, keyword: '答疑辅导', assessment: '答疑响应及时率≥95%，学生问题解决率≥90%，薄弱学生有跟踪记录', completionHint: '如：开展40次答疑，问题解决率92%' },
      { key: 'teaching-files', title: '教学资料提交与归档', expectedUnits: 16, targetUnit: '份', targetCycle: '年度', baseScore: 0.25, keyword: '资料归档', assessment: '教学文档提交完整率100%，归档规范达标，版本可追溯', completionHint: '如：提交16份教学资料，归档审核一次通过' }
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

function formatExpectedTarget(task) {
  const expectedUnits = Number(task.expectedUnits || 0)
  const unit = task.targetUnit || '次'
  const cycle = task.targetCycle || '年度'
  return `${expectedUnits}${unit}/${cycle}`
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

function fixedTaskDescription(task, yearText) {
  const doneUnits = Number(task.doneUnits || 0)
  const expectedUnits = Number(task.expectedUnits || 0)
  const unit = task.targetUnit || '次'
  const cycle = task.targetCycle || '年度'
  const note = task.note?.trim()
  const assessment = task.assessment ? `；考核标准：${task.assessment}` : ''
  const base = `年度固定任务：${task.title}（${yearText}年，完成 ${doneUnits}/${expectedUnits} ${unit}，目标周期：${cycle}）${assessment}`
  return note ? `${base}。完成说明：${note}` : base
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
    const yearText = resolveTaskYear()
    const submitDate = `${yearText}-12-31`
    for (const task of doneTasks) {
      const typeId = matchFixedTypeId(task)
      if (!typeId) {
        throw new Error('未找到可用的工作量类型，请联系管理员先配置工作量类型')
      }
      const payload = {
        teacherId,
        typeId,
        workloadTitle: `${task.title}（${yearText}年度）`,
        amount: Number(calculateTaskScore(task).toFixed(2)),
        description: fixedTaskDescription(task, yearText),
        submitDate,
        status: 'PENDING',
        sourceType: 'ANNUAL_FIXED'
      }
      await request('/api/workloads', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
    }

    ElMessage.success(`已提交 ${doneTasks.length} 项年度固定任务，管理员可在工作量列表查看`)
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
