/**
 * 一次性迁移工具：把组件里写死的「品牌蓝」替换为 --sys-* 主题令牌。
 *
 * 用法：node scripts/migrate-theme-colors.js
 * 幂等：替换后不再含原色值，可重复执行。
 *
 * 边界（刻意保守，避免误伤）：
 *   - 只处理 .vue 的 <style> 块、静态 style="..." 与 :style="..." 内联样式；
 *     不碰 <script> 里的图表 option / 业务常量。
 *   - 不处理 .scss 之外的 node_modules/dist，也不处理已经变量化的
 *     element-ui.theme.css 与 theme.scss（后者是橙色兜底值）。
 *   - 中性蓝灰文字色（#3d4566 / #7079aa / #8a93b8 等）保留原值：
 *     它们承担文字层级，不属于系统色，染成橙色会损失可读性。
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SRC = path.join(ROOT, 'src');

const GROUPS = {
  'var(--sys-primary)': [
    '5778ff', '409eff', '4a7cfd', '5f70f3', '1890ff', '1677ff', '3b82f6',
    '4167ed', '3765f8', '4998ff', '4d94f7', '467afe', '4a6ae8', '3d5cd6',
    '6b80eb', '2563eb', '5b8cff'
  ],
  'var(--sys-primary-deep)': ['2f5bff', '2a6dfe'],
  'var(--sys-primary-light)': ['6b8cff', '6a5cff'],
  'var(--sys-gradient-to)': ['8154fc', 'a966ff', 'b8a4fd', 'c8cafb'],
  'var(--sys-primary-border)': ['adbdff', 'a0b4fd', 'd0d8ff', 'd6dcfc', 'dfeafe'],
  'var(--sys-primary-lighter)': ['6baaff', '9ebbfc'],
  'var(--sys-surface)': [
    'eff4ff', 'e8f0ff', 'e2eeff', 'dfe7ff', 'eef4ff', 'e6f7ff', 'e6eff9',
    'e6f0ff', 'e6f0fe', 'ebf1fe', 'cfe1fb', 'edeafe', 'e8ecfb', 'f5f8fd'
  ],
  'var(--sys-surface-soft)': ['e6ebff'],
  'var(--sys-primary-bg-soft)': ['f5f8fd'],
  'var(--sys-scrollbar-thumb)': ['a1c9fd'],
  'var(--sys-scrollbar-track)': ['f0f3fe']
};

const MAP = new Map();
Object.entries(GROUPS).forEach(([variable, colors]) => {
  colors.forEach((c) => {
    if (!MAP.has('#' + c)) MAP.set('#' + c, variable);
  });
});

// color: 属性上的主色文字改用压暗一档的 --sys-primary-text（提升白底可读性）
const TEXT_COLORS = new Set([
  '#5778ff', '#409eff', '#4a7cfd', '#5f70f3', '#1890ff', '#1677ff',
  '#3b82f6', '#6b8cff', '#2f5bff'
]);

// 项目里出现过的品牌蓝 rgba/rgb 形式，统一换成基于主色的半透明
const RGB_PRIMARY = new Set([
  '95,112,243', '59,130,246', '47,91,255', '74,124,253', '107,140,255', '87,120,255'
]);

function rgbToHls(r, g, b) {
  const rn = r / 255, gn = g / 255, bn = b / 255;
  const max = Math.max(rn, gn, bn), min = Math.min(rn, gn, bn);
  const l = (max + min) / 2;
  const d = max - min;
  let h = 0, s = 0;
  if (d !== 0) {
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    if (max === rn) h = ((gn - bn) / d + (gn < bn ? 6 : 0)) / 6;
    else if (max === gn) h = ((bn - rn) / d + 2) / 6;
    else h = ((rn - gn) / d + 4) / 6;
  }
  return [h, l, s];
}

/**
 * 兜底分类：处理第一轮枚举之外的零散蓝色（浅底、辅助蓝、渐变端点）。
 * 低饱和的蓝灰文字色（#3d4566 / #7079aa / #8a93b8 等）原样保留。
 */
