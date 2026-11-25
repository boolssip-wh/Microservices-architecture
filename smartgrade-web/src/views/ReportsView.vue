<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, guarded } from '../api.js'
import AnimatedNumber from '../components/AnimatedNumber.vue'
import { notify, session } from '../session.js'

const insights = ref(null)
const format = ref('XLSX')
const exportInfo = ref(null)
const loading = ref(false)
const exporting = ref(false)
const downloading = ref(false)

const averagePercent = computed(() => {
  const average = Number(insights.value?.averageScore || 0)
  const maximum = Number(insights.value?.maxScore || 0)
  return maximum > 0 ? Math.min(100, Math.max(0, average / maximum * 100)) : 0
})

const gaugeStyle = computed(() => ({
  '--gauge-value': `${averagePercent.value * 3.6}deg`,
}))

const scoreBands = computed(() => insights.value?.scoreBands || [])
const chartMaximum = computed(() => Math.max(4, Math.ceil(Math.max(0, ...scoreBands.value.map(band => band.count)) / 4) * 4))
const chartTicks = computed(() => Array.from({ length: 5 }, (_, index) => ({
  value: chartMaximum.value * index / 4,
  y: 246 - index * 45,
})))
const chartBars = computed(() => {
  const slot = 516 / Math.max(1, scoreBands.value.length)
  const width = Math.min(72, slot * 0.56)
  return scoreBands.value.map((band, index) => ({
    ...band,
    center: 68 + slot * (index + 0.5),
    width,
    height: Number(band.count) / chartMaximum.value * 180,
  }))
})
const distributionDescription = computed(() => scoreBands.value.map(band => `${band.label}${band.count}份，占${(band.percentage * 100).toFixed(0)}%`).join('；'))
const scoreDecimals = value => Math.min(2, String(value ?? 0).split('.')[1]?.length || 0)

const worstPoint = computed(() => {
  const points = insights.value?.pointInsights || []
  return [...points].sort((a, b) => b.missRate - a.missRate)[0] || null
})

async function loadInsights() {
  if (!session.selectedBatchId) {
    notify('请先在答卷批次中选择批次', true)
    return
  }
  loading.value = true
  try {
    insights.value = await api(`/api/reports/batches/${session.selectedBatchId}/insights`)
  } catch (error) {
    notify(error.message, true)
  } finally {
    loading.value = false
  }
}

function inspectMetric(label, value) {
  notify(`${label}：${value}`)
}

function inspectPoint(item) {
  notify(`${item.pointName}漏点率 ${(item.missRate * 100).toFixed(0)}%，共 ${item.missCount} 人次`)
}

async function createExport() {
  if (!session.selectedBatchId) {
    notify('请先选择批次', true)
    return
  }
  exporting.value = true
  try {
    const created = await guarded(
      () => api('/api/reports/exports', {
        method: 'POST',
        body: { batchId: session.selectedBatchId, format: format.value },
      }),
      '导出文件已生成',
    )
    exportInfo.value = await api(`/api/reports/exports/${created.exportId}`)
  } finally {
    exporting.value = false
  }
}

async function download() {
  if (!exportInfo.value?.downloadUrl) return
  downloading.value = true
  try {
    const response = await fetch(exportInfo.value.downloadUrl, {
      headers: { Authorization: `Bearer ${session.token}` },
    })
    if (!response.ok) {
      notify('下载失败', true)
      return
    }
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `smartgrade-${session.selectedBatchId}.${format.value.toLowerCase()}`
    link.click()
    URL.revokeObjectURL(link.href)
    notify('成绩文件已开始下载')
  } finally {
    downloading.value = false
  }
}

onMounted(loadInsights)
</script>

