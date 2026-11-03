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
                <el-select v-model="form.metric" :placeholder="$t('aiDataAnalysis.dataPrediction.selectMetric')" style="width: 280px">
                  <el-option v-for="m in metrics" :key="m" :label="m" :value="m" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPrediction.horizon')">
                <el-input-number v-model="form.horizon" :min="1" :max="72" :step="1" />
                <span class="unit-hint">{{ $t('aiDataAnalysis.dataPrediction.hours') }}</span>
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
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HeaderBar from '@/components/HeaderBar.vue'
import CustomButton from '@/components/CustomButton.vue'

export default {
  name: 'DataPrediction',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      predicting: false,
      form: {
        metric: '温度',
        horizon: 12,
      },
      metrics: ['温度', '湿度', '音量', '在线时长'],
      result: [],
    }
  },
  methods: {
    handlePredict() {
      this.predicting = true
      // 此处应调用后端预测服务；当前为前端演示，使用本地随机模拟生成预测区间。
      setTimeout(() => {
        const base = 40 + Math.random() * 20
        const now = Date.now()
        const rows = []
        for (let i = 1; i <= this.form.horizon; i++) {
          const value = base + Math.sin(i / 2) * 8 + Math.random() * 4
          const delta = 3 + Math.random() * 3
          rows.push({
            time: new Date(now + i * 3600 * 1000).toLocaleString('zh-CN', { hour12: false }),
            predicted: value.toFixed(1),
            lower: (value - delta).toFixed(1),
            upper: (value + delta).toFixed(1),
          })
        }
        this.result = rows
        this.predicting = false
        this.$message.success(this.$t('aiDataAnalysis.dataPrediction.predictSuccess'))
      }, 700)
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
</style>
