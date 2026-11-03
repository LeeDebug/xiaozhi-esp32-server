<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('header.dataPushConfig') }}</h2>
              <span class="page-desc">{{ $t('aiDataAnalysis.dataPushConfig.desc') }}</span>
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

            <el-form :model="form" label-width="110px" class="push-form">
              <el-form-item :label="$t('aiDataAnalysis.dataPushConfig.targetType')">
                <el-radio-group v-model="form.targetType">
                  <el-radio label="device">{{ $t('aiDataAnalysis.dataPushConfig.targetDevice') }}</el-radio>
                  <el-radio label="group">{{ $t('aiDataAnalysis.dataPushConfig.targetGroup') }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPushConfig.device')" v-if="form.targetType === 'device'">
                <el-select v-model="form.device" :placeholder="$t('aiDataAnalysis.dataPushConfig.selectDevice')" style="width: 280px">
                  <el-option v-for="d in devices" :key="d.id" :label="d.name" :value="d.id" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPushConfig.group')" v-else>
                <el-select v-model="form.group" :placeholder="$t('aiDataAnalysis.dataPushConfig.selectGroup')" style="width: 280px">
                  <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPushConfig.samplingInterval')">
                <el-input-number v-model="form.samplingInterval" :min="1" :max="3600" :step="5" />
                <span class="unit-hint">{{ $t('aiDataAnalysis.dataPushConfig.seconds') }}</span>
              </el-form-item>
              <el-form-item :label="$t('aiDataAnalysis.dataPushConfig.uploadEndpoint')">
                <el-input v-model="form.uploadEndpoint" :placeholder="$t('aiDataAnalysis.dataPushConfig.endpointPlaceholder')" style="width: 420px" />
              </el-form-item>
              <el-form-item>
                <CustomButton icon="el-icon-upload2" type="confirm" :loading="pushing" @click="handlePush">
                  {{ $t('aiDataAnalysis.dataPushConfig.pushBtn') }}
                </CustomButton>
              </el-form-item>
            </el-form>

            <div class="divider"></div>

            <h3 class="section-title">{{ $t('aiDataAnalysis.dataPushConfig.historyTitle') }}</h3>
            <el-table :data="tasks" size="small" class="ai-table" border>
              <el-table-column prop="target" :label="$t('aiDataAnalysis.dataPushConfig.colTarget')" min-width="160" />
              <el-table-column prop="interval" :label="$t('aiDataAnalysis.dataPushConfig.colInterval')" min-width="120" />
              <el-table-column prop="endpoint" :label="$t('aiDataAnalysis.dataPushConfig.colEndpoint')" min-width="220" show-overflow-tooltip />
              <el-table-column prop="status" :label="$t('aiDataAnalysis.dataPushConfig.colStatus')" min-width="100">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status === 'done' ? 'success' : 'warning'" size="mini">
                    {{ scope.row.status === 'done' ? $t('aiDataAnalysis.dataPushConfig.statusDone') : $t('aiDataAnalysis.dataPushConfig.statusPending') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="time" :label="$t('aiDataAnalysis.dataPushConfig.colTime')" min-width="170" />
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
  name: 'DataPushConfig',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      pushing: false,
      form: {
        targetType: 'device',
        device: '',
        group: '',
        samplingInterval: 30,
        uploadEndpoint: '',
      },
      devices: [
        { id: 'dev-001', name: '客厅音箱-001' },
        { id: 'dev-002', name: '卧室音箱-002' },
        { id: 'dev-003', name: '车间网关-003' },
      ],
      groups: [
        { id: 'grp-home', name: '家庭设备组' },
        { id: 'grp-factory', name: '工厂设备组' },
      ],
      tasks: [
        { target: '客厅音箱-001', interval: '30 s', endpoint: 'https://api.example.com/ingest', status: 'done', time: '2026-11-03 19:42' },
        { target: '家庭设备组', interval: '60 s', endpoint: 'https://api.example.com/ingest', status: 'pending', time: '2026-11-03 20:10' },
      ],
    }
  },
  methods: {
    handlePush() {
      if (this.form.targetType === 'device' && !this.form.device) {
        this.$message.warning(this.$t('aiDataAnalysis.dataPushConfig.selectDevice'))
        return
      }
      if (this.form.targetType === 'group' && !this.form.group) {
        this.$message.warning(this.$t('aiDataAnalysis.dataPushConfig.selectGroup'))
        return
      }
      if (!this.form.uploadEndpoint) {
        this.$message.warning(this.$t('aiDataAnalysis.dataPushConfig.endpointPlaceholder'))
        return
      }
      this.pushing = true
      // 此处应调用后端下发配置接口；当前为前端演示，使用本地模拟。
      setTimeout(() => {
        this.pushing = false
        const label = this.form.targetType === 'device'
          ? this.devices.find((d) => d.id === this.form.device)?.name
          : this.groups.find((g) => g.id === this.form.group)?.name
        this.tasks.unshift({
          target: label,
          interval: `${this.form.samplingInterval} s`,
          endpoint: this.form.uploadEndpoint,
          status: 'done',
          time: new Date().toLocaleString('zh-CN', { hour12: false }),
        })
        this.$message.success(this.$t('aiDataAnalysis.dataPushConfig.pushSuccess'))
      }, 600)
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

.push-form {
  max-width: 640px;
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
