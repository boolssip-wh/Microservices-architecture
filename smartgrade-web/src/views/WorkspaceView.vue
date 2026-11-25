<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { api, guarded } from '../api.js'
import AnimatedNumber from '../components/AnimatedNumber.vue'
import { notify, selectBatch, session } from '../session.js'
import { createImportAttempts } from '../importAttempts.js'
import { useSpeechToText } from '../speech.js'

const props = defineProps({ newCaseRequest: { type: Number, default: 0 } })

const QUESTION_TEXT = `在今年五四青年节到来之际，中共中央总书记、国家主席、中央军委主席习近平代表党中央，向全国广大青年致以节日祝贺和诚挚问候。

习近平总书记指出，新时代新征程上，全国各族青年听从党和人民的召唤，在科技创新、乡村振兴、绿色发展、社会服务、卫国戍边等各领域各方面勇当排头兵和生力军，展现出自信自强、刚健有为的精神风貌。党中央对广大青年充分信任、寄予厚望！

习近平总书记强调，今年是新中国成立75周年，是五四运动105周年。广大青年要继承和发扬五四精神，坚定不移听党话、跟党走，争做有理想、敢担当能吃苦、肯奋斗的新时代好青年，在推进强国建设、民族复兴伟业中展现青春作为、彰显青春风采、贡献青春力量，奋力书写为中国式现代化挺膺担当的青春篇章。

作为一名入党积极分子，通过校史、院史和众多优秀校友的学习、了解，请你结合学习心得和自身实际，来谈谈如何争做新时代好青年，早日入党。`

const REFERENCE_ANSWER = '从争做有理想、敢担当、能吃苦、肯奋斗的新时代好青年展开作答，并适当结合自身经历，言之有理即可。'
const CASE_KEYWORDS = ['有理想', '敢担当', '能吃苦', '肯奋斗']

const batches = ref([])
const batch = ref(null)
const progress = ref(null)
const answers = ref([])
const importEntries = ref([])
const importAttempts = createImportAttempts()
const rubricId = ref('')
const parseResult = ref(null)
const compiled = ref(null)
const importing = ref(false)
const dragActive = ref(false)
const fileInput = ref(null)
const cameraInput = ref(null)
const pollTimer = ref(null)
const questionExpanded = ref(false)
const createCaseOpen = ref(false)
const creatingCase = ref(false)
let batchesRequest = 0
let batchRequest = 0
let progressRequest = 0
let rubricRequest = 0
let disposed = false

function isCurrentBatch(batchId, request) {
  return !disposed && request === batchRequest && session.selectedBatchId === batchId
}

watch(() => props.newCaseRequest, async () => {
  createCaseOpen.value = true
  await nextTick()
  document.getElementById('create-case')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
})

const batchForm = reactive({
  examName: '五四青年节 · 新时代好青年论述题',
  questionNo: 'Q1',
  questionTitle: '如何争做新时代好青年，早日入党',
  questionText: QUESTION_TEXT,
  maxScore: 20,
  baseScore: 10,
})

const rubricSource = ref(
  '有理想：有理想|理想信念|树立理想|听党话|跟党走|坚定信念\n' +
  '敢担当：敢担当|敢于担当|勇于担当|责任担当|挺膺担当\n' +
  '能吃苦：能吃苦|吃苦耐劳|艰苦奋斗|不怕困难|迎难而上\n' +
  '肯奋斗：肯奋斗|不懈奋斗|拼搏奋斗|接续奋斗|努力奋斗\n' +
  '作答篇幅：LENGTH_COVERAGE',
)

const rubricSpeech = useSpeechToText({
  onFinal(text) {
    const current = rubricSource.value.trimEnd()
    rubricSource.value = current ? `${current}\n${text}` : text
    notify('语音内容已转成文字，可继续修改')
  },
  onError: (message) => notify(message, true),
})

function toggleRubricVoice() {
  if (rubricSpeech.listening.value) {
    rubricSpeech.stop()
    return
  }
  rubricSpeech.start()
}

const canReview = computed(() => ['WAITING_REVIEW', 'REVIEWING'].includes(batch.value?.status))
const processingCount = computed(() => progress.value
  ? progress.value.pending + progress.value.running
  : 0)
