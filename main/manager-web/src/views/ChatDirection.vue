<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('chatDirection.title') }}</h2>
              <span class="page-desc">{{ $t('chatDirection.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadDirection">
                  {{ $t('chatDirection.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <!-- 筛选：地区 / 智能体 / 设备 / 时间范围 / 每组词数，任一变化立即重新统计 -->
            <div class="filter-bar">
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatDirection.province') }}</span>
                <el-select v-model="filters.province" filterable clearable class="control-province"
                  :placeholder="$t('chatDirection.allProvinces')" @change="handleSearch">
                  <el-option v-for="item in provinceOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatDirection.agent') }}</span>
                <el-select v-model="filters.agentId" filterable clearable class="control-agent"
                  :placeholder="$t('chatDirection.allAgents')" @change="handleSearch">
                  <el-option v-for="item in agentOptions" :key="item.agentId"
                    :label="agentLabel(item)" :value="item.agentId" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatDirection.device') }}</span>
                <el-select v-model="filters.macAddress" filterable clearable class="control-device"
                  :placeholder="$t('chatDirection.allDevices')" @change="handleSearch">
                  <el-option v-for="item in deviceOptions" :key="item.macAddress"
                    :label="deviceLabel(item)" :value="item.macAddress" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatDirection.timeRange') }}</span>
                <el-date-picker v-model="dateRange" type="datetimerange" class="control-time"
                  value-format="yyyy-MM-dd HH:mm:ss" :default-time="['00:00:00', '23:59:59']"
                  range-separator="~" :start-placeholder="$t('chatDirection.timeRange')"
                  :end-placeholder="$t('chatDirection.timeRange')" @change="handleSearch" />
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatDirection.topN') }}</span>
                <el-select v-model="topN" class="control-topn" @change="handleSearch">
                  <el-option v-for="n in topNOptions" :key="n" :label="n" :value="n" />
                </el-select>
              </div>
              <div class="filter-actions">
                <CustomButton icon="el-icon-refresh-left" @click="handleReset">
                  {{ $t('chatDirection.reset') }}
                </CustomButton>
              </div>
            </div>

            <div class="quick-bar">
              <el-button type="text" size="mini" @click="applyQuickRange(0)">{{ $t('chatDirection.today') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(7)">{{ $t('chatDirection.last7Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(30)">{{ $t('chatDirection.last30Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(-1)">{{ $t('chatDirection.allTime') }}</el-button>
            </div>

            <el-alert v-if="loadError" class="dashboard-alert" type="error" show-icon :closable="false"
              :title="loadError" />
            <el-alert v-if="direction.truncated" class="dashboard-alert" type="warning" show-icon :closable="false"
              :title="$t('chatDirection.truncated')" />

            <template v-if="hasData">
              <div class="stat-cards">
                <div class="stat-card stat-primary">
                  <div class="stat-label">{{ $t('chatDirection.statMessages') }}</div>
                  <div class="stat-value">{{ direction.messageCount }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatDirection.statRegions') }}</div>
                  <div class="stat-value">{{ direction.regions.length }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatDirection.statAgents') }}</div>
                  <div class="stat-value">{{ direction.agents.length }}</div>
                </div>
                <div class="stat-card stat-muted">
                  <div class="stat-label">{{ $t('chatDirection.statDevices') }}</div>
                  <div class="stat-value">{{ direction.devices.length }}</div>
                </div>
              </div>

              <!-- 通过 AI 大模型总结：对当前筛选结果归纳提问方向，记录每一次分析，默认展示最近一次 -->
              <div class="chart-card ai-panel">
                <div class="ai-header">
                  <div>
                    <h3 class="section-title">{{ $t('chatDirection.aiTitle') }}</h3>
                    <p class="chart-hint">{{ $t('chatDirection.aiHint') }}</p>
                  </div>
                  <CustomButton icon="el-icon-magic-stick" type="primary" :loading="analyzing" :disabled="analyzing"
                    @click="analyzeByLlm">
                    {{ analyzing ? $t('chatDirection.aiRunning') : $t('chatDirection.aiButton') }}
                  </CustomButton>
                </div>

                <div v-if="currentAnalysis" class="ai-result">
                  <div class="ai-meta">
                    <span class="ai-meta-item">{{ $t('chatDirection.aiViewing') }}</span>
                    <el-tag v-if="isLatest(currentAnalysis)" size="mini" type="success">{{ $t('chatDirection.aiLatest') }}</el-tag>
                    <span class="ai-meta-item">{{ $t('chatDirection.aiTime') }}：{{ currentAnalysis.createdAt || '-' }}</span>
                    <span class="ai-meta-item">{{ $t('chatDirection.aiModel') }}：{{ currentAnalysis.modelName || '-' }}</span>
                    <span v-if="analysisOwner(currentAnalysis)" class="ai-meta-item">{{ analysisOwner(currentAnalysis) }}</span>
                  </div>
                  <div class="ai-condition">{{ $t('chatDirection.aiCondition') }}：{{ conditionText(currentAnalysis) }}</div>
                  <!-- 大模型输出按纯文本展示，不使用 v-html，避免输出内容中的标签被执行 -->
                  <div class="ai-text">{{ currentAnalysis.result }}</div>
                </div>
                <div v-else class="ai-empty">{{ $t('chatDirection.aiEmpty') }}</div>

                <div v-if="history.length > 1" class="ai-history">
                  <div class="ai-history-title">{{ $t('chatDirection.aiHistory') }}</div>
                  <div v-for="item in history" :key="item.id" class="ai-history-item"
                    :class="{ active: currentAnalysis && currentAnalysis.id === item.id }" @click="currentAnalysis = item">
                    <span class="ai-history-time">{{ item.createdAt }}</span>
                    <span class="ai-history-cond">{{ conditionText(item) }}</span>
                  </div>
                </div>
              </div>

              <div class="chart-card">
                <el-tabs v-model="activeTab">
                  <el-tab-pane :label="$t('chatDirection.tabRegion')" name="regions" />
                  <el-tab-pane :label="$t('chatDirection.tabAgent')" name="agents" />
                  <el-tab-pane :label="$t('chatDirection.tabDevice')" name="devices" />
                </el-tabs>
                <el-table :data="activeGroups" border stripe style="width: 100%">
                  <el-table-column :label="$t('chatDirection.colName')" prop="name" width="220" show-overflow-tooltip />
                  <el-table-column :label="$t('chatDirection.colMessages')" prop="messageCount" width="110" align="center" />
                  <el-table-column :label="$t('chatDirection.colWords')">
                    <template slot-scope="scope">
                      <span v-if="!scope.row.words.length" class="no-words">{{ $t('chatDirection.noWords') }}</span>
                      <el-tag v-for="w in scope.row.words" :key="w.word" size="mini" class="word-tag"
                        effect="plain">{{ w.word }} · {{ w.count }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>

            <div class="empty-state" v-else-if="!loading && !loadError">
              <i class="el-icon-chat-line-square empty-icon"></i>
              <p class="empty-title">{{ $t('chatDirection.emptyTitle') }}</p>
              <p class="empty-desc">{{ $t('chatDirection.emptyDesc') }}</p>
              <!-- 当前条件没有数据时仍展示历史分析记录，默认显示最近一次 -->
              <div v-if="currentAnalysis" class="chart-card ai-panel ai-panel-standalone">
                <div class="ai-meta">
                  <span class="ai-meta-item">{{ $t('chatDirection.aiViewing') }}</span>
                  <span class="ai-meta-item">{{ $t('chatDirection.aiTime') }}：{{ currentAnalysis.createdAt || '-' }}</span>
                  <span class="ai-meta-item">{{ $t('chatDirection.aiModel') }}：{{ currentAnalysis.modelName || '-' }}</span>
                </div>
                <div class="ai-condition">{{ $t('chatDirection.aiCondition') }}：{{ conditionText(currentAnalysis) }}</div>
                <div class="ai-text">{{ currentAnalysis.result }}</div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <el-footer><version-footer /></el-footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import { ownerDisplayName, hasOwner } from '@/utils/ownerDisplay';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import CustomButton from '@/components/CustomButton.vue';

const CN_OFFSET_MS = 8 * 60 * 60 * 1000;

const EMPTY_DIRECTION = {
  messageCount: 0,
  truncated: false,
  regions: [],
  agents: [],
  devices: []
};

function pad(value) {
  return String(value).padStart(2, '0');
}

/** 把「中国时间 yyyy-MM-dd HH:mm:ss」字符串按 UTC+8 解析成绝对毫秒 */
function chinaStringToMs(value) {
  if (!value) return null;
  const ms = new Date(String(value).replace(' ', 'T') + '+08:00').getTime();
  return Number.isNaN(ms) ? null : ms;
}

/** 把绝对毫秒格式化成「中国时间 yyyy-MM-dd HH:mm:ss」 */
function msToChinaString(ms) {
  const shifted = new Date(ms + CN_OFFSET_MS);
  return `${shifted.getUTCFullYear()}-${pad(shifted.getUTCMonth() + 1)}-${pad(shifted.getUTCDate())} `
    + `${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}:${pad(shifted.getUTCSeconds())}`;
}

/** 中国时间当天 00:00:00 对应的绝对毫秒 */
function chinaStartOfToday() {
  const shifted = new Date(Date.now() + CN_OFFSET_MS);
  return Date.UTC(shifted.getUTCFullYear(), shifted.getUTCMonth(), shifted.getUTCDate()) - CN_OFFSET_MS;
}

export default {
  name: 'ChatDirection',
  components: { HeaderBar, VersionFooter, CustomButton },
  data() {
    return {
      filters: { province: '', agentId: '', macAddress: '' },
      dateRange: [],
      topN: 20,
      topNOptions: [10, 20, 30, 50],
      provinceOptions: [],
      deviceOptions: [],
      agentOptions: [],
      loading: false,
      loadError: '',
      direction: Object.assign({}, EMPTY_DIRECTION),
      activeTab: 'regions',
      // 用于丢弃过期响应：筛选快速切换时只采用最后一次请求的结果
      requestSeq: 0,
      // 大模型分析
      analyzing: false,
      history: [],
      currentAnalysis: null
    };
  },
  computed: {
    hasData() {
      return this.direction.regions.length > 0;
    },
    activeGroups() {
      return this.direction[this.activeTab] || [];
    }
  },
  created() {
    // 智能体卡片 / 设备管理页可带参数直接进入筛选
    this.filters.macAddress = this.$route.query.macAddress || '';
    this.filters.agentId = this.$route.query.agentId || '';
    this.fetchFilterOptions();
    this.loadDirection();
    this.loadHistory();
  },
  methods: {
    ownerDisplayName,
    hasOwner,

    fetchFilterOptions() {
      Api.chatHistory.getFilterOptions(({ data }) => {
        if (data && data.code === 0 && data.data) {
          this.deviceOptions = data.data.devices || [];
          this.agentOptions = data.data.agents || [];
        }
      }, () => {
        this.$message.error(this.$t('chatDirection.filterFailed'));
      });
      Api.chatHistory.getChatDirectionProvinces(({ data }) => {
        if (data && data.code === 0) {
          this.provinceOptions = data.data || [];
        }
      }, () => {
        this.$message.error(this.$t('chatDirection.filterFailed'));
      });
    },

    showOwnerTags() {
      return this.deviceOptions.some((item) => item.isOwn === false)
        || this.agentOptions.some((item) => item.isOwn === false);
    },

    ownerSuffix(item) {
      if (!this.showOwnerTags() || !this.hasOwner(item)) return '';
      const tag = item.isOwn === false ? this.$t('common.ownerOther') : this.$t('common.ownerMine');
      return `（${tag} · ${this.ownerDisplayName(item)}）`;
    },

    deviceLabel(item) {
      const base = item.alias ? `${item.alias} · ${item.macAddress}` : item.macAddress;
      return `${base}${this.ownerSuffix(item)}`;
    },

    agentLabel(item) {
      const base = item.agentName || item.agentId;
      return `${base}${this.ownerSuffix(item)}`;
    },

    buildParams() {
      const params = {};
      if (this.filters.province) params.province = this.filters.province;
      if (this.filters.agentId) params.agentId = this.filters.agentId;
      if (this.filters.macAddress) params.macAddress = this.filters.macAddress;
      if (this.dateRange && this.dateRange.length === 2) {
        const start = chinaStringToMs(this.dateRange[0]);
        const end = chinaStringToMs(this.dateRange[1]);
        if (start !== null) params.startTime = start;
        if (end !== null) params.endTime = end;
      }
      return params;
    },

    loadDirection() {
      this.loading = true;
      this.loadError = '';
      const seq = ++this.requestSeq;
      const params = Object.assign({ topN: this.topN }, this.buildParams());
      Api.chatHistory.getChatDirection(params, ({ data }) => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        if (data && data.code === 0 && data.data) {
          this.direction = Object.assign({}, EMPTY_DIRECTION, data.data);
        } else {
          this.direction = Object.assign({}, EMPTY_DIRECTION);
          this.loadError = (data && data.msg) || this.$t('chatDirection.loadFailed');
        }
      }, () => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        this.direction = Object.assign({}, EMPTY_DIRECTION);
        this.loadError = this.$t('chatDirection.loadFailed');
      });
    },

    handleSearch() {
      this.loadDirection();
    },

    handleReset() {
      this.filters = { province: '', agentId: '', macAddress: '' };
      this.dateRange = [];
      this.topN = 20;
      this.loadDirection();
    },

    applyQuickRange(days) {
      if (days < 0) {
        this.dateRange = [];
      } else {
        const end = Date.now();
        const start = days === 0 ? chinaStartOfToday() : end - days * 24 * 60 * 60 * 1000;
        this.dateRange = [msToChinaString(start), msToChinaString(end)];
      }
      this.loadDirection();
    },

    // ---------------- 大模型分析 ----------------

    /** 进入页面时加载历史分析记录，默认展示最近一次 */
    loadHistory() {
      Api.chatHistory.getChatDirectionHistory({ limit: 20 }, ({ data }) => {
        if (data && data.code === 0) {
          this.history = data.data || [];
          // 最新的在前；用户已经手动点选了某条时不覆盖
          if (!this.currentAnalysis && this.history.length) {
            this.currentAnalysis = this.history[0];
          }
        }
      }, () => {
        this.$message.error(this.$t('chatDirection.aiHistoryFailed'));
      });
    },

    analyzeByLlm() {
      if (!this.hasData) {
        this.$message.warning(this.$t('chatDirection.aiNeedData'));
        return;
      }
      this.analyzing = true;
      const body = Object.assign({ topN: this.topN }, this.buildParams());
      Api.chatHistory.analyzeChatDirection(body, ({ data }) => {
        this.analyzing = false;
        if (data && data.code === 0 && data.data) {
          this.$message.success(this.$t('chatDirection.aiSuccess'));
          // 新记录插到最前并选中，历史列表保留之前的记录
          this.history = [data.data].concat(this.history.filter((h) => h.id !== data.data.id));
          this.currentAnalysis = data.data;
        } else {
          this.$message.error((data && data.msg) || this.$t('chatDirection.aiFailed'));
        }
      }, (err) => {
        this.analyzing = false;
        const msg = err && err.data && err.data.msg;
        this.$message.error(msg || this.$t('chatDirection.aiFailed'));
      });
    },

    isLatest(item) {
      return this.history.length > 0 && this.history[0].id === item.id;
    },

    /** 把一条分析记录当时的筛选条件拼成一句话展示 */
    conditionText(item) {
      const all = this.$t('chatDirection.aiAll');
      const unlimited = this.$t('chatDirection.aiUnlimited');
      const time = (item.startTime || item.endTime)
        ? `${item.startTime || unlimited} ~ ${item.endTime || unlimited}`
        : unlimited;
      return `${this.$t('chatDirection.province')}：${item.province || all}；`
        + `${this.$t('chatDirection.agent')}：${item.agentName || all}；`
        + `${this.$t('chatDirection.device')}：${item.macAddress || all}；`
        + `${this.$t('chatDirection.timeRange')}：${time}`;
    },

    /** 只在查看他人的分析记录时标注归属，与其它列表的归属展示保持一致 */
    analysisOwner(item) {
      if (item.isOwn !== false || !this.hasOwner(item)) return '';
      return `${this.$t('common.ownerOther')} · ${this.ownerDisplayName(item)}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  min-width: 900px;
  min-height: 506px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--sys-surface);
  overflow: hidden;
}

.main-wrapper {
  height: calc(100vh - 63px - 35px);
  padding: 20px 22px 0;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
}

.content-panel {
  display: flex;
  height: 100%;
  border-radius: 15px;
  border: 1px solid #fff;
  overflow: hidden;
}

.content-area {
  flex: 1;
  min-width: 600px;
  overflow: auto;
  background: #fff;
  border-radius: 15px;
}

.ai-card {
  background: white;
  border: none;
  box-shadow: none;
  overflow: visible;
}

.ai-card ::v-deep .el-card__body {
  padding: 22px 26px 28px;
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
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  padding-bottom: 8px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.control-province { width: 150px; }
.control-agent { width: 180px; }
.control-device { width: 230px; }
.control-time { width: 340px; }
.control-topn { width: 90px; }

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.quick-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0 14px;
}

.dashboard-alert {
  margin: 0 0 16px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, var(--sys-primary-bg-soft), var(--sys-primary-bg-soft));
  border: 1px solid var(--sys-primary-bg-soft);
  border-radius: 12px;
  padding: 18px 20px;
}

.stat-card.stat-primary {
  background: linear-gradient(135deg, #fff3e8, #fff8f1);
  border-color: #fde3cc;
}

.stat-card.stat-muted {
  background: linear-gradient(135deg, #f4f5fa, #f8f9fc);
  border-color: #eceef6;
}

.stat-label {
  font-size: 13px;
  color: #8a93b8;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: #2c3354;
  line-height: 1.15;
}

.chart-card {
  background: #fff;
  border: 1px solid #f3f1f1;
  border-radius: 12px;
  padding: 18px 20px 14px;
  min-width: 0;
  margin-bottom: 22px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #2c3354;
  line-height: 1.5;
}

.chart-hint {
  font-size: 13px;
  color: #8a93b8;
  margin: 0 0 14px;
  line-height: 1.7;
}

.ai-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.ai-result {
  margin-top: 4px;
}

.ai-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  font-size: 13px;
  color: #606266;
}

.ai-condition {
  margin: 8px 0 12px;
  font-size: 13px;
  color: #8a93b8;
  line-height: 1.7;
}

/* 大模型输出保留换行和缩进，按纯文本展示 */
.ai-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.85;
  color: #2c3354;
  background: #faf9f8;
  border-radius: 8px;
  padding: 14px 16px;
}

.ai-empty {
  font-size: 13px;
  color: #8a93b8;
  padding: 12px 0 4px;
}

.ai-history {
  margin-top: 16px;
  border-top: 1px dashed #ebe8e8;
  padding-top: 12px;
}

.ai-history-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.ai-history-item {
  display: flex;
  gap: 14px;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #606266;
}

.ai-history-item:hover {
  background: #faf9f8;
}

.ai-history-item.active {
  background: var(--sys-primary-bg-soft);
  color: #2c3354;
}

.ai-history-time {
  flex: none;
  width: 160px;
}

.ai-history-cond {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.word-tag {
  margin: 2px 6px 2px 0;
}

.no-words {
  font-size: 13px;
  color: #8a93b8;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 16px;
  color: #8a93b8;
}

.empty-icon {
  font-size: 52px;
  color: var(--sys-primary-lighter, #f7c69e);
  margin-bottom: 16px;
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #2c3354;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  margin: 0;
  text-align: center;
}

.ai-panel-standalone {
  width: 100%;
  margin: 24px 0 0;
  text-align: left;
}
</style>
