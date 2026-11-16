<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('header.dataPreviewDashboard') }}</h2>
              <span class="page-desc">{{ $t('aiDataAnalysis.dataPreviewDashboard.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadDashboard">
                  {{ $t('aiDataAnalysis.dataPreview.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <div class="range-line" v-if="summary.firstReportTime">
              <i class="el-icon-time"></i>
              {{ $t('aiDataAnalysis.dataPreviewDashboard.range') }}：{{ summary.firstReportTime }} ~ {{ summary.lastReportTime }}
            </div>

            <div class="stat-cards">
              <div class="stat-card">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataPreviewDashboard.statDevices') }}</div>
                <div class="stat-value">{{ summary.deviceCount }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataPreviewDashboard.statRecords') }}</div>
                <div class="stat-value">{{ summary.recordCount }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataPreviewDashboard.statDays') }}</div>
                <div class="stat-value">{{ summary.activeDays }}</div>
              </div>
              <div class="stat-card stat-danger">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataPreviewDashboard.statAbnormal') }}</div>
                <div class="stat-value">{{ summary.abnormalCount }}</div>
              </div>
            </div>

            <template v-if="hasData">
              <div class="chart-grid">
                <div class="chart-card chart-wide">
                  <h3 class="section-title">{{ $t('aiDataAnalysis.dataPreviewDashboard.dailyTitle') }}</h3>
                  <div ref="dailyChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('aiDataAnalysis.dataPreviewDashboard.deviceRankTitle') }}</h3>
                  <div ref="deviceChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('aiDataAnalysis.dataPreviewDashboard.statusTitle') }}</h3>
                  <div ref="statusChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('aiDataAnalysis.dataPreviewDashboard.metricDistTitle') }}</h3>
                  <div ref="metricChart" class="chart-box chart-box-tall"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('aiDataAnalysis.dataPreviewDashboard.heatmapTitle') }}</h3>
                  <div ref="heatmapChart" class="chart-box chart-box-tall"></div>
                </div>
              </div>
            </template>

            <div class="empty-hint" v-else-if="!loading">
              {{ $t('aiDataAnalysis.dataPreview.emptyHint') }}
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import HeaderBar from '@/components/HeaderBar.vue'
import CustomButton from '@/components/CustomButton.vue'
import dataAnalysis from '@/apis/module/dataAnalysis.js'

const EMPTY_SUMMARY = {
  deviceCount: 0,
  recordCount: 0,
  activeDays: 0,
  abnormalCount: 0,
  firstReportTime: '',
  lastReportTime: '',
}

export default {
  name: 'DataPreviewDashboard',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      loading: false,
      summary: Object.assign({}, EMPTY_SUMMARY),
      dailyUploads: [],
      deviceUploads: [],
      metricDistribution: [],
      hourlyDistribution: [],
      charts: [],
      resizeHandler: null,
    }
  },
  computed: {
    hasData() {
      return this.dailyUploads.length > 0 || this.deviceUploads.length > 0
    },
  },
  created() {
    this.loadDashboard()
  },
  mounted() {
    this.resizeHandler = () => this.charts.forEach((c) => c.resize())
    window.addEventListener('resize', this.resizeHandler)
  },
  beforeDestroy() {
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler)
    }
    this.charts.forEach((c) => c.dispose())
    this.charts = []
  },
  methods: {
    // 从运行时主题令牌取色，保证切换系统色后图表同步
    themeColors() {
      const read = (name, fallback) => {
        const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
        return v || fallback
      }
      return {
        primary: read('--sys-primary', '#eb6800'),
        light: read('--sys-primary-light', '#f3a161'),
        lighter: read('--sys-primary-lighter', '#f7c69e'),
        text: '#2c3354',
        muted: '#8a93b8',
        line: '#f3f1f1',
        danger: '#e8554e',
        palette: [
          read('--sys-primary', '#eb6800'),
          read('--sys-primary-light', '#f3a161'),
          read('--sys-primary-lighter', '#f7c69e'),
          '#f7b267', '#f4a261', '#e9c46a', '#90be6d', '#43aa8b', '#577590',
        ],
      }
    },
    initChart(refName) {
      const el = this.$refs[refName]
      if (!el) return null
      let chart = echarts.getInstanceByDom(el)
      if (!chart) {
        chart = echarts.init(el)
        this.charts.push(chart)
      }
      return chart
    },
    baseTooltip() {
      return {
        trigger: 'axis',
        backgroundColor: '#fff',
        borderColor: '#f3f1f1',
        textStyle: { color: '#2c3354', fontSize: 12 },
        extraCssText: 'box-shadow:0 4px 16px rgba(44,51,84,.12);border-radius:8px;',
      }
    },
    loadDashboard() {
      this.loading = true
      dataAnalysis.dashboard((res) => {
        this.loading = false
        const body = res && res.data
        if (body && body.code === 0 && body.data) {
          const d = body.data
          this.summary = Object.assign({}, EMPTY_SUMMARY, d.summary || {})
          this.dailyUploads = d.dailyUploads || []
          this.deviceUploads = d.deviceUploads || []
          this.metricDistribution = d.metricDistribution || []
          this.hourlyDistribution = d.hourlyDistribution || []
          this.$nextTick(() => this.renderCharts())
        } else {
          this.$message.error((body && body.msg) || this.$t('aiDataAnalysis.dataPreviewDashboard.loadError'))
        }
      })
    },
    renderCharts() {
      this.renderDailyChart()
      this.renderDeviceChart()
      this.renderStatusChart()
      this.renderMetricChart()
      this.renderHeatmapChart()
      this.charts.forEach((c) => c.resize())
    },
    renderDailyChart() {
      const chart = this.initChart('dailyChart')
      if (!chart) return
      const c = this.themeColors()
      const dates = this.dailyUploads.map((d) => d.date)
      chart.setOption({
        color: [c.primary, c.light],
        tooltip: Object.assign(this.baseTooltip(), { axisPointer: { type: 'shadow' } }),
        legend: {
          data: [
            this.$t('aiDataAnalysis.dataPreviewDashboard.recordsPerDay'),
            this.$t('aiDataAnalysis.dataPreviewDashboard.devicesPerDay'),
          ],
          textStyle: { color: c.muted },
          right: 0,
          top: 0,
        },
        grid: { left: 12, right: 12, bottom: 8, top: 40, containLabel: true },
        xAxis: {
          type: 'category',
          data: dates,
          axisLine: { lineStyle: { color: c.line } },
          axisLabel: { color: c.muted, fontSize: 11, rotate: dates.length > 15 ? 30 : 0 },
        },
        yAxis: [
          {
            type: 'value',
            name: this.$t('aiDataAnalysis.dataPreviewDashboard.uploads'),
            nameTextStyle: { color: c.muted, fontSize: 11 },
            axisLabel: { color: c.muted },
            splitLine: { lineStyle: { color: c.line, type: 'dashed' } },
          },
          {
            type: 'value',
            name: this.$t('aiDataAnalysis.dataPreviewDashboard.statDevices'),
            nameTextStyle: { color: c.muted, fontSize: 11 },
            axisLabel: { color: c.muted },
            splitLine: { show: false },
          },
        ],
        series: [
          {
            name: this.$t('aiDataAnalysis.dataPreviewDashboard.recordsPerDay'),
            type: 'bar',
            barMaxWidth: 36,
            itemStyle: { borderRadius: [4, 4, 0, 0] },
            data: this.dailyUploads.map((d) => d.count),
          },
          {
            name: this.$t('aiDataAnalysis.dataPreviewDashboard.devicesPerDay'),
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbolSize: 6,
            data: this.dailyUploads.map((d) => d.deviceCount),
          },
        ],
      }, true)
    },
    renderDeviceChart() {
      const chart = this.initChart('deviceChart')
      if (!chart) return
      const c = this.themeColors()
      const rows = this.deviceUploads.slice(0, 10).slice().reverse()
      chart.setOption({
        tooltip: Object.assign(this.baseTooltip(), {
          formatter: (params) => {
            const p = params[0]
            const full = p.name
            const short = full.length > 24 ? full.slice(0, 24) + '…' : full
            return `${short}<br/>${this.$t('aiDataAnalysis.dataPreviewDashboard.uploads')}: <b>${p.value}</b>`
          },
        }),
        grid: { left: 12, right: 24, bottom: 8, top: 12, containLabel: true },
        xAxis: {
          type: 'value',
          axisLabel: { color: c.muted },
          splitLine: { lineStyle: { color: c.line, type: 'dashed' } },
        },
        yAxis: {
          type: 'category',
          data: rows.map((d) => d.deviceId),
          axisLine: { lineStyle: { color: c.line } },
          axisLabel: {
            color: c.muted,
            fontSize: 11,
            formatter: (v) => (v.length > 20 ? v.slice(0, 20) + '…' : v),
          },
        },
        series: [
          {
            type: 'bar',
            data: rows.map((d) => d.count),
            barMaxWidth: 18,
            itemStyle: {
              borderRadius: [0, 4, 4, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: c.lighter },
                { offset: 1, color: c.primary },
              ]),
            },
            label: { show: true, position: 'right', color: c.muted, fontSize: 11 },
          },
        ],
      }, true)
    },
    renderStatusChart() {
      const chart = this.initChart('statusChart')
      if (!chart) return
      const c = this.themeColors()
      const abnormal = this.summary.abnormalCount || 0
      const normal = Math.max(0, (this.summary.recordCount || 0) - abnormal)
      chart.setOption({
        tooltip: {
          trigger: 'item',
          backgroundColor: '#fff',
          borderColor: c.line,
          textStyle: { color: c.text, fontSize: 12 },
          formatter: '{b}: {c} ({d}%)',
        },
        legend: { bottom: 0, textStyle: { color: c.muted } },
        color: [c.primary, c.danger],
        series: [
          {
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '44%'],
            avoidLabelOverlap: true,
            itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
            label: { show: true, formatter: '{d}%', color: c.text, fontSize: 12 },
            data: [
              { name: this.$t('aiDataAnalysis.dataPreviewDashboard.normal'), value: normal },
              { name: this.$t('aiDataAnalysis.dataPreviewDashboard.abnormal'), value: abnormal },
            ],
          },
        ],
      }, true)
    },
    renderMetricChart() {
      const chart = this.initChart('metricChart')
      if (!chart) return
      const c = this.themeColors()
      const all = this.metricDistribution.slice().sort((a, b) => b.count - a.count)
      const top = all.slice(0, 8)
      const rest = all.slice(8).reduce((sum, m) => sum + (m.count || 0), 0)
      if (rest > 0) {
        top.push({ metric: this.$t('aiDataAnalysis.dataPreviewDashboard.others'), count: rest })
      }
      chart.setOption({
        tooltip: {
          trigger: 'item',
          backgroundColor: '#fff',
          borderColor: c.line,
          textStyle: { color: c.text, fontSize: 12 },
          formatter: (p) => `${p.name}<br/>${this.$t('aiDataAnalysis.dataPreviewDashboard.uploads')}: <b>${p.value}</b> (${p.percent}%)`,
        },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 0,
          top: 'middle',
          textStyle: { color: c.muted, fontSize: 11 },
        },
        color: c.palette,
        series: [
          {
            type: 'pie',
            radius: ['35%', '62%'],
            center: ['34%', '50%'],
            itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
            label: { show: false },
            data: top.map((m) => ({ name: m.metric, value: m.count })),
          },
        ],
      }, true)
    },
    renderHeatmapChart() {
      const chart = this.initChart('heatmapChart')
      if (!chart) return
      const c = this.themeColors()
      const weekKeys = [
        'aiDataAnalysis.dataPreviewDashboard.weekMon',
        'aiDataAnalysis.dataPreviewDashboard.weekTue',
        'aiDataAnalysis.dataPreviewDashboard.weekWed',
        'aiDataAnalysis.dataPreviewDashboard.weekThu',
        'aiDataAnalysis.dataPreviewDashboard.weekFri',
        'aiDataAnalysis.dataPreviewDashboard.weekSat',
        'aiDataAnalysis.dataPreviewDashboard.weekSun',
      ]
      const weekLabels = weekKeys.map((k) => this.$t(k))
      // MySQL DAYOFWEEK: 1=周日 ... 7=周六 → ECharts y 轴周一在上
      const toRow = (dow) => (dow === 1 ? 6 : dow - 2)
      const points = this.hourlyDistribution
        .filter((h) => h.hour >= 0 && h.hour <= 23 && h.dayOfWeek >= 1 && h.dayOfWeek <= 7)
        .map((h) => [h.hour, toRow(h.dayOfWeek), h.count])
      const max = points.reduce((m, p) => Math.max(m, p[2]), 0)
      chart.setOption({
        tooltip: {
          position: 'top',
          backgroundColor: '#fff',
          borderColor: c.line,
          textStyle: { color: c.text, fontSize: 12 },
          formatter: (p) => `${weekLabels[p.value[1]]} ${p.value[0]}:00<br/>${this.$t('aiDataAnalysis.dataPreviewDashboard.uploads')}: <b>${p.value[2]}</b>`,
        },
        grid: { left: 12, right: 16, bottom: 24, top: 12, containLabel: true },
        xAxis: {
          type: 'category',
          data: Array.from({ length: 24 }, (_, i) => i),
          axisLabel: { color: c.muted, fontSize: 10, interval: 1 },
          axisLine: { lineStyle: { color: c.line } },
        },
        yAxis: {
          type: 'category',
          data: weekLabels,
          axisLabel: { color: c.muted, fontSize: 11 },
          axisLine: { lineStyle: { color: c.line } },
        },
        visualMap: {
          min: 0,
          max: Math.max(1, max),
          calculable: false,
          orient: 'horizontal',
          left: 'center',
          bottom: 0,
          itemWidth: 12,
          itemHeight: 90,
          textStyle: { color: c.muted, fontSize: 10 },
          inRange: {
            color: ['#fef7f2', c.lighter, c.light, c.primary],
          },
        },
        series: [
          {
            type: 'heatmap',
            data: points,
            emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,.2)' } },
            itemStyle: { borderColor: '#fff', borderWidth: 1 },
          },
        ],
      }, true)
    },
  },
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.welcome {
  min-width: 900px;
  min-height: 506px;
  height: 100vh;
  display: flex;
  position: relative;
  flex-direction: column;
  background: var(--sys-surface);
  overflow: hidden;
}