const selectedNames = computed(() => importEntries.value.map((entry) => entry.file.name))
const batchBaseScore = computed(() => Number(batch.value?.baseScore ?? Math.min(10, Number(batch.value?.maxScore || 20))))
const pointBudget = computed(() => Math.max(0, Number(batch.value?.maxScore || 20) - batchBaseScore.value))
const keywordScore = computed(() => Number((parseResult.value?.points
  ? parseResult.value.points.filter(point => point.pointType !== 'LENGTH_COVERAGE').reduce((sum, point) => sum + Number(point.maxScore || 0), 0)
  : pointBudget.value * 0.9).toFixed(2)))
const coverageScore = computed(() => Number((parseResult.value?.points
  ? parseResult.value.points.filter(point => point.pointType === 'LENGTH_COVERAGE').reduce((sum, point) => sum + Number(point.maxScore || 0), 0)
  : pointBudget.value * 0.1).toFixed(2)))

async function loadBatches() {
  const request = ++batchesRequest
  const selectionRequest = batchRequest
  const data = await api('/api/batches?page=1&size=50')
  if (disposed || request !== batchesRequest) return
  batches.value = data.items || []
  if (selectionRequest !== batchRequest) return
  if (!session.selectedBatchId && batches.value.length) {
    selectBatch(batches.value[0].batchId)
  }
  const selected = batches.value.find((item) => item.batchId === session.selectedBatchId)
  if (selected) {
    await openBatch(selected)
  } else if (batches.value.length) {
    await openBatch(batches.value[0])
  } else {
    ++batchRequest
    ++rubricRequest
    window.clearInterval(pollTimer.value)
    pollTimer.value = null
    batch.value = null
    progress.value = null
    answers.value = []
    rubricId.value = ''
    compiled.value = null
    parseResult.value = null
  }
}

async function createBatch() {
  if (creatingCase.value) return
  if (!/^[A-Za-z0-9_-]{1,20}$/.test(batchForm.questionNo.trim())) {
    notify('题号请填写1–20位字母、数字、下划线或短横线，例如 Q1', true)
    return
  }
  if (!Number.isFinite(batchForm.baseScore) || batchForm.baseScore < 0 || batchForm.baseScore > batchForm.maxScore) {
    notify('基础分应在0到满分之间', true)
    return
  }
  creatingCase.value = true
  try {
    const created = await guarded(
      () => api('/api/batches', { method: 'POST', body: { ...batchForm, questionNo: batchForm.questionNo.trim() } }),
      '阅卷批次已创建，下一步核对评分要点',
    )
    selectBatch(created.batchId)
    createCaseOpen.value = false
    await loadBatches()
  } catch { /* guarded has already displayed the failure. */ }
  finally { creatingCase.value = false }
}

async function openBatch(item) {
  const batchId = item.batchId
  const request = ++batchRequest
  ++rubricRequest
  ++progressRequest
  selectBatch(batchId)
  window.clearInterval(pollTimer.value)
  pollTimer.value = null
  batch.value = null
  progress.value = null
  answers.value = []
  rubricId.value = ''
  compiled.value = null
  parseResult.value = null
  try {
    const [details, answerPage, current] = await Promise.all([
      api(`/api/batches/${batchId}`),
      api(`/api/batches/${batchId}/answers?page=1&size=100`),
      api(`/api/rubrics/batches/${batchId}/current`).catch((error) => {
        if (error.status === 404) return null
        throw error
      }),
    ])
    if (!isCurrentBatch(batchId, request)) return
    batch.value = details
    progress.value = details.progress
    answers.value = answerPage.items || []
    rubricId.value = current?.rubricId || localStorage.getItem(`smartgrade.rubric.${batchId}`) || ''
    compiled.value = current
    parseResult.value = current ? { points: current.points || [] } : null
    if (current) {
      localStorage.setItem(`smartgrade.rubric.${batchId}`, current.rubricId)
      localStorage.setItem(`smartgrade.compiled.${batchId}`, JSON.stringify(current))
    } else {
      localStorage.removeItem(`smartgrade.compiled.${batchId}`)
    }
    schedulePolling()
  } catch (error) {
    if (isCurrentBatch(batchId, request)) notify(error.message, true)
  }
}

