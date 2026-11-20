// 设备地域相关的纯函数（不依赖 Vue/echarts，便于单元测试）。
// 地域层级：province(省) -> city(市) -> district(区/县)。

export const LEVELS = ['province', 'city', 'district'];

// 行政后缀，比较时去掉，避免“广东”与“广东省”、“广西”与“广西壮族自治区”匹配不上。
// 按从长到短排列，保证“特别行政区”先于“区”被匹配。
const SUFFIXES = [
  '特别行政区',
  '壮族自治区',
  '回族自治区',
  '维吾尔自治区',
  '自治区',
  '自治州',
  '自治县',
  '自治旗',
  '地区',
  '省',
  '市',
  '区',
  '县',
  '盟',
  '旗',
];

/**
 * 去掉空白与行政后缀。名字只剩后缀时（如“市”）原样返回，防止把整个名字剥空。
 */
export function normalizeRegionName(name) {
  if (name === null || name === undefined) return '';
  const text = String(name).trim();
  if (!text) return '';
  for (const suffix of SUFFIXES) {
    if (text.length > suffix.length && text.endsWith(suffix)) {
      return text.slice(0, -suffix.length);
    }
  }
  return text;
}

/**
 * 区县层级必须严格相等：同一地级市下存在“临夏市/临夏县”“和田市/和田县”“伊宁市/伊宁县”这类
 * 去后缀后重名的情况（已用真实 GeoJSON 扫描确认），忽略后缀会把两个区域的设备数合并。
 * 省/市层级经扫描无重名，可以容忍“广东”与“广东省”这样的写法差异。
 */
export function isSameName(level, a, b) {
  if (level === 'district') {
    const left = a === null || a === undefined ? '' : String(a).trim();
    const right = b === null || b === undefined ? '' : String(b).trim();
    return left !== '' && left === right;
  }
  const left = normalizeRegionName(a);
  const right = normalizeRegionName(b);
  return left !== '' && left === right;
}

export function isSameRegion(a, b) {
  return isSameName('province', a, b);
}

/**
 * 设备是否属于选中区域。selection 里未设置的层级不参与判断；全部未设置时不过滤。
 */
export function matchesSelection(device, selection) {
  if (!selection) return true;
  for (const level of LEVELS) {
    if (selection[level] && !isSameName(level, device[level], selection[level])) {
      return false;
    }
  }
  return true;
}

/**
 * 统计键：区县用原始名称，省/市用去后缀的规范化名称。
 */
function regionKey(level, name) {
  if (name === null || name === undefined) return '';
  return level === 'district' ? String(name).trim() : normalizeRegionName(name);
}

export function hasSelection(selection) {
  return !!selection && LEVELS.some((level) => !!selection[level]);
}

/**
 * 统计当前层级下各地区的设备数。
 * @param devices   全部设备（不应是被搜索过滤后的列表，否则地图会随搜索跳动）
 * @param level     要统计的层级 province/city/district
 * @param parent    上级限定，例如统计某省下的市时传 { province: '广东省' }
 * @returns Map<规范化名称, { name, count }>，name 保留设备上最先出现的原始写法
 */
export function countByLevel(devices, level, parent) {
  const result = new Map();
  for (const device of devices || []) {
    if (!matchesSelection(device, parent)) continue;
    const raw = device[level];
    const key = regionKey(level, raw);
    if (!key) continue;
    const entry = result.get(key);
    if (entry) {
      entry.count += 1;
    } else {
      result.set(key, { name: String(raw).trim(), count: 1 });
    }
  }
  return result;
}

/**
 * 给定某层级的 GeoJSON feature 名称，取出对应设备数（没有则 0）。
 */
export function countFor(counts, featureName, level = 'province') {
  const entry = counts.get(regionKey(level, featureName));
  return entry ? entry.count : 0;
}

/**
 * 没有任何地域信息的设备数（IP 为内网/境外或尚未上报时会出现）。
 */
export function countUnlocated(devices) {
  let n = 0;
  for (const device of devices || []) {
    if (!normalizeRegionName(device.province)) n += 1;
  }
  return n;
}

/**
 * 给定当前选中层级，返回下一层级名；已经是区县则返回 null。
 */
export function nextLevel(selection) {
  if (!selection || !selection.province) return 'province';
  if (!selection.city) return 'city';
  if (!selection.district) return 'district';
  return null;
}
