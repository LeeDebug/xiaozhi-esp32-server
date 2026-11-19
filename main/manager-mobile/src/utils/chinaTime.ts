/**
 * 中国时间（UTC+8）工具，与 manager-web 的会话记录页保持同一口径：
 * 前后端统一用 epoch 毫秒作为绝对时刻，展示一律按中国时间，不依赖手机本地时区。
 */
const CN_OFFSET_MS = 8 * 60 * 60 * 1000
const DAY_MS = 24 * 60 * 60 * 1000

/** 中国时间当天 00:00:00 对应的绝对毫秒 */
export function chinaStartOfToday(now: number = Date.now()): number {
  const shifted = new Date(now + CN_OFFSET_MS)
  return Date.UTC(shifted.getUTCFullYear(), shifted.getUTCMonth(), shifted.getUTCDate()) - CN_OFFSET_MS
}

/** 中国时间当天 23:59:59.999 对应的绝对毫秒 */
export function chinaEndOfToday(now: number = Date.now()): number {
  return chinaStartOfToday(now) + DAY_MS - 1
}

/**
 * 快捷时间范围（含今天）：
 * - recentDays=1 表示今天
 * - recentDays=7 表示近 7 天
 * - recentDays=30 表示近 30 天
 * 传入 null 表示全部时间，返回 null（调用方不带时间条件）。
 */
export function chinaRecentRange(recentDays: number | null, now: number = Date.now()): { startTime: number, endTime: number } | null {
  if (recentDays == null || recentDays <= 0)
    return null
  return {
    startTime: chinaStartOfToday(now) - (recentDays - 1) * DAY_MS,
    endTime: chinaEndOfToday(now),
  }
}
