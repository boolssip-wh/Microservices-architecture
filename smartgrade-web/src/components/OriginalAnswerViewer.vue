<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { fetchOriginalAnswer } from '../api.js'

const props = defineProps({ batchId: { type: String, required: true }, answerId: { type: String, required: true }, studentNo: String })
const opened = ref(false)
const loading = ref(false)
const error = ref('')
const original = ref(null)
const text = ref('')
const zoom = ref(100)
let controller
let requestId = 0

function reset() {
  requestId++
  controller?.abort()
  if (original.value?.url) URL.revokeObjectURL(original.value.url)
  original.value = null
  text.value = ''
  error.value = ''
  loading.value = false
  opened.value = false
  zoom.value = 100
}

async function open() {
  opened.value = true
  if (original.value || loading.value) return
  const request = ++requestId
  controller = new AbortController()
  loading.value = true
  error.value = ''
  try {
    const file = await fetchOriginalAnswer(props.batchId, props.answerId, controller.signal)
    const content = file.blob.type.startsWith('text/') ? await file.blob.text() : ''
    if (request !== requestId) return
    text.value = content
    original.value = { ...file, url: URL.createObjectURL(file.blob) }
  } catch (failure) {
    if (request === requestId && failure.name !== 'AbortError') error.value = failure.message
  } finally {
    if (request === requestId) loading.value = false
  }
}

function download() {
  if (!original.value) return
  const link = document.createElement('a')
  link.href = original.value.url
  link.download = original.value.filename
  link.click()
}

watch(() => [props.batchId, props.answerId], reset)
onBeforeUnmount(reset)
</script>

<template>
  <section class="panel original-file-panel">
    <div class="panel-head">
      <div><p class="eyebrow">原始上传文件</p><h2>原试卷</h2></div>
      <button v-if="!opened" class="secondary" @click="open">查看原试卷</button>
      <button v-else class="text-button" @click="reset">收起原试卷</button>
    </div>
    <p v-if="!opened" class="muted">浏览 {{ studentNo }} 的上传文件，与识别文字和命中证据对照复核。</p>
    <div v-else :aria-busy="loading" class="original-file-content">
      <p v-if="loading" role="status">正在加载原始文件…</p>
      <div v-else-if="error" class="original-file-error" role="alert">
        <p>{{ error }}</p><button class="secondary" @click="open">重新加载</button>
      </div>
      <template v-else-if="original">
        <div class="original-file-toolbar">
          <strong>{{ original.filename }}</strong>
          <div class="original-file-actions">
            <template v-if="original.blob.type.startsWith('image/')">
              <button class="secondary" :disabled="zoom <= 50" aria-label="缩小原试卷" @click="zoom -= 25">−</button>
              <span>{{ zoom }}%</span>
              <button class="secondary" :disabled="zoom >= 250" aria-label="放大原试卷" @click="zoom += 25">＋</button>
            </template>
            <button class="primary" @click="download">下载原文件</button>
          </div>
        </div>
        <div v-if="original.blob.type.startsWith('image/')" class="original-image-scroll">
          <img :src="original.url" :alt="`${studentNo} 的原始试卷`" :style="{ width: `${zoom}%` }" />
        </div>
        <iframe v-else-if="original.blob.type === 'application/pdf'" :src="original.url" title="原始 PDF 试卷" class="original-pdf"></iframe>
        <pre v-else-if="original.blob.type.startsWith('text/')" class="original-text">{{ text }}</pre>
        <p v-else class="muted">此文件暂不支持在线预览，请下载原文件查看。</p>
        <p class="original-file-hint">若预览未显示完整内容，可下载原文件后查看。</p>
      </template>
    </div>
  </section>
</template>
