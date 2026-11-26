<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { api, guarded } from '../api.js'
import { notify, session } from '../session.js'
import OriginalAnswerViewer from '../components/OriginalAnswerViewer.vue'
import { useSpeechToText } from '../speech.js'

const results = ref([])
const selected = ref(null)
const suggestions = ref([])
const generationMode = ref('')
const suggesting = ref(false)
const loading = ref(false)
const statusFilter = ref('WAITING_REVIEW')
let resultsRequest = 0
let selectionRequest = 0
let suggestionRequest = 0
let disposed = false
const review = reactive({
  finalScore: 0,
  reviewNote: '教师已核对证据并确认给分。',
  finalComment: '',
})

const commentSpeech = useSpeechToText({
  onFinal(text) {
    const current = review.finalComment.trim()
    review.finalComment = current ? `${current}\n${text}` : text
    notify('语音评语已转成文字，可继续修改')
  },
  onError: (message) => notify(message, true),
})

function toggleCommentVoice() {
  if (commentSpeech.listening.value) {
    commentSpeech.stop()
    return
  }
  commentSpeech.start()
}

function isCurrentResult(batchId, answerId, request) {
  return !disposed && request === selectionRequest
    && session.selectedBatchId === batchId && selected.value?.answerId === answerId
}

async function loadResults() {
  const batchId = session.selectedBatchId
  const status = statusFilter.value
  const request = ++resultsRequest
  ++selectionRequest
  ++suggestionRequest
  results.value = []
  selected.value = null
  suggestions.value = []
  generationMode.value = ''
  suggesting.value = false
  if (!batchId) {
    notify('请先在阅卷工作台选择批次', true)
    return
  }
  loading.value = true
  try {
    const data = await api(
      `/api/results?batchId=${encodeURIComponent(batchId)}&status=${status}&page=1&size=100`,
    )
    if (disposed || request !== resultsRequest || session.selectedBatchId !== batchId || statusFilter.value !== status) return
    results.value = data.items || []
    if (results.value.length) await openResult(results.value[0])
  } catch (error) {
    if (!disposed && request === resultsRequest && session.selectedBatchId === batchId) notify(error.message, true)
  } finally {
    if (!disposed && request === resultsRequest) loading.value = false
  }
}

async function openResult(item) {
  const batchId = session.selectedBatchId
  const request = ++selectionRequest
  ++suggestionRequest
  selected.value = null
  suggestions.value = []
  generationMode.value = ''
  suggesting.value = false
  try {
    const result = await api(`/api/results/${item.answerId}`)
    if (disposed || request !== selectionRequest || session.selectedBatchId !== batchId) return
    selected.value = result
    review.finalScore = result.finalScore ?? result.suggestedScore ?? 0
    review.finalComment = result.comment?.finalText || result.comment?.suggestedText || ''
    review.reviewNote = '教师已核对证据并确认给分。'
  } catch (error) {
    if (!disposed && request === selectionRequest && session.selectedBatchId === batchId) notify(error.message, true)
  }
}

async function suggest() {
  if (!selected.value || suggesting.value) return
  const batchId = session.selectedBatchId
  const answerId = selected.value.answerId
  const currentSelection = selectionRequest
  const request = ++suggestionRequest
  const finalScore = Number(review.finalScore)
  const profileVersion = selected.value.comment?.profileVersion
  suggesting.value = true
  try {
    const data = await api(`/api/results/${answerId}/comment-suggestion`, {
      method: 'POST',
      body: {
        finalScore,
        profileVersion,
      },
    })
    if (!isCurrentResult(batchId, answerId, currentSelection) || request !== suggestionRequest || Number(review.finalScore) !== finalScore) return
    suggestions.value = data.suggestedComments || []
    generationMode.value = data.generationMode || ''
    selected.value.comment = { ...selected.value.comment, profileVersion: data.profileVersion }
    if (!review.finalComment && suggestions.value[0]) {
      review.finalComment = suggestions.value[0]
    }
    const prefix = generationMode.value === 'AI' ? 'AI ' : '本地模板'
    notify(`${prefix}已根据评分证据生成两条评语`)
  } catch (error) {
    if (isCurrentResult(batchId, answerId, currentSelection) && request === suggestionRequest) notify(error.message, true)
  } finally {
    if (isCurrentResult(batchId, answerId, currentSelection) && request === suggestionRequest) {
      suggesting.value = false
    }
  }
}

async function finalize() {
  if (!selected.value) return
  if (!review.finalComment.trim()) {
    notify('请填写或选择最终评语', true)
    return
  }
  const batchId = session.selectedBatchId
  const answerId = selected.value.answerId
  const currentSelection = selectionRequest
  ++suggestionRequest
  await guarded(
    () => api(`/api/results/${answerId}/final-score`, {
      method: 'PUT',
      body: {
        finalScore: Number(review.finalScore),
        reviewNote: review.reviewNote,
        finalComment: review.finalComment,
        commentProfileVersion: selected.value.comment?.profileVersion,
      },
    }),
    '最终分、成绩快照与审计记录已保存',
  )
  if (isCurrentResult(batchId, answerId, currentSelection)) await loadResults()
}

