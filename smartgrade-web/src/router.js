import { createRouter, createWebHistory } from 'vue-router'
import { session } from './session.js'
import LoginView from './views/LoginView.vue'
import WorkspaceView from './views/WorkspaceView.vue'
import ResultsView from './views/ResultsView.vue'
import ReportsView from './views/ReportsView.vue'
import SettingsView from './views/SettingsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/workspace' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/workspace', component: WorkspaceView },
    { path: '/results', component: ResultsView },
    { path: '/reports', component: ReportsView },
    { path: '/settings', component: SettingsView },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !session.token) {
    return '/login'
  }
  if (to.path === '/login' && session.token) {
    return '/workspace'
  }
})

export default router
