<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearSession, session } from './session.js'

const route = useRoute()
const router = useRouter()
const showShell = computed(() => route.path !== '/login')
const newCaseRequest = ref(0)

const navItems = [
  { path: '/workspace', label: '阅卷空间', code: '01' },
  { path: '/results', label: '复核定分', code: '02' },
  { path: '/reports', label: '统计报告', code: '03' },
  { path: '/settings', label: '教师偏好与任务通知', code: '04' },
]

function logout() {
  clearSession()
  router.push('/login')
}
</script>

<template>
  <div v-if="showShell" class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/workspace" class="brand" aria-label="返回答卷批次">
        <span class="brand-mark">阅</span>
        <div>
          <strong>同标阅卷</strong>
          <small>CONSISTENT GRADING</small>
        </div>
      </RouterLink>

      <nav class="nav-list">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          <span>{{ item.code }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>

      <RouterLink to="/reports" class="sidebar-note">
        <span class="status-dot"></span>
        <div>
          <strong>治理链路可观测</strong>
          <small>Nacos · Sentinel · Gateway</small>
        </div>
      </RouterLink>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div class="topbar-heading">
          <p class="eyebrow">同标阅卷 · 教师工作台</p>
          <h1>{{ navItems.find((item) => item.path === route.path)?.label || '同标阅卷' }}</h1>
          <p class="workspace-tagline">前后同尺度，主观同标准。</p>
        </div>
        <div v-if="route.path === '/workspace'" id="workspace-batches" class="topbar-batches"></div>
        <div class="topbar-actions">
          <button v-if="route.path === '/workspace'" class="primary topbar-create" @click="newCaseRequest++">＋ 新建案例</button>
          <div class="user-area">
          <div class="avatar">{{ session.user?.name?.slice(0, 1) || '师' }}</div>
          <div>
            <strong>{{ session.user?.name || '未登录' }}</strong>
            <small>{{ { ADMIN: '管理员', ASSISTANT: '助教', TEACHER: '教师' }[session.user?.role] || '未登录' }}</small>
          </div>
          <button class="text-button" @click="logout">退出</button>
          </div>
        </div>
      </header>
      <section class="page-content">
        <RouterView v-slot="{ Component }">
          <Transition name="page-swap" mode="out-in">
            <component :is="Component" :key="route.path" v-bind="route.path === '/workspace' ? { newCaseRequest } : {}" />
          </Transition>
        </RouterView>
      </section>
    </main>
  </div>

  <RouterView v-else v-slot="{ Component }">
    <Transition name="page-swap" mode="out-in">
      <component :is="Component" :key="route.path" />
    </Transition>
  </RouterView>

  <Transition name="toast">
    <div v-if="session.toast" class="toast" :class="{ error: session.toastError }">
      {{ session.toast }}
    </div>
  </Transition>
</template>