.main-wrapper {
  height: calc(100vh - 63px - 35px);
  margin: 20px 22px 0;
  border-radius: 15px;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.content-panel {
  flex: 1;
  display: flex;
  overflow: hidden;
  height: 100%;
  border-radius: 15px;
  background: transparent;
  border: 1px solid #fff;
}

.content-area {
  flex: 1;
  height: 100%;
  min-width: 600px;
  overflow: auto;
  background-color: white;
  display: flex;
  flex-direction: column;
}

.ai-card {
  background: white;
  flex: 1;
  display: flex;
  flex-direction: column;
  border: none;
  box-shadow: none;
  overflow: hidden;
}

.ai-card ::v-deep .el-card__body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  font-weight: 500;
  font-size: 24px;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #8a93b8;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.divider {
  height: 1px;
  background: #f3f1f1;
  margin: 16px 0;
}

.range-line {
  font-size: 12px;
  color: #8a93b8;
  margin: -6px 0 16px;
  i {
    margin-right: 4px;
  }
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, var(--sys-primary-bg-soft), var(--sys-primary-bg-soft));
  border: 1px solid var(--sys-primary-bg-soft);
  border-radius: 12px;
  padding: 18px 20px;
}

.stat-card.stat-danger {
  background: linear-gradient(135deg, #fff0f0, #fff6f6);
  border-color: #ffd9d9;
}

.stat-label {
  font-size: 13px;
  color: #8a93b8;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  color: #2c3354;
  line-height: 1.1;
}

.stat-danger .stat-value {
  color: #e8554e;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  background: #fff;
  border: 1px solid #f3f1f1;
  border-radius: 12px;
  padding: 14px 16px 10px;
  min-width: 0;
}

.chart-wide {
  grid-column: 1 / -1;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  margin: 0 0 8px;
  color: #2c3354;
}

.chart-box {
  width: 100%;
  height: 280px;
}

.chart-box-tall {
  height: 320px;
}

.empty-hint {
  margin: 48px 0;
  text-align: center;
  color: #8a93b8;
  font-size: 13px;
}
</style>
