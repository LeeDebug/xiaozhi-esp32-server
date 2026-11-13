/**
 * 全站主题引擎（manager-web）
 *
 * 系统色只有一条链路：
 *   1. 静态配置：public/theme.js -> window.__XIAOZHI_THEME__.primary
 *      （打包后为 dist/theme.js，直接编辑即可换色，无需重新 build）
 *   2. 后台下发（可选）：/user/pub-config 返回 themeColor 时，
 *      store 的 setPubConfig 会调用 applyTheme 覆盖静态配置
 *   3. 控制台临时调试：window.setThemeColor('#xxxxxx')（刷新后失效）
 *
 * 实现：把主色派生出的整套色板写成 --sys-* CSS 自定义属性注入 :root。
 * 组件样式与 Element UI 主题 CSS 都只引用变量，所以换色是运行时行为。
 */

export const DEFAULT_PRIMARY = '#eb6800';

const HEX_RE = /^#?([0-9a-f]{3}|[0-9a-f]{6})$/i;

function normalizeHex(input) {
  if (typeof input !== 'string') return null;
  const value = input.trim();
  if (!HEX_RE.test(value)) return null;
  let hex = value.replace('#', '');
  if (hex.length === 3) {
    hex = hex.split('').map((c) => c + c).join('');
  }
  return '#' + hex.toLowerCase();
}

function toRgb(hex) {
  const h = hex.slice(1);
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16)
  ];
}

function toHex(rgb) {
  return (
    '#' +
    rgb
      .map((c) => Math.max(0, Math.min(255, Math.round(c))).toString(16).padStart(2, '0'))
      .join('')
  );
}

const mixWhite = (rgb, weight) => rgb.map((c) => c + (255 - c) * weight);
const mixBlack = (rgb, weight) => rgb.map((c) => c * (1 - weight));
const mix = (a, b, weight) => a.map((c, i) => c * (1 - weight) + b[i] * weight);

/**
 * 由任意主色生成完整语义色板。
 * 各层级对应原蓝色系统里的：主按钮 / hover / 按下 / 渐变深端 /
 * 浅底 / 边框 / 文字 / 滚动条 / 渐变另一端。
 */
export function buildPalette(primary) {
  const hex = normalizeHex(primary) || DEFAULT_PRIMARY;
  const rgb = toRgb(hex);
  const rgbText = rgb.join(', ');
  // 暖色锚点：让任意主色的渐变都保留明暗层次（不再出现蓝->紫的突兀跳色）
  const warmAnchor = [255, 210, 138];

  return {
    '--sys-primary': hex,
    '--sys-primary-rgb': rgbText,
    '--sys-primary-hover': toHex(mixWhite(rgb, 0.18)),
    '--sys-primary-active': toHex(mixBlack(rgb, 0.12)),
    '--sys-primary-deep': toHex(mixBlack(rgb, 0.22)),
    '--sys-primary-light': toHex(mixWhite(rgb, 0.38)),
    '--sys-primary-lighter': toHex(mixWhite(rgb, 0.62)),
    '--sys-primary-border': toHex(mixWhite(rgb, 0.72)),
    '--sys-primary-bg': toHex(mixWhite(rgb, 0.9)),
    '--sys-primary-bg-soft': toHex(mixWhite(rgb, 0.95)),
    '--sys-surface': toHex(mixWhite(rgb, 0.92)),
    '--sys-surface-soft': toHex(mixWhite(rgb, 0.85)),
    // 白底上的小字主色，压暗一档以满足可读性（#eb6800 对白底约 3.22:1）
    '--sys-primary-text': toHex(mixBlack(rgb, 0.2)),
    '--sys-primary-glow': `rgba(${rgbText}, 0.12)`,
    '--sys-primary-glow-soft': `rgba(${rgbText}, 0.06)`,
    '--sys-gradient-from': hex,
    '--sys-gradient-to': toHex(mix(rgb, warmAnchor, 0.42)),
    '--sys-scrollbar-thumb': toHex(mixWhite(rgb, 0.45)),
    '--sys-scrollbar-track': toHex(mixWhite(rgb, 0.94))
  };
}

let activePrimary = DEFAULT_PRIMARY;

/** 应用主色；传入非法值会回退到默认主色 */
export function applyTheme(color) {
  const hex = normalizeHex(color) || DEFAULT_PRIMARY;
  activePrimary = hex;
  if (typeof document === 'undefined') return hex;

  const root = document.documentElement;
  const palette = buildPalette(hex);
  Object.keys(palette).forEach((name) => root.style.setProperty(name, palette[name]));

  // 同步移动端浏览器地址栏 / PWA 主题色
  let meta = document.querySelector('meta[name="theme-color"]');
  if (!meta) {
    meta = document.createElement('meta');
    meta.setAttribute('name', 'theme-color');
    document.head.appendChild(meta);
  }
  meta.setAttribute('content', hex);
  return hex;
}

/** 应用启动时调用一次，读取 public/theme.js 的配置 */
export function initTheme() {
  const config =
    (typeof window !== 'undefined' && window.__XIAOZHI_THEME__) || {};
  const applied = applyTheme(config.primary || DEFAULT_PRIMARY);
  if (typeof window !== 'undefined') {
    // 便于临时预览：控制台执行 setThemeColor('#ff8c00') 即可，不写盘
    window.setThemeColor = applyTheme;
  }
  return applied;
}

/** 供后台公共配置覆盖主题（配置值优先于静态 theme.js） */
export function applyThemeFromConfig(config) {
  if (!config) return null;
  const value = config.themeColor || config.theme_color || config.primaryColor;
  if (!value || !normalizeHex(value)) return null;
  return applyTheme(value);
}

export function getActivePrimary() {
  return activePrimary;
}

/**
 * 读取某个主题令牌当前的计算值。
 * 用于 canvas fillStyle、SVG fill、组件 prop 颜色等「必须拿到真实色值」的场景；
 * 纯 CSS 场景请直接用 var(--sys-*)。
 */
export function getThemeToken(name) {
  if (typeof window === 'undefined' || typeof document === 'undefined') return '';
  return window.getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
}
