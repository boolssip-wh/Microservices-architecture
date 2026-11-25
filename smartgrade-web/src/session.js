import { reactive } from 'vue'

// Credentials are scoped to this browser tab, as specified by C01.
localStorage.removeItem('smartgrade.token')
localStorage.removeItem('smartgrade.user')

export const session = reactive({
  token: sessionStorage.getItem('smartgrade.token') || '',
  user: JSON.parse(sessionStorage.getItem('smartgrade.user') || 'null'),
  selectedBatchId: localStorage.getItem('smartgrade.batch') || '',
  toast: '',
  toastError: false,
})

export function setSession(data) {
  session.token = data.accessToken
  session.user = {
    teacherId: data.teacherId,
    name: data.name,
    role: data.role,
  }
  sessionStorage.setItem('smartgrade.token', session.token)
  sessionStorage.setItem('smartgrade.user', JSON.stringify(session.user))
}

export function clearSession() {
  session.token = ''
  session.user = null
  session.selectedBatchId = ''
  sessionStorage.removeItem('smartgrade.token')
  sessionStorage.removeItem('smartgrade.user')
  localStorage.removeItem('smartgrade.batch')
}

export function selectBatch(batchId) {
  session.selectedBatchId = batchId || ''
  if (batchId) {
    localStorage.setItem('smartgrade.batch', batchId)
  } else {
    localStorage.removeItem('smartgrade.batch')
  }
}

export function notify(message, error = false) {
  session.toast = message
  session.toastError = error
  window.clearTimeout(notify.timer)
  notify.timer = window.setTimeout(() => {
    session.toast = ''
  }, 3200)
}
