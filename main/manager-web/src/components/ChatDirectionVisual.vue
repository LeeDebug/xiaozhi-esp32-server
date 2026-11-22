<template>
  <!-- 会话内容分析丨高频方向：AI 分析记录的可视化视图。数据来自大模型输出的 JSON，任何字段都可能为 null，渲染时一律容错 -->
  <div class="cdv">
    <div class="cdv-cards">
      <div class="cdv-card cdv-card-primary">
        <div class="cdv-label">{{ $t('chatDirection.visualTop') }}</div>
        <div class="cdv-value">{{ text(top.name) }}</div>
        <div class="cdv-sub">
          {{ $t('chatDirection.visualRatio') }}：{{ ratioText(top.ratio) }}
          <span v-if="num(top.count) !== null"> · {{ $t('chatDirection.visualCount') }}：{{ num(top.count) }}</span>
        </div>
      </div>
      <div class="cdv-card">
        <div class="cdv-label">{{ $t('chatDirection.visualTotal') }}</div>
        <div class="cdv-value">{{ num(data.totalQuestions) === null ? '-' : num(data.totalQuestions) }}</div>
      </div>
      <div class="cdv-card">
        <div class="cdv-label">{{ $t('chatDirection.visualDirectionCount') }}</div>
        <div class="cdv-value">{{ directions.length }}</div>
      </div>
      <div class="cdv-card">
        <div class="cdv-label">{{ $t('chatDirection.visualRegionCount') }}</div>
        <div class="cdv-value">{{ regions.length }}</div>
      </div>
    </div>

    <div v-if="data.summary" class="cdv-block">
      <div class="cdv-title">{{ $t('chatDirection.visualSummary') }}</div>
      <div class="cdv-text">{{ data.summary }}</div>
    </div>

    <div class="cdv-grid">
      <div class="cdv-block">
        <div class="cdv-title">{{ $t('chatDirection.visualPie') }}</div>
        <div v-if="pieData.length" ref="pie" class="cdv-chart"></div>
        <div v-else class="cdv-empty">{{ $t('chatDirection.visualNoData') }}</div>
      </div>
      <div class="cdv-block">
        <div class="cdv-title">{{ $t('chatDirection.visualBar') }}</div>
        <div v-if="barData.length" ref="bar" class="cdv-chart"></div>
        <div v-else class="cdv-empty">{{ $t('chatDirection.visualNoData') }}</div>
      </div>
    </div>

    <div class="cdv-block">
      <div class="cdv-title">{{ $t('chatDirection.visualRegion') }}</div>
      <div v-if="regionChart.regions.length && regionChart.series.length" ref="region" class="cdv-chart cdv-chart-tall"
        :style="{ height: Math.max(260, regionChart.regions.length * 42 + 80) + 'px' }"></div>
      <div v-else class="cdv-empty">{{ $t('chatDirection.visualNoData') }}</div>
      <el-table v-if="regions.length" :data="regions" size="mini" border class="cdv-table">
        <el-table-column :label="$t('chatDirection.province')" width="120">
          <template slot-scope="s">{{ text(s.row.name) }}</template>
        </el-table-column>
        <el-table-column :label="$t('chatDirection.colMessages')" width="90" align="center">
          <template slot-scope="s">{{ num(s.row.questionCount) === null ? '-' : num(s.row.questionCount) }}</template>
        </el-table-column>
        <el-table-column :label="$t('chatDirection.visualTop')" width="140">
          <template slot-scope="s">{{ text(s.row.topDirection) }}</template>
        </el-table-column>
        <el-table-column :label="$t('chatDirection.visualKeywords')" min-width="160">
          <template slot-scope="s">
            <el-tag v-for="(k, i) in list(s.row.keywords)" :key="i" size="mini" effect="plain" class="cdv-tag">{{ k }}</el-tag>
            <span v-if="!list(s.row.keywords).length">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('chatDirection.visualDifference')" min-width="200">
          <template slot-scope="s">{{ text(s.row.difference) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="directions.length" class="cdv-block">
      <div class="cdv-title">{{ $t('chatDirection.visualDirections') }}</div>
      <div v-for="(d, i) in directions" :key="i" class="cdv-direction">
        <div class="cdv-direction-head">
          <span class="cdv-rank">#{{ num(d.rank) === null ? i + 1 : num(d.rank) }}</span>
          <span class="cdv-direction-name">{{ text(d.name) }}</span>
          <span class="cdv-direction-ratio">{{ ratioText(d.ratio) }}</span>
        </div>
        <el-progress v-if="num(d.ratio) !== null" :percentage="clamp(num(d.ratio))" :show-text="false" :stroke-width="6" />
        <div v-if="d.description" class="cdv-desc">{{ d.description }}</div>
        <div v-if="list(d.keywords).length" class="cdv-line">
          <el-tag v-for="(k, j) in list(d.keywords)" :key="j" size="mini" effect="plain" class="cdv-tag">{{ k }}</el-tag>
        </div>
        <ul v-if="list(d.representativeQuestions).length" class="cdv-questions">
          <li v-for="(q, j) in list(d.representativeQuestions)" :key="j">{{ q }}</li>
        </ul>
      </div>
    </div>

    <div class="cdv-block">
      <div class="cdv-title">{{ $t('chatDirection.visualSuggestions') }}</div>
      <div v-if="!suggestions.length" class="cdv-empty">{{ $t('chatDirection.visualNoData') }}</div>
      <div v-for="(s, i) in suggestions" :key="i" class="cdv-suggestion">
        <div class="cdv-direction-head">
          <span class="cdv-rank">{{ num(s.rank) === null ? i + 1 : num(s.rank) }}</span>
          <span class="cdv-direction-name">{{ text(s.title) }}</span>
          <el-tag v-if="s.priority" size="mini" :type="priorityType(s.priority)">{{ s.priority }}</el-tag>
        </div>
        <div class="cdv-desc">{{ text(s.content) }}</div>
        <div v-if="s.targetDirection || s.targetRegion" class="cdv-meta">
          <span v-if="s.targetDirection">{{ $t('chatDirection.visualTargetDirection') }}：{{ s.targetDirection }}</span>
          <span v-if="s.targetRegion">{{ $t('chatDirection.visualTargetRegion') }}：{{ s.targetRegion }}</span>
        </div>
      </div>
      <div v-if="data.comprehensiveAdvice" class="cdv-advice">
        <b>{{ $t('chatDirection.visualAdvice') }}：</b>{{ data.comprehensiveAdvice }}
      </div>
    </div>

    <el-alert v-if="quality.notes || quality.sufficient === false" class="cdv-alert"
      :type="quality.sufficient === false ? 'warning' : 'info'" show-icon :closable="false"
      :title="$t('chatDirection.visualQuality') + '：' + text(quality.notes)" />
  </div>
</template>

<script>
import * as echarts from 'echarts';

const COLORS = ['#eb6800', '#5b8ff9', '#5ad8a6', '#f6bd16', '#e8684a', '#6dc8ec', '#9270ca', '#ff9d4d', '#269a99', '#ff99c3'];

function isObj(v) {
  return v !== null && typeof v === 'object' && !Array.isArray(v);
}

export default {
  name: 'ChatDirectionVisual',
  props: {
    visual: { type: Object, default: null }
  },
  data() {
    return { charts: [], resizeObserver: null };
  },
  computed: {
    data() {
      return isObj(this.visual) ? this.visual : {};
    },
    directions() {
      return this.list(this.data.directions).filter(isObj);
    },
    regions() {
      return this.list(this.data.regions).filter(isObj);
    },
    suggestions() {
      return this.list(this.data.suggestions).filter(isObj);
    },
    quality() {
      return isObj(this.data.dataQuality) ? this.data.dataQuality : {};
    },
    /** 优先用 topDirection，缺失时取 directions 的第一项 */
    top() {
      if (isObj(this.data.topDirection) && this.data.topDirection.name) return this.data.topDirection;
      return this.directions[0] || {};
    },
    pieData() {
      return this.directions
        .map((d) => ({ name: this.text(d.name), value: this.num(d.ratio) !== null ? this.num(d.ratio) : this.num(d.count) }))
        .filter((d) => d.value !== null && d.value > 0);
    },
    barData() {
      return this.directions
        .map((d) => ({ name: this.text(d.name), value: this.num(d.count) }))
        .filter((d) => d.value !== null);
    },
    /** 地区 × 方向 堆叠条形图：优先用占比，没有占比时用次数 */
    regionChart() {
      const regions = this.regions.filter((r) => this.list(r.directions).length);
      const useRatio = regions.some((r) => this.list(r.directions).some((d) => isObj(d) && this.num(d.ratio) !== null));
      const names = [];
      regions.forEach((r) => this.list(r.directions).forEach((d) => {
        if (isObj(d) && d.name && !names.includes(d.name)) names.push(d.name);
      }));
      const series = names.map((name) => ({
        name,
        type: 'bar',
        stack: 'total',
        emphasis: { focus: 'series' },
        data: regions.map((r) => {
          const hit = this.list(r.directions).find((d) => isObj(d) && d.name === name);
          if (!hit) return 0;
          const v = useRatio ? this.num(hit.ratio) : this.num(hit.count);
          return v === null ? 0 : v;
        })
      }));
      return { regions: regions.map((r) => this.text(r.name)), series, useRatio };
    }
  },
  watch: {
    visual() {
      this.$nextTick(this.render);
    }
  },
  mounted() {
    this.$nextTick(this.render);
    // 折叠面板展开动画期间容器宽度会变化，尺寸变化时重新计算图表大小
    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(() => this.resize());
      this.resizeObserver.observe(this.$el);
    }
    window.addEventListener('resize', this.resize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resize);
    if (this.resizeObserver) this.resizeObserver.disconnect();
    this.charts.forEach((c) => c.dispose());
    this.charts = [];
  },
  methods: {
    list(v) {
      return Array.isArray(v) ? v.filter((x) => x !== null && x !== undefined && x !== '') : [];
    },
    num(v) {
      if (v === null || v === undefined || v === '') return null;
      const n = typeof v === 'number' ? v : parseFloat(String(v).replace('%', ''));
      return Number.isFinite(n) ? n : null;
    },
    text(v) {
      return v === null || v === undefined || v === '' ? '-' : String(v);
    },
    clamp(v) {
      return Math.max(0, Math.min(100, Math.round(v * 10) / 10));
    },
    ratioText(v) {
      const n = this.num(v);
      return n === null ? '-' : `${n}%`;
    },
    priorityType(p) {
      if (p === '高' || /high/i.test(p)) return 'danger';
      if (p === '中' || /medium/i.test(p)) return 'warning';
      return 'info';
    },
    chart(ref) {
      const el = this.$refs[ref];
      if (!el) return null;
      let c = echarts.getInstanceByDom(el);
      if (!c) {
        c = echarts.init(el);
        this.charts.push(c);
      }
      return c;
    },
    render() {
      const pie = this.chart('pie');
      if (pie) {
        pie.setOption({
          color: COLORS,
          tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
          legend: { type: 'scroll', bottom: 0, textStyle: { color: '#606266' } },
          series: [{
            type: 'pie', radius: ['38%', '66%'], center: ['50%', '45%'],
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            label: { color: '#2c3354', formatter: '{b}\n{d}%' },
            data: this.pieData
          }]
        }, true);
      }
      const bar = this.chart('bar');
      if (bar) {
        const items = this.barData.slice().reverse();
        bar.setOption({
          color: COLORS,
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          grid: { left: 8, right: 36, top: 10, bottom: 10, containLabel: true },
          xAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#8a93b8' }, splitLine: { lineStyle: { color: '#f3f1f1' } } },
          yAxis: { type: 'category', data: items.map((d) => d.name), axisLabel: { color: '#2c3354', width: 110, overflow: 'truncate' } },
          series: [{
            type: 'bar', barMaxWidth: 22, label: { show: true, position: 'right', color: '#2c3354' },
            itemStyle: { borderRadius: [0, 4, 4, 0], color: COLORS[0] },
            data: items.map((d) => d.value)
          }]
        }, true);
      }
      const region = this.chart('region');
      if (region) {
        const unit = this.regionChart.useRatio ? '%' : '';
        region.setOption({
          color: COLORS,
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${v}${unit}` },
          legend: { type: 'scroll', top: 0, textStyle: { color: '#606266' } },
          grid: { left: 8, right: 20, top: 36, bottom: 10, containLabel: true },
          xAxis: { type: 'value', axisLabel: { color: '#8a93b8', formatter: `{value}${unit}` }, splitLine: { lineStyle: { color: '#f3f1f1' } } },
          yAxis: { type: 'category', data: this.regionChart.regions, axisLabel: { color: '#2c3354' } },
          series: this.regionChart.series
        }, true);
      }
      this.$nextTick(this.resize);
    },
    resize() {
      this.charts.forEach((c) => {
        if (!c.isDisposed()) c.resize();
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.cdv { padding: 4px 0; }
.cdv-cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.cdv-card { background: #f8f9fc; border: 1px solid #eceef6; border-radius: 10px; padding: 12px 14px; min-width: 0; }
.cdv-card-primary { background: linear-gradient(135deg, #fff3e8, #fff8f1); border-color: #fde3cc; }
.cdv-label { font-size: 12px; color: #8a93b8; margin-bottom: 6px; }
.cdv-value { font-size: 20px; font-weight: 600; color: #2c3354; line-height: 1.3; word-break: break-word; }
.cdv-sub { font-size: 12px; color: #606266; margin-top: 4px; }
.cdv-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.cdv-block { background: #fff; border: 1px solid #f3f1f1; border-radius: 10px; padding: 12px 14px; margin-bottom: 12px; min-width: 0; }
.cdv-title { font-size: 14px; font-weight: 600; color: #2c3354; margin-bottom: 10px; }
.cdv-text, .cdv-desc { font-size: 13px; line-height: 1.75; color: #2c3354; white-space: pre-wrap; word-break: break-word; }
.cdv-chart { width: 100%; height: 280px; }
.cdv-empty { font-size: 13px; color: #8a93b8; padding: 20px 0; text-align: center; }
.cdv-table { margin-top: 10px; }
.cdv-tag { margin: 2px 6px 2px 0; }
.cdv-direction, .cdv-suggestion { padding: 10px 0; border-top: 1px dashed #ebe8e8; }
.cdv-direction:first-of-type, .cdv-suggestion:first-of-type { border-top: none; padding-top: 0; }
.cdv-direction-head { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.cdv-rank { flex: none; min-width: 26px; font-weight: 700; color: var(--sys-primary, #eb6800); }
.cdv-direction-name { flex: 1; min-width: 0; font-size: 14px; font-weight: 600; color: #2c3354; }
.cdv-direction-ratio { flex: none; font-size: 13px; color: var(--sys-primary, #eb6800); }
.cdv-line { margin-top: 6px; }
.cdv-questions { margin: 6px 0 0; padding-left: 20px; font-size: 13px; line-height: 1.75; color: #606266; }
.cdv-meta { display: flex; flex-wrap: wrap; gap: 4px 16px; margin-top: 4px; font-size: 12px; color: #8a93b8; }
.cdv-advice { margin-top: 10px; padding: 10px 12px; background: #faf9f8; border-radius: 8px; font-size: 13px; line-height: 1.75; color: #2c3354; white-space: pre-wrap; }
.cdv-alert { margin-bottom: 4px; }
</style>