onMounted(loadResults)
onBeforeUnmount(() => { disposed = true })
</script>

<template>
  <div class="results-layout">
    <section class="panel result-queue">
      <div class="panel-head">
        <div><p class="eyebrow">REVIEW QUEUE</p><h2>待复核答卷</h2></div>
        <button class="text-button" @click="loadResults">刷新</button>
      </div>
      <p class="muted">批次：{{ session.selectedBatchId || '未选择' }}</p>
      <label>复核状态<select v-model="statusFilter" @change="loadResults"><option value="WAITING_REVIEW">待复核</option><option value="MANUAL_REVIEW">人工复核</option><option value="REVIEWED">已复核</option></select></label>
      <button
        v-for="item in results"
        :key="item.answerId"
        class="result-item"
        :class="{ active: selected?.answerId === item.answerId }"
        @click="openResult(item)"
      >
        <span>
          <b>{{ item.studentNo }}</b>
          <small>建议 {{ item.suggestedScore }}/{{ item.maxScore }} · 置信度 {{ (item.confidence * 100).toFixed(0) }}%</small>
        </span>
        <em>{{ item.status }}</em>
      </button>
      <p v-if="!results.length" class="empty-state">暂无结果。请先在阅卷工作台完成初筛。</p>
    </section>

    <section v-if="selected" class="column">
      <OriginalAnswerViewer :key="selected.answerId" :batch-id="session.selectedBatchId" :answer-id="selected.answerId" :student-no="selected.studentNo" />
      <div class="panel">
        <div class="panel-head">
          <div><p class="eyebrow">EVIDENCE</p><h2>原文与命中证据</h2></div>
          <span class="score-badge">{{ selected.suggestedScore }}/{{ selected.maxScore }}</span>
        </div>
        <div class="review-metrics">
          <span><b>{{ selected.answerText?.replace(/\s+/g, '').length || 0 }}</b> 有效字符</span>
          <span><b>{{ selected.answerCoverageRatio == null ? '-' : Math.round(Number(selected.answerCoverageRatio) * 100) + '%' }}</b> 卷面覆盖</span>
          <span><b>{{ (Number(selected.ocrConfidence || 0) * 100).toFixed(0) }}%</b> 识别置信度</span>
          <span><b>{{ selected.baseScore ?? Math.min(10, Number(selected.maxScore || 20)) }}</b> 基础分起算</span>
        </div>
        <div class="answer-text">{{ selected.answerText }}</div>
        <div class="evidence-list">
          <article v-for="item in selected.evidence" :key="item.evidenceId" class="evidence-card">
            <span class="match-type">{{ item.matchType }}</span>
            <b>{{ item.pointName }}</b>
            <p>“{{ item.sentence }}”</p>
            <small>字符区间 {{ item.start }}–{{ item.end }} · 置信度 {{ (item.confidence * 100).toFixed(0) }}%</small>
          </article>
          <p v-if="!selected.evidence.length" class="empty-state">未识别到命中要点，需要人工复核。</p>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div><p class="eyebrow">FINAL DECISION</p><h2>教师定分</h2></div>
          <button class="secondary" :disabled="suggesting" @click="suggest('DIRECT')">
            {{ suggesting ? 'AI 正在生成…' : 'AI 直接生成' }}
          </button>
        </div>
        <div class="review-form">
          <label>
            最终分（0–{{ selected.maxScore }}）
            <input v-model.number="review.finalScore" type="number" min="0" :max="selected.maxScore" step="0.01" />
          </label>
          <label>
            复核说明
            <input v-model="review.reviewNote" />
          </label>
          <label class="span-2">
            <span class="voice-label-row">
              <span>最终评语（教师确认）</span>
              <button
                type="button"
                class="secondary voice-button"
                :class="{ listening: commentSpeech.listening.value }"
                @click="toggleCommentVoice"
              >
                {{ commentSpeech.listening.value ? '停止识别' : '语音口述评语' }}
              </button>
            </span>
            <textarea
              v-model="review.finalComment"
              rows="5"
              placeholder="AI 草稿会显示在下方；可选择、修改或直接手工填写"
            ></textarea>
            <small v-if="commentSpeech.interimText.value" class="voice-interim">
              正在识别：{{ commentSpeech.interimText.value }}
            </small>
          </label>
        </div>
        <div v-if="suggestions.length" class="suggestion-list">
          <p class="generation-mode">
            {{ generationMode === 'AI'
              ? 'AI 已根据评分证据直接生成'
              : '外部 AI 未启用，当前使用本地模板' }}
          </p>
          <button v-for="text in suggestions" :key="text" @click="review.finalComment = text">{{ text }}</button>
        </div>
        <button class="primary" @click="finalize">确认定分并写入快照</button>
      </div>
    </section>
    <section v-else class="panel empty-panel">
      <span class="empty-mark">SG</span>
      <h2>选择一份答卷开始复核</h2>
      <p class="muted">机器建议分只是初稿，最终结果必须由教师确认。</p>
    </section>
  </div>
</template>
