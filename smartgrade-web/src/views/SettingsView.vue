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
const smsDeliveries = ref([])
const sms = reactive({
  phoneInput: '',
  maskedPhone: '',
  bound: false,
  enabled: true,
  updatedAt: '',
  saving: false,
  testing: false,
})
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

async function loadNotificationChannel() {
  const data = await api('/api/teachers/me/notification-channel')
  sms.bound = Boolean(data.bound)
  sms.maskedPhone = data.maskedPhone || ''
  sms.enabled = Boolean(data.enabled)
  sms.updatedAt = data.updatedAt || ''
}

async function saveNotificationChannel() {
  const phone = sms.phoneInput.replace(/\s+/g, '')
  if (!/^(?:\+?86)?1[3-9]\d{9}$/.test(phone)) {
    notify('请输入有效的中国大陆手机号', true)
    return
  }
  sms.saving = true
  try {
    const data = await guarded(
      () => api('/api/teachers/me/notification-channel', {
        method: 'PUT',
        body: { phone, enabled: sms.enabled },
      }),
      sms.enabled ? '手机号已绑定并开启任务提醒' : '手机号已绑定，任务提醒已关闭',
    )
    sms.phoneInput = ''
    sms.bound = Boolean(data.bound)
    sms.maskedPhone = data.maskedPhone || ''
    sms.enabled = Boolean(data.enabled)
    sms.updatedAt = data.updatedAt || ''
  } catch { /* guarded already displayed the failure */ }
  finally { sms.saving = false }
}

async function loadSmsDeliveries() {
  smsDeliveries.value = await api('/api/notifications/sms-deliveries')
}

async function testSms() {
  if (sms.testing) return
  sms.testing = true
  try {
    const delivery = await guarded(
      () => api('/api/notifications/test', { method: 'POST' }),
      '测试提醒已写入发送记录',
    )
    if (delivery?.status === 'MOCKED') {
      notify(`模拟发送已完成，目标 ${delivery.targetMasked}；未调用真实短信平台`)
    }
    await loadSmsDeliveries()
  } catch { /* guarded already displayed the failure */ }
  finally { sms.testing = false }
}

async function markRead(item) {
  if (item.readAt) return
  await api(`/api/notifications/${item.notificationId}/read`, { method: 'PUT' })
  await loadNotifications()
}

onMounted(() => {
  loadProfile().catch((error) => notify(error.message, true))
  loadNotifications().catch((error) => notify(error.message, true))
  loadNotificationChannel().catch((error) => notify(error.message, true))
  loadSmsDeliveries().catch((error) => notify(error.message, true))
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

    <div class="column">
      <section class="panel sms-channel-card">
        <div class="panel-head">
          <div><p class="eyebrow">MOBILE REMINDER</p><h2>手机任务提醒</h2></div>
          <span class="state-tag" :class="sms.bound && sms.enabled ? 'success' : 'warning'">
            {{ sms.bound && sms.enabled ? '已开启' : '未开启' }}
          </span>
        </div>
        <p class="muted">
          初筛完成和整批复核完成时发送关键节点提醒。电脑端仍通过页面实时查看处理进度；
          短信不承担实时百分比推送。
        </p>
        <div class="sms-binding-summary">
          <span>绑定号码</span>
          <b>{{ sms.bound ? sms.maskedPhone : '尚未绑定' }}</b>
        </div>
        <div class="form-grid sms-form-grid">
          <label>
            手机号
            <input v-model="sms.phoneInput" inputmode="tel" maxlength="14" placeholder="请输入11位手机号" />
          </label>
          <label class="toggle-label">
            任务短信提醒
            <span class="toggle-row">
              <input v-model="sms.enabled" type="checkbox" />
              <span>{{ sms.enabled ? '开启' : '关闭' }}</span>
            </span>
          </label>
        </div>
        <div class="action-row">
          <button class="primary" :disabled="sms.saving" @click="saveNotificationChannel">
            {{ sms.saving ? '正在保存…' : (sms.bound ? '更新绑定' : '绑定手机') }}
          </button>
          <button class="secondary" :disabled="sms.testing || !sms.bound || !sms.enabled" @click="testSms">
            {{ sms.testing ? '正在发送…' : '发送测试提醒' }}
          </button>
        </div>
        <p class="sms-provider-note">
          当前默认使用模拟短信网关，只生成可核验记录，不会把手机号暴露到页面或日志；
          接入真实短信服务商后改为通用 Webhook 即可。
        </p>
        <div v-if="smsDeliveries.length" class="sms-delivery-list">
          <article v-for="item in smsDeliveries.slice(0, 5)" :key="item.deliveryId">
            <span>
              <b>{{ item.eventType === 'sms.test' ? '通道测试' : item.eventType }}</b>
              <small>{{ item.targetMasked }} · {{ item.provider }} · {{ item.status }}</small>
            </span>
            <time>{{ new Date(item.createdAt).toLocaleString('zh-CN') }}</time>
          </article>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div><p class="eyebrow">INBOX</p><h2>站内任务通知</h2></div>
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
  </div>
</template>