async function refreshProgress() {
  if (disposed || !batch.value) return
  const batchId = batch.value.batchId
  const selectionRequest = batchRequest
  const request = ++progressRequest
  try {
    const [nextProgress, answerPage] = await Promise.all([
      api(`/api/batches/${batchId}/progress`),
      api(`/api/batches/${batchId}/answers?page=1&size=100`),
    ])
    if (!isCurrentBatch(batchId, selectionRequest) || request !== progressRequest) return
    progress.value = nextProgress
    answers.value = answerPage.items || []
    if (!answers.value.some((item) => ['PENDING', 'RUNNING'].includes(item.processStatus))) {
      window.clearInterval(pollTimer.value)
      pollTimer.value = null
    }
  } catch (error) {
    if (isCurrentBatch(batchId, selectionRequest) && request === progressRequest) notify(error.message, true)
  }
}

function schedulePolling() {
  window.clearInterval(pollTimer.value)
  pollTimer.value = null
  if (!disposed && batch.value && answers.value.some((item) => ['PENDING', 'RUNNING'].includes(item.processStatus))) {
    pollTimer.value = window.setInterval(refreshProgress, 2200)
  }
}

async function createRubric() {
  if (!batch.value) {
    notify('请先选择一个阅卷批次', true)
    return
  }
  const batchId = batch.value.batchId
  const selectionRequest = batchRequest
  const request = ++rubricRequest
  const created = await guarded(
    () => api('/api/rubrics', {
      method: 'POST',
      body: {
        batchId,
        sourceText: rubricSource.value,
        maxScore: batch.value.maxScore,
        baseScore: batchBaseScore.value,
      },
    }),
    '评分规则草稿已创建',
  )
  if (!isCurrentBatch(batchId, selectionRequest) || request !== rubricRequest) return
  rubricId.value = created.rubricId
  compiled.value = null
  parseResult.value = null
  localStorage.removeItem(`smartgrade.compiled.${batchId}`)
  localStorage.setItem(`smartgrade.rubric.${batchId}`, created.rubricId)
  await parseRubric()
}

async function parseRubric() {
  if (!batch.value || !rubricId.value) {
    notify('请先创建评分规则草稿', true)
    return
  }
  const batchId = batch.value.batchId
  const selectionRequest = batchRequest
  const parsingRubricId = rubricId.value
  const request = ++rubricRequest
  parseResult.value = null
  const parsed = await guarded(
    () => api(`/api/rubrics/${parsingRubricId}/parse`, {
      method: 'POST',
      body: { sourceText: rubricSource.value },
    }),
    '评分要点已解析',
  )
  if (!isCurrentBatch(batchId, selectionRequest) || request !== rubricRequest || rubricId.value !== parsingRubricId) return
  parseResult.value = parsed
  applyCaseWeights(parseResult.value.points)
}

function applyCaseWeights(points) {
  const keywordPoints = points?.filter((point) => point.pointType !== 'LENGTH_COVERAGE') || []
  const coveragePoints = points?.filter((point) => point.pointType === 'LENGTH_COVERAGE') || []
  const names = keywordPoints.map((point) => point.pointName)
  const isExplanationCase = keywordPoints.length === 4
    && coveragePoints.length === 1
    && CASE_KEYWORDS.every((keyword) => names.includes(keyword))
  if (!isExplanationCase) return

  keywordPoints.forEach((point) => {
    point.weight = 22.5
    point.maxScore = Math.floor(pointBudget.value * 0.225 * 100 + 1e-8) / 100
  })
  coveragePoints[0].weight = 10
  coveragePoints[0].maxScore = Number((pointBudget.value - keywordPoints.reduce((sum, point) => sum + point.maxScore, 0)).toFixed(2))
}

