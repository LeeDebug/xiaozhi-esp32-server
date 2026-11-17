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
                <CustomButton icon="el-icon-magic-stick" size="medium" :loading="seeding" @click="handleSeed">
                  {{ $t('aiDataAnalysis.dataPreview.generateDemo') }}
                </CustomButton>
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadData">
                  {{ $t('aiDataAnalysis.dataPreview.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <div class="filter-bar">
              <div class="filter-item">
                <span class="filter-label">{{ $t('aiDataAnalysis.dataPreview.device') }}</span>
                <el-select
                  v-model="deviceFilter"
                  filterable
                  clearable
                  class="filter-select"
                  :placeholder="$t('aiDataAnalysis.dataPreview.allDevices')"
                  @change="handleFilterChange"
                >
                  <el-option v-for="d in deviceOptions" :key="d.deviceId" :label="d.deviceId" :value="d.deviceId" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('aiDataAnalysis.dataPreview.metric') }}</span>
                <el-select
                  v-model="metricFilter"
                  filterable
                  clearable
                  class="filter-select"
                  :placeholder="$t('aiDataAnalysis.dataPreview.allMetrics')"
                  @change="handleFilterChange"
                >
                  <el-option v-for="m in metricOptions" :key="m.value" :label="m.label" :value="m.value" />
                </el-select>
              </div>
            </div>

            <div class="table-container">
              <el-table :data="pagedData" size="small" class="ai-table" border height="100%" v-loading="loading">
                <el-table-column prop="deviceId" :label="$t('aiDataAnalysis.dataPreview.deviceId')" min-width="160" />
                <el-table-column prop="time" :label="$t('aiDataAnalysis.dataPreview.time')" min-width="170" />
                <el-table-column prop="metric" :label="$t('aiDataAnalysis.dataPreview.metric')" min-width="120" />
                <el-table-column prop="metricKey" label="metric_key" min-width="220" show-overflow-tooltip />
                <el-table-column prop="valueRange" label="取值范围" min-width="240" show-overflow-tooltip />
                <el-table-column prop="value" :label="$t('aiDataAnalysis.dataPreview.value')" min-width="100" />
                <el-table-column prop="status" :label="$t('aiDataAnalysis.dataPreview.status')" min-width="100">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.status === 'normal' ? 'success' : 'danger'" size="mini">
                      {{ scope.row.status === 'normal' ? $t('aiDataAnalysis.dataPreview.statusNormal') : $t('aiDataAnalysis.dataPreview.statusAbnormal') }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="empty-hint" v-if="!loading && pagedData.length === 0">
              {{ $t('aiDataAnalysis.dataPreview.emptyHint') }}
            </div>

            <div class="pager">
              <span class="total-hint">{{ $t('aiDataAnalysis.dataPreview.total', { total: total }) }}</span>
              <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="pageSize"
                :current-page.sync="currentPage"
                @current-change="handlePageChange"
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
import dataAnalysis from '@/apis/module/dataAnalysis.js'
import { REPORT_METRICS, getMetric, metricSeqText } from '@/constants/reportMetrics.js'

// 后端字段目录接口不可用时的兜底：直接使用 Excel 全量字段目录
const FALLBACK_METRICS = REPORT_METRICS.map((m) => ({
  value: m.key,
  label: `#${metricSeqText(m)} ${m.label}`,
}))

export default {
  name: 'DataPreview',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      loading: false,
      seeding: false,
      deviceFilter: '',
      deviceOptions: [],
      metricFilter: '',
      metricOptions: FALLBACK_METRICS,
      pagedData: [],
      pageSize: 12,
      currentPage: 1,
      total: 0,
    }
  },
  created() {
    this.loadDevices()
    this.loadMetrics()
    this.loadData()
  },
  methods: {
    loadDevices() {
      dataAnalysis.devices((res) => {
        const body = res && res.data
        if (body && body.code === 0 && Array.isArray(body.data)) {
          this.deviceOptions = body.data
            .map((d) => ({ deviceId: d.deviceId, cnt: d.cnt }))
            .filter((d) => d.deviceId)
        }
      })
    },
    handleFilterChange() {
      this.currentPage = 1
      this.loadData()
    },
    loadMetrics() {
      dataAnalysis.metrics((res) => {
        const body = res && res.data
        if (body && body.code === 0 && body.data && body.data.length) {
          this.metricOptions = body.data.map((m) => ({
            value: m.key,
            label: m.seq ? `#${metricSeqText({ seq: m.seq, bit: m.bit })} ${m.name}` : m.name,
            valueRange: m.valueRange,
          }))
        }
      })
    },
    loadData() {
      this.loading = true
      dataAnalysis.preview(
        { deviceId: this.deviceFilter, metric: this.metricFilter, page: this.currentPage, limit: this.pageSize },
        (res) => {
          this.loading = false
          const body = res && res.data
          if (body && body.code === 0 && body.data) {
            this.pagedData = (body.data.list || []).map((row) => {
              const meta = getMetric(row.metricKey)
              return Object.assign({}, row, {
                valueRange: row.valueRange || (meta ? meta.range : ''),
              })
            })
            this.total = body.data.total || 0
          } else {
            this.$message.error((body && body.msg) || this.$t('aiDataAnalysis.dataPreview.loadError'))
          }
        }
      )
    },
    handlePageChange(p) {
      this.currentPage = p
      this.loadData()
    },
    handleSeed() {
      this.seeding = true
      dataAnalysis.seed((res) => {
        this.seeding = false
        const body = res && res.data
        if (body && body.code === 0) {
          if (body.data === -1) {
            this.$message.info(this.$t('aiDataAnalysis.dataPreview.demoExists'))
          } else if (body.data === -2) {
            this.$message.warning(this.$t('aiDataAnalysis.dataPreview.demoNoDevice'))
          } else {
            this.$message.success(this.$t('aiDataAnalysis.dataPreview.demoSuccess'))
          }
          this.currentPage = 1
          this.deviceFilter = ''
          this.metricFilter = ''
          this.loadDevices()
          this.loadMetrics()
          this.loadData()
        } else {
          this.$message.error((body && body.msg) || this.$t('aiDataAnalysis.dataPreview.loadError'))
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
  flex: 1;
  min-height: 0;
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
  min-height: 0;
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
  min-height: 0;
  border: none;
  box-shadow: none;
  overflow: hidden;
}

.ai-card ::v-deep .el-card__body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.filter-select {
  width: 200px;
}

.table-container {
  flex: 1;
  min-height: 0;
}

.ai-table {
  width: 100%;

  ::v-deep .el-table__body-wrapper {
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 6px;
    }
    &::-webkit-scrollbar-thumb {
      background: var(--sys-scrollbar-thumb);
      border-radius: 3px;
    }
    &::-webkit-scrollbar-track {
      background: var(--sys-scrollbar-track);
      border-radius: 3px;
    }
  }
}

.empty-hint {
  margin: 24px 0;
  text-align: center;
  color: #8a93b8;
  font-size: 13px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.total-hint {
  font-size: 13px;
  color: #8a93b8;
}
</style>
