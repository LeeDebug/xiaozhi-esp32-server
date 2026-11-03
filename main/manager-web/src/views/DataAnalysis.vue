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
                <div class="stat-value">{{ stats.avgValue }}</div>
              </div>
              <div class="stat-card stat-danger">
                <div class="stat-label">{{ $t('aiDataAnalysis.dataAnalysis.statAbnormal') }}</div>
                <div class="stat-value">{{ stats.abnormal }}</div>
              </div>
            </div>

            <h3 class="section-title">{{ $t('aiDataAnalysis.dataAnalysis.metricDist') }}</h3>
            <el-table :data="distribution" size="small" class="ai-table" border>
              <el-table-column prop="metric" :label="$t('aiDataAnalysis.dataAnalysis.colMetric')" min-width="140" />
              <el-table-column prop="count" :label="$t('aiDataAnalysis.dataAnalysis.colCount')" min-width="100" />
              <el-table-column prop="avg" :label="$t('aiDataAnalysis.dataAnalysis.colAvg')" min-width="100" />
              <el-table-column prop="max" :label="$t('aiDataAnalysis.dataAnalysis.colMax')" min-width="100" />
              <el-table-column prop="min" :label="$t('aiDataAnalysis.dataAnalysis.colMin')" min-width="100" />
            </el-table>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HeaderBar from '@/components/HeaderBar.vue'

export default {
  name: 'DataAnalysis',
  components: { HeaderBar },
  data() {
    return {
      stats: {
        deviceCount: 4,
        recordCount: 12860,
        avgValue: 47.3,
        abnormal: 152,
      },
      distribution: [
        { metric: '温度', count: 4210, avg: '46.8', max: '89.2', min: '12.1' },
        { metric: '湿度', count: 3980, avg: '51.2', max: '95.0', min: '20.4' },
        { metric: '音量', count: 2670, avg: '42.0', max: '78.5', min: '5.0' },
        { metric: '在线时长', count: 2000, avg: '49.1', max: '99.9', min: '1.2' },
      ],
    }
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
</style>
