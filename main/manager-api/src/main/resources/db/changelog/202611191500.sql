-- 设备地域分布：为设备管理页的地图汇总、省市区筛选提供数据。
-- region_source：ip=由公网IP自动解析（仅精确到市，可被后续解析更新）；manual=用户手动填写（不再被 IP 解析覆盖）。
-- 省/市/区存完整名称（如「广东省」「深圳市」「南山区」），与前端 GeoJSON 的 name 直接对应。

ALTER TABLE ai_device
    ADD COLUMN province VARCHAR(32) DEFAULT NULL COMMENT '所在省份(完整名称)' AFTER app_version,
    ADD COLUMN city VARCHAR(32) DEFAULT NULL COMMENT '所在城市(完整名称)' AFTER province,
    ADD COLUMN district VARCHAR(32) DEFAULT NULL COMMENT '所在区县(完整名称，仅手动填写)' AFTER city,
    ADD COLUMN region_source VARCHAR(16) DEFAULT NULL COMMENT '地域来源: ip/manual' AFTER district,
    ADD COLUMN last_ip VARCHAR(64) DEFAULT NULL COMMENT '最近一次 OTA 请求的公网IP' AFTER region_source;

ALTER TABLE ai_device
    ADD INDEX idx_ai_device_region (province, city, district);
