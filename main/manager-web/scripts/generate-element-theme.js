/**
 * 生成 Element UI 2.x 的「主色变量化」主题样式。
 *
 * 背景：Element UI 2.x 发布的是预编译 theme-chalk/index.css，主色 #409EFF
 * 以字面量散落 142 处，不支持 CSS 变量。本项目改为引入生成后的 CSS，
 * 把 primary 色板替换成 --sys-* 变量，运行时即可随 public/theme.js 换色。
 *
 * 用法：node scripts/generate-element-theme.js
 * 时机：仅在升级 element-ui 版本后需要重新执行；产物已提交到仓库。
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SOURCE = path.join(ROOT, 'node_modules/element-ui/lib/theme-chalk/index.css');
const OUTPUT = path.join(ROOT, 'src/styles/element-ui.theme.css');

// primary 色板 -> 语义变量（与 src/utils/theme.js 的 buildPalette 对应）
const COLOR_MAP = {
  '#409eff': 'var(--sys-primary)',
  '#66b1ff': 'var(--sys-primary-hover)',
  '#53a8ff': 'var(--sys-primary-hover)',
  '#3a8ee6': 'var(--sys-primary-active)',
  '#8cc5ff': 'var(--sys-primary-light)',
  '#a0cfff': 'var(--sys-primary-lighter)',
  '#b3d8ff': 'var(--sys-primary-lighter)',
  '#c6e2ff': 'var(--sys-primary-border)',
  '#d9ecff': 'var(--sys-primary-lighter)',
  '#ecf5ff': 'var(--sys-primary-bg-soft)',
  '#e6f1fe': 'var(--sys-primary-bg-soft)'
};

if (!fs.existsSync(SOURCE)) {
  console.error('[theme] 未找到 element-ui 样式，请先执行 npm install:', SOURCE);
  process.exit(1);
}

let css = fs.readFileSync(SOURCE, 'utf8');
const stats = {};

Object.entries(COLOR_MAP).forEach(([color, variable]) => {
  // 大小写不敏感；负向断言避免误伤 8 位 hex（如 #409eff00）
  const re = new RegExp(color.replace('#', '#') + '(?![0-9a-fA-F])', 'gi');
  css = css.replace(re, () => {
    stats[color] = (stats[color] || 0) + 1;
    return variable;
  });
});

// primary 的半透明高亮（el-upload dragover 等）
const rgbaRe = /rgba\(\s*32\s*,\s*159\s*,\s*255\s*,\s*\.?0?\.06\s*\)/gi;
css = css.replace(rgbaRe, () => {
  stats['rgba(32,159,255,.06)'] = (stats['rgba(32,159,255,.06)'] || 0) + 1;
  return 'var(--sys-primary-glow-soft)';
});

// CSS 从 node_modules 复制到 src/styles 后，@font-face 里的相对字体路径会失效，
// 需改写成 css-loader 可解析的路径（从 src/styles/ 回到 node_modules 的 theme-chalk/fonts）
css = css.replace(
  /url\(fonts\/(element-icons\.(?:woff|ttf))\)/g,
  'url(../../node_modules/element-ui/lib/theme-chalk/fonts/$1)'
);

const header = `/*!
 * Element UI 2.15.14 theme-chalk —— 主色变量化版本（自动生成，请勿手工编辑）
 * 来源: node_modules/element-ui/lib/theme-chalk/index.css
 * 生成: node scripts/generate-element-theme.js
 * 说明: primary 色板已替换为 --sys-* CSS 变量，由 src/utils/theme.js 在运行时注入；
 *       中性灰阶（#C0C4CC / #F2F6FC 等）保持不变。
 */
`;

fs.writeFileSync(OUTPUT, header + css, 'utf8');

console.log('[theme] 已生成', path.relative(ROOT, OUTPUT));
Object.entries(stats).forEach(([color, count]) => {
  console.log(`  ${color.padEnd(24)} ${String(count).padStart(4)} 处 -> ${COLOR_MAP[color] || 'var(--sys-primary-glow-soft)'}`);
});
const total = Object.values(stats).reduce((a, b) => a + b, 0);
console.log(`  合计 ${total} 处主色替换`);