async function saveAndCompile() {
  if (!batch.value || !rubricId.value || !parseResult.value?.points?.length) {
    notify('请先解析评分要点', true)
    return
  }
  const batchId = batch.value.batchId
  const selectionRequest = batchRequest
  const compilingRubricId = rubricId.value
  const request = ++rubricRequest
  const payload = {
    points: parseResult.value.points.map((point) => ({
      pointId: point.pointId?.startsWith('P') && point.pointId.length > 4 ? point.pointId : null,
      pointName: point.pointName,
      pointType: point.pointType || 'CONCEPT',
      keywords: point.pointType === 'LENGTH_COVERAGE' ? [] : [...new Set((point.keywords || []).map(word => word.trim()).filter(Boolean))],
      variants: point.pointType === 'LENGTH_COVERAGE' ? [] : [...new Set((point.variants || []).map(word => word.trim()).filter(Boolean))],
      weight: Number(point.weight),
      maxScore: Number(point.maxScore),
    })),
  }
  await guarded(
    () => api(`/api/rubrics/${compilingRubricId}/points`, { method: 'PUT', body: payload }),
  )
  if (!isCurrentBatch(batchId, selectionRequest) || request !== rubricRequest || rubricId.value !== compilingRubricId) return
  const frozen = await guarded(
    () => api(`/api/rubrics/${compilingRubricId}/compile`, {
      method: 'POST',
      body: { expectedWeightTotal: 100 },
    }),
    '评分规则已冻结',
  )
  if (!isCurrentBatch(batchId, selectionRequest) || request !== rubricRequest || rubricId.value !== compilingRubricId) return
  compiled.value = { ...frozen, points: payload.points }
  parseResult.value = { ...parseResult.value, points: payload.points }
  localStorage.setItem(`smartgrade.compiled.${batchId}`, JSON.stringify(compiled.value))
}

function openFilePicker() {
  fileInput.value?.click()
}

function openCamera() {
  cameraInput.value?.click()
}

async function chooseFiles(event) {
  await addFiles(event.target.files)
  event.target.value = ''
}

async function handleDrop(event) {
  dragActive.value = false
  await addFiles(event.dataTransfer?.files)
}

async function addFiles(fileList) {
  const supported = ['.txt', '.jpg', '.jpeg', '.png', '.pdf', '.zip']
  const incoming = Array.from(fileList || []).filter((file) => {
    const lower = file.name.toLowerCase()
    return supported.some((extension) => lower.endsWith(extension))
  })
  const rejected = Array.from(fileList || []).length - incoming.length
  if (rejected > 0) {
    notify(`已忽略 ${rejected} 个不支持的文件`, true)
  }
  for (const original of incoming) {
    if (importEntries.value.length >= 20) {
      notify('单次最多选择20个文件（ZIP解压后也须满足20份限制）', true)
      break
    }
    if (original.size > 20 * 1024 * 1024) {
      notify(`${original.name} 超过单文件20MB限制`, true)
      continue
    }
    const exists = importEntries.value.some((entry) =>
      entry.file.name === original.name && entry.file.size === original.size)
    if (exists) continue
    const file = original
    const kind = /\.txt$/i.test(file.name) ? 'TEXT' : /\.pdf$/i.test(file.name) ? 'PDF' : /\.zip$/i.test(file.name) ? 'ZIP' : 'IMAGE'
    if (importEntries.value.reduce((sum, entry) => sum + entry.file.size, 0) + file.size > 100 * 1024 * 1024) {
      notify('单次上传总大小不得超过100MB', true)
      continue
    }
    importEntries.value.push({
      uploadId: crypto.randomUUID(),
      file,
      kind,
      preview: kind === 'IMAGE' ? URL.createObjectURL(file) : '',
      status: 'READY',
    })
  }
}

function removeFile(index) {
  const [removed] = importEntries.value.splice(index, 1)
  if (removed?.preview) URL.revokeObjectURL(removed.preview)
}

async function importAnswers() {
  if (!compiled.value) {
    notify('请先冻结评分规则', true)
    return
  }
  if (!importEntries.value.length) {
    notify('请点击选择，或把答卷拖到上传区域', true)
    return
  }
  importing.value = true
  const importingBatchId = batch.value.batchId
  const selectionRequest = batchRequest
  const submittedEntries = [...importEntries.value]
  try {
    let accepted = 0
    let rejected = 0
    for (const sourceType of [...new Set(submittedEntries.map((entry) => entry.kind))]) {
      const group = submittedEntries.filter((entry) => entry.kind === sourceType)
      const form = new FormData()
      group.forEach((entry) => form.append('files', entry.file))
      const key = importAttempts.keyFor(importingBatchId, sourceType, group)
      const result = await api(
        `/api/batches/${importingBatchId}/answers/import?sourceType=${sourceType}&numberingRule=filename`,
        { method: 'POST', body: form, headers: { 'Idempotency-Key': key } },
      )
      accepted += result.acceptedCount
      rejected += result.rejectedCount || 0
      group.forEach((entry) => entry.preview && URL.revokeObjectURL(entry.preview))
      importEntries.value = importEntries.value.filter((entry) => !group.includes(entry))
    }
    notify(`已接收 ${accepted} 份答卷，校验拒绝 ${rejected} 份，可解释初筛已开始`)
    if (batch.value?.batchId === importingBatchId) batch.value.status = 'PROCESSING'
    window.setTimeout(async () => {
      if (!isCurrentBatch(importingBatchId, selectionRequest)) return
      await refreshProgress()
      if (isCurrentBatch(importingBatchId, selectionRequest)) schedulePolling()
    }, 900)
  } catch (error) {
    notify(error.message, true)
  } finally {
    importing.value = false
  }
}

