<template>
  <div class="device-geo-map" :class="{ 'is-fill': fill }">
    <div class="map-head">
      <div class="map-title">
        <span class="title-text">{{ $t('deviceGeo.title') }}</span>
        <span class="total-chip">{{ $t('deviceGeo.total', { count: devices.length }) }}</span>
        <span v-if="unlocatedCount > 0" class="unlocated-chip" :title="$t('deviceGeo.unlocatedTip')">
          {{ $t('deviceGeo.unlocated', { count: unlocatedCount }) }}
        </span>
      </div>
      <div class="map-crumbs">
        <span class="crumb" :class="{ active: !hasSel }" @click="goTo(0)">{{ $t('deviceGeo.country') }}</span>
        <template v-for="(crumb, index) in path">
          <i :key="'s' + index" class="el-icon-arrow-right crumb-sep"></i>
          <span :key="'c' + index" class="crumb" :class="{ active: index === path.length - 1 }"
            @click="goTo(index + 1)">{{ crumb.name }}</span>
        </template>
        <el-button v-if="hasSel" type="text" size="mini" class="reset-btn" @click="goTo(0)">
          {{ $t('deviceGeo.reset') }}
        </el-button>
      </div>
    </div>
    <div class="map-body">
      <div ref="chart" class="map-canvas" v-loading="mapLoading"></div>
      <div class="map-side">
        <div class="side-title">{{ sideTitle }}</div>
        <ul v-if="rankList.length" class="rank-list">
          <li v-for="item in rankList" :key="item.name" class="rank-item" :class="{ selected: item.selected }"
            @click="selectFeature(item.name)">
            <span class="rank-name" :title="item.name">{{ item.name }}</span>
            <span class="rank-bar"><i :style="{ width: item.percent + '%' }"></i></span>
            <span class="rank-count">{{ item.count }}</span>
          </li>
        </ul>
        <div v-else class="rank-empty">{{ $t('deviceGeo.empty') }}</div>
      </div>
    </div>
    <div v-if="loadError" class="map-error">{{ loadError }}</div>
  </div>
</template>

