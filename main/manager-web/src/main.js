// 引入「主色变量化」的 Element UI 主题（由 scripts/generate-element-theme.js 生成）
// 不要改回 element-ui/lib/theme-chalk/index.css：官方包把 #409EFF 写死了，无法换色
import './styles/element-ui.theme.css';
import 'normalize.css/normalize.css'; // A modern alternative to CSS resets
import './styles/theme.scss'; // 主题令牌静态兜底值（JS 未执行时也不会闪蓝）
import Vue from 'vue';
import ElementUI from 'element-ui';
import App from './App.vue';
import router from './router';
import store from './store';
import i18n from './i18n';
import locale from 'element-ui/lib/locale'
import './styles/global.scss';
import { register as registerServiceWorker } from './registerServiceWorker';
import featureManager from './utils/featureManager';
import { initTheme, getActivePrimary } from './utils/theme';

// 换系统色只需改 public/theme.js（部署后为 dist/theme.js）里的 primary，不要改本文件
// 在任何组件渲染之前注入主题，避免首帧闪回默认蓝色
initTheme();

// 模板 / 脚本中需要「真实色值」时取 $themePrimary
// （el-switch 的 active-color、SVG fill、canvas 等场景不能直接用 CSS 变量）
Object.defineProperty(Vue.prototype, '$themePrimary', {
  get: () => getActivePrimary()
});

// 创建事件总线，用于组件间通信
Vue.prototype.$eventBus = new Vue();

Vue.use(ElementUI);
locale.i18n((key, value) => i18n.t(key, value))

Vue.config.productionTip = false

// 注册Service Worker
registerServiceWorker();

// 应用启动时把 localStorage 里的 token 同步进 store，避免守卫放行后首次请求时
// httpRequest 读到 state.token=null 而漏带 Authorization 头触发 401 跳登录
const cachedToken = localStorage.getItem('token')
if (cachedToken) {
  store.commit('setToken', cachedToken)
}

// 创建Vue实例
new Vue({
  router,
  store,
  i18n,
  render: function (h) { return h(App) }
}).$mount('#app')
