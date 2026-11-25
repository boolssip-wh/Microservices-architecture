<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  decimals: { type: Number, default: 0 },
  duration: { type: Number, default: 720 },
})

const displayValue = ref(0)
let frameId = null

function targetValue() {
  const value = Number(props.value)
  return Number.isFinite(value) ? value : 0
}

function animateTo(value) {
  window.cancelAnimationFrame(frameId)
  const target = Number.isFinite(Number(value)) ? Number(value) : 0
  const startValue = displayValue.value
  const startedAt = performance.now()
  const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  const duration = reducedMotion ? 0 : props.duration

  function tick(now) {
    const progress = duration === 0 ? 1 : Math.min(1, (now - startedAt) / duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    displayValue.value = startValue + (target - startValue) * eased
    if (progress < 1) {
      frameId = window.requestAnimationFrame(tick)
    } else {
      displayValue.value = target
    }
  }

  frameId = window.requestAnimationFrame(tick)
}

watch(() => props.value, (value) => animateTo(value), { immediate: true })
onMounted(() => animateTo(targetValue()))
onBeforeUnmount(() => window.cancelAnimationFrame(frameId))
</script>

<template>
  <span class="animated-number">{{ displayValue.toFixed(decimals) }}</span>
</template>
