<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { notify, setSession } from '../session.js'

const router = useRouter()
const username = ref('teacher1')
const password = ref('Teach2026')
const loading = ref(false)

async function login(account = username.value) {
  loading.value = true
  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: { username: account, password: password.value },
    })
    setSession(data)
    notify(`欢迎，${data.name}`)
    router.push('/workspace')
  } catch (error) {
    notify(error.message, true)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-story">
      <div class="brand login-brand">
        <span class="brand-mark">阅</span>
        <div>
          <strong>同标阅卷</strong>
          <small>CONSISTENT GRADING</small>
        </div>
      </div>
      <div class="story-copy">
        <p class="eyebrow light">统一评分规则 · 前后一致给分</p>
        <h1>前后同尺度，<br />主观同标准。</h1>
        <p>
          同一套评分规则贯穿整批答卷，让前后的给分尺度一致，让主观题判断有统一标准。
          关键词证据辅助复核，每一份最终成绩由教师确认。
        </p>
        <div class="story-grid">
          <div><b>同尺度</b><span>前后评分保持一致</span></div>
          <div><b>有依据</b><span>逐项呈现给分证据</span></div>
          <div><b>可复核</b><span>教师确认最终成绩</span></div>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <form class="login-card" @submit.prevent="login()">
        <p class="eyebrow">教师工作台</p>
        <h2>进入同标阅卷</h2>
        <p class="muted">从统一评分规则开始，让每一份判断前后一致。</p>
        <label>
          账号
          <input v-model="username" autocomplete="username" placeholder="请输入账号" />
        </label>
        <label>
          密码
          <input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" />
        </label>
        <button class="primary wide" :disabled="loading">{{ loading ? '正在登录…' : '进入工作台' }}</button>
        <button type="button" class="secondary wide" @click="login('admin')">以管理员账号演示</button>
        <p class="demo-tip">演示账号：teacher1 / Teach2026 · 另有 assistant1、admin</p>
      </form>
    </section>
  </div>
</template>