<template>
  <div class="reports-page dashboard-page" :class="{ 'is-loading': loading }" :aria-busy="loading">
    <section class="dashboard-heading">
      <div>
        <p class="eyebrow">LIVE INSIGHT DASHBOARD</p>
        <h2>批次统计报告</h2>
        <p>所有统计均来自已确认的真实阅卷结果，刷新后即时重算。</p>
      </div>
      <button class="refresh-control" :disabled="loading" @click="loadInsights">
        <span class="refresh-icon" aria-hidden="true">↻</span>
        {{ loading ? '同步中' : '刷新实时数据' }}
      </button>
    </section>

    <section class="dashboard-metrics">
      <button
        class="metric-card score-overview"
        @click="inspectMetric('平均得分', `${insights?.averageScore ?? 0} / ${insights?.maxScore ?? '-'}`)"
      >
        <div class="score-gauge" :style="gaugeStyle">
          <div class="gauge-core">
            <strong>
              <AnimatedNumber v-if="insights?.averageScore != null" :value="insights.averageScore" :decimals="1" />
              <template v-else>—</template>
            </strong>
            <span>/ {{ insights?.maxScore || '—' }}</span>
          </div>
        </div>
        <div class="metric-copy">
          <p class="eyebrow light">AVERAGE SCORE</p>
          <h3>平均得分</h3>
          <span>占满分 {{ averagePercent.toFixed(0) }}%</span>
        </div>
      </button>

      <button class="metric-card compact-metric" @click="inspectMetric('已定分答卷', `${insights?.reviewedCount ?? 0} 份`) ">
        <span class="metric-symbol" aria-hidden="true">✓</span>
        <p class="eyebrow">REVIEWED</p>
        <strong><AnimatedNumber :value="insights?.reviewedCount ?? 0" /></strong>
        <span>份答卷完成教师确认</span>
      </button>

      <button
        class="metric-card compact-metric range-metric"
        @click="inspectMetric('得分区间', `${insights?.lowestScore ?? 0} – ${insights?.highestScore ?? 0}`)"
      >
        <span class="metric-symbol" aria-hidden="true">↗</span>
        <p class="eyebrow">SCORE RANGE</p>
        <strong><AnimatedNumber :value="insights?.lowestScore ?? 0" :decimals="scoreDecimals(insights?.lowestScore)" />–<AnimatedNumber :value="insights?.highestScore ?? 0" :decimals="scoreDecimals(insights?.highestScore)" /></strong>
        <span>本批次最低分 / 最高分</span>
      </button>

      <button class="metric-card batch-metric" @click="inspectMetric('当前批次', session.selectedBatchId || '未选择')">
        <span class="metric-symbol" aria-hidden="true">◎</span>
        <p class="eyebrow">CURRENT BATCH</p>
        <strong class="batch-code">{{ session.selectedBatchId || '未选择' }}</strong>
        <span>点击查看当前统计对象</span>
      </button>
    </section>

    <section class="analytics-grid">
      <article class="panel score-distribution-panel">
        <div class="panel-head">
          <div><p class="eyebrow">SCORE DISTRIBUTION</p><h2>得分分布</h2></div>
          <span class="data-badge"><i></i> 实时结果</span>
        </div>
        <figure v-if="scoreBands.length" class="distribution-chart">
          <svg viewBox="0 0 600 310" role="img" aria-labelledby="distribution-title distribution-description">
            <title id="distribution-title">已定分答卷得分分布柱状图</title>
            <desc id="distribution-description">{{ distributionDescription }}</desc>
            <text x="22" y="30" class="chart-unit">答卷 / 份</text>
            <g v-for="tick in chartTicks" :key="tick.value" class="chart-grid">
              <line x1="68" x2="584" :y1="tick.y" :y2="tick.y" />
              <text x="52" :y="tick.y + 5" text-anchor="end">{{ tick.value }}</text>
            </g>
            <g v-for="band in chartBars" :key="band.key" class="chart-column" :class="`band-${band.key}`">
              <title>{{ band.label }}：{{ band.count }} 份，占 {{ (band.percentage * 100).toFixed(0) }}%</title>
              <rect :x="band.center - band.width / 2" :y="246 - band.height" :width="band.width" :height="band.height" rx="7" />
              <text :x="band.center" :y="234 - band.height" text-anchor="middle" class="chart-value">{{ band.count }}</text>
              <text :x="band.center" y="272" text-anchor="middle" class="chart-label">{{ band.label }}</text>
              <text :x="band.center" y="298" text-anchor="middle" class="chart-percentage">{{ (band.percentage * 100).toFixed(0) }}%</text>
            </g>
          </svg>
          <figcaption>按得分等级统计 · 已定分 {{ insights?.reviewedCount || 0 }} 份</figcaption>
        </figure>
        <p v-if="!insights?.reviewedCount" class="empty-state">教师确认最终分后，这里会生成真实得分分布。</p>
      </article>

      <article class="panel miss-panel">
        <div class="panel-head">
          <div><p class="eyebrow">POINT INSIGHTS</p><h2>要点漏点雷达</h2></div>
          <span v-if="worstPoint" class="risk-chip">最高 {{ (worstPoint.missRate * 100).toFixed(0) }}%</span>
        </div>
        <div class="insight-list radar-list">
          <button
            v-for="(item, index) in insights?.pointInsights"
            :key="item.pointId"
            class="insight-row"
            @click="inspectPoint(item)"
          >
            <span class="point-index">0{{ index + 1 }}</span>
            <div class="point-copy">
              <b>{{ item.pointName }}</b>
              <small>未命中 {{ item.missCount }} 人次</small>
            </div>
            <div class="progress-track"><i :style="{ width: `${Math.min(100, item.missRate * 100)}%` }"></i></div>
            <strong>{{ (item.missRate * 100).toFixed(0) }}%</strong>
          </button>
          <p v-if="!insights?.pointInsights?.length" class="empty-state">完成定分后显示真实要点遗漏情况。</p>
        </div>
      </article>
    </section>

    <section class="panel export-console">
      <div>
        <p class="eyebrow">AUDITED EXPORT</p>
        <h2>审计式成绩导出</h2>
        <p class="muted">包含学生编号、最终分和满分，每次生成都会写入真实下载审计记录。</p>
      </div>
      <div class="export-actions">
        <div class="format-choice" aria-label="导出格式">
          <button :class="{ active: format === 'XLSX' }" @click="format = 'XLSX'; notify('已选择 Excel XLSX 格式')">XLSX</button>
          <button :class="{ active: format === 'CSV' }" @click="format = 'CSV'; notify('已选择 CSV 格式')">CSV</button>
        </div>
        <button class="primary" :disabled="exporting" @click="createExport">
          {{ exporting ? '正在生成…' : '生成导出文件' }}
        </button>
        <button
          v-if="exportInfo?.downloadUrl"
          class="secondary"
          :disabled="downloading"
          @click="download"
        >
          {{ downloading ? '准备下载…' : '下载成绩文件' }}
        </button>
      </div>
      <div v-if="exportInfo" class="export-audit">
        <span class="state-tag success">{{ exportInfo.status }}</span>
        <small>审计编号 {{ exportInfo.auditId }}</small>
      </div>
    </section>
  </div>
</template>
