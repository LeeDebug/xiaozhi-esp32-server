<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('header.dataPreview') }}</h2>
              <span class="page-desc">{{ $t('aiDataAnalysis.dataPreview.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadData">
                  {{ $t('aiDataAnalysis.dataPreview.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <div class="filter-bar">
              <el-input
                v-model="keyword"
                :placeholder="$t('aiDataAnalysis.dataPreview.searchPlaceholder')"
                clearable
                class="filter-input"
                @clear="loadData"
                @keyup.enter.native="loadData"
              />
              <el-select v-model="metricFilter" :placeholder="$t('aiDataAnalysis.dataPreview.metric')" clearable class="filter-select" @change="loadData">
                <el-option v-for="m in metrics" :key="m" :label="m" :value="m" />
              </el-select>
            </div>

            <el-table :data="pagedData" size="small" class="ai-table" border v-loading="loading">
              <el-table-column prop="deviceId" :label="$t('aiDataAnalysis.dataPreview.deviceId')" min-width="160" />
              <el-table-column prop="time" :label="$t('aiDataAnalysis.dataPreview.time')" min-width="170" />
              <el-table-column prop="metric" :label="$t('aiDataAnalysis.dataPreview.metric')" min-width="120" />
              <el-table-column prop="value" :label="$t('aiDataAnalysis.dataPreview.value')" min-width="100" />
              <el-table-column prop="status" :label="$t('aiDataAnalysis.dataPreview.status')" min-width="100">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status === 'normal' ? 'success' : 'danger'" size="mini">
                    {{ scope.row.status === 'normal' ? $t('aiDataAnalysis.dataPreview.statusNormal') : $t('aiDataAnalysis.dataPreview.statusAbnormal') }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager">
              <span class="total-hint">{{ $t('aiDataAnalysis.dataPreview.total', { total: filtered.length }) }}</span>
              <el-pagination
                background
                layout="prev, pager, next"
                :total="filtered.length"
                :page-size="pageSize"
                :current-page.sync="currentPage"
              />
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

const METRICS = ['温度', '湿度', '音量', '在线时长']
const DEVICES = ['客厅音箱-001', '卧室音箱-002', '车间网关-003', '前台音箱-004']

function buildMock(total) {
  const rows = []
  const now = Date.now()
  for (let i = 0; i < total; i++) {
    const abnormal = Math.random() < 0.12
    rows.push({
      deviceId: DEVICES[i % DEVICES.length],
      time: new Date(now - i * 60000).toLocaleString('zh-CN', { hour12: false }),
      metric: METRICS[i % METRICS.length],
      value: abnormal ? (Math.random() * 100).toFixed(1) : (20 + Math.random() * 60).toFixed(1),
      status: abnormal ? 'abnormal' : 'normal',
    })
  }
  return rows
}

export default {
  name: 'DataPreview',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      loading: false,
      keyword: '',
      metricFilter: '',
      metrics: METRICS,
      allData: buildMock(86),
      pageSize: 12,
      currentPage: 1,
    }
  },
  computed: {
    filtered() {
      const kw = this.keyword.trim().toLowerCase()
      return this.allData.filter((r) => {
        const matchKw = !kw || r.deviceId.toLowerCase().includes(kw) || r.metric.toLowerCase().includes(kw)
        const matchMetric = !this.metricFilter || r.metric === this.metricFilter
        return matchKw && matchMetric
      })
    },
    pagedData() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filtered.slice(start, start + this.pageSize)
    },
  },
  methods: {
    loadData() {
      this.loading = true
      // 此处应调用后端数据查询接口；当前为前端演示，模拟刷新。
      setTimeout(() => {
        this.loading = false
        this.currentPage = 1
      }, 400)
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
}

.divider {
  height: 1px;
  background: #f3f1f1;
  margin: 16px 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-input {
  width: 280px;
}

.filter-select {
  width: 180px;
}

.ai-table {
  width: 100%;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.total-hint {
  font-size: 13px;
  color: #8a93b8;
}
</style>
