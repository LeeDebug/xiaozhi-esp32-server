<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('header.dataPrediction') }}</h2>
              <span class="page-desc">{{ $t('aiDataAnalysis.dataPrediction.desc') }}</span>
            </div>
            <div class="divider"></div>

            <el-alert
              :title="$t('aiDataAnalysis.tipTitle')"
              :description="$t('aiDataAnalysis.tipDesc')"
              type="info"
              :closable="false"
              show-icon
              class="tip-alert"
            />

            <el-form :model="form" label-width="110px" class="predict-form">
              <el-form-item :label="$t('aiDataAnalysis.dataPrediction.metric')">
                <el-select v-model="form.metric" :placeholder="$t('aiDataAnalysis.dataPrediction.selectMetric')" filterable style="width: 360px" @change="clearResult">
                  <el-option v-for="m in metricOptions" :key="m.value" :label="m.label" :value="m.value" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPrediction.horizon')">
                <el-input-number v-model="form.horizon" :min="1" :max="72" :step="1" />
                <span class="unit-hint">{{ $t('aiDataAnalysis.dataPrediction.hours') }}</span>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPrediction.useLLM')">
                <el-switch v-model="form.useLLM" @change="clearResult" />
              </el-form-item>
              <el-form-item>
                <CustomButton icon="el-icon-magic-stick" type="confirm" :loading="predicting" @click="handlePredict">
                  {{ $t('aiDataAnalysis.dataPrediction.predictBtn') }}
                </CustomButton>
              </el-form-item>
            </el-form>

            <div class="divider"></div>

            <h3 class="section-title">{{ $t('aiDataAnalysis.dataPrediction.resultTitle') }}</h3>
            <el-table :data="result" size="small" class="ai-table" border v-loading="predicting">
              <el-table-column prop="time" :label="$t('aiDataAnalysis.dataPrediction.colTime')" min-width="170" />
              <el-table-column prop="predicted" :label="$t('aiDataAnalysis.dataPrediction.colPredicted')" min-width="120" />
              <el-table-column prop="lower" :label="$t('aiDataAnalysis.dataPrediction.colLower')" min-width="120" />
              <el-table-column prop="upper" :label="$t('aiDataAnalysis.dataPrediction.colUpper')" min-width="120" />
            </el-table>

            <div class="empty-hint" v-if="!predicting && result.length === 0">
              {{ $t('aiDataAnalysis.dataPrediction.emptyHint') }}
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
import { REPORT_METRICS, metricSeqText } from '@/constants/reportMetrics.js'

// 后端字段目录接口不可用时的兜底：直接使用 Excel 全量字段目录
const FALLBACK_METRICS = REPORT_METRICS.map((m) => ({
  value: m.key,
  label: `#${metricSeqText(m)} ${m.label}`,
}))

export default {
  name: 'DataPrediction',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      predicting: false,
      form: {
        metric: '',
        horizon: 12,
        useLLM: false,
      },
      metricOptions: FALLBACK_METRICS,
      result: [],
    }
  },
  created() {
    this.loadMetrics()
  },
  methods: {
    loadMetrics() {
      dataAnalysis.metrics((res) => {
        const body = res && res.data
        if (body && body.code === 0 && body.data && body.data.length) {
          this.metricOptions = body.data.map((m) => ({
            value: m.key,
            label: m.seq ? `#${metricSeqText({ seq: m.seq, bit: m.bit })} ${m.name}` : m.name,
          }))
          if (!this.form.metric) {
            this.form.metric = this.metricOptions[0].value
          }
        }
      })
    },
    clearResult() {
      this.result = []
    },
    handlePredict() {
      if (!this.form.metric) {
        this.$message.warning(this.$t('aiDataAnalysis.dataPrediction.selectMetric'))
        return
      }
      this.predicting = true
      dataAnalysis.predict(
        { metric: this.form.metric, horizon: this.form.horizon, useLLM: this.form.useLLM },
        (res) => {
          this.predicting = false
          const body = res && res.data
          if (body && body.code === 0 && body.data) {
            this.result = body.data
            if (this.result.length === 0) {
              this.$message.info(this.$t('aiDataAnalysis.dataPrediction.noData'))
            } else {
              this.$message.success(this.$t('aiDataAnalysis.dataPrediction.predictSuccess'))
            }
          } else {
            this.$message.error((body && body.msg) || this.$t('aiDataAnalysis.dataPrediction.loadError'))
          }
        }
      )
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

.divider {
  height: 1px;
  background: #f3f1f1;
  margin: 16px 0;
}

.tip-alert {
  margin-bottom: 16px;
}

.predict-form {
  max-width: 560px;
}

.unit-hint {
  margin-left: 10px;
  color: #8a93b8;
  font-size: 13px;
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
