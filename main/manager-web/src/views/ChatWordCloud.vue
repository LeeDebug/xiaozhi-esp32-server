<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('chatWordCloud.title') }}</h2>
              <span class="page-desc">{{ $t('chatWordCloud.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadWordCloud">
                  {{ $t('chatWordCloud.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <!-- 筛选：设备 / 智能体 / 角色 / 时间范围 / 展示词数，任一变化立即重新统计 -->
            <div class="filter-bar">
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatWordCloud.device') }}</span>
                <el-select v-model="filters.macAddress" filterable clearable class="control-device"
                  :placeholder="$t('chatWordCloud.allDevices')" @change="handleSearch">
                  <el-option v-for="item in deviceOptions" :key="item.macAddress"
                    :label="deviceLabel(item)" :value="item.macAddress" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatWordCloud.agent') }}</span>
                <el-select v-model="filters.agentId" filterable clearable class="control-agent"
                  :placeholder="$t('chatWordCloud.allAgents')" @change="handleSearch">
                  <el-option v-for="item in agentOptions" :key="item.agentId"
                    :label="agentLabel(item)" :value="item.agentId" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatWordCloud.role') }}</span>
                <el-select v-model="filters.chatType" class="control-role" @change="handleSearch">
                  <el-option :label="$t('chatWordCloud.roleAll')" :value="''" />
                  <el-option :label="$t('chatWordCloud.roleUser')" :value="1" />
                  <el-option :label="$t('chatWordCloud.roleAgent')" :value="2" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatWordCloud.timeRange') }}</span>
                <el-date-picker v-model="dateRange" type="datetimerange" class="control-time"
                  value-format="yyyy-MM-dd HH:mm:ss" :default-time="['00:00:00', '23:59:59']"
                  range-separator="~" :start-placeholder="$t('chatWordCloud.timeRange')"
                  :end-placeholder="$t('chatWordCloud.timeRange')" @change="handleSearch" />
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatWordCloud.topN') }}</span>
                <el-select v-model="topN" class="control-topn" @change="handleSearch">
                  <el-option v-for="n in topNOptions" :key="n" :label="n" :value="n" />
                </el-select>
              </div>
              <div class="filter-actions">
                <CustomButton icon="el-icon-refresh-left" @click="handleReset">
                  {{ $t('chatWordCloud.reset') }}
                </CustomButton>
              </div>
            </div>

            <div class="quick-bar">
              <el-button type="text" size="mini" @click="applyQuickRange(0)">{{ $t('chatWordCloud.today') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(7)">{{ $t('chatWordCloud.last7Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(30)">{{ $t('chatWordCloud.last30Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(-1)">{{ $t('chatWordCloud.allTime') }}</el-button>
            </div>

            <el-alert v-if="loadError" class="dashboard-alert" type="error" show-icon :closable="false"
              :title="loadError" />
            <el-alert v-if="summary.truncated" class="dashboard-alert" type="warning" show-icon :closable="false"
              :title="$t('chatWordCloud.truncated')" />

            <template v-if="hasData">
              <div class="stat-cards">
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatWordCloud.statMessages') }}</div>
                  <div class="stat-value">{{ summary.messageCount }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatWordCloud.statUser') }}</div>
                  <div class="stat-value">{{ summary.userMessageCount }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatWordCloud.statAgent') }}</div>
                  <div class="stat-value">{{ summary.agentMessageCount }}</div>
                </div>
                <div class="stat-card stat-primary">
                  <div class="stat-label">{{ $t('chatWordCloud.statTotalWords') }}</div>
                  <div class="stat-value">{{ summary.totalWords }}</div>
                </div>
                <div class="stat-card stat-muted">
                  <div class="stat-label">{{ $t('chatWordCloud.statDistinct') }}</div>
                  <div class="stat-value">{{ summary.distinctWords }}</div>
                </div>
              </div>

              <div class="chart-grid">
                <div class="chart-card chart-wide">
                  <h3 class="section-title">{{ $t('chatWordCloud.cloudTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatWordCloud.cloudHint') }}</p>
                  <div ref="cloudChart" class="chart-box chart-box-cloud"></div>
                </div>

                <div class="chart-card chart-wide">
                  <h3 class="section-title">{{ $t('chatWordCloud.rankTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatWordCloud.rankHint') }}</p>
                  <div ref="rankChart" class="chart-box chart-box-tall"></div>
                </div>

                <template v-if="filters.chatType === ''">
                  <div class="chart-card">
                    <h3 class="section-title">{{ $t('chatWordCloud.userCloudTitle') }}</h3>
                    <div ref="userCloudChart" class="chart-box"></div>
                  </div>
                  <div class="chart-card">
                    <h3 class="section-title">{{ $t('chatWordCloud.agentCloudTitle') }}</h3>
                    <div ref="agentCloudChart" class="chart-box"></div>
                  </div>
                </template>
              </div>
            </template>

            <div class="empty-state" v-else-if="!loading && !loadError">
              <i class="el-icon-chat-line-square empty-icon"></i>
              <p class="empty-title">{{ $t('chatWordCloud.emptyTitle') }}</p>
              <p class="empty-desc">{{ $t('chatWordCloud.emptyDesc') }}</p>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <el-footer><version-footer /></el-footer>
  </div>
</template>

<script>
import * as echarts from 'echarts';
// echarts-wordcloud 通过副作用向 echarts 注册 wordCloud 系列，只需引入、不需要取导出值
import 'echarts-wordcloud';
import Api from '@/apis/api';
import { ownerDisplayName, hasOwner } from '@/utils/ownerDisplay';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import CustomButton from '@/components/CustomButton.vue';

const CN_OFFSET_MS = 8 * 60 * 60 * 1000;
const RANK_SIZE = 15;

const EMPTY_SUMMARY = {
  messageCount: 0,
  userMessageCount: 0,
  agentMessageCount: 0,
  totalWords: 0,
  distinctWords: 0,
  truncated: false
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
  name: 'ChatWordCloud',
  components: { HeaderBar, VersionFooter, CustomButton },
  data() {
    return {
      // chatType: ''=用户+智能体，1=只看用户，2=只看智能体
      filters: { macAddress: '', agentId: '', chatType: '' },
      dateRange: [],
      topN: 100,
      topNOptions: [50, 100, 150, 200],
      deviceOptions: [],
      agentOptions: [],
      loading: false,
      loadError: '',
      summary: Object.assign({}, EMPTY_SUMMARY),
      words: [],
      userWords: [],
      agentWords: [],
      charts: [],
      resizeHandler: null,
      // 用于丢弃过期响应：筛选快速切换时只采用最后一次请求的结果
      requestSeq: 0
    };
  },
  computed: {
    hasData() {
      return this.words.length > 0;
    }
  },
  created() {
    // 设备管理页 / 首页智能体卡片可带参数直接进入筛选
    this.filters.macAddress = this.$route.query.macAddress || '';
    this.filters.agentId = this.$route.query.agentId || '';
    this.fetchFilterOptions();
    this.loadWordCloud();
  },
  mounted() {
    this.resizeHandler = () => this.charts.forEach((c) => c.resize());
    window.addEventListener('resize', this.resizeHandler);
  },
  beforeDestroy() {
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler);
    }
    this.disposeCharts();
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
        this.$message.error(this.$t('chatWordCloud.filterFailed'));
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
      const params = { topN: this.topN };
      if (this.filters.macAddress) params.macAddress = this.filters.macAddress;
      if (this.filters.agentId) params.agentId = this.filters.agentId;
      if (this.filters.chatType !== '') params.chatType = this.filters.chatType;
      if (this.dateRange && this.dateRange.length === 2) {
        const start = chinaStringToMs(this.dateRange[0]);
        const end = chinaStringToMs(this.dateRange[1]);
        if (start !== null) params.startTime = start;
        if (end !== null) params.endTime = end;
      }
      return params;
    },

    loadWordCloud() {
      this.loading = true;
      this.loadError = '';
      const seq = ++this.requestSeq;
      Api.chatHistory.getWordCloud(this.buildParams(), ({ data }) => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        // 每次刷新先释放旧图表，避免数据变空后残留已卸载 DOM 上的实例
        this.disposeCharts();
        if (data && data.code === 0 && data.data) {
          const d = data.data;
          this.summary = Object.assign({}, EMPTY_SUMMARY, d.summary || {});
          this.words = d.words || [];
          this.userWords = d.userWords || [];
          this.agentWords = d.agentWords || [];
          this.$nextTick(() => this.renderCharts());
        } else {
          this.resetData();
          this.loadError = (data && data.msg) || this.$t('chatWordCloud.loadFailed');
        }
      }, () => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        this.disposeCharts();
        this.resetData();
        this.loadError = this.$t('chatWordCloud.loadFailed');
      });
    },

    resetData() {
      this.summary = Object.assign({}, EMPTY_SUMMARY);
      this.words = [];
      this.userWords = [];
      this.agentWords = [];
    },

    handleSearch() {
      this.loadWordCloud();
    },

    handleReset() {
      this.filters = { macAddress: '', agentId: '', chatType: '' };
      this.dateRange = [];
      this.topN = 100;
      this.loadWordCloud();
    },

    applyQuickRange(days) {
      if (days < 0) {
        this.dateRange = [];
      } else {
        const end = Date.now();
        const start = days === 0 ? chinaStartOfToday() : end - days * 24 * 60 * 60 * 1000;
        this.dateRange = [msToChinaString(start), msToChinaString(end)];
      }
      this.loadWordCloud();
    },

    themeColors() {
      const read = (name, fallback) => {
        const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
        return v || fallback;
      };
      return {
        primary: read('--sys-primary', '#eb6800'),
        light: read('--sys-primary-light', '#f3a161'),
        lighter: read('--sys-primary-lighter', '#f7c69e'),
        text: '#2c3354',
        muted: '#8a93b8',
        line: '#f3f1f1',
        palette: [
          read('--sys-primary', '#eb6800'),
          read('--sys-primary-light', '#f3a161'),
          '#577590', '#43aa8b', '#e9c46a', '#90be6d', '#f4a261', '#8a93b8'
        ]
      };
    },

    initChart(refName) {
      const el = this.$refs[refName];
      if (!el) return null;
      let chart = echarts.getInstanceByDom(el);
      if (!chart) {
        chart = echarts.init(el);
        this.charts.push(chart);
      }
      return chart;
    },

    disposeCharts() {
      this.charts.forEach((c) => c.dispose());
      this.charts = [];
    },

    renderCharts() {
      this.renderCloud('cloudChart', this.words, true);
      this.renderRank();
      if (this.filters.chatType === '') {
        this.renderCloud('userCloudChart', this.userWords, false);
        this.renderCloud('agentCloudChart', this.agentWords, false);
      }
      this.charts.forEach((c) => c.resize());
    },

    // 词云：字号和颜色深浅都随词频变化，词频越高越大、颜色越深
    renderCloud(refName, list, large) {
      const chart = this.initChart(refName);
      if (!chart) return;
      const c = this.themeColors();
      const max = list.reduce((m, w) => Math.max(m, w.count), 1);
      const min = list.reduce((m, w) => Math.min(m, w.count), max);
      // 词频归一化到 0~1，高频词颜色更深
      const ratio = (count) => (max === min ? 1 : (count - min) / (max - min));
      const palette = [c.lighter, c.light, c.primary];
      chart.setOption({
        tooltip: {
          backgroundColor: '#fff',
          borderColor: '#f3f1f1',
          textStyle: { color: c.text, fontSize: 12 },
          formatter: (p) => `${p.name}<br/>${this.$t('chatWordCloud.count')}：${p.value}`
        },
        series: [{
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '96%',
          height: '96%',
          sizeRange: large ? [14, 64] : [12, 36],
          rotationRange: [-45, 45],
          rotationStep: 45,
          gridSize: large ? 8 : 6,
          drawOutOfBound: false,
          layoutAnimation: true,
          textStyle: {
            fontFamily: 'sans-serif',
            fontWeight: 'bold',
            color: (params) => {
              const r = ratio(params.value);
              return r > 0.66 ? palette[2] : (r > 0.33 ? palette[1] : palette[0]);
            }
          },
          emphasis: { textStyle: { color: c.text } },
          data: list.map((w) => ({ name: w.word, value: w.count }))
        }]
      });
    },

    // 高频词排行：横向条形图，给出词云看不准的确切次数
    renderRank() {
      const chart = this.initChart('rankChart');
      if (!chart) return;
      const c = this.themeColors();
      // 次数最高的显示在最上方，所以反向绘制
      const top = this.words.slice(0, RANK_SIZE).reverse();
      chart.setOption({
        color: [c.primary],
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          backgroundColor: '#fff',
          borderColor: '#f3f1f1',
          textStyle: { color: c.text, fontSize: 12 }
        },
        grid: { left: 12, right: 48, bottom: 8, top: 8, containLabel: true },
        xAxis: { type: 'value', minInterval: 1, axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        yAxis: {
          type: 'category',
          data: top.map((w) => w.word),
          axisLabel: { color: c.text, fontSize: 13 },
          axisLine: { lineStyle: { color: c.line } }
        },
        series: [{
          name: this.$t('chatWordCloud.count'),
          type: 'bar',
          barMaxWidth: 20,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: c.primary }, { offset: 1, color: c.lighter }
            ])
          },
          label: { show: true, position: 'right', color: c.muted },
          data: top.map((w) => w.count)
        }]
      });
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

.control-device { width: 230px; }
.control-agent { width: 180px; }
.control-role { width: 150px; }
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.chart-card {
  background: #fff;
  border: 1px solid #f3f1f1;
  border-radius: 12px;
  padding: 18px 20px 14px;
  min-width: 0;
}

.chart-wide {
  grid-column: 1 / -1;
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

.chart-box {
  width: 100%;
  height: 300px;
}

.chart-box-tall {
  height: 420px;
}

.chart-box-cloud {
  height: 460px;
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
</style>
