<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('header.dataAnalysis') }}</h2>
              <span class="page-desc">{{ $t('aiDataAnalysis.dataAnalysis.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadStatistics">
                  {{ $t('aiDataAnalysis.dataPreview.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <div class="stat-cards">
              <div class="stat-card">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataAnalysis.statDevice') }}</div>
                <div class="stat-value">{{ stats.deviceCount }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataAnalysis.statRecords') }}</div>
                <div class="stat-value">{{ stats.recordCount }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataAnalysis.statAvg') }}</div>
                <div class="stat-value">{{ formatNum(stats.avgValue) }}</div>
              </div>
              <div class="stat-card stat-danger">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataAnalysis.statAbnormal') }}</div>
                <div class="stat-value">{{ stats.abnormal }}</div>
              </div>
            </div>

            <h3 class="section-title">{{ $t('aiDataAnalysis.dataAnalysis.metricDist') }}</h3>
            <el-table :data="distribution" size="small" class="ai-table" border v-loading="loading">
              <el-table-column prop="metric" :label="$t('aiDataAnalysis.dataAnalysis.colMetric')" min-width="140" />
              <el-table-column prop="valueRange" label="取值范围" min-width="240" show-overflow-tooltip />
              <el-table-column prop="count" :label="$t('aiDataAnalysis.dataAnalysis.colCount')" min-width="100" />
              <el-table-column prop="avg" :label="$t('aiDataAnalysis.dataAnalysis.colAvg')" min-width="100" />
              <el-table-column prop="max" :label="$t('aiDataAnalysis.dataAnalysis.colMax')" min-width="100" />
              <el-table-column prop="min" :label="$t('aiDataAnalysis.dataAnalysis.colMin')" min-width="100" />
            </el-table>

            <div class="empty-hint" v-if="!loading && distribution.length === 0">
              {{ $t('aiDataAnalysis.dataPreview.emptyHint') }}
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HeaderBar from '@/components/HeaderBar.vue'
import CustomButton from '@/components/CustomButton.vue'
import dataAnalysis from '@/apis/module/dataAnalysis.js'
import { REPORT_METRICS } from '@/constants/reportMetrics.js'

// 按中文名称索引字段目录，用于展示「取值范围」
const RANGE_BY_NAME = REPORT_METRICS.reduce((map, m) => {
  map[m.label] = m.range
  return map
}, {})

export default {
  name: 'DataAnalysis',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      loading: false,
      stats: { deviceCount: 0, recordCount: 0, avgValue: null, abnormal: 0 },
      distribution: [],
    }
  },
  created() {
    this.loadStatistics()
  },
  methods: {
    formatNum(v) {
      if (v === null || v === undefined || v === '') return '—'
      const n = typeof v === 'number' ? v : parseFloat(v)
      return isNaN(n) ? '—' : n.toFixed(1)
    },
    loadStatistics() {
      this.loading = true
      dataAnalysis.statistics((res) => {
        this.loading = false
        const body = res && res.data
        if (body && body.code === 0 && body.data) {
          const d = body.data
          this.stats = {
            deviceCount: d.deviceCount || 0,
            recordCount: d.recordCount || 0,
            avgValue: d.avgValue,
            abnormal: d.abnormal || 0,
          }
          this.distribution = (d.distribution || []).map((row) =>
            Object.assign({}, row, { valueRange: RANGE_BY_NAME[row.metric] || '' })
          )
        } else {
          this.$message.error((body && body.msg) || this.$t('aiDataAnalysis.dataAnalysis.loadError'))
        }
      })
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
  background: #eff4ff;
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

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #eef3ff, #f7f9ff);
  border: 1px solid #e3ebff;
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

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin: 4px 0 12px;
  color: #2c3354;
}

.ai-table {
  width: 100%;
}

.empty-hint {
  margin: 24px 0;
  text-align: center;
  color: #8a93b8;
  font-size: 13px;
}
</style>
