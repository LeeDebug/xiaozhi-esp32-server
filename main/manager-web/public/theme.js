/**
 * ============================================================
 * 全站系统色配置 —— 唯一需要修改的文件
 * ============================================================
 *
 * 本文件是静态资源，不参与打包：
 *   - 开发时：manager-web/public/theme.js
 *   - 部署后：dist/theme.js（或 Nginx 静态目录里的 theme.js）
 *
 * 修改 primary 后「刷新浏览器」即可生效，无需重新 npm run build，
 * 也无需改动任何 .vue 组件或重启后端服务。
 *
 * 配置优先级（高 -> 低）：
 *   1. 后台下发：/user/pub-config 返回 themeColor 字段（见 src/store/index.js）
 *   2. 本文件的 window.__XIAOZHI_THEME__.primary
 *   3. 内置默认 #eb6800
 *
 * 控制台可执行 setThemeColor('#1677ff') 临时预览其它颜色（刷新后失效）。
 * 色值支持 3 位或 6 位十六进制，例如 '#eb6800'、'#f80'。
 */
window.__XIAOZHI_THEME__ = {
  primary: '#eb6800'
};