<script>
import * as echarts from 'echarts/core';
import { MapChart } from 'echarts/charts';
import { TooltipComponent, VisualMapComponent, GeoComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { countByLevel, countFor, countUnlocated, hasSelection, isSameName } from '@/utils/deviceRegion.mjs';
import { COUNTRY_CODE as COUNTRY, MUNICIPALITY_CODES, loadGeo } from '@/utils/geoLoader';

echarts.use([MapChart, TooltipComponent, VisualMapComponent, GeoComponent, CanvasRenderer]);

// ip2region 给直辖市的是“北京市|北京市”，所以选中直辖市时 city 也要填同名，筛选才能命中。

/**
 * 给定面包屑最后一项，返回“应该展示的地图”：
 * - 空路径：全国，各项是省
 * - 省：省级文件，各项是市（直辖市则是区县）
 * - 市：市级文件，各项是区县
 * - 区县：没有更下级，继续展示其所在市的区县地图
 */
function mapFor(path) {
  const last = path[path.length - 1];
  if (!last) return { code: COUNTRY, level: 'province' };
  if (last.level === 'province') {
    return MUNICIPALITY_CODES.has(last.adcode)
      ? { code: last.adcode, level: 'district' }
      : { code: last.adcode, level: 'city' };
  }
  if (last.level === 'city') {
    // 直辖市的虚拟“市”节点和省共用同一张地图
    return { code: last.adcode, level: 'district' };
  }
  const city = path[path.length - 2];
  return { code: city.adcode, level: 'district' };
}

export default {
  name: 'DeviceGeoMap',
  props: {
    // 全部设备（未经搜索过滤），地图统计不随搜索框变化
    devices: { type: Array, default: () => [] },
    // 当前选中 { province, city, district }，由父组件持有（唯一的选中来源）
    value: { type: Object, default: () => ({}) },
    // 占满父容器高度（独立分析页使用）；默认为固定高度的紧凑模式
    fill: { type: Boolean, default: false },
  },
  data() {
    return {
      chart: null,
      mapLoading: false,
      loadError: '',
      // 当前展示的地图：{ code, level }
      current: { code: COUNTRY, level: 'province' },
      // 当前地图里各要素的元信息：[{ name, adcode, hasChildren }]
      features: [],
      // 选中路径，每级一项：[{ level, name, adcode }]。只保存 adcode，选中值仍以 value 为准
      path: [],
      loadToken: 0,
      resizeHandler: null,
    };
  },
  computed: {
    hasSel() {
      return hasSelection(this.value);
    },
    // 当前展示的是全国地图（还没选中任何省）
    isNational() {
      return this.current.code === COUNTRY;
    },
    // 全国层级放大到 110%，省/市/区保持 100%。
    // 实测 100% 时全国地图已占画布高度的 96%，120% 会把南北各切掉约 50-60px，所以取 110%
    layoutSize() {
      return this.isNational ? '130%' : '100%';
    },
    // 只有全国层级把地图往左下挪一点，省/市/区仍居中。
    // 实测：左移不会裁切（左右宽裕），下移 2% 会让底部多裁约 13px（南海诸岛一带），可拖动查看
    layoutCenter() {
      return this.isNational ? ['40%', '65%'] : ['50%', '45%'];
    },
    unlocatedCount() {
      return countUnlocated(this.devices);
    },
    // 当前层级的统计，受上级选择限定
    counts() {
      return countByLevel(this.devices, this.current.level, this.parentSelection);
    },
    parentSelection() {
      const sel = {};
      const order = ['province', 'city', 'district'];
      for (const level of order) {
        if (level === this.current.level) break;
        if (this.value[level]) sel[level] = this.value[level];
      }
      return sel;
    },
    sideTitle() {
      const key = { province: 'deviceGeo.byProvince', city: 'deviceGeo.byCity', district: 'deviceGeo.byDistrict' }[
        this.current.level
      ];
      return this.$t(key);
    },
    rankList() {
      const level = this.current.level;
      const list = this.features
        .map((f) => ({
          name: f.name,
          count: countFor(this.counts, f.name, level),
          selected: !!this.value[level] && isSameName(level, f.name, this.value[level]),
        }))
        .filter((f) => f.count > 0)
        .sort((a, b) => b.count - a.count);
      const max = list.length ? list[0].count : 1;
      return list.map((f) => ({ ...f, percent: Math.max(6, Math.round((f.count / max) * 100)) }));
    },
  },
  watch: {
    devices() {
      this.render();
    },
    // 选中被外部清空（例如父组件的“清除筛选”）时，地图回到全国
    value: {
      deep: true,
      handler(sel) {
        if (!hasSelection(sel) && this.path.length) {
          this.path = [];
          this.showMap();
        } else {
          this.render();
        }
      },
    },
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chart);
    this.chart.on('click', (params) => {
      if (params && params.name) this.selectFeature(params.name);
    });
    this.resizeHandler = () => this.chart && this.chart.resize();
    window.addEventListener('resize', this.resizeHandler);
    // 占满模式下容器高度由 flex 布局决定，挂载时可能还没算完，监听容器尺寸变化才不会画错大小
    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(this.resizeHandler);
      this.resizeObserver.observe(this.$refs.chart);
    }
    this.showMap();
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeHandler);
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
      this.resizeObserver = null;
    }
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  },
  methods: {
    selectionFromPath(path) {
      const sel = {};
      path.forEach((crumb) => {
        sel[crumb.level] = crumb.name;
      });
      return sel;
    },

    emitSelection(path) {
      const sel = this.selectionFromPath(path);
      this.$emit('input', sel);
      this.$emit('change', sel);
    },

    /**
     * 按 this.path 载入并展示对应的地图。连点时只采用最后一次请求。
     * 加载失败时保留当前显示的地图并把选中回退到那张地图对应的路径。
     */
    async showMap() {
      const token = ++this.loadToken;
      const target = mapFor(this.path);
      this.mapLoading = true;
      this.loadError = '';
      try {
        const geo = await loadGeo(target.code);
        if (token !== this.loadToken || !this.chart) return;
        echarts.registerMap(`geo-${target.code}`, geo);
        this.current = target;
        this.features = geo.features
          .filter((f) => f.properties && f.properties.name)
          .map((f) => ({
            name: f.properties.name,
            adcode: String(f.properties.adcode || ''),
            // n 是子节点数；区县层级没有更下级
            hasChildren: target.level !== 'district' && Number(f.properties.n || 0) > 0,
          }));
        this.render();
      } catch (e) {
        if (token !== this.loadToken) return;
        this.loadError = this.$t('deviceGeo.loadFailed', { code: target.code });
        // 回退：去掉最后一级选中，避免列表筛选与地图显示不一致
        if (this.path.length) {
          this.path = this.path.slice(0, -1);
          this.emitSelection(this.path);
        }
      } finally {
        if (token === this.loadToken) this.mapLoading = false;
      }
    },

    // 点击地图或右侧排行里的某个地区
    selectFeature(name) {
      const feature = this.features.find((f) => f.name === name);
      if (!feature) return;
      const level = this.current.level;
      let path;
      if (level === 'province') {
        path = [{ level: 'province', name: feature.name, adcode: feature.adcode }];
        // 直辖市：同时把“市”填成同名，设备的 city 就是“北京市”
        if (MUNICIPALITY_CODES.has(feature.adcode)) {
          path.push({ level: 'city', name: feature.name, adcode: feature.adcode });
        }
      } else if (level === 'city') {
        path = [...this.path.slice(0, 1), { level: 'city', name: feature.name, adcode: feature.adcode }];
      } else {
        // 区县：保留已选的省/市，只替换或追加区县
        const upper = this.path.filter((crumb) => crumb.level !== 'district');
        path = [...upper, { level: 'district', name: feature.name, adcode: feature.adcode }];
      }
      this.path = path;
      this.emitSelection(path);
      this.showMap();
    },

    /**
     * 面包屑：回到第 n 级（0=全国）。
     * 直辖市的“省”与虚拟“市”共用一张地图，点“省”时一并保留两级，否则设备的 city 字段没法匹配。
     */
    goTo(n) {
      let keep = this.path.slice(0, n);
      const last = keep[keep.length - 1];
      if (last && last.level === 'province' && MUNICIPALITY_CODES.has(last.adcode)) {
        keep = [last, { level: 'city', name: last.name, adcode: last.adcode }];
      }
      this.path = keep;
      this.emitSelection(keep);
      this.showMap();
    },

    render() {
      if (!this.chart || !this.features.length) return;
      const level = this.current.level;
      const data = this.features.map((f) => ({
        name: f.name,
        value: countFor(this.counts, f.name, level),
        selected: !!this.value[level] && isSameName(level, f.name, this.value[level]),
      }));
      const maxValue = Math.max(1, ...data.map((d) => d.value));
      const css = getComputedStyle(document.documentElement);
      const primary = (css.getPropertyValue('--sys-primary') || '#eb6800').trim();
      this.chart.setOption(
        {
          tooltip: {
            trigger: 'item',
            formatter: (p) => `${p.name}<br/>${this.$t('deviceGeo.deviceCount')}: ${p.value || 0}`,
          },
          visualMap: {
            min: 0,
            max: maxValue,
            left: 12,
            bottom: 12,
            itemWidth: 12,
            itemHeight: 90,
            text: [String(maxValue), '0'],
            calculable: false,
            inRange: { color: ['#fdf1e6', '#f7c69e', primary] },
            textStyle: { color: '#8a93b8', fontSize: 11 },
          },
          series: [
            {
              type: 'map',
              map: `geo-${this.current.code}`,
              // 默认情况下地图只占画布的约 75% 并按 0.75 压扁，占满模式下要让地图铺满画布
              ...(this.fill
                ? { layoutCenter: this.layoutCenter, layoutSize: this.layoutSize, aspectScale: 0.85 }
                : {}),
              // 全国层级放大后上下会超出画布，允许拖动查看；只开拖动不开滚轮缩放，避免影响页面滚动。
              // center/zoom 每次显式复位，否则在全国拖过的位置会带到下一级地图
              roam: this.isNational ? 'move' : false,
              center: null,
              zoom: 1,
              selectedMode: 'single',
              label: { show: level !== 'province', fontSize: 10, color: '#2c3354' },
              itemStyle: { areaColor: '#f6f7fb', borderColor: '#ffffff', borderWidth: 0.8 },
              emphasis: {
                label: { color: '#2c3354' },
                itemStyle: { areaColor: '#f3a161' },
              },
              select: {
                label: { color: '#ffffff' },
                itemStyle: { areaColor: primary },
              },
              data,
            },
          ],
        },
        true,
      );
    },
  },
};
</script>

