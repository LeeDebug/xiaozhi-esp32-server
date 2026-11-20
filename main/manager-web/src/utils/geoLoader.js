// 行政区划 GeoJSON 的加载与缓存，供设备地图和地域编辑对话框共用。
// 文件位于 public/geo/{adcode}.json：100000=全国(各省)，省码=该省的市(直辖市则是区县)，市码=该市的区县。

export const COUNTRY_CODE = '100000';

// 直辖市的省级文件里直接是区县，没有“市”这一层。
export const MUNICIPALITY_CODES = new Set(['110000', '120000', '310000', '500000']);

const geoCache = new Map(); // adcode -> Promise<geojson>

export function geoUrl(code) {
  const base = process.env.BASE_URL || '/';
  return `${base}geo/${code}.json`;
}

export function loadGeo(code) {
  if (!geoCache.has(code)) {
    const task = fetch(geoUrl(code)).then((res) => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return res.json();
    });
    // 失败时不缓存，允许重试
    task.catch(() => geoCache.delete(code));
    geoCache.set(code, task);
  }
  return geoCache.get(code);
}

/**
 * 取某个 GeoJSON 文件里的全部有名字的要素（九段线等无名要素会被过滤）。
 * @returns [{ name, adcode }]
 */
export async function loadRegionOptions(code) {
  const geo = await loadGeo(code);
  return geo.features
    .filter((f) => f.properties && f.properties.name)
    .map((f) => ({ name: f.properties.name, adcode: String(f.properties.adcode || '') }));
}
