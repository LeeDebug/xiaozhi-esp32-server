import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  normalizeRegionName,
  isSameRegion,
  matchesSelection,
  hasSelection,
  countByLevel,
  countFor,
  countUnlocated,
  nextLevel,
} from './deviceRegion.mjs';

const GEO_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../public/geo');

test('normalizeRegionName 去掉行政后缀', () => {
  assert.equal(normalizeRegionName('广东省'), '广东');
  assert.equal(normalizeRegionName('深圳市'), '深圳');
  assert.equal(normalizeRegionName('南山区'), '南山');
  assert.equal(normalizeRegionName('广西壮族自治区'), '广西');
  assert.equal(normalizeRegionName('宁夏回族自治区'), '宁夏');
  assert.equal(normalizeRegionName('新疆维吾尔自治区'), '新疆');
  assert.equal(normalizeRegionName('香港特别行政区'), '香港');
  assert.equal(normalizeRegionName('内蒙古自治区'), '内蒙古');
});

test('normalizeRegionName 容错', () => {
  assert.equal(normalizeRegionName(null), '');
  assert.equal(normalizeRegionName(undefined), '');
  assert.equal(normalizeRegionName('  '), '');
  // 只剩后缀的名字不能被剥空
  assert.equal(normalizeRegionName('市'), '市');
  assert.equal(normalizeRegionName(' 广东省 '), '广东');
});

test('isSameRegion 忽略后缀写法差异，但空值永不相等', () => {
  assert.equal(isSameRegion('广东', '广东省'), true);
  assert.equal(isSameRegion('深圳市', '深圳'), true);
  assert.equal(isSameRegion('广东省', '湖南省'), false);
  assert.equal(isSameRegion('', ''), false);
  assert.equal(isSameRegion(null, undefined), false);
});

const devices = [
  { province: '广东省', city: '深圳市', district: '南山区' },
  { province: '广东省', city: '深圳市', district: '' },
  { province: '广东省', city: '广州市', district: '天河区' },
  { province: '湖南省', city: '长沙市', district: null },
  { province: '', city: '', district: '' },
];

test('matchesSelection 按已选层级逐级过滤', () => {
  assert.equal(matchesSelection(devices[0], {}), true);
  assert.equal(matchesSelection(devices[0], null), true);
  assert.equal(matchesSelection(devices[0], { province: '广东' }), true);
  assert.equal(matchesSelection(devices[0], { province: '广东省', city: '深圳市' }), true);
  assert.equal(matchesSelection(devices[0], { province: '广东省', city: '广州市' }), false);
  assert.equal(matchesSelection(devices[3], { province: '广东省' }), false);
  // 没有地域的设备在选中任何省后都不匹配
  assert.equal(matchesSelection(devices[4], { province: '广东省' }), false);
  assert.equal(
    matchesSelection(devices[0], { province: '广东省', city: '深圳市', district: '南山区' }),
    true,
  );
  assert.equal(
    matchesSelection(devices[1], { province: '广东省', city: '深圳市', district: '南山区' }),
    false,
  );
});

test('hasSelection / nextLevel', () => {
  assert.equal(hasSelection(null), false);
  assert.equal(hasSelection({}), false);
  assert.equal(hasSelection({ province: '广东省' }), true);
  assert.equal(nextLevel({}), 'province');
  assert.equal(nextLevel({ province: '广东省' }), 'city');
  assert.equal(nextLevel({ province: '广东省', city: '深圳市' }), 'district');
  assert.equal(nextLevel({ province: '广东省', city: '深圳市', district: '南山区' }), null);
});