function statusLabel(status) {
  return {
    PENDING: '等待识别',
    RUNNING: '视觉识别中',
    WAITING_REVIEW: '待复核',
    REVIEWING: '待复核',
    MANUAL_REVIEW: '人工复核',
    FAILED: '识别失败',
    PROCESSING: '正在处理',
    CREATED: '待配置',
    DRAFT: '待配置',
    COMPLETED: '已完成',
    REVIEWED: '已复核',
  }[status] || status
}

async function retryAnswer(answer) {
  if (!batch.value) return
  const batchId = batch.value.batchId
  const selectionRequest = batchRequest
  const key = answer.retryKey || crypto.randomUUID()
  answer.retryKey = key
  try {
    await guarded(() => api(`/api/grading-tasks/${answer.taskId}/retry`, {
      method: 'POST', headers: { 'Idempotency-Key': key },
      body: { retryReason: '教师接管失败初筛任务后重新提交' },
    }), '重试已提交；超过3次失败将转入人工复核')
    if (!isCurrentBatch(batchId, selectionRequest)) return
    await refreshProgress()
    if (isCurrentBatch(batchId, selectionRequest)) schedulePolling()
  } catch { /* guarded already displays the failure reason */ }
}

onMounted(loadBatches)
onBeforeUnmount(() => {
  disposed = true
  window.clearInterval(pollTimer.value)
  importEntries.value.forEach((entry) => entry.preview && URL.revokeObjectURL(entry.preview))
})
</script>