<style lang="scss" scoped>
.device-geo-map {
  margin-bottom: 14px;
  padding: 12px 16px;
  border: 1px solid #f3f1f1;
  border-radius: 8px;
  background: #fff;
}

.map-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  gap: 12px;
  flex-wrap: wrap;
}

.map-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 15px;
  font-weight: 500;
  color: #2c3354;
}

.total-chip,
.unlocated-chip {
  padding: 1px 8px;
  font-size: 12px;
  border-radius: 10px;
}

.total-chip {
  color: var(--sys-primary);
  background: var(--sys-primary-bg-soft, #fdf1e6);
}

.unlocated-chip {
  color: #8a93b8;
  background: #f3f4f9;
  cursor: help;
}

.map-crumbs {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #8a93b8;
}

.crumb {
  cursor: pointer;

  &:hover {
    color: var(--sys-primary);
  }

  &.active {
    color: #2c3354;
    font-weight: 500;
  }
}

.crumb-sep {
  margin: 0 4px;
  font-size: 10px;
}

.reset-btn {
  margin-left: 10px;
  color: var(--sys-primary);
}

.map-body {
  display: flex;
  gap: 12px;
}

.map-canvas {
  flex: 1;
  min-width: 0;
  height: 280px;
}

// 占满模式：组件撑满父容器，地图占据标题栏之外的全部高度，右侧排行也跟着加高
.device-geo-map.is-fill {
  // 用 flex 链路而不是百分比高度：父元素是 flex 子项且没有显式高度时，
  // height: 100% 会退化为 auto，地图就停在 min-height 不会随屏幕变大
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  margin-bottom: 0;
  padding: 0;
  border: none;

  .map-body {
    flex: 1;
    min-height: 0;
  }

  .map-canvas {
    // 画布绝对定位铺满 flex 子项，高度完全由父级分配
    position: relative;
    height: auto;
    min-height: 420px;
  }

  .map-side {
    width: 300px;
    min-height: 0;
  }

  .rank-list {
    flex: 1;
    max-height: none;
  }
}

.map-side {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.side-title {
  margin-bottom: 6px;
  font-size: 12px;
  color: #8a93b8;
}

.rank-list {
  margin: 0;
  padding: 0;
  list-style: none;
  max-height: 250px;
  overflow-y: auto;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  font-size: 12px;
  color: #2c3354;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background: #f6f7fb;
  }

  &.selected {
    background: var(--sys-primary-bg-soft, #fdf1e6);
  }
}

.rank-name {
  width: 64px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-bar {
  flex: 1;
  height: 6px;
  background: #f3f4f9;
  border-radius: 3px;

  i {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: var(--sys-primary);
  }
}

.rank-count {
  width: 28px;
  text-align: right;
  color: #8a93b8;
}

.rank-empty {
  margin-top: 40px;
  text-align: center;
  font-size: 12px;
  color: #b4bad2;
}

.map-error {
  margin-top: 6px;
  font-size: 12px;
  color: #e8554e;
}
</style>
