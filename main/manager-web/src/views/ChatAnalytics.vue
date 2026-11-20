<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('chatAnalytics.title') }}</h2>
              <span class="page-desc">{{ $t('chatAnalytics.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadAnalytics">
                  {{ $t('chatAnalytics.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <!-- 筛选：设备 / 智能体 / 时间范围，任一变化立即重新统计 -->
            <div class="filter-bar">
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatAnalytics.device') }}</span>
                <el-select v-model="filters.macAddress" filterable clearable class="control-device"
                  :placeholder="$t('chatAnalytics.allDevices')" @change="handleSearch">
                  <el-option v-for="item in deviceOptions" :key="item.macAddress"
                    :label="deviceLabel(item)" :value="item.macAddress" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatAnalytics.agent') }}</span>
                <el-select v-model="filters.agentId" filterable clearable class="control-agent"
                  :placeholder="$t('chatAnalytics.allAgents')" @change="handleSearch">
                  <el-option v-for="item in agentOptions" :key="item.agentId"
                    :label="agentLabel(item)" :value="item.agentId" />
                </el-select>
              </div>
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatAnalytics.timeRange') }}</span>
                <el-date-picker v-model="dateRange" type="datetimerange" class="control-time"
                  value-format="yyyy-MM-dd HH:mm:ss" :default-time="['00:00:00', '23:59:59']"
                  range-separator="~" :start-placeholder="$t('chatAnalytics.timeRange')"
                  :end-placeholder="$t('chatAnalytics.timeRange')" @change="handleSearch" />
              </div>
              <div class="filter-actions">
                <CustomButton icon="el-icon-refresh-left" @click="handleReset">
                  {{ $t('chatAnalytics.reset') }}
                </CustomButton>
              </div>
            </div>

            <div class="quick-bar">
              <el-button type="text" size="mini" @click="applyQuickRange(0)">{{ $t('chatAnalytics.today') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(7)">{{ $t('chatAnalytics.last7Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(30)">{{ $t('chatAnalytics.last30Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(-1)">{{ $t('chatAnalytics.allTime') }}</el-button>
              <span v-if="summary.firstTime" class="range-text">
                <i class="el-icon-time"></i>
                {{ $t('chatAnalytics.range') }}：{{ summary.firstTime }} ~ {{ summary.lastTime }}
              </span>
            </div>

            <el-alert v-if="loadError" class="dashboard-alert" type="error" show-icon :closable="false"
              :title="loadError" />

            <template v-if="hasData">
              <div class="stat-cards">
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatAnalytics.statSessions') }}</div>
                  <div class="stat-value">{{ summary.sessionCount }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatAnalytics.statMessages') }}</div>
                  <div class="stat-value">{{ summary.messageCount }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatAnalytics.statAvgRounds') }}</div>
                  <div class="stat-value">{{ summary.avgRounds }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatAnalytics.statAvgSpan') }}</div>
                  <div class="stat-value">{{ formatDuration(summary.avgSpanSeconds) }}</div>
                </div>
                <div class="stat-card stat-primary">
                  <div class="stat-label">{{ $t('chatAnalytics.statActive') }}</div>
                  <div class="stat-value">{{ formatDuration(summary.activeSeconds) }}</div>
                </div>
                <div class="stat-card stat-muted">
                  <div class="stat-label">{{ $t('chatAnalytics.statWait') }}</div>
                  <div class="stat-value">{{ formatDuration(summary.waitSeconds) }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatAnalytics.statFirstAudio') }}</div>
                  <div class="stat-value">{{ summary.avgFirstAudioMs ? Math.round(summary.avgFirstAudioMs) + ' ms' : '-' }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">{{ $t('chatAnalytics.statDevices') }}</div>
                  <div class="stat-value">{{ summary.deviceCount }}</div>
                </div>
              </div>

              <div class="coverage-hint" v-if="summary.metricSessionCount < summary.sessionCount">
                <i class="el-icon-info"></i>
                {{ $t('chatAnalytics.metricCoverage', { covered: summary.metricSessionCount, total: summary.sessionCount }) }}
              </div>

              <div class="chart-grid">
                <div class="chart-card chart-wide">
                  <h3 class="section-title">{{ $t('chatAnalytics.dailyTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.dailyHint') }}</p>
                  <div ref="dailyChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.deviceTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.deviceHint') }}</p>
                  <div ref="deviceChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.agentTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.agentHint') }}</p>
                  <div ref="agentChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.hourlyTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.hourlyHint') }}</p>
                  <div ref="hourlyChart" class="chart-box chart-box-tall"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.heatmapTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.heatmapHint') }}</p>
                  <div ref="heatmapChart" class="chart-box chart-box-tall"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.durationTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.durationHint') }}</p>
                  <div ref="durationChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.roundTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.roundHint') }}</p>
                  <div ref="roundChart" class="chart-box"></div>
                </div>

                <div class="chart-card chart-wide">
                  <h3 class="section-title">{{ $t('chatAnalytics.activeWaitTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.activeWaitHint') }}</p>
                  <div ref="activeWaitChart" class="chart-box chart-box-tall"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.ratioTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.ratioHint') }}</p>
                  <div ref="ratioChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.deviceRadarTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.deviceRadarHint') }}</p>
                  <div ref="radarChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.deviceTreeTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.deviceTreeHint') }}</p>
                  <div ref="treeChart" class="chart-box"></div>
                </div>

                <div class="chart-card">
                  <h3 class="section-title">{{ $t('chatAnalytics.scatterTitle') }}</h3>
                  <p class="chart-hint">{{ $t('chatAnalytics.scatterHint') }}</p>
                  <div ref="scatterChart" class="chart-box"></div>
                </div>
              </div>
            </template>

            <div class="empty-state" v-else-if="!loading && !loadError">
              <i class="el-icon-data-analysis empty-icon"></i>
              <p class="empty-title">{{ $t('chatAnalytics.emptyTitle') }}</p>
              <p class="empty-desc">{{ $t('chatAnalytics.emptyDesc') }}</p>
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
import Api from '@/apis/api';
import { ownerDisplayName, hasOwner } from '@/utils/ownerDisplay';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import CustomButton from '@/components/CustomButton.vue';

const CN_OFFSET_MS = 8 * 60 * 60 * 1000;
const WEEK_KEYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

const EMPTY_SUMMARY = {
  sessionCount: 0,
  messageCount: 0,
  userMessageCount: 0,
  deviceCount: 0,
  agentCount: 0,
  avgMessages: 0,
  avgRounds: 0,
  avgSpanSeconds: 0,
  activeSeconds: 0,
  waitSeconds: 0,
  metricSessionCount: 0,
  avgFirstAudioMs: 0,
  firstTime: '',
  lastTime: ''
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
  name: 'ChatAnalytics',
  components: { HeaderBar, VersionFooter, CustomButton },
  data() {
    return {
      filters: { macAddress: '', agentId: '' },
      dateRange: [],
      deviceOptions: [],
      agentOptions: [],
      loading: false,
      loadError: '',
      summary: Object.assign({}, EMPTY_SUMMARY),
      daily: [],
      devices: [],
      agents: [],
      hourly: [],
      weekHour: [],
      durationBuckets: [],
      roundBuckets: [],
      topSessions: [],
      scatter: [],
      charts: [],
      resizeHandler: null,
      // 用于丢弃过期响应：筛选快速切换时只采用最后一次请求的结果
      requestSeq: 0
    };
  },
  computed: {
    hasData() {
      return (this.summary.sessionCount || 0) > 0;
    }
  },
  created() {
    // 设备管理页 / 首页智能体卡片可带参数直接进入筛选
    this.filters.macAddress = this.$route.query.macAddress || '';
    this.filters.agentId = this.$route.query.agentId || '';
    this.fetchFilterOptions();
    this.loadAnalytics();
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
        this.$message.error(this.$t('chatAnalytics.filterFailed'));
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
      if (this.filters.macAddress) params.macAddress = this.filters.macAddress;
      if (this.filters.agentId) params.agentId = this.filters.agentId;
      if (this.dateRange && this.dateRange.length === 2) {
        const start = chinaStringToMs(this.dateRange[0]);
        const end = chinaStringToMs(this.dateRange[1]);
        if (start !== null) params.startTime = start;
        if (end !== null) params.endTime = end;
      }
      return params;
    },

    loadAnalytics() {
      this.loading = true;
      this.loadError = '';
      const seq = ++this.requestSeq;
      Api.chatHistory.getAnalytics(this.buildParams(), ({ data }) => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        // 每次刷新先释放旧图表，避免数据变空后残留已卸载 DOM 上的实例
        this.disposeCharts();
        if (data && data.code === 0 && data.data) {
          const d = data.data;
          this.summary = Object.assign({}, EMPTY_SUMMARY, d.summary || {});
          this.daily = d.daily || [];
          this.devices = d.devices || [];
          this.agents = d.agents || [];
          this.hourly = d.hourly || [];
          this.weekHour = d.weekHour || [];
          this.durationBuckets = d.durationBuckets || [];
          this.roundBuckets = d.roundBuckets || [];
          this.topSessions = d.topSessions || [];
          this.scatter = d.scatter || [];
          this.$nextTick(() => this.renderCharts());
        } else {
          this.resetData();
          this.loadError = (data && data.msg) || this.$t('chatAnalytics.loadFailed');
        }
      }, () => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        this.disposeCharts();
        this.resetData();
        this.loadError = this.$t('chatAnalytics.loadFailed');
      });
    },

    resetData() {
      this.summary = Object.assign({}, EMPTY_SUMMARY);
      this.daily = [];
      this.devices = [];
      this.agents = [];
      this.hourly = [];
      this.weekHour = [];
      this.durationBuckets = [];
      this.roundBuckets = [];
      this.topSessions = [];
      this.scatter = [];
    },

    handleSearch() {
      this.loadAnalytics();
    },

    handleReset() {
      this.filters = { macAddress: '', agentId: '' };
      this.dateRange = [];
      this.loadAnalytics();
    },

    applyQuickRange(days) {
      if (days < 0) {
        this.dateRange = [];
      } else {
        const end = Date.now();
        const start = days === 0 ? chinaStartOfToday() : end - days * 24 * 60 * 60 * 1000;
        this.dateRange = [msToChinaString(start), msToChinaString(end)];
      }
      this.loadAnalytics();
    },

    /** 秒 -> 可读时长，如 1h 5m / 3m 20s / 45s */
    formatDuration(seconds) {
      const total = Math.round(Number(seconds) || 0);
      if (total <= 0) return '0s';
      const h = Math.floor(total / 3600);
      const m = Math.floor((total % 3600) / 60);
      const s = total % 60;
      if (h > 0) return `${h}h ${m}m`;
      if (m > 0) return `${m}m ${s}s`;
      return `${s}s`;
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
        blue: '#577590',
        green: '#43aa8b',
        palette: [
          read('--sys-primary', '#eb6800'),
          read('--sys-primary-light', '#f3a161'),
          '#577590', '#43aa8b', '#e9c46a', '#90be6d', '#f4a261', '#8a93b8', '#e8554e'
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

    baseTooltip(trigger) {
      return {
        trigger: trigger || 'axis',
        backgroundColor: '#fff',
        borderColor: '#f3f1f1',
        textStyle: { color: '#2c3354', fontSize: 12 },
        extraCssText: 'box-shadow:0 4px 16px rgba(44,51,84,.12);border-radius:8px;'
      };
    },

    disposeCharts() {
      this.charts.forEach((c) => c.dispose());
      this.charts = [];
    },

    renderCharts() {
      this.renderDaily();
      this.renderDevice();
      this.renderAgent();
      this.renderHourly();
      this.renderHeatmap();
      this.renderBuckets('durationChart', this.durationBuckets, 'primary');
      this.renderBuckets('roundChart', this.roundBuckets, 'blue');
      this.renderActiveWait();
      this.renderRatio();
      this.renderRadar();
      this.renderTree();
      this.renderScatter();
      this.charts.forEach((c) => c.resize());
    },

    // 每日趋势：柱状(会话数) + 折线(消息数)，双纵轴
    renderDaily() {
      const chart = this.initChart('dailyChart');
      if (!chart) return;
      const c = this.themeColors();
      const dates = this.daily.map((d) => d.date);
      chart.setOption({
        color: [c.primary, c.blue],
        tooltip: Object.assign(this.baseTooltip(), { axisPointer: { type: 'shadow' } }),
        legend: {
          data: [this.$t('chatAnalytics.sessions'), this.$t('chatAnalytics.messages')],
          textStyle: { color: c.muted }, right: 12, top: 0
        },
        grid: { left: 12, right: 16, bottom: 8, top: 40, containLabel: true },
        xAxis: {
          type: 'category', data: dates,
          axisLine: { lineStyle: { color: c.line } },
          axisLabel: { color: c.muted, fontSize: 11, rotate: dates.length > 15 ? 30 : 0 }
        },
        yAxis: [
          { type: 'value', minInterval: 1, axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
          { type: 'value', minInterval: 1, axisLabel: { color: c.muted }, splitLine: { show: false } }
        ],
        dataZoom: dates.length > 31 ? [{ type: 'inside' }, { type: 'slider', height: 14, bottom: 0 }] : [],
        series: [
          {
            name: this.$t('chatAnalytics.sessions'), type: 'bar', barMaxWidth: 28,
            itemStyle: { borderRadius: [4, 4, 0, 0] },
            data: this.daily.map((d) => d.sessions)
          },
          {
            name: this.$t('chatAnalytics.messages'), type: 'line', yAxisIndex: 1, smooth: true,
            symbolSize: 6, data: this.daily.map((d) => d.messages)
          }
        ]
      });
    },

    // 设备排行：横向条形图（按会话数升序绘制，最多的在上方）
    renderDevice() {
      const chart = this.initChart('deviceChart');
      if (!chart) return;
      const c = this.themeColors();
      const top = this.devices.slice(0, 10).reverse();
      chart.setOption({
        color: [c.primary],
        tooltip: Object.assign(this.baseTooltip(), { axisPointer: { type: 'shadow' } }),
        grid: { left: 12, right: 40, bottom: 8, top: 8, containLabel: true },
        xAxis: { type: 'value', minInterval: 1, axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        yAxis: {
          type: 'category',
          data: top.map((d) => d.name || d.macAddress),
          axisLabel: { color: c.text, width: 110, overflow: 'truncate' },
          axisLine: { lineStyle: { color: c.line } }
        },
        series: [{
          name: this.$t('chatAnalytics.sessions'), type: 'bar', barMaxWidth: 18,
          itemStyle: { borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right', color: c.muted },
          data: top.map((d) => d.sessions)
        }]
      });
    },

    // 智能体占比：玫瑰图
    renderAgent() {
      const chart = this.initChart('agentChart');
      if (!chart) return;
      const c = this.themeColors();
      chart.setOption({
        color: c.palette,
        tooltip: Object.assign(this.baseTooltip('item'), { formatter: '{b}<br/>{c} ({d}%)' }),
        legend: { type: 'scroll', bottom: 0, textStyle: { color: c.muted } },
        series: [{
          name: this.$t('chatAnalytics.sessions'), type: 'pie', roseType: 'radius',
          radius: ['18%', '68%'], center: ['50%', '45%'],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { color: c.text, formatter: '{b}' },
          data: this.agents.map((a) => ({ name: a.name, value: a.sessions }))
        }]
      });
    },

    // 24 小时分布：极坐标柱状图
    renderHourly() {
      const chart = this.initChart('hourlyChart');
      if (!chart) return;
      const c = this.themeColors();
      const hours = Array.from({ length: 24 }, (_, i) => `${pad(i)}:00`);
      chart.setOption({
        color: [c.primary],
        tooltip: Object.assign(this.baseTooltip(), { axisPointer: { type: 'shadow' } }),
        polar: { radius: ['14%', '78%'], center: ['50%', '52%'] },
        angleAxis: {
          type: 'category', data: hours, startAngle: 90,
          axisLabel: { color: c.muted, fontSize: 10, interval: 2 },
          axisLine: { lineStyle: { color: c.line } }
        },
        radiusAxis: { minInterval: 1, axisLabel: { color: c.muted, fontSize: 10 }, splitLine: { lineStyle: { color: c.line } } },
        series: [{
          name: this.$t('chatAnalytics.sessions'), type: 'bar', coordinateSystem: 'polar',
          itemStyle: { borderRadius: 3 }, data: this.hourly
        }]
      });
    },

    // 星期 x 小时热力图
    renderHeatmap() {
      const chart = this.initChart('heatmapChart');
      if (!chart) return;
      const c = this.themeColors();
      const hours = Array.from({ length: 24 }, (_, i) => pad(i));
      const max = this.weekHour.reduce((m, item) => Math.max(m, item[2]), 1);
      chart.setOption({
        tooltip: Object.assign(this.baseTooltip('item'), {
          formatter: (p) => `${WEEK_KEYS[p.value[1]]} ${pad(p.value[0])}:00<br/>${this.$t('chatAnalytics.sessions')}：${p.value[2]}`
        }),
        grid: { left: 8, right: 12, bottom: 44, top: 8, containLabel: true },
        xAxis: { type: 'category', data: hours, splitArea: { show: true }, axisLabel: { color: c.muted, fontSize: 10 } },
        yAxis: { type: 'category', data: WEEK_KEYS, inverse: true, axisLabel: { color: c.muted } },
        visualMap: {
          min: 0, max, calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
          inRange: { color: ['#fff3e8', c.lighter, c.primary] }, textStyle: { color: c.muted }
        },
        series: [{
          type: 'heatmap', data: this.weekHour,
          itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 3 }
        }]
      });
    },

    // 分桶分布（时长 / 轮数）：带渐变的柱状图
    renderBuckets(refName, buckets, colorKey) {
      const chart = this.initChart(refName);
      if (!chart) return;
      const c = this.themeColors();
      const base = c[colorKey] || c.primary;
      chart.setOption({
        tooltip: Object.assign(this.baseTooltip(), { axisPointer: { type: 'shadow' } }),
        grid: { left: 12, right: 12, bottom: 8, top: 24, containLabel: true },
        xAxis: { type: 'category', data: buckets.map((b) => b.label), axisLabel: { color: c.muted }, axisLine: { lineStyle: { color: c.line } } },
        yAxis: { type: 'value', minInterval: 1, axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        series: [{
          name: this.$t('chatAnalytics.sessions'), type: 'bar', barMaxWidth: 36,
          itemStyle: {
            borderRadius: [5, 5, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: base }, { offset: 1, color: c.lighter }
            ])
          },
          label: { show: true, position: 'top', color: c.muted },
          data: buckets.map((b) => b.count)
        }]
      });
    },

    // 有效时长 vs 等待时长：最长会话的堆叠条形图
    renderActiveWait() {
      const chart = this.initChart('activeWaitChart');
      if (!chart) return;
      const c = this.themeColors();
      const list = this.topSessions;
      chart.setOption({
        color: [c.primary, c.lighter],
        tooltip: Object.assign(this.baseTooltip(), {
          axisPointer: { type: 'shadow' },
          formatter: (params) => {
            const idx = params[0].dataIndex;
            const item = list[idx] || {};
            const lines = params.map((p) => `${p.marker}${p.seriesName}：${this.formatDuration(p.value)}`);
            return `${item.title || ''}<br/>${item.deviceName || ''} · ${this.$t('chatAnalytics.rounds')} ${item.rounds || 0}<br/>${lines.join('<br/>')}`;
          }
        }),
        legend: { data: [this.$t('chatAnalytics.activeSeries'), this.$t('chatAnalytics.waitSeries')], textStyle: { color: c.muted }, right: 12, top: 0 },
        grid: { left: 12, right: 16, bottom: 8, top: 40, containLabel: true },
        xAxis: {
          type: 'category', data: list.map((s) => s.title),
          axisLabel: { color: c.muted, interval: 0, rotate: 25, width: 80, overflow: 'truncate' },
          axisLine: { lineStyle: { color: c.line } }
        },
        yAxis: { type: 'value', name: this.$t('chatAnalytics.seconds'), axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        series: [
          { name: this.$t('chatAnalytics.activeSeries'), type: 'bar', stack: 'total', barMaxWidth: 32, data: list.map((s) => s.activeSeconds) },
          { name: this.$t('chatAnalytics.waitSeries'), type: 'bar', stack: 'total', barMaxWidth: 32, itemStyle: { borderRadius: [4, 4, 0, 0] }, data: list.map((s) => s.waitSeconds) }
        ]
      });
    },

    // 有效占比：仪表盘
    renderRatio() {
      const chart = this.initChart('ratioChart');
      if (!chart) return;
      const c = this.themeColors();
      const total = (this.summary.activeSeconds || 0) + (this.summary.waitSeconds || 0);
      const percent = total > 0 ? Math.round((this.summary.activeSeconds || 0) * 1000 / total) / 10 : 0;
      chart.setOption({
        series: [{
          type: 'gauge', startAngle: 200, endAngle: -20, min: 0, max: 100, radius: '88%',
          progress: { show: true, width: 16, itemStyle: { color: c.primary } },
          axisLine: { lineStyle: { width: 16, color: [[1, c.line]] } },
          pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false },
          axisLabel: { show: false },
          anchor: { show: false },
          title: { show: true, offsetCenter: [0, '42%'], color: c.muted, fontSize: 13 },
          detail: { valueAnimation: true, offsetCenter: [0, '8%'], fontSize: 34, color: c.text, formatter: '{value}%' },
          data: [{ value: percent, name: this.$t('chatAnalytics.activeSeries') }]
        }]
      });
    },

    // 设备对比雷达图：取会话最多的前 4 台
    renderRadar() {
      const chart = this.initChart('radarChart');
      if (!chart) return;
      const c = this.themeColors();
      const top = this.devices.slice(0, 4);
      const dims = [
        { key: 'sessions', name: this.$t('chatAnalytics.sessions') },
        { key: 'messages', name: this.$t('chatAnalytics.messages') },
        { key: 'activeSeconds', name: this.$t('chatAnalytics.activeSeries') },
        { key: 'waitSeconds', name: this.$t('chatAnalytics.waitSeries') }
      ];
      const indicator = dims.map((d) => ({
        name: d.name,
        max: Math.max(1, ...top.map((t) => t[d.key] || 0)) * 1.15
      }));
      chart.setOption({
        color: c.palette,
        tooltip: this.baseTooltip('item'),
        legend: { type: 'scroll', bottom: 0, textStyle: { color: c.muted } },
        radar: { indicator, radius: '62%', center: ['50%', '45%'], axisName: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        series: [{
          type: 'radar',
          areaStyle: { opacity: 0.12 },
          data: top.map((t) => ({ name: t.name || t.macAddress, value: dims.map((d) => t[d.key] || 0) }))
        }]
      });
    },

    // 设备消息量矩形树图
    renderTree() {
      const chart = this.initChart('treeChart');
      if (!chart) return;
      const c = this.themeColors();
      chart.setOption({
        color: c.palette,
        tooltip: Object.assign(this.baseTooltip('item'), { formatter: '{b}<br/>{c}' }),
        series: [{
          type: 'treemap', roam: false, nodeClick: false, breadcrumb: { show: false },
          width: '100%', height: '100%',
          label: { show: true, formatter: '{b}\n{c}', color: '#fff', fontSize: 12 },
          itemStyle: { borderColor: '#fff', borderWidth: 2, gapWidth: 2, borderRadius: 4 },
          data: this.devices.map((d) => ({ name: d.name || d.macAddress, value: d.messages }))
        }]
      });
    },

    // 轮数 x 总跨度散点图
    renderScatter() {
      const chart = this.initChart('scatterChart');
      if (!chart) return;
      const c = this.themeColors();
      chart.setOption({
        color: [c.primary],
        tooltip: Object.assign(this.baseTooltip('item'), {
          formatter: (p) => `${p.data[2] || ''}<br/>${this.$t('chatAnalytics.rounds')}：${p.data[0]}<br/>${this.formatDuration(p.data[1])}`
        }),
        grid: { left: 12, right: 20, bottom: 24, top: 16, containLabel: true },
        xAxis: { type: 'value', name: this.$t('chatAnalytics.rounds'), minInterval: 1, axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        yAxis: { type: 'value', name: this.$t('chatAnalytics.spanSeconds'), axisLabel: { color: c.muted }, splitLine: { lineStyle: { color: c.line } } },
        series: [{
          type: 'scatter', symbolSize: 11, itemStyle: { opacity: 0.7 },
          data: this.scatter.map((s) => [s.rounds, s.spanSeconds, s.title])
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
.control-time { width: 340px; }

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

.range-text {
  margin-left: 12px;
  font-size: 12px;
  color: #8a93b8;
  i { margin-right: 4px; }
}

.dashboard-alert {
  margin: 0 0 16px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 14px;
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

.coverage-hint {
  font-size: 12px;
  color: #8a93b8;
  margin-bottom: 18px;
  i { margin-right: 4px; }
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
  height: 280px;
}

.chart-box-tall {
  height: 340px;
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