<template>
  <div class="studio-page">
    <Teleport defer to="#workspace-batches">
    <section v-if="batches.length" class="batch-switcher" aria-label="选择阅卷案例">
      <button
        v-for="item in batches"
        :key="item.batchId"
        class="batch-pill"
        :class="{ active: item.batchId === session.selectedBatchId }"
        :aria-pressed="item.batchId === session.selectedBatchId"
        :title="item.examName"
        @click="openBatch(item)"
      >
        <span>{{ item.questionNo }}</span>
        <b>{{ item.examName }}</b>
        <small>{{ item.answerCount }} 份 · {{ statusLabel(item.status) }}</small>
      </button>
    </section>
    </Teleport>

    <Transition name="reveal">
      <form v-if="createCaseOpen || !batches.length" id="create-case" class="panel create-batch-card" @submit.prevent="createBatch">
        <div class="panel-head">
          <div><p class="eyebrow">NEW CASE</p><h3>创建本案例</h3></div>
          <button v-if="batches.length" type="button" class="text-button" @click="createCaseOpen = false">取消新建</button>
          <span v-else class="sticker">{{ batchForm.maxScore }} 分</span>
        </div>
        <div class="form-grid compact-form">
          <label>案例名称<input v-model="batchForm.examName" required minlength="2" maxlength="100" /></label>
          <label>题号<input v-model="batchForm.questionNo" required pattern="[A-Za-z0-9_\-]{1,20}" maxlength="20" placeholder="例如 Q1" title="1–20位字母、数字、下划线或短横线" /><small class="field-hint">例如 Q1、1-1；题目内容填写在下方。</small></label>
          <label>满分<input v-model.number="batchForm.maxScore" type="number" min="0.01" max="100" step="0.01" required /></label>
          <label>基础分<input v-model.number="batchForm.baseScore" type="number" min="0" :max="batchForm.maxScore" step="0.01" required /><small class="field-hint">其余 {{ Math.max(0, Number((batchForm.maxScore - batchForm.baseScore).toFixed(2))) }} 分按评分要点分配。</small></label>
          <label class="span-2">短标题<input v-model="batchForm.questionTitle" required minlength="2" maxlength="200" /></label>
        </div>
        <label class="field-label">完整题干
          <textarea v-model="batchForm.questionText" rows="7"></textarea>
        </label>
        <button class="primary" type="submit" :disabled="creatingCase">{{ creatingCase ? '正在创建…' : '创建案例' }}</button>
      </form>
    </Transition>

    <div v-if="batch" class="studio-grid">
      <section class="studio-main">
        <article
          class="paper-card interactive-card"
          role="button"
          tabindex="0"
          @click="questionExpanded = !questionExpanded"
          @keydown.enter="questionExpanded = !questionExpanded"
          @keydown.space.prevent="questionExpanded = !questionExpanded"
        >
          <header class="paper-head">
            <div>
              <span class="paper-index">{{ batch.questionNo }}</span>
              <h3>{{ batch.questionTitle }}</h3>
            </div>
            <span class="score-stamp">{{ batch.maxScore }}<small>分</small></span>
          </header>
          <p class="question-text" :class="{ clamped: !questionExpanded }">
            {{ batch.questionText || QUESTION_TEXT }}
          </p>
          <button class="read-more" @click.stop="questionExpanded = !questionExpanded">
            {{ questionExpanded ? '收起题干' : '展开完整题干' }}
          </button>
          <div class="reference-note">
            <span>参考答案</span>
            <p>{{ REFERENCE_ANSWER }}</p>
          </div>
        </article>

        <article class="panel rubric-card">
          <div class="panel-head">
            <div><p class="eyebrow">RUBRIC STUDIO</p><h3>评分要点</h3></div>
            <span v-if="compiled" class="state-tag success">已冻结 v{{ compiled.rubricVersion }}</span>
            <span v-else class="state-tag warning">等待冻结</span>
          </div>
          <div class="score-formula">
            <span><b>{{ batchBaseScore }}</b>基础分</span>
            <i>＋</i>
            <span><b>{{ keywordScore }}</b>要点分</span>
            <i>＋</i>
            <span><b>{{ coverageScore }}</b>卷面篇幅</span>
            <i>＝</i>
            <span class="formula-total"><b>{{ batch.maxScore }}</b>满分</span>
          </div>
          <div class="voice-field">
            <div class="voice-field-head">
              <span>可直接编辑，也可以口述规则；系统只提交识别后的文字，不保存录音。</span>
              <button
                type="button"
                class="secondary voice-button"
                :class="{ listening: rubricSpeech.listening.value }"
                :disabled="!!compiled"
                @click="toggleRubricVoice"
              >
                {{ rubricSpeech.listening.value ? '停止识别' : '语音录入要点' }}
              </button>
            </div>
            <p v-if="rubricSpeech.interimText.value" class="voice-interim">
              正在识别：{{ rubricSpeech.interimText.value }}
            </p>
            <textarea v-model="rubricSource" class="source-editor" rows="7"></textarea>
            <small v-if="!rubricSpeech.supported.value" class="voice-hint">
              当前浏览器不支持语音识别，请使用最新版 Edge 或 Chrome。
            </small>
          </div>
          <div class="action-row">
            <button class="secondary" @click="createRubric">创建并解析</button>
            <button class="secondary" :disabled="!!compiled" @click="parseRubric">重新解析</button>
            <button class="primary" :disabled="!!compiled" @click="saveAndCompile">保存并冻结</button>
          </div>

          <div v-if="parseResult?.points?.length" class="criteria-grid">
            <div
              v-for="(point, index) in parseResult.points"
              :key="point.pointId"
              class="criterion-card"
              :class="{ length: point.pointType === 'LENGTH_COVERAGE' }"
            >
              <span class="criterion-number">{{ String(index + 1).padStart(2, '0') }}</span>
              <input v-model="point.pointName" :disabled="!!compiled" class="criterion-name" />
              <template v-if="point.pointType === 'LENGTH_COVERAGE'">
                <p>视觉模型估计作答区覆盖率 ≥ 50%</p>
              </template>
              <template v-else>
                <input
                  :value="point.keywords?.join('|')"
                  :disabled="!!compiled"
                  class="criterion-keywords"
                  @input="point.keywords = $event.target.value.split('|')"
                />
              </template>
              <div class="criterion-meta">
                <label>权重<input v-model.number="point.weight" :disabled="!!compiled" type="number" min="0" step="0.1" /></label>
                <label>分值<input v-model.number="point.maxScore" :disabled="!!compiled" type="number" min="0" step="0.25" /></label>
              </div>
            </div>
          </div>
        </article>
      </section>

      <aside class="studio-side">
        <article class="status-card">
          <div class="status-top">
            <span>LIVE BATCH</span>
            <i class="live-dot"></i>
          </div>
          <h3>{{ batch.examName }}</h3>
          <p>{{ batch.questionNo }} · 满分 {{ batch.maxScore }} · {{ statusLabel(batch.status) }}</p>
          <div v-if="progress" class="status-numbers">
            <div><b><AnimatedNumber :value="progress.total" /></b><span>答卷</span></div>
            <div><b><AnimatedNumber :value="processingCount" /></b><span>识别中</span></div>
            <div><b><AnimatedNumber :value="progress.waitingReview" /></b><span>待复核</span></div>
          </div>
          <RouterLink v-if="canReview || progress?.waitingReview" to="/results" class="inverse-button">
            打开复核定分
          </RouterLink>
        </article>

        <article class="panel upload-card">
          <div class="panel-head">
            <div><p class="eyebrow">CAPTURE</p><h3>导入答卷</h3></div>
            <span class="tiny-note">TXT · 图片 · PDF · ZIP</span>
          </div>
          <div
            class="upload-zone"
            :class="{ dragging: dragActive, ready: importEntries.length }"
            role="button"
            tabindex="0"
            @click="openFilePicker"
            @keydown.enter="openFilePicker"
            @dragenter.prevent="dragActive = true"
            @dragover.prevent="dragActive = true"
            @dragleave.prevent="dragActive = false"
            @drop.prevent="handleDrop"
          >
            <input
              ref="fileInput"
              class="visually-hidden"
              multiple
              type="file"
              accept=".txt,.jpg,.jpeg,.png,.pdf,.zip"
              @change="chooseFiles"
            />
            <span class="upload-illustration">
              <i>＋</i>
              <em>SCAN</em>
            </span>
            <strong>{{ dragActive ? '松手放下答卷' : '拖入答卷，或点击选择' }}</strong>
            <small>保留上传原文件供复核查看；单文件不超过 20 MB</small>
            <button type="button" class="ghost-chip" @click.stop="openCamera">打开相机拍照</button>
            <input
              ref="cameraInput"
              class="visually-hidden"
              type="file"
              accept="image/*"
              capture="environment"
              @change="chooseFiles"
            />
          </div>

          <div v-if="importEntries.length" class="file-wall">
            <article v-for="(entry, index) in importEntries" :key="`${entry.file.name}-${entry.file.size}`" class="file-tile">
              <img v-if="entry.preview" :src="entry.preview" alt="" />
              <span v-else class="text-file-mark">{{ entry.kind }}</span>
              <div>
                <b>{{ entry.file.name }}</b>
                <small>{{ { IMAGE: '手写图片', TEXT: '文本答卷', PDF: 'PDF答卷', ZIP: 'ZIP答卷包' }[entry.kind] }} · {{ Math.ceil(entry.file.size / 1024) }} KB</small>
              </div>
              <button type="button" aria-label="移除文件" @click="removeFile(index)">×</button>
            </article>
          </div>
          <p v-if="selectedNames.length" class="file-summary">已准备 {{ selectedNames.length }} 份答卷</p>
          <button class="primary wide pulse-button" :disabled="importing" @click="importAnswers">
            {{ importing ? '正在上传…' : '开始识别并初筛' }}
          </button>
        </article>

        <article v-if="answers.length" class="panel answer-queue-card">
          <div class="panel-head">
            <div><p class="eyebrow">READING QUEUE</p><h3>识别轨迹</h3></div>
            <button class="text-button" @click="refreshProgress">刷新</button>
          </div>
          <div class="answer-list">
            <div v-for="answer in answers" :key="answer.answerId" class="answer-row">
              <span class="student-avatar">{{ answer.studentNo.slice(-2) }}</span>
              <span class="answer-student">
                <b>{{ answer.studentNo }}</b>
                <small>{{ answer.sourceType === 'IMAGE' ? '手写图片' : '文本答卷' }}</small>
              </span>
              <span class="answer-state">{{ statusLabel(answer.processStatus) }}</span>
              <button v-if="answer.processStatus === 'FAILED' && answer.taskId" class="text-button" @click="retryAnswer(answer)">重试初筛</button>
              <span v-if="answer.ocrCoverageRatio != null" class="coverage-pill">
                卷面 {{ Math.round(Number(answer.ocrCoverageRatio) * 100) }}%
              </span>
            </div>
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>