function classifyBlue(hex) {
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  const [h, l, s] = rgbToHls(r, g, b);
  if (h < 0.53 || h > 0.80) return null;      // 不是蓝/紫系
  if (b - r < 3) return null;                  // 近中性灰（极浅蓝底仍要收敛为暖白）
  if (s < 0.45 && l < 0.92) return null;      // 中性蓝灰文字，保留
  if (l >= 0.90) return 'var(--sys-primary-bg-soft)';
  if (l >= 0.80) return 'var(--sys-primary-border)';
  if (l >= 0.65) return 'var(--sys-primary-lighter)';
  if (h > 0.71) return 'var(--sys-gradient-to)';
  if (l < 0.45) return 'var(--sys-primary-deep)';
  return 'var(--sys-primary)';
}

function replaceFragment(fragment) {
  let out = fragment;

  // 1) color: #主色 -> color: var(--sys-primary-text)
  //    (?<![-\w]) 排除 background-color / border-color / -webkit-text-fill-color
  out = out.replace(
    /(?<![-\w])color(\s*:\s*)#([0-9a-fA-F]{6})\b/g,
    (match, sep, hex) => {
      const key = '#' + hex.toLowerCase();
      return TEXT_COLORS.has(key) ? 'color' + sep + 'var(--sys-primary-text)' : match;
    }
  );

  // 2) 其余 hex -> 语义变量（显式映射优先，其次 HSL 兜底分类）
  out = out.replace(/#([0-9a-fA-F]{6})\b/g, (match, hex) => {
    return MAP.get('#' + hex.toLowerCase()) || classifyBlue(hex.toLowerCase()) || match;
  });

  // 3) rgba/rgb 品牌蓝 -> rgba(var(--sys-primary-rgb), alpha)
  out = out.replace(
    /\brgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+)\s*)?\)/g,
    (match, r, g, b, alpha) => {
      const key = [r, g, b].join(',');
      if (!RGB_PRIMARY.has(key)) return match;
      return alpha === undefined
        ? 'rgb(var(--sys-primary-rgb))'
        : `rgba(var(--sys-primary-rgb), ${alpha})`;
    }
  );

  return out;
}

function processVue(text) {
  return text
    .split(/(<style[^>]*>[\s\S]*?<\/style>)/g)
    .map((part) => {
      if (/^<style/.test(part)) {
        return part.replace(
          /(<style[^>]*>)([\s\S]*?)(<\/style>)/,
          (m, open, body, close) => open + replaceFragment(body) + close
        );
      }
      // template 中的静态 style 与 :style 绑定（字符串里的 CSS 值同样支持 var()）
      return part.replace(
        /(style\s*=\s*")([^"]*)(")/g,
        (m, open, body, close) => open + replaceFragment(body) + close
      );
    })
    .join('');
}

const changed = [];
const skipped = [
  path.join(SRC, 'styles/element-ui.theme.css'),
  path.join(SRC, 'styles/theme.scss')
];

function walk(dir) {
  fs.readdirSync(dir, { withFileTypes: true }).forEach((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) return walk(full);
    const ext = path.extname(entry.name).toLowerCase();
    if (!['.vue', '.scss', '.css'].includes(ext)) return;
    if (skipped.includes(full)) return;

    const before = fs.readFileSync(full, 'utf8');
    const after = ext === '.vue' ? processVue(before) : replaceFragment(before);
    if (after !== before) {
      fs.writeFileSync(full, after, 'utf8');
      const count = (before.match(/#(?:[0-9a-fA-F]{6})\b/g) || []).length -
        (after.match(/#(?:[0-9a-fA-F]{6})\b/g) || []).length;
      changed.push({ file: path.relative(ROOT, full), removed: count });
    }
  });
}

walk(SRC);

console.log(`[theme] 已替换 ${changed.length} 个文件：`);
changed.forEach((c) => console.log(`  ${String(c.removed).padStart(4)} 处  ${c.file}`));
console.log(`  合计移除 ${changed.reduce((a, b) => a + b.removed, 0)} 处硬编码色值`);