test('countByLevel 统计各级设备数并受上级限定', () => {
  const provinces = countByLevel(devices, 'province');
  assert.equal(provinces.size, 2);
  assert.equal(countFor(provinces, '广东省'), 3);
  assert.equal(countFor(provinces, '湖南省'), 1);
  assert.equal(countFor(provinces, '四川省'), 0);

  const cities = countByLevel(devices, 'city', { province: '广东省' });
  assert.equal(countFor(cities, '深圳市'), 2);
  assert.equal(countFor(cities, '广州市'), 1);
  assert.equal(countFor(cities, '长沙市'), 0);

  const districts = countByLevel(devices, 'district', { province: '广东省', city: '深圳市' });
  assert.equal(countFor(districts, '南山区', 'district'), 1);
  assert.equal(districts.size, 1);
});

test('区县层级严格相等，临夏市与临夏县不会合并', () => {
  const rows = [
    { province: '甘肃省', city: '临夏回族自治州', district: '临夏市' },
    { province: '甘肃省', city: '临夏回族自治州', district: '临夏市' },
    { province: '甘肃省', city: '临夏回族自治州', district: '临夏县' },
  ];
  const counts = countByLevel(rows, 'district', { province: '甘肃省' });
  assert.equal(countFor(counts, '临夏市', 'district'), 2);
  assert.equal(countFor(counts, '临夏县', 'district'), 1);
  const city = { province: '甘肃省', city: '临夏回族自治州', district: '临夏县' };
  assert.equal(matchesSelection(rows[0], city), false);
  assert.equal(matchesSelection(rows[2], city), true);
});

test('countUnlocated 统计没有地域的设备', () => {
  assert.equal(countUnlocated(devices), 1);
  assert.equal(countUnlocated([]), 0);
  assert.equal(countUnlocated(undefined), 0);
});

function readGeo(file) {
  return JSON.parse(fs.readFileSync(path.join(GEO_DIR, file), 'utf-8'));
}

// 省、市层级使用去后缀的规范化名称匹配，必须确认真实数据里没有重名。
// 直辖市的省级文件里直接是区县（区县层级严格相等，不在此检查范围）。
test('真实 GeoJSON：省/市层级的兄弟地区规范化后不重名', () => {
  const country = readGeo('100000.json');
  const municipalities = new Set(['110000', '120000', '310000', '500000']);
  const collisions = [];
  const check = (label, features) => {
    const seen = new Map();
    for (const feature of features) {
      const name = feature.properties.name;
      if (!name) continue; // 九段线等无名要素
      const key = normalizeRegionName(name);
      if (seen.has(key)) collisions.push(`${label}: ${seen.get(key)} / ${name}`);
      else seen.set(key, name);
    }
  };
  check('100000', country.features);
  for (const feature of country.features) {
    const code = String(feature.properties.adcode || '');
    if (!code || municipalities.has(code) || !fs.existsSync(path.join(GEO_DIR, `${code}.json`))) continue;
    check(`${code}.json`, readGeo(`${code}.json`).features);
  }
  assert.deepEqual(collisions, []);
});

// 记录真实存在的区县层级重名，说明为什么区县必须严格相等。
test('真实 GeoJSON：确认区县层级确实存在去后缀重名（防止有人误改成不严格匹配）', () => {
  for (const [file, a, b] of [
    ['622900.json', '临夏市', '临夏县'],
    ['653200.json', '和田市', '和田县'],
    ['654000.json', '伊宁市', '伊宁县'],
  ]) {
    const names = readGeo(file).features.map((f) => f.properties.name);
    assert.ok(names.includes(a) && names.includes(b), `${file} 应同时包含 ${a} 和 ${b}`);
    assert.equal(normalizeRegionName(a), normalizeRegionName(b));
  }
});

test('真实 GeoJSON：全部要素都有 adcode 和几何，省级文件能找到全部省', () => {
  const country = JSON.parse(fs.readFileSync(path.join(GEO_DIR, '100000.json'), 'utf-8'));
  const named = country.features.filter((f) => f.properties.name);
  assert.equal(named.length, 34);
  for (const feature of named) {
    assert.ok(feature.properties.adcode, `${feature.properties.name} 缺少 adcode`);
    assert.ok(feature.geometry && feature.geometry.coordinates.length > 0, `${feature.properties.name} 缺少几何`);
  }
});
