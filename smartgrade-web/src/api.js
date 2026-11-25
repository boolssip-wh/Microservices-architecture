import { clearSession, notify, session } from './session.js'

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  if (session.token) {
    headers.set('Authorization', `Bearer ${session.token}`)
  }
  const isForm = options.body instanceof FormData
  if (options.body && !isForm && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(path, {
    ...options,
    headers,
    body: options.body && !isForm && typeof options.body !== 'string'
      ? JSON.stringify(options.body)
      : options.body,
  })
  const payload = await response.json().catch(() => ({
    code: -1,
    message: `请求失败（${response.status}）`,
  }))
  if (response.status === 401) {
    clearSession()
  }
  if (!response.ok || payload.code !== 0) {
    const error = new Error(payload.message || '请求失败')
    error.traceId = payload.traceId
    error.status = response.status
    error.code = payload.code
    throw error
  }
  return payload.data
}

export async function fetchOriginalAnswer(batchId, answerId, signal) {
  const response = await fetch(`/api/batches/${encodeURIComponent(batchId)}/answers/${encodeURIComponent(answerId)}/original`, {
    headers: { Authorization: `Bearer ${session.token}` }, signal, cache: 'no-store',
  })
  if (response.status === 401) clearSession()
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}))
    throw new Error(payload.message || `原始文件读取失败（${response.status}）`)
  }
  const disposition = response.headers.get('Content-Disposition') || ''
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  let filename = `${answerId}.bin`
  try { filename = encoded ? decodeURIComponent(encoded) : disposition.match(/filename="([^"]+)"/i)?.[1] || filename } catch { /* use the safe fallback */ }
  return { filename, blob: await response.blob() }
}

export async function guarded(action, success) {
  try {
    const result = await action()
    if (success) notify(success)
    return result
  } catch (error) {
    notify(`${error.message}${error.traceId ? `（traceId ${error.traceId}）` : ''}`, true)
    throw error
  }
}
