<script setup>
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { api, guarded } from '../api.js'
import { session, notify } from '../session.js'

const profile = reactive({
  tone: 'NEUTRAL',
  lengthMode: 'MEDIUM',
  openingStyle: 'DIRECT',
  commonPhrases: ['继续保持'],
  bannedPhrases: ['你太差了'],
})
const notifications = ref([])
const unreadCount = ref(0)
let streamController
let notificationTimer
let reconnectTimer
let active = true

async function connectNotifications() {
  streamController = new AbortController()
  try {
    const response = await fetch('/api/notifications/stream', {
      headers: { Authorization: `Bearer ${session.token}`, Accept: 'text/event-stream' },
      signal: streamController.signal,
    })
    if (!response.ok || !response.body) throw new Error('通知流暂不可用')
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (active) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let boundary
      while ((boundary = buffer.indexOf('\n\n')) >= 0) {
        const message = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        if (message.split('\n').some((line) => line.startsWith('data:'))) await loadNotifications()
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') return
  }
  if (active) reconnectTimer = window.setTimeout(connectNotifications, 5000)
}

async function loadProfile() {
  const data = await api('/api/teachers/me/comment-profile')
  Object.assign(profile, {
    tone: data.tone,
    lengthMode: data.lengthMode,
    openingStyle: data.openingStyle,
    commonPhrases: data.commonPhrases || [],
    bannedPhrases: data.bannedPhrases || [],
  })
}

async function saveProfile() {
  await guarded(
    () => api('/api/teachers/me/comment-profile', {
      method: 'PUT',
      body: {
        ...profile,
        commonPhrases: phrases(profile.commonPhrases),
        bannedPhrases: phrases(profile.bannedPhrases),
      },
    }),
    '个性化评语偏好已更新',
  )
}

function phrases(value) {
  return [...new Set((Array.isArray(value) ? value : value.split('|')).map((text) => text.trim()).filter(Boolean))]
}

async function loadNotifications() {
  const data = await api('/api/notifications?page=1&size=20')
  notifications.value = data.items
  unreadCount.value = data.unreadCount
}

async function markRead(item) {
  if (item.readAt) return
  await api(`/api/notifications/${item.notificationId}/read`, { method: 'PUT' })
  await loadNotifications()
}

onMounted(() => {
  loadProfile().catch((error) => notify(error.message, true))
  loadNotifications().catch((error) => notify(error.message, true))
  connectNotifications()
  notificationTimer = window.setInterval(() => loadNotifications().catch(() => {}), 5000)
})

onBeforeUnmount(() => {
  active = false
  streamController?.abort()
  window.clearInterval(notificationTimer)
  window.clearTimeout(reconnectTimer)
})
</script>

<template>
  <div class="settings-grid">
    <section class="panel">
      <div class="panel-head">
        <div><p class="eyebrow">COMMENT PROFILE</p><h2>我的评语偏好</h2></div>
      </div>
      <p class="muted">草稿生成会读取这些偏好；最终文字仍由教师确认。</p>
      <div class="form-grid">
        <label>语气<select v-model="profile.tone"><option value="NEUTRAL">中性</option><option value="ENCOURAGING">鼓励</option><option value="STRICT">严格</option></select></label>
        <label>长度<select v-model="profile.lengthMode"><option value="SHORT">简短</option><option value="MEDIUM">适中</option><option value="LONG">详细</option></select></label>
        <label>开头方式<select v-model="profile.openingStyle"><option value="DIRECT">直接说明</option><option value="PRAISE_FIRST">肯定优先</option><option value="QUESTION">问题引导</option></select></label>
        <label>常用表达<input v-model="profile.commonPhrases" placeholder="多个表达用 | 分隔" /></label>
        <label class="span-2">禁用表达<input v-model="profile.bannedPhrases" placeholder="多个表达用 | 分隔" /></label>
      </div>
      <button class="primary" @click="saveProfile">保存偏好</button>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div><p class="eyebrow">INBOX</p><h2>任务通知</h2></div>
        <span class="count-pill">{{ unreadCount }} 条未读</span>
      </div>
      <div class="notification-list">
        <button
          v-for="item in notifications"
          :key="item.notificationId"
          class="notification-item"
          :class="{ unread: !item.readAt }"
          @click="markRead(item)"
        >
          <span class="notice-dot"></span>
          <span>
            <b>{{ item.title }}</b>
            <small>{{ item.content }}</small>
          </span>
        </button>
        <p v-if="!notifications.length" class="empty-state">暂无通知。</p>
      </div>
    </section>
  </div>
</template>
