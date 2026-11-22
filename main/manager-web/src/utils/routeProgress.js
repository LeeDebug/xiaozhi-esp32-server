/**
 * 顶部全局进度条（无第三方依赖，类似 NProgress）
 *
 * 两个独立来源，任一活跃即显示：
 * - 路由切换：router.beforeEach 调 start()，afterEach / onError 调 done()
 *   （路由守卫可能重定向导致多次 beforeEach，所以路由侧是布尔开关而非计数）
 * - 接口请求：httpRequest 每发出一个请求 requestStart()，结束时 requestDone()（计数）
 */

const BAR_ID = 'xz-route-progress';
let routeActive = false;
let requestCount = 0;
let percent = 0;
let visible = false;
let trickleTimer = null;
let hideTimer = null;

function getBar() {
  let bar = document.getElementById(BAR_ID);
  if (!bar) {
    bar = document.createElement('div');
    bar.id = BAR_ID;
    bar.className = 'xz-route-progress';
    bar.innerHTML = '<div class="xz-route-progress__bar"><div class="xz-route-progress__peg"></div></div>';
    document.body.appendChild(bar);
  }
  return bar;
}

function render() {
  getBar().firstChild.style.transform = `translate3d(${percent - 100}%, 0, 0)`;
}

function trickle() {
  // 越接近 100 增长越慢，停在 95% 之前，直到全部结束
  if (percent < 95) {
    const step = percent < 30 ? 8 : percent < 60 ? 4 : percent < 80 ? 2 : 0.5;
    percent = Math.min(95, percent + step);
    render();
  }
}

function show() {
  if (typeof document === 'undefined') return;
  clearTimeout(hideTimer);
  if (visible) return;
  visible = true;
  const bar = getBar();
  bar.classList.remove('is-done');
  bar.classList.add('is-active');
  percent = 8;
  render();
  clearInterval(trickleTimer);
  trickleTimer = setInterval(trickle, 200);
}

function tryHide() {
  if (typeof document === 'undefined') return;
  if (routeActive || requestCount > 0 || !visible) return;
  visible = false;
  clearInterval(trickleTimer);
  trickleTimer = null;
  percent = 100;
  render();
  const bar = getBar();
  bar.classList.add('is-done');
  clearTimeout(hideTimer);
  hideTimer = setTimeout(() => {
    bar.classList.remove('is-active', 'is-done');
    percent = 0;
    render();
  }, 350);
}

/** 路由开始切换 */
export function start() {
  routeActive = true;
  show();
}

/** 路由切换结束（成功或失败） */
export function done() {
  routeActive = false;
  tryHide();
}

/** 接口请求开始 */
export function requestStart() {
  requestCount += 1;
  show();
}

/** 接口请求结束（成功、业务失败、网络失败都要调用一次） */
export function requestDone() {
  requestCount = Math.max(0, requestCount - 1);
  tryHide();
}

export default { start, done, requestStart, requestDone };
