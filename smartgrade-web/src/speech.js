import { onBeforeUnmount, ref } from 'vue'

const ERROR_MESSAGES = {
  'not-allowed': '麦克风权限未开启，请在浏览器设置中允许本页面使用麦克风。',
  'service-not-allowed': '当前浏览器未允许语音识别服务。',
  'no-speech': '没有检测到语音，请靠近麦克风后重试。',
  'audio-capture': '没有找到可用的麦克风。',
  network: '语音识别网络连接失败，请检查网络后重试。',
  aborted: '语音输入已取消。',
}

export function useSpeechToText({ onFinal, onError } = {}) {
  const Recognition = typeof window === 'undefined'
    ? null
    : window.SpeechRecognition || window.webkitSpeechRecognition
  const supported = Boolean(Recognition)
  const listening = ref(false)
  const interimText = ref('')
  let recognition = null

  function stop() {
    recognition?.stop()
  }

  function start() {
    if (!Recognition) {
      onError?.('语音识别服务不可用，请检查浏览器麦克风权限后重试。')
      return false
    }
    stop()
    recognition = new Recognition()
    recognition.lang = 'zh-CN'
    recognition.continuous = false
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onstart = () => {
      listening.value = true
      interimText.value = ''
    }
    recognition.onresult = (event) => {
      let finalText = ''
      let pendingText = ''
      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const result = event.results[index]
        if (result.isFinal) finalText += result[0].transcript
        else pendingText += result[0].transcript
      }
      interimText.value = pendingText
      if (finalText.trim()) {
        onFinal?.(finalText.trim())
        interimText.value = ''
      }
    }
    recognition.onerror = (event) => {
      listening.value = false
      if (event.error !== 'aborted') {
        onError?.(ERROR_MESSAGES[event.error] || `语音识别失败：${event.error}`)
      }
    }
    recognition.onend = () => {
      listening.value = false
      interimText.value = ''
    }

    try {
      recognition.start()
      return true
    } catch {
      listening.value = false
      onError?.('麦克风正在启动，请稍后再试。')
      return false
    }
  }

  onBeforeUnmount(() => {
    recognition?.abort()
    recognition = null
  })

  return { supported, listening, interimText, start, stop }
}
